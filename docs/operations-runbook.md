# Runbook operacional — Nonnas Stock

Procedimentos para incidentes recorrentes em produção. Atualize este documento sempre que um novo modo de falha for descoberto e o pessoal de plantão tiver que improvisar.

## 1. Health checks

| Endpoint                                | O que valida                                  |
|-----------------------------------------|-----------------------------------------------|
| `GET /actuator/health/liveness`         | App responde                                  |
| `GET /actuator/health/readiness`        | App + dependências (banco, Flyway)            |
| `GET /actuator/health/db`               | Latência da query `SELECT 1` < 200ms          |
| `GET /actuator/health/migrations`       | Flyway sem migrações pendentes                |

Probe sugerido para load balancer: `liveness` para keep-alive, `readiness` para roteamento.

## 2. Banco lento (queries acima de 200ms)

**Sintomas:**
- `db` health indicator vira `DOWN` ou alerta de latência.
- Frontend acusa 504/timeout em /relatorios/* ou /estoque.
- `pg_stat_statements` mostra queries da `mv_curva_abc` ou `mv_ruptura_iminente` em destaque.

**Procedimento:**
1. Verificar que o `RefreshViewsScheduledJob` rodou nas últimas 30min:
   ```sql
   SELECT * FROM pg_matviews WHERE schemaname = 'reporting';
   ```
2. Se materialized view está stale, forçar refresh:
   ```bash
   curl -X POST -H "Authorization: Bearer $TOKEN" \
     https://app.nonnas.com.br/api/v1/relatorios/refresh
   ```
3. Investigar plan:
   ```sql
   EXPLAIN (ANALYZE, BUFFERS) SELECT ... ;
   ```
4. Se índice faltando: criar migration `V0XX__index_<descrição>.sql` e fazer rolling restart.
5. Se carga sustentada: escalar Postgres (vertical) ou habilitar read replica para reporting (T17).

## 3. Alerta falso positivo (disparou sem motivo)

**Sintomas:**
- Operadora reporta alerta de RUPTURA em insumo com saldo cheio.
- Estoque mostra saldo > estoque mínimo, mas alerta veio.

**Procedimento:**
1. Buscar a config do alerta:
   ```sql
   SELECT * FROM alerts.alerta_disparado WHERE id = '<id>';
   SELECT * FROM alerts.alerta_configuracao WHERE id = '<config_id>';
   ```
2. Conferir o saldo da `mv_curva_abc` no momento do disparo (`saldo_no_disparo` do disparo).
3. Se saldo no disparo era de fato baixo (e o saldo subiu depois com uma entrada), o alerta era válido na hora — não é falso positivo. Marque como "RESOLVIDO_MANUAL" pela UI.
4. Se saldo no disparo está inconsistente com `posicao_estoque_filial`, há bug. Capture stack do scheduler de alertas e abra issue.

## 4. Rollback de migração Flyway

**Cenário típico:** migration nova quebrou produção (ex.: índice unique violado).

**Procedimento de emergência:**
1. **Pare deploys novos** — feature freeze imediato.
2. Se a migration quebrou no startup (app não sobe), o estado é misto: algumas DDLs aplicaram, registro em `flyway_schema_history` ficou com `success = false`.
3. Rollback DDL manual:
   ```sql
   BEGIN;
   -- desfazer o que a migration fez (DROP INDEX/TABLE/etc)
   DELETE FROM flyway_schema_history WHERE version = '0XX' AND success = false;
   COMMIT;
   ```
4. Reverter a imagem para a versão anterior:
   ```bash
   docker pull ghcr.io/jeffersonago007/nonnas-stock:sha-<hash-anterior>
   docker compose up -d app
   ```
5. **Não** edite migrations já aplicadas em produção. Crie uma nova migration que conserta o efeito.

Flyway out-of-order é proibido (`spring.flyway.out-of-order=false`); migration rejected → corrigir nome ou versão.

## 5. App não sobe (crash loop)

**Sintomas:**
- Container reinicia em loop.
- `docker logs app` mostra exception no Spring boot startup.

**Procedimento:**
1. Verificar variáveis de ambiente:
   ```bash
   docker compose exec app env | grep -E '(SPRING|NONNAS)'
   ```
2. Se mensagem de Flyway: ver seção Rollback de migração.
3. Se mensagem de JWT: `NONNAS_JWT_SECRET` precisa ter ≥ 32 bytes.
4. Se `Connection refused` em postgres: verificar `docker compose ps` e logs do Postgres.
5. Se OOM: subir heap via `JAVA_OPTS=-Xmx768m` no `.env`.

## 6. Carga inicial inconsistente

**Sintomas:** operadora reporta que carga inicial deu certo (preview OK + confirmar OK) mas saldo no estoque está zerado ou diferente.

**Procedimento:**
1. Buscar o `CargaInicial` no banco:
   ```sql
   SELECT * FROM operations.carga_inicial WHERE filial_id = '<filial>'
     ORDER BY created_at DESC LIMIT 5;
   ```
2. Conferir `registros_processados` vs `registros_falhos`. Se há falhas, a planilha tinha linhas com insumo_id/unidade_id inválidos — aceitas linhas válidas, rejeitadas inválidas.
3. Validar que o saldo materializado refletiu — abrir /estoque na filial.
4. Se saldo está zerado mesmo com `registros_processados > 0`: bug no scheduler que materializa saldos. Abrir issue + restaurar de backup.

## 7. Rate limit fechando demais

**Sintomas:** usuários reportam 429 em horário de pico.

**Procedimento:**
1. Verificar log do `RateLimitFilter` — IP que está consumindo.
2. Se IP é de proxy interno: configurar `proxy_set_header X-Forwarded-For` e ajustar filter pra usar o header.
3. Se carga sustentada legítima: subir `app/src/main/resources/application-prod.yml` rate limit (ex.: 100→200 req/min) e fazer rolling restart.

## 8. Backup

Script: `scripts/backup-postgres.sh` (versionado no repo).

Cron sugerido (no servidor de banco):
```cron
0 3 * * * /opt/nonnas-stock/scripts/backup-postgres.sh > /var/log/nonnas-backup.log 2>&1
```

**Restore exercise (faça mensalmente):**
```bash
# Em ambiente de staging/teste, NUNCA em prod
psql -U postgres -c "DROP DATABASE IF EXISTS nonnas_restore;"
psql -U postgres -c "CREATE DATABASE nonnas_restore;"
gunzip -c /var/backups/nonnas/nonnas-2026-05-09.sql.gz | psql -U postgres -d nonnas_restore
psql -U postgres -d nonnas_restore -c "SELECT count(*) FROM identity.usuarios;"
```

Se contagem bate com prod → backup é restaurável. Documente data + tamanho do backup.
