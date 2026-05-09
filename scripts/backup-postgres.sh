#!/usr/bin/env bash
#
# Backup automático do Postgres do Nonnas Stock.
#
# Uso (cron):
#   0 3 * * * /opt/nonnas-stock/scripts/backup-postgres.sh > /var/log/nonnas-backup.log 2>&1
#
# Variáveis lidas do ambiente (carregue de /opt/nonnas-stock/.env via 'set -a; source ...'):
#   PG_HOST, PG_PORT, PG_USER, PG_DB, PG_PASSWORD
#   BACKUP_DIR              — onde salvar (default /var/backups/nonnas)
#   BACKUP_RETENTION_DAYS   — manter N dias (default 30)
#   BACKUP_S3_BUCKET        — opcional: enviar para s3://<bucket>/<prefix>
#
# Restore: ver docs/operations-runbook.md "Backup" seção 8.

set -euo pipefail

PG_HOST="${PG_HOST:-localhost}"
PG_PORT="${PG_PORT:-5432}"
PG_USER="${PG_USER:?PG_USER é obrigatório}"
PG_DB="${PG_DB:?PG_DB é obrigatório}"
BACKUP_DIR="${BACKUP_DIR:-/var/backups/nonnas}"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-30}"

mkdir -p "$BACKUP_DIR"

TIMESTAMP=$(date -u +"%Y-%m-%d_%H%M%S")
DUMP_FILE="$BACKUP_DIR/nonnas-${TIMESTAMP}.sql.gz"

echo "[$(date -Iseconds)] Iniciando dump de $PG_DB para $DUMP_FILE"

# pg_dump --format=plain (compatível com psql restore) + gzip.
# Use --format=custom (-Fc) se preferir restore por pg_restore com filtros.
PGPASSWORD="${PG_PASSWORD:-}" pg_dump \
    --host="$PG_HOST" \
    --port="$PG_PORT" \
    --username="$PG_USER" \
    --dbname="$PG_DB" \
    --no-owner \
    --no-privileges \
    --clean \
    --if-exists \
    | gzip -9 > "$DUMP_FILE"

SIZE=$(du -h "$DUMP_FILE" | cut -f1)
echo "[$(date -Iseconds)] Dump concluído ($SIZE)"

# Off-site opcional via AWS CLI.
if [[ -n "${BACKUP_S3_BUCKET:-}" ]]; then
    echo "[$(date -Iseconds)] Enviando para s3://${BACKUP_S3_BUCKET}/"
    aws s3 cp "$DUMP_FILE" "s3://${BACKUP_S3_BUCKET}/$(basename "$DUMP_FILE")" \
        --storage-class STANDARD_IA
fi

# Retenção: apaga dumps mais antigos que N dias.
echo "[$(date -Iseconds)] Limpando backups com mais de $RETENTION_DAYS dias"
find "$BACKUP_DIR" -name 'nonnas-*.sql.gz' -mtime "+$RETENTION_DAYS" -delete

echo "[$(date -Iseconds)] Backup OK"
