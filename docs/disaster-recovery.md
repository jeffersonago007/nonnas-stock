# Disaster Recovery — Nonnas Stock

Plano operacional para 5 cenários cobertos no master doc 14.2. Cada cenário tem um RTO alvo (tempo até voltar) e ações concretas. Treinar pelo menos uma vez por trimestre — vai-se aprender mais com o drill que com o documento.

## RTO/RPO globais

- **RTO alvo**: 4h (sistema online novamente após desastre detectado).
- **RPO alvo**: 24h (perda máxima de dados aceitável).
- **Detecção alvo**: < 15min (alerta operacional via Grafana ou usuário reportando).

## Cenário 1 — App crash loop após deploy

**Sintoma**: container `app` reinicia em loop; `/actuator/health/readiness` retorna 503.

**Detecção**: alerta Grafana + usuários reportando 502 do Nginx.

**RTO**: 15 min.

**Procedimento**:
1. `docker logs --tail=100 app` — identificar exception.
2. **Rollback rápido** sem investigar:
   ```bash
   docker pull ghcr.io/jeffersonago007/nonnas-stock:sha-<hash-anterior>
   sed -i 's|:latest|:sha-<hash-anterior>|' docker-compose.prod.yml
   docker compose up -d app
   ```
3. Investigar offline qual commit quebrou — revert no Git, novo deploy, voltar pra `:latest`.

Ver também: `runbooks/restore-backup.md` (se a causa for migration corrompida).

## Cenário 2 — Banco corrompido / perda de dados

**Sintoma**: queries falhando com erros de integridade, ou DELETE/UPDATE sem WHERE feito por humano. Dados visivelmente errados na UI.

**Detecção**: alerta operacional (Sentry + suporte de usuário).

**RTO**: 4h.

**Procedimento**:
1. **Parar a app** imediatamente (`docker compose stop app`) pra evitar mais escrita.
2. Restaurar último backup conhecido bom em **banco temporário** (não em prod direto):
   ```bash
   ./scripts/restore.sh s3://nonnas-backups/nonnas-2026-05-09_030000.dump.gz.gpg nonnas_recovery
   ```
3. Comparar `nonnas_recovery` vs `nonnas` (prod corrompido) — diff em tabelas críticas. Decidir:
   - **Restore total** (perde mudanças desde o backup): drop prod + rename recovery → prod.
   - **Restore parcial** (só tabela X): `pg_dump -t schema.tabela` do recovery + `psql` em prod.
4. Subir app: `docker compose up -d app`.
5. Smoke: login admin + abrir dashboard.
6. Postmortem em < 72h: documentar em `docs/post-mortems/`.

Ver também: `runbooks/restore-backup.md` para o procedimento detalhado de restore.

## Cenário 3 — Servidor de produção indisponível (VM down, datacenter)

**Sintoma**: ping/SSH não responde, Nginx fora.

**Detecção**: monitoramento externo (uptime-kuma, statuscake) ou usuários.

**RTO**: 4h.

**Procedimento**:
1. Verificar status do provedor (AWS Health, status do datacenter).
2. **Failover manual**:
   - Provisionar nova VM em região diferente.
   - `docs/deployment.md` cobre setup zero-to-running.
   - Restaurar último backup do S3.
   - Apontar DNS para novo IP (TTL baixo ajuda — manter em 300s).
3. Atualizar `docs/post-mortems/` com tempo total de recuperação.

Mitigação futura (pós-T18): segundo servidor em standby com replicação WAL.

## Cenário 4 — Perda de chave de criptografia GPG

**Sintoma**: `restore.sh` falha com "no secret key" — backup criptografado mas a chave privada foi perdida.

**Detecção**: tentativa de restore (esperamos drill mensal pegar isso).

**RTO**: depende de quanto tempo desde o último backup ainda recuperável.

**Procedimento**:
1. **Verificar cofre** (1Password Business / Bitwarden) — chave privada deve estar lá. Se está, importar (`gpg --import private.asc`) e seguir restore normal.
2. Se a chave do cofre também foi perdida (improvável mas catastrófico), os backups criptografados se tornam inúteis.
3. **Único caminho**: dados são perdidos a partir do último ponto recuperável (que provavelmente é um dump local não-criptografado de teste).
4. Postmortem em < 24h, comunicação ANPD em < 72h se houver dados pessoais não recuperáveis (LGPD Art. 48).
5. **Geração de nova chave**: documentar em `secrets-management.md` o passo a passo de rotação de chaves.

Mitigação operacional: chave privada vive em **dois cofres independentes** (1Password + impressão em papel num cofre físico).

## Cenário 5 — Acesso não-autorizado / vazamento de dados

**Sintoma**: `audit_log` com login de IP estranho, ou alerta externo (terceiro reportando dados na deep web).

**Detecção**: dependerá de monitoramento (Sentry breadcrumbs, audit_log, alerta Grafana de login fora de padrão).

**RTO**: contenção em 1h, comunicação em 24h.

**Procedimento**:
1. **Conter**: revogar todos os JWT (`UPDATE refresh_tokens SET revoked_at = NOW();`), forçar troca de senha de todos os usuários ativos, rotacionar `NONNAS_JWT_SECRET` e `NONNAS_MASTER_KEY` (ver `secrets-management.md`).
2. **Investigar**: `audit_log` filtrado por janela suspeita; `runbooks/alerta-falso-positivo.md` ajuda a rastrear.
3. **Comunicar**:
   - DPO interno em < 1h.
   - ANPD em < 72h se houver risco aos titulares (LGPD Art. 48).
   - Usuários afetados (e-mail diretamente OU notificação interna).
4. **Postmortem público** com:
   - O que aconteceu, quando, quanto tempo.
   - Quantos usuários afetados, quais dados.
   - Mitigações aplicadas e quando.
   - Ações preventivas para o futuro.

Mitigação futura (pós-T18): WAF (Cloudflare ou similar) + 2FA obrigatório para ADMIN (T16 já tem o backend; falta forçar em produção).

## Drill semestral

Master doc T18 exige uma simulação completa antes do go-live e depois semestralmente:

1. Marcar 2-3h numa janela de baixo tráfego, com Jeff e Ewerton presentes.
2. Escolher um cenário (rotacionar a cada drill).
3. Cronometrar do "primeiro alerta" até "sistema validado em produção".
4. Documentar em `docs/post-mortems/simulacao-dr-<data>.md` o que foi feito, quanto tempo, o que falhou, o que melhorar.
5. Ajustar runbooks com base no aprendizado.

## Métricas a observar

- **Backup mensal restorado**: workflow `backup-restore-test.yml` deve ficar verde.
- **Tamanho do backup**: crescimento exponencial = sinal de tabela inflada (auditoria, logs antigos não-purgados).
- **Tempo de restore**: > 30min em ambiente de teste = preocupação para RTO.
- **Idade do último backup verificado**: se > 30 dias, drill atrasado.
