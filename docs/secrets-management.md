# Gestão de segredos — Nonnas Stock

Master doc 13.2: chaves criptográficas e segredos só vivem em variáveis de ambiente, nunca em código nem em arquivos versionados.

## Inventário

| Variável                          | Onde é usada                                       | Geração                                                            | Rotação                                  |
|-----------------------------------|----------------------------------------------------|--------------------------------------------------------------------|------------------------------------------|
| `NONNAS_MASTER_KEY`               | `CryptoService` (AES-256-GCM em campos sensíveis)  | `openssl rand -base64 32`                                          | Manual a cada 12 meses ou em incidente   |
| `NONNAS_JWT_SECRET`               | `JwtTokenProvider` (HMAC-SHA256 do JWT)            | `openssl rand -base64 32`                                          | Manual a cada 6 meses                    |
| `NONNAS_ADMIN_SENHA`              | `AdminBootstrap` (1ª criação do admin)             | `openssl rand -base64 24`                                          | Trocada no 1º login, env pode ser apagada após |
| `SPRING_DATASOURCE_PASSWORD`      | Conexão Postgres                                   | Gerado pelo provedor managed (RDS/Supabase) ou `openssl rand`      | Rotação coordenada com Postgres user     |
| `NVD_API_KEY`                     | OWASP Dependency Check (CI semanal)                | https://nvd.nist.gov/developers/request-an-api-key                 | Não rotacionado (read-only API)          |
| `GHCR_TOKEN`                      | Push de imagem em `main.yml`                       | GitHub Personal Access Token com `write:packages`                  | A cada PR de manutenção                  |

## Onde NÃO armazenar

- ❌ Em arquivos `.env` versionados (`.env.example` é OK; o real fica fora do repo).
- ❌ Hardcoded em `application*.yml` ou `*.java`.
- ❌ Em logs/audit_log/sentry — o `BeforeSendCallback` (T17) já filtra.
- ❌ Em comentários de PR ou Slack — use referência ao secret name.

## Onde armazenar

- **Servidor de produção**: `/opt/nonnas-stock/.env` (root:root, mode 600), carregado pelo Docker Compose ou systemd unit.
- **CI**: GitHub Actions Secrets (`Settings → Secrets and variables → Actions`).
- **Dev local**: arquivo `.env.local` (no `.gitignore`) carregado por `direnv` ou similar; senão, uma vez por shell session.
- **Backup off-site dos segredos**: cofre da empresa (1Password / Bitwarden Business). Documentar quem tem acesso e por quê.

## Geração de chave AES-256

```bash
openssl rand -base64 32
# Exemplo: ABCdef123...= (44 chars base64 → 32 bytes raw)
```

Em prod, valor da `NONNAS_MASTER_KEY` deve resultar em **exatamente 32 bytes** após decode base64. O `CryptoService.init()` aborta o boot se a chave estiver com tamanho errado.

## Rotação de `NONNAS_MASTER_KEY`

A chave criptografa campos sensíveis em `usuarios_2fa.secret_cifrado` e (futuramente) outros campos PII. Rotação requer migração:

1. Gerar nova chave: `NEW_KEY=$(openssl rand -base64 32)`.
2. Configurar `NONNAS_MASTER_KEY_OLD=<atual>` e `NONNAS_MASTER_KEY=<NEW_KEY>` em produção.
3. Deploy de uma versão da app que tente decrypt com `NONNAS_MASTER_KEY` primeiro, fallback `NONNAS_MASTER_KEY_OLD`, e re-encrypt+save em qualquer leitura. **(TODO T17 — implementar essa lógica de fallback no `CryptoService`.)**
4. Após 90 dias (todos os registros foram lidos pelo menos uma vez), remover `NONNAS_MASTER_KEY_OLD`.
5. Auditoria: registrar a rotação em `audit_log` com `event_type=MASTER_KEY_ROTATED`.

## Detecção de exposição

- **CI**: Trivy (`pr.yml`) detecta segredos em arquivos versionados — não está em CI atual mas planejado em T17.
- **GitHub**: secret scanning automático em PRs com Push Protection.
- **Operacional**: cron diário grep no `/var/log/nginx/access.log` por padrões de exposição (TODO operacional).

## Em caso de vazamento

1. **Imediato** (< 5min): rotacionar a chave/secret comprometida.
2. **< 1h**: invalidar todas as sessões ativas se for `NONNAS_JWT_SECRET` (limpar tabela `refresh_tokens`).
3. **< 24h**: postmortem documentado, CVE interna se aplicável.
4. **Notificações**: ANPD em até 72h se houver vazamento de dados pessoais (LGPD Art. 48).
