#!/usr/bin/env bash
#
# Backup criptografado do Postgres do Nonnas Stock (master doc 14.1 / T18).
#
# Pipeline:
#   pg_dump --format=custom -Fc  →  gzip  →  gpg --encrypt  →  S3/B2 upload
#
# Cron sugerido (servidor de banco):
#   0 3 * * * /opt/nonnas-stock/scripts/backup.sh > /var/log/nonnas-backup.log 2>&1
#
# Variáveis de ambiente (carregar via /opt/nonnas-stock/.env + 'set -a; source'):
#   PG_HOST, PG_PORT, PG_USER, PG_DB, PG_PASSWORD
#   BACKUP_DIR              — local de staging antes do upload (default /var/backups/nonnas)
#   GPG_RECIPIENT           — fingerprint do destinatário (chave pública importada)
#   BACKUP_BUCKET           — s3://bucket/prefix ou b2://bucket/prefix
#   BACKUP_RETENTION_DAYS   — manter localmente N dias (default 7; nuvem retém pela lifecycle rule)
#   AWS_PROFILE             — opcional, perfil aws cli pra o upload
#
# Política de retenção no bucket (master doc T18):
#   30 backups diários + 12 semanais + 12 mensais via lifecycle rules.
#   Configurar uma vez via terraform/aws-cli (ver docs/backup-restore.md).
#
# Idempotência: dump tem timestamp no nome — uploads acidentais duplicam
# arquivos diferentes por segundo, sem sobrescrever.

set -euo pipefail

PG_HOST="${PG_HOST:-localhost}"
PG_PORT="${PG_PORT:-5432}"
PG_USER="${PG_USER:?PG_USER e obrigatorio}"
PG_DB="${PG_DB:?PG_DB e obrigatorio}"
BACKUP_DIR="${BACKUP_DIR:-/var/backups/nonnas}"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-7}"
GPG_RECIPIENT="${GPG_RECIPIENT:?GPG_RECIPIENT e obrigatorio (use o fingerprint da chave publica)}"
BACKUP_BUCKET="${BACKUP_BUCKET:-}"

mkdir -p "$BACKUP_DIR"

TIMESTAMP=$(date -u +"%Y-%m-%d_%H%M%S")
DUMP_FILE="$BACKUP_DIR/nonnas-${TIMESTAMP}.dump.gz.gpg"

echo "[$(date -Iseconds)] Iniciando dump criptografado de ${PG_DB}@${PG_HOST}"

# Pipeline: dump custom-format (-Fc) → gzip → gpg
# --format=custom permite pg_restore parcial (uma tabela só, p.ex.) e é mais
# compacto que SQL plain.
PGPASSWORD="${PG_PASSWORD:-}" pg_dump \
    --host="$PG_HOST" \
    --port="$PG_PORT" \
    --username="$PG_USER" \
    --dbname="$PG_DB" \
    --format=custom \
    --no-owner \
    --no-privileges \
    --compress=0 \
| gzip -9 \
| gpg --batch --yes --trust-model always \
       --encrypt --recipient "$GPG_RECIPIENT" \
       --output "$DUMP_FILE"

SIZE=$(du -h "$DUMP_FILE" | cut -f1)
SHA=$(sha256sum "$DUMP_FILE" | cut -d' ' -f1)
echo "[$(date -Iseconds)] Dump concluido (${SIZE}, sha256=${SHA:0:16}...)"

# Upload para o bucket. Mantemos uma cópia local pelo BACKUP_RETENTION_DAYS
# para restore rápido sem rede; após esse prazo, só fica na nuvem.
if [[ -n "$BACKUP_BUCKET" ]]; then
    case "$BACKUP_BUCKET" in
        s3://*)
            echo "[$(date -Iseconds)] Upload S3: $BACKUP_BUCKET"
            aws s3 cp "$DUMP_FILE" "$BACKUP_BUCKET/$(basename "$DUMP_FILE")" \
                --storage-class STANDARD_IA \
                --metadata "pg-db=${PG_DB},sha256=${SHA}"
            ;;
        b2://*)
            echo "[$(date -Iseconds)] Upload B2 (Backblaze): $BACKUP_BUCKET"
            BUCKET_NAME=$(echo "$BACKUP_BUCKET" | sed 's|^b2://||;s|/.*||')
            PREFIX=$(echo "$BACKUP_BUCKET" | sed -E 's|^b2://[^/]+/?||')
            b2 upload-file "$BUCKET_NAME" "$DUMP_FILE" "${PREFIX}/$(basename "$DUMP_FILE")"
            ;;
        *)
            echo "[$(date -Iseconds)] BACKUP_BUCKET '$BACKUP_BUCKET' nao suportado (use s3:// ou b2://)" >&2
            exit 1
            ;;
    esac
else
    echo "[$(date -Iseconds)] BACKUP_BUCKET nao configurado — ficou apenas local em $BACKUP_DIR"
fi

# Retenção local (lifecycle do bucket cobre a retenção em nuvem).
echo "[$(date -Iseconds)] Limpando backups locais com mais de $RETENTION_DAYS dias"
find "$BACKUP_DIR" -name 'nonnas-*.dump.gz.gpg' -mtime "+$RETENTION_DAYS" -delete
# Limpa formato antigo (T15) — pode coexistir alguns dias após upgrade.
find "$BACKUP_DIR" -name 'nonnas-*.sql.gz' -mtime "+$RETENTION_DAYS" -delete

echo "[$(date -Iseconds)] Backup OK"
