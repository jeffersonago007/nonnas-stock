# ADR 0012 — Estratégia de Backup, Restore e Disaster Recovery

- **Status:** Aceita
- **Data:** 2026-05-09
- **Contexto da decisão:** T18 do master doc exige backup automático off-site, restore validado mensalmente e RTO alvo < 4h. Stack atual é PostgreSQL 16 single-node, sem replica em outra região no MVP. Precisamos definir formato do dump, ferramenta de criptografia, provedor de armazenamento off-site, retenção e quem testa restore.

## Contexto

O Nonnas Stock guarda movimentações fiscais (5 anos de retenção CTN) e dados pessoais sob LGPD (anonimização imediata via `/api/v1/lgpd/exclusao`). Perda de dados = inviabilização operacional + risco de multa ANPD.

Avaliamos cinco opções de combinação:

| Opção | Formato dump        | Criptografia    | Off-site                       | Restore tooling                |
|-------|---------------------|-----------------|--------------------------------|--------------------------------|
| A     | `pg_dump --plain`   | Nenhuma         | Mesma VM                       | `psql` simples                 |
| B     | `pg_dump --plain`   | gzip + GPG      | S3                             | Shell pipeline                 |
| C     | `pg_dump -Fc`       | gzip + GPG      | S3 (Standard-IA)               | `pg_restore` parcial           |
| D     | Streaming WAL       | nativo Postgres | S3 via wal-g                   | Point-in-time recovery (PITR)  |
| E     | Replica em outra AZ | TLS in-flight   | Replica live                   | Failover automático            |

A é insustentável (sem off-site = sem DR real). E é a melhor RTO/RPO mas custa 2× o orçamento de DB e exige operação 24×7. D é o ponto sweet a longo prazo mas exige operacional WAL archiving que ainda não temos. B vs C: C ganha em flexibilidade (`pg_restore --table=...` para recuperação parcial após corrupção pontual) com mesma resiliência.

## Decisão

**Adotamos a Opção C** para o MVP:

- `pg_dump --format=custom` (binário, comprimível, restorável parcialmente).
- Pipeline: `pg_dump | gzip -9 | gpg --encrypt --recipient <fingerprint>`.
- Upload para **AWS S3** (Standard-IA storage class) ou **Backblaze B2** (alternativa custo-baixo). O bucket vive em região distinta do servidor da app.
- Lifecycle rules no bucket: 30 backups diários quentes → 12 semanais → 12 mensais (1 ano de horizonte total). Lixo após 1 ano.
- Backup roda no **servidor de banco** via cron (não na app — separa responsabilidades).
- **Validação mensal automatizada**: workflow `.github/workflows/backup-restore-test.yml` provisiona Postgres em container, baixa último backup, restaura, executa smoke queries (count > 0 em `identity.usuarios` e `flyway_schema_history`).

### Sobre o GPG e gerenciamento de chaves

- Chave **pública** GPG vive no servidor de banco (recipient do encrypt). Compromisso da chave pública = não-issue (assimétrica).
- Chave **privada** vive em dois lugares apenas: (1) cofre da empresa (1Password Business) e (2) GitHub Actions secret `GPG_PRIVATE_KEY` para o workflow de validação. **Nunca** em servidores de produção.
- Restore manual em caso de DR: um operador autorizado importa a chave privada do cofre na máquina onde vai restaurar, executa `restore.sh`, depois apaga a chave. Documentado em `runbooks/restore-backup.md`.

### RTO/RPO

- **RPO** (perda máxima aceitável): **24h** — backup diário às 03:00. Reduzir requer WAL archiving (Opção D, pós-T18).
- **RTO** (tempo máximo até voltar ao ar): **4h** — validado mensalmente via workflow + drill manual semestral com Ewerton e Jeff (master doc T18 / DR simulation).

## Consequências

**Positivas:**

- Baixa complexidade operacional — `cron + bash + aws cli` cabe em qualquer servidor.
- GPG dá criptografia forte at-rest; mesmo bucket comprometido não expõe dados.
- `pg_restore` parcial permite recuperar uma tabela específica em incidentes pontuais (ex.: dev rodou DELETE sem WHERE).
- Custo mensal estimado < US$ 5 (S3-IA: 4GB × 30 dias × 0.0125/GB = US$ 1.5).

**Negativas:**

- RPO de 24h é alto para um sistema de estoque ativo durante o expediente — perda potencial = 1 dia inteiro de movimentações. **Mitigação**: dump diário às 03:00 (fora do expediente) + WAL archiving futuro (D) reduz RPO pra ≤ 5min.
- Restore por dump custom é serial — datasets grandes demoram. Mitigação: `pg_restore --jobs=4` paraleliza. Em testes locais, dump de ~10k movimentações restaura em ~30s.
- Operação manual de chave privada GPG é vetor humano de erro. Mitigação: runbook detalhado + simulação semestral.
- Não cobre desastre regional do provedor cloud (S3 us-east-1 cair junto com app em us-east-1). Mitigação: bucket em **região diferente** da app + replicação cross-region como evolução pós-T18.

## Referências

- Master doc seção 14.1 (backup), 14.2 (DR), 14.6 (runbooks).
- ADR 0001 (modular monolith — single banco facilita o backup unificado).
- `docs/backup-restore.md` (procedimento operacional).
- `docs/disaster-recovery.md` (5 cenários e runbooks).
- `scripts/backup.sh` e `scripts/restore.sh`.
- `.github/workflows/backup-restore-test.yml`.
