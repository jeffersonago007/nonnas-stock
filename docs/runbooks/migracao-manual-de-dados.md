# Runbook — Migração manual de dados

**Quando usar**: precisamos fazer mudança em massa que não cabe numa Flyway migration normal — ex.: corrigir dados após bug em produção, importar dados externos, recategorizar insumos em massa.

**Pré-requisitos**:
- Backup recente (idealmente menos de 1h atrás).
- Janela de manutenção comunicada se a operação afeta tabelas quentes.
- Aprovação por escrito (Slack thread, e-mail) de Jeff ou Ewerton.

## Princípio: nada manual sem rastro

Toda alteração precisa:
1. **Backup** antes (assume-se feito automaticamente, mas confirmar).
2. **Auditoria explícita** — registro em `audit_log` com `event_type=MIGRACAO_MANUAL` e metadata explicando o porquê.
3. **Plano de rollback** documentado antes de começar.

## Procedimento padrão

### 1. Documentar o plano

Cria um arquivo `docs/post-mortems/migracao-<data>-<descricao>.md` com:
- O que vai mudar.
- Por que (link pra issue/Slack).
- Tabelas afetadas + estimativa de linhas.
- Rollback plan (SQL ou restore).
- Quem aprovou.

### 2. Fazer o backup ad-hoc

```bash
sudo -u postgres bash -c "set -a; source /opt/nonnas-stock/.env; /opt/nonnas-stock/scripts/backup.sh"
LATEST=$(ls -t /var/backups/nonnas/*.dump.gz.gpg | head -1)
echo "Backup pré-migração: $LATEST"
```

Anotar essa filename — é seu salva-vidas.

### 3. Conexão com transação

**Sempre** dentro de transação. **Sempre** com count antes/depois.

```sql
BEGIN;

-- Antes
SELECT count(*) AS antes FROM <tabela> WHERE <condicao>;

-- A migração
UPDATE <tabela> SET <campo> = <valor> WHERE <condicao>;

-- Depois
SELECT count(*) AS depois_atualizadas FROM <tabela> WHERE <campo> = <valor>;

-- Se bater com o esperado:
COMMIT;
-- Se NÃO bater:
ROLLBACK;
```

**Nunca** rodar UPDATE/DELETE sem transação aberta. **Nunca** rodar UPDATE/DELETE sem WHERE explícito (mesmo que seja `WHERE 1=1` pra deixar visível).

### 4. Auditoria

```sql
INSERT INTO identity.audit_log
    (event_type, actor_id, actor_email, target_kind, metadata)
VALUES
    ('MIGRACAO_MANUAL',
     '<seu-uuid>',
     '<seu-email>',
     '<tabela_afetada>',
     '{"linhas":<numero>,"motivo":"<descricao>","backup":"<filename>"}');
```

Sem essa linha, ninguém em 6 meses vai saber por que aquela tabela tem dados diferentes.

### 5. Verificar saldos materializados (se aplica)

Se mudou movimentação/lote/saldo, refresh nas MVs:

```bash
curl -X POST -H "Authorization: Bearer $ADMIN_TOKEN" \
    https://app.nonnas.com.br/api/v1/relatorios/refresh
```

### 6. Smoke pós-migração

- Login admin.
- Abrir uma tela que usa os dados mudados.
- Conferir visualmente que aparece como esperado.

### 7. Atualizar o post-mortem

Voltar no arquivo de docs/post-mortems/ e preencher:
- Tempo total decorrido.
- Linhas afetadas (real, não estimado).
- Anomalias observadas.
- Decisão de rollback ou commit.

## Padrões para tipos comuns

### Recategorizar insumos em massa

```sql
BEGIN;

-- Quantos vão mudar
SELECT count(*) FROM catalog.insumo WHERE codigo LIKE 'BEB-%';

-- A mudança
UPDATE catalog.insumo
   SET categoria_id = '<nova-categoria-uuid>',
       updated_at = NOW()
 WHERE codigo LIKE 'BEB-%';

-- Confere
SELECT count(*) FROM catalog.insumo
 WHERE categoria_id = '<nova-categoria-uuid>'
   AND codigo LIKE 'BEB-%';

-- Audita
INSERT INTO identity.audit_log (event_type, actor_id, target_kind, metadata)
VALUES ('MIGRACAO_MANUAL', '<id>', 'INSUMO',
        '{"acao":"recategorizar","filtro":"codigo LIKE BEB-%","de":"X","para":"Y"}');

COMMIT;
```

### Apagar dados antigos (purga manual)

```sql
BEGIN;

-- Quantos vão apagar
SELECT count(*) FROM identity.audit_log
 WHERE occurred_at < NOW() - INTERVAL '18 months';

-- O delete
DELETE FROM identity.audit_log
 WHERE occurred_at < NOW() - INTERVAL '18 months';

-- Audita (paradoxo — está deletando audit, mas o registro do delete fica)
INSERT INTO identity.audit_log (event_type, actor_id, target_kind, metadata)
VALUES ('MIGRACAO_MANUAL', '<id>', 'AUDIT_LOG',
        '{"acao":"purga","retencao":"18 meses","linhas_apagadas":<n>}');

COMMIT;
```

### Importar dados externos

Para importações grandes (>10k linhas), usar `\COPY` em vez de INSERT loops:

```bash
psql -h prod-db -U nonnas_app -d nonnas <<'EOF'
BEGIN;
\COPY catalog.insumo (codigo, nome, categoria_id, unidade_base_id) FROM '/tmp/import.csv' CSV HEADER;
SELECT count(*) FROM catalog.insumo WHERE created_at > NOW() - INTERVAL '5 minutes';
COMMIT;
EOF
```

## Quando NÃO fazer manual

- Mudança que vai precisar ser reaplicada em outros ambientes (staging, devs locais) → vira **migration Flyway** normal.
- Alteração de schema (CREATE/ALTER/DROP TABLE) → **migration Flyway**, mesmo que urgente. Manual aqui vira inconsistência entre ambientes.
- "Vou só corrigir uma coisinha" → pare. Se vale a pena, vale com transação + auditoria.

## Pós-incidente

Postmortem em `docs/post-mortems/`. Aprendizados que mudam o runbook ficam aqui.
