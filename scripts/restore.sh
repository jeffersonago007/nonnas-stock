#!/usr/bin/env bash
#
# Restore de um backup criado por backup.sh (master doc 14.1 / T18).
#
# Uso:
#   restore.sh <BACKUP_FILE_GPG> <TARGET_DB>
#
# Pipeline:
#   download (se s3:// ou b2://)  →  gpg --decrypt  →  gunzip  →  pg_restore
#
# Após restaurar, executa smoke queries — confere que tabelas críticas têm
# pelo menos uma linha (admin bootstrap, pelo menos uma migration aplicada).
#
# Variáveis de ambiente (idem backup.sh): PG_HOST, PG_PORT, PG_USER,
# PG_PASSWORD, GPG_RECIPIENT (precisa da chave PRIVADA importada
# correspondente).
#
# RTO alvo: 4h (master doc 14.1). Esse script é a parte automatizada;
# resto do tempo é DNS, certificado, smoke manual.

set -euo pipefail

if [[ $# -lt 2 ]]; then
    echo "Uso: $0 <BACKUP_FILE_OR_S3_URL> <TARGET_DB>" >&2
    echo "  Ex.: $0 /var/backups/nonnas/nonnas-2026-05-09_030000.dump.gz.gpg nonnas_restore" >&2
    echo "  Ex.: $0 s3://nonnas-backups/nonnas-2026-05-09_030000.dump.gz.gpg nonnas_staging" >&2
    exit 2
fi

BACKUP_REF="$1"
TARGET_DB="$2"
PG_HOST="${PG_HOST:-localhost}"
PG_PORT="${PG_PORT:-5432}"
PG_USER="${PG_USER:?PG_USER e obrigatorio}"

WORK_DIR=$(mktemp -d)
trap 'rm -rf "$WORK_DIR"' EXIT

# Localiza/baixa o arquivo cifrado
case "$BACKUP_REF" in
    s3://*)
        echo "[$(date -Iseconds)] Baixando de S3: $BACKUP_REF"
        LOCAL_GPG="$WORK_DIR/$(basename "$BACKUP_REF")"
        aws s3 cp "$BACKUP_REF" "$LOCAL_GPG"
        ;;
    b2://*)
        BUCKET_NAME=$(echo "$BACKUP_REF" | sed 's|^b2://||;s|/.*||')
        REMOTE_PATH=$(echo "$BACKUP_REF" | sed -E 's|^b2://[^/]+/||')
        LOCAL_GPG="$WORK_DIR/$(basename "$REMOTE_PATH")"
        echo "[$(date -Iseconds)] Baixando de B2: $BACKUP_REF"
        b2 download-file-by-name "$BUCKET_NAME" "$REMOTE_PATH" "$LOCAL_GPG"
        ;;
    *)
        if [[ ! -f "$BACKUP_REF" ]]; then
            echo "Arquivo local nao encontrado: $BACKUP_REF" >&2
            exit 1
        fi
        LOCAL_GPG="$BACKUP_REF"
        ;;
esac

# Descriptografa + descompacta
DUMP_FILE="$WORK_DIR/restore.dump"
echo "[$(date -Iseconds)] Descriptografando + descompactando"
gpg --batch --yes --decrypt "$LOCAL_GPG" | gunzip > "$DUMP_FILE"

# Cria o database de destino. Aborta se já existe — pra não destruir
# acidentalmente um banco em uso. Forçar via DROP_TARGET=1.
EXISTS=$(PGPASSWORD="${PG_PASSWORD:-}" psql -h "$PG_HOST" -p "$PG_PORT" -U "$PG_USER" \
    -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='$TARGET_DB';" || echo "")
if [[ "$EXISTS" == "1" ]]; then
    if [[ "${DROP_TARGET:-0}" == "1" ]]; then
        echo "[$(date -Iseconds)] DROP_TARGET=1 — derrubando $TARGET_DB"
        PGPASSWORD="${PG_PASSWORD:-}" psql -h "$PG_HOST" -p "$PG_PORT" -U "$PG_USER" \
            -d postgres -c "DROP DATABASE $TARGET_DB;"
    else
        echo "Database $TARGET_DB ja existe. Use DROP_TARGET=1 pra forcar." >&2
        exit 3
    fi
fi

PGPASSWORD="${PG_PASSWORD:-}" psql -h "$PG_HOST" -p "$PG_PORT" -U "$PG_USER" \
    -d postgres -c "CREATE DATABASE $TARGET_DB;"

echo "[$(date -Iseconds)] Restaurando para $TARGET_DB"
PGPASSWORD="${PG_PASSWORD:-}" pg_restore \
    --host="$PG_HOST" \
    --port="$PG_PORT" \
    --username="$PG_USER" \
    --dbname="$TARGET_DB" \
    --no-owner \
    --no-privileges \
    --jobs=4 \
    "$DUMP_FILE"

# Smoke queries — sanity checks que a base parece intacta.
echo "[$(date -Iseconds)] Validando smoke checks"

USUARIOS=$(PGPASSWORD="${PG_PASSWORD:-}" psql -h "$PG_HOST" -p "$PG_PORT" -U "$PG_USER" \
    -d "$TARGET_DB" -tAc "SELECT count(*) FROM identity.usuarios;" 2>/dev/null || echo "0")
MIGRATIONS=$(PGPASSWORD="${PG_PASSWORD:-}" psql -h "$PG_HOST" -p "$PG_PORT" -U "$PG_USER" \
    -d "$TARGET_DB" -tAc "SELECT count(*) FROM flyway_schema_history WHERE success = true;" 2>/dev/null || echo "0")

echo "  usuarios: $USUARIOS"
echo "  migrations aplicadas: $MIGRATIONS"

if [[ "$USUARIOS" -lt 1 ]]; then
    echo "FALHA: nenhum usuario na tabela identity.usuarios" >&2
    exit 4
fi
if [[ "$MIGRATIONS" -lt 1 ]]; then
    echo "FALHA: nenhuma migration aplicada" >&2
    exit 5
fi

echo "[$(date -Iseconds)] Restore validado com sucesso. RTO: registre o tempo total decorrido."
