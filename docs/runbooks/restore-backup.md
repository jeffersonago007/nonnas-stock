# Runbook — Restore de backup

**Quando usar**: dados em produção foram corrompidos/perdidos e precisamos voltar a um estado anterior. Para drill mensal, ver `backup-restore.md`.

**Pré-requisitos**:
- Acesso ao bucket de backup (`s3://nonnas-backups-prod/`).
- Chave privada GPG **disponível** (do cofre 1Password ou similar).
- Janela de manutenção comunicada ao usuário (UI vai cair).

**RTO alvo**: 4h (master doc 14.1).

## Decisão prévia: total ou parcial?

| Cenário                                  | Total?   | Risco                                          |
|------------------------------------------|----------|------------------------------------------------|
| `DROP TABLE` acidental por humano        | Parcial  | Recupera só a tabela, evita perder o dia       |
| Migration corrompida                     | Total    | Estado interno do schema é inconsistente       |
| Dados pessoais vazaram + LGPD obriga apagar histórico | Sim, mas estratégico | Coordenar com DPO antes        |
| Suspeita de invasão com escrita maliciosa | Total    | Confiabilidade da prod é zero até auditoria    |

## Procedimento — Restore total

### 1. Parar a aplicação

```bash
ssh prod
docker compose stop app
```

A partir daqui, ninguém escreve mais no banco. Cron jobs também devem parar (Flyway, refresh de MVs).

### 2. Backup do estado atual (mesmo corrompido)

```bash
sudo -u postgres bash -c "set -a; source /opt/nonnas-stock/.env; /opt/nonnas-stock/scripts/backup.sh"
mv /var/backups/nonnas/nonnas-*.dump.gz.gpg /var/backups/nonnas/PRE-RESTORE-$(date +%s).dump.gz.gpg
```

Sempre temos o "antes" caso precisemos comparar depois.

### 3. Importar a chave privada GPG

Em uma máquina segura (laptop do operador ou bastion), com a chave do cofre:

```bash
gpg --import nonnas-backup-private.asc
gpg --list-secret-keys backup@nonnas.com.br
```

### 4. Identificar qual backup restaurar

```bash
aws s3 ls s3://nonnas-backups-prod/diarios/ | sort -r | head -10
```

Escolher o mais recente que ainda esteja "bom". Se não temos certeza qual é o último bom, restaurar o anterior em staging primeiro.

### 5. Restore em banco temporário (sanity check)

```bash
export PG_HOST=localhost PG_USER=nonnas_app PG_PASSWORD=<senha>
./scripts/restore.sh \
    s3://nonnas-backups-prod/diarios/nonnas-2026-05-09_060000.dump.gz.gpg \
    nonnas_recovery_check
```

Smoke checks que `restore.sh` faz automaticamente: count de usuários e migrations. Se passou, o dump está íntegro.

### 6. Comparação rápida (estado atual vs recovery)

```sql
-- Em duas conexões — uma no banco prod corrompido, outra no recovery_check.
SELECT count(*) AS movimentacoes FROM operations.movimentacao;
SELECT count(*) AS alertas_ativos FROM alerts.alerta_disparado WHERE status = 'ATIVO';
SELECT max(updated_at) AS ultimo_update_usuario FROM identity.usuarios;
```

Se as contagens batem com o esperado, o backup é confiável.

### 7. Restore final em prod

```bash
# DERRUBA o banco prod e recria a partir do backup.
DROP_TARGET=1 ./scripts/restore.sh \
    s3://nonnas-backups-prod/diarios/nonnas-2026-05-09_060000.dump.gz.gpg \
    nonnas
```

### 8. Subir app

```bash
docker compose up -d app
sleep 10
curl -fsS http://localhost:8080/actuator/health/readiness
```

### 9. Smoke manual via UI

- Login admin.
- Dashboard carrega.
- Listar 1 filial.
- Listar movimentações do dia (esperado: até a janela do backup).

### 10. Comunicar conclusão

- Atualizar status page interno.
- Enviar notificação aos usuários ativos (operacionalmente — sistema está de volta, qualquer movimentação após Xh foi perdida e precisa ser refeita).
- Postmortem em < 72h.

## Procedimento — Restore parcial (uma tabela só)

```bash
# Baixa + decripta + descomprime para arquivo local
LATEST=$(aws s3 ls s3://nonnas-backups-prod/diarios/ | sort -r | head -1 | awk '{print $4}')
aws s3 cp "s3://nonnas-backups-prod/diarios/$LATEST" .
gpg --decrypt "$LATEST" | gunzip > /tmp/restore.dump

# Restaura UMA tabela em banco temporário
psql -h localhost -U nonnas_app -d postgres -c "CREATE DATABASE nonnas_partial;"
pg_restore --table=catalog.insumos --data-only \
    -h localhost -U nonnas_app -d nonnas_partial /tmp/restore.dump

# Inspeciona, faz INSERT/UPDATE seletivo no prod via psql
```

Útil quando alguém deletou uma tabela mas o resto do banco tá intacto.

## Recuperação de chave perdida

Cenário 4 do `disaster-recovery.md`. Só procedere se a chave **realmente** foi perdida — confirmar antes:

```bash
gpg --list-secret-keys
# Procurar fingerprint conhecido. Vazio = sumiu mesmo.
```

Se sumiu de **todos** os lugares (laptop + cofre digital + cofre físico), backups encriptados são inúteis e o melhor caminho é recriar o ambiente do zero a partir do que sobrou (logs do Sentry, audit_log se tiver dump em outro lugar, etc).

## Pós-incidente

- Documentar tempo total decorrido vs RTO alvo (4h).
- Atualizar este runbook se algo não estava previsto.
- Postmortem em `docs/post-mortems/`.
