# Backup e Restore — Procedimento operacional

Master doc 14.1 / ADR 0012. Documento prático: como configurar pela primeira vez, como rodar, como validar.

## Setup inicial (uma vez por ambiente)

### 1. Gerar par GPG dedicado a backups

No laptop do operador (NÃO no servidor):

```bash
gpg --batch --gen-key <<EOF
%echo Generating Nonnas Stock backup key
Key-Type: RSA
Key-Length: 4096
Subkey-Type: RSA
Subkey-Length: 4096
Name-Real: Nonnas Stock Backup
Name-Email: backup@nonnas.com.br
Expire-Date: 0
%no-protection
%commit
EOF

# Lista a chave gerada
gpg --list-keys backup@nonnas.com.br
# Anota o fingerprint (40 chars hex)
```

### 2. Distribuir as chaves

| Local              | Chave    | Justificativa                                                    |
|--------------------|----------|------------------------------------------------------------------|
| Servidor de banco  | Pública  | `gpg --import nonnas-backup-pub.asc` — usado para encriptar dump |
| Cofre 1Password    | Privada  | Único lugar onde a privada vive em produção                       |
| Cofre físico       | Privada  | Cópia impressa, em papel, dentro de cofre de banco                |
| GitHub Secret      | Privada  | `GPG_PRIVATE_KEY` para o workflow mensal de validação            |

**Nunca** colocar a chave privada na máquina de produção.

### 3. Provisionar bucket S3 (ou Backblaze B2)

```bash
aws s3 mb s3://nonnas-backups-prod --region sa-east-1

aws s3api put-bucket-versioning --bucket nonnas-backups-prod \
    --versioning-configuration Status=Enabled

# Lifecycle: 30 dias quentes, 12 semanais (90d), 12 mensais (365d).
cat > /tmp/lifecycle.json <<'EOF'
{
  "Rules": [
    {
      "Id": "ExpiraBackupsAntigos",
      "Status": "Enabled",
      "Filter": { "Prefix": "" },
      "Expiration": { "Days": 365 }
    },
    {
      "Id": "TransicaoGlacier",
      "Status": "Enabled",
      "Filter": { "Prefix": "" },
      "Transitions": [
        { "Days": 90, "StorageClass": "GLACIER_IR" }
      ]
    }
  ]
}
EOF
aws s3api put-bucket-lifecycle-configuration \
    --bucket nonnas-backups-prod \
    --lifecycle-configuration file:///tmp/lifecycle.json
```

### 4. Configurar `.env` no servidor de banco

```ini
PG_HOST=localhost
PG_PORT=5432
PG_USER=nonnas_app
PG_PASSWORD=<senha-do-banco>
PG_DB=nonnas
GPG_RECIPIENT=<fingerprint-da-chave-publica>
BACKUP_BUCKET=s3://nonnas-backups-prod/diarios
BACKUP_RETENTION_DAYS=7
AWS_PROFILE=backup-uploader
```

Permissões: `chown root:postgres /opt/nonnas-stock/.env && chmod 640 /opt/nonnas-stock/.env`.

### 5. Cron diário

```cron
# /etc/cron.d/nonnas-backup
SHELL=/bin/bash
PATH=/usr/local/bin:/usr/bin:/bin

# Carrega .env e roda backup às 03:00 BRT (UTC-3 = 06:00 UTC).
0 6 * * * postgres set -a; source /opt/nonnas-stock/.env; /opt/nonnas-stock/scripts/backup.sh >> /var/log/nonnas-backup.log 2>&1
```

Ative: `systemctl reload cron` (ou equivalente).

## Operação diária

### Verificar último backup

```bash
aws s3 ls s3://nonnas-backups-prod/diarios/ | tail -3
```

Sinais de problema:
- Mais de 36h desde o último arquivo → cron não rodou ou backup falhou.
- Tamanho do arquivo zerado ou < 100KB → dump quebrou no meio.
- Tamanho 10× maior que ontem → tabela explodiu (auditoria, log antigo) → investigar.

### Forçar backup ad-hoc

```bash
sudo -u postgres bash -c "set -a; source /opt/nonnas-stock/.env; /opt/nonnas-stock/scripts/backup.sh"
```

### Restore para staging (validação manual)

Em uma máquina **diferente** da produção, com a chave privada GPG importada:

```bash
export PG_HOST=staging-db.local PG_USER=nonnas_app PG_PASSWORD=<senha>
./scripts/restore.sh s3://nonnas-backups-prod/diarios/nonnas-2026-05-09_060000.dump.gz.gpg nonnas_staging
```

Saída esperada termina com `Restore validado com sucesso. RTO: registre o tempo total decorrido.`

### Restore parcial (só uma tabela)

```bash
# Baixa + descriptografa + descomprime
aws s3 cp s3://nonnas-backups-prod/diarios/nonnas-XXX.dump.gz.gpg ./
gpg --decrypt nonnas-XXX.dump.gz.gpg | gunzip > /tmp/restore.dump

# Lista tabelas no dump
pg_restore --list /tmp/restore.dump | grep TABLE

# Restaura só uma
pg_restore --table=catalog.insumos --data-only \
    -h prod-db.local -U nonnas_app -d nonnas /tmp/restore.dump
```

## Workflow mensal automatizado

`.github/workflows/backup-restore-test.yml` roda toda 1ª semana do mês — provisiona Postgres em container, baixa o backup mais recente do bucket, restaura, valida smoke. Se falhar, abre alerta operacional (TODO: webhook para notificação interna).

Secrets necessários no GitHub:

- `GPG_PRIVATE_KEY` — chave privada armored
- `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION` — IAM com `s3:GetObject` no bucket
- `BACKUP_BUCKET` — `s3://nonnas-backups-prod/diarios`

## Drill semestral

Ver `disaster-recovery.md` seção "Drill semestral" — exercício manual de 2-3h reservado para Jeff + Ewerton.

## Troubleshooting

Sintomas comuns e como debugar — ver `runbooks/restore-backup.md`.
