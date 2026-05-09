# Runbook — PostgreSQL lento

**Sintoma**: queries demorando mais que o usual; usuários reclamam de lentidão; alerta `db` health indicator vira `DOWN`; latência p95 da API sobe.

**Detecção**:
- Grafana dashboard `Banco`: painel "Tempo de query JDBC p95" acima de 200ms.
- `/actuator/health/db` retornando `DOWN` (latência da query `SELECT 1` > threshold).
- Operadora reportando "tela do estoque congelada".

## Diagnóstico — 5 minutos

### 1. Confirmar que o problema é mesmo no banco

```bash
# Latência da query trivial — se demorar, é banco mesmo.
time PGPASSWORD=$PG_PASSWORD psql -h $PG_HOST -U $PG_USER -d nonnas -c "SELECT 1;"
# Esperado: < 50ms. Se > 1s, o gargalo é Postgres.
```

### 2. Olhar conexões ativas

```sql
SELECT state, count(*)
  FROM pg_stat_activity
 WHERE datname = 'nonnas'
 GROUP BY state;
```

- Muitas em `idle in transaction` → app está deixando transação aberta sem commit. Reiniciar app pode ajudar como medida temporária.
- Muitas em `active` com query antiga → query lenta provavelmente.

### 3. Identificar query culpada

```sql
SELECT pid,
       now() - query_start AS duracao,
       state,
       substring(query, 1, 200) AS query_curta
  FROM pg_stat_activity
 WHERE datname = 'nonnas'
   AND state = 'active'
   AND query NOT ILIKE '%pg_stat_activity%'
 ORDER BY duracao DESC
 LIMIT 10;
```

Top 1 query por duração geralmente é o problema.

### 4. Plano de execução

Pegando a query da etapa anterior:

```sql
EXPLAIN (ANALYZE, BUFFERS) <a query suspeita>;
```

Sinais de alerta:
- `Seq Scan` em tabela grande → falta índice.
- `Rows Removed by Filter` >> `Rows`: filtro pós-leitura, índice não ajudou.
- `Buffers: hit=N read=M` com `read` >> `hit` → cache miss alto.

## Mitigação — 30 minutos

### Cenário A — query específica está lenta (faltando índice)

1. Rascunhar o índice em ambiente de staging:
   ```sql
   CREATE INDEX CONCURRENTLY idx_<descricao> ON <tabela> (<colunas>);
   ```
2. Validar que o `EXPLAIN` mudou de `Seq Scan` para `Index Scan`.
3. Criar **migration nova** `V0XX__index_<descricao>.sql` (não editar migration existente).
4. Rolling restart da app pega a nova migration.
5. Em prod, considerar criar com `CONCURRENTLY` manualmente antes do deploy se a tabela for grande (evita lock).

### Cenário B — materialized view stale

Reporting tem `mv_curva_abc` e `mv_ruptura_iminente`. Se queries em `/relatorios/*` estão lentas:

```sql
SELECT * FROM pg_matviews WHERE schemaname = 'reporting';
```

Se `last_refresh` é antigo:

```bash
curl -X POST -H "Authorization: Bearer $ADMIN_TOKEN" \
    https://app.nonnas.com.br/api/v1/relatorios/refresh
```

Ver também: scheduler `RefreshViewsScheduledJob` deveria rodar a cada 30min — confira se está habilitado no profile prod.

### Cenário C — connection pool exausto

Se `pg_stat_activity` mostra dezenas de conexões em `idle` mas a app reclama de "connection pool exhausted":

1. Verificar `hikaricp_connections_active` no Grafana — se está perto do `max`, app não está liberando.
2. Aumentar pool temporariamente em `application-prod.yml`:
   ```yaml
   spring:
     datasource:
       hikari:
         maximum-pool-size: 30   # default era 10
   ```
3. Investigar leak no código — provavelmente algum `@Transactional` mal-formado.

### Cenário D — locks bloqueando tudo

```sql
SELECT blocked.pid AS blocked_pid,
       blocked.query AS blocked_query,
       blocking.pid AS blocking_pid,
       blocking.query AS blocking_query
  FROM pg_stat_activity blocked
  JOIN pg_stat_activity blocking ON blocking.pid = ANY(pg_blocking_pids(blocked.pid));
```

Se há cadeia de bloqueios, `SELECT pg_terminate_backend(<blocking_pid>)` libera. Não é solução de longo prazo — investigar transação que segurou o lock.

## Escalação

Se nada acima resolveu em 30 minutos:

1. Backup imediato (caso precise restaurar):
   ```bash
   /opt/nonnas-stock/scripts/backup.sh
   ```
2. Avisar Ewerton (sênior) — banco ou dependência externa precisa de olho experiente.
3. Considerar restart do Postgres como **última opção**:
   ```bash
   sudo systemctl restart postgresql
   ```
   Causa downtime de ~30s e mata todas as queries em andamento. Útil só se locks travados não saem com `pg_terminate_backend`.

## Pós-incidente

- Postmortem em `docs/post-mortems/<data>-postgres-lento.md`.
- Se foi falta de índice, criar item no backlog para revisar todas as queries de `/relatorios/*` que rodam frequentemente.
- Se foi conexão leak, abrir bug com stack trace + thread dump.
