#!/usr/bin/env bash
#
# Validação mensal de restore — restaura o backup mais recente em um
# database de testes e roda smoke checks para garantir que o dump é
# realmente recuperável (master doc T15 "restore validado").
#
# Uso:
#   ./restore-validate.sh /var/backups/nonnas
#
# Pré-requisito: usuário tem permissão de CREATE DATABASE no Postgres.

set -euo pipefail

BACKUP_DIR="${1:-/var/backups/nonnas}"
TEST_DB="${TEST_DB:-nonnas_restore_test}"
PG_HOST="${PG_HOST:-localhost}"
PG_PORT="${PG_PORT:-5432}"
PG_USER="${PG_USER:?PG_USER é obrigatório}"

LATEST=$(ls -t "$BACKUP_DIR"/nonnas-*.sql.gz 2>/dev/null | head -1)
if [[ -z "$LATEST" ]]; then
    echo "Nenhum backup encontrado em $BACKUP_DIR" >&2
    exit 1
fi

echo "Validando backup: $LATEST"

PGPASSWORD="${PG_PASSWORD:-}" psql -h "$PG_HOST" -p "$PG_PORT" -U "$PG_USER" -d postgres \
    -c "DROP DATABASE IF EXISTS $TEST_DB;" \
    -c "CREATE DATABASE $TEST_DB;"

gunzip -c "$LATEST" | PGPASSWORD="${PG_PASSWORD:-}" psql -h "$PG_HOST" -p "$PG_PORT" \
    -U "$PG_USER" -d "$TEST_DB" -v ON_ERROR_STOP=1

# Smoke: deve haver pelo menos o admin bootstrap criado.
COUNT=$(PGPASSWORD="${PG_PASSWORD:-}" psql -h "$PG_HOST" -p "$PG_PORT" -U "$PG_USER" \
    -d "$TEST_DB" -tAc "SELECT count(*) FROM identity.usuarios;")

if [[ "$COUNT" -lt 1 ]]; then
    echo "FALHA: nenhum usuário no restore (esperava ≥ 1)" >&2
    exit 1
fi

echo "Restore validado: $COUNT usuário(s) recuperado(s)."

# Cleanup: derruba o DB de teste.
PGPASSWORD="${PG_PASSWORD:-}" psql -h "$PG_HOST" -p "$PG_PORT" -U "$PG_USER" -d postgres \
    -c "DROP DATABASE $TEST_DB;"
