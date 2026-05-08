# ADENDO AO PROMPT_CLAUDE_CODE.md — Profissionalização

Este adendo acrescenta três seções (13, 14, 15) e três tarefas (T16, T17, T18) ao documento mestre. Cobre os gaps de segurança/LGPD, confiabilidade operacional e observabilidade — sem incluir email e WhatsApp, que ficam reservados para roadmap futuro. Para o MVP 1.0, **toda comunicação com usuário acontece via notificações internas no sistema** (badge + tela de notificações) **e logs estruturados centralizados**.

## Como integrar

1. Cole as seções **13**, **14** e **15** abaixo após a seção **12. Anexos** do documento original (ou antes, se preferir agrupar por afinidade — Anexos no final).
2. Cole as tarefas **T16**, **T17** e **T18** ao final da seção **10. Tarefas sequenciais**.
3. Adicione as três linhas correspondentes em **STATUS.md** (snippet ao final deste adendo).
4. Atualize a estimativa de esforço conforme a recalibração ao final.

---

## 13. Segurança e LGPD

Sistema profissional para restaurante em rede coleta dado pessoal (clientes via canais de venda, funcionários, fornecedores PF). Sem tratamento adequado, ANPD multa, cliente perde reputação. Estes itens são **inegociáveis** para o go-live.

### 13.1 LGPD compliance

- Mapeamento explícito dos campos pessoais em `docs/lgpd-mapping.md`: tabela com `(entidade, campo, finalidade, base_legal, retencao)`. Atualizado a cada nova entidade que captura PII.
- Anonimização programada: job mensal que substitui campos identificáveis por hash em registros sem atividade há 5 anos (configurável). Mantém estatísticas, perde individualidade.
- Endpoints REST para exercício de direitos do titular: `GET /api/v1/lgpd/meus-dados` (consultar), `POST /api/v1/lgpd/correcao` (corrigir), `DELETE /api/v1/lgpd/exclusao` (excluir/anonimizar). Acessíveis com autenticação especial via link enviado por canal interno.
- Registro de Operações de Tratamento (ROPA) em `docs/lgpd-ropa.md` — quem trata, com que finalidade, base legal, compartilhamento com terceiros (canais de venda, contabilidade).
- Termo de uso e política de privacidade obrigatoriamente aceitos na primeira sessão; aceite registrado em tabela `aceites_termos` com versão do termo + timestamp + IP.
- Política de retenção documentada por entidade (movimentação fiscal: 5 anos por exigência da Receita; dados de cliente sem atividade: anonimizar após 5 anos; logs de auditoria: 2 anos).

### 13.2 Criptografia em camadas

**Em repouso:**
- Campos sensíveis criptografados via `pgcrypto` (extensão Postgres) ou JPA `AttributeConverter` com BouncyCastle: CPF, CNPJ de pessoa física, telefone pessoal, endereço residencial, dados bancários se houver. Hash de senha continua BCrypt.
- Chave mestra de criptografia em variável de ambiente, gerenciada externamente. Em produção, usar AWS KMS, HashiCorp Vault ou equivalente. Em dev, arquivo `.env` ignorado pelo git.
- Backup do banco também criptografado (chave separada da chave de aplicação) — ver T18.

**Em trânsito:**
- TLS 1.3 obrigatório em produção. Certificado Let's Encrypt automatizado via certbot ou Traefik.
- HSTS habilitado com `max-age=31536000; includeSubDomains; preload`.
- Headers de segurança via Spring Security: `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: strict-origin-when-cross-origin`, `Permissions-Policy` restritiva.
- Content Security Policy (CSP) restritiva no frontend: `default-src 'self'`, sem `unsafe-inline` ou `unsafe-eval`.

### 13.3 Autenticação reforçada

- **Política de senha** validada client-side (Zod) e server-side: mínimo 10 caracteres, ao menos 1 letra, 1 número, 1 caractere especial. Histórico das últimas 5 senhas em hash — não permite reuso. Expiração opcional por perfil (ADMIN: 90 dias; outros: sem expiração obrigatória).
- **Brute force progressivo** com bloqueio escalonado:
  - 3 tentativas falhas → bloqueio de 15 minutos
  - 5 tentativas falhas → bloqueio de 1 hora
  - 10 tentativas falhas → conta travada, libera por intervenção de ADMIN
  - Reset do contador após login bem-sucedido
- **2FA opcional via TOTP** (Google Authenticator, Authy, Microsoft Authenticator). Obrigatório para perfil ADMIN. Bibliotecas: `otp-java` ou `java-totp`. Backup codes de 10 códigos de uso único entregues no setup.
- **JWT com refresh token rotation**: cada uso do refresh token o invalida e emite novo. Detecta replay (refresh token usado duas vezes → revoga toda a família, força re-login).
- **Blacklist de tokens em logout**: tabela `tokens_revogados (jti, expires_at)` com índice TTL ou job de limpeza. Filtro JWT consulta antes de validar.
- **Notificação interna de login suspeito**: login a partir de IP novo (não visto nos últimos 30 dias) gera notificação interna pro próprio usuário ("Detectamos login de IP X em data Y. Foi você?"). Sem email no MVP — só dentro do sistema.

### 13.4 Audit log estruturado

- Hibernate Envers configurado em todas as entidades de domínio (gera tabelas `_aud` automáticas com versionamento).
- Wrapper customizado `AuditLogService` para eventos críticos não-CRUD: login bem-sucedido, login falho, alteração de permissão, ajuste manual aprovado, transferência aprovada, exclusão lógica de registro, exercício de direito LGPD.
- Schema da tabela `audit_log`:
  ```
  id (uuid), usuario_id (uuid), evento (string),
  entidade (string), entidade_id (uuid),
  ip (inet), user_agent (text),
  dados_antes (jsonb), dados_depois (jsonb),
  registrado_em (timestamp)
  ```
- Endpoint `GET /api/v1/admin/audit-log` com filtros (usuário, entidade, período, tipo de evento) — acesso restrito a perfil ADMIN.
- Retenção 2 anos em hot storage; após isso, exporta para arquivo criptografado em armazenamento frio (S3 Glacier, Backblaze).

### 13.5 OWASP Top 10 — mitigações específicas

- **Injection**: JPA com prepared statements protege SQL automaticamente. Validar com queries nativas em `@Query` (revisar todas em code review).
- **XXE no parser de NF-e** (relevante na onda 1.1): desabilitar resolução de entidades externas no parser XML — `factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)`. Documentar na ADR de NF-e.
- **Broken Access Control**: testes de integração obrigatórios cobrindo: usuário de filial A não vê dados de filial B; perfil OPERADOR não acessa endpoints de ADMIN; endpoint `/admin/*` retorna 403 sem perfil correto.
- **Sensitive Data Exposure**: campos sensíveis nunca aparecem em logs (filtro Logback). DTOs de resposta omitem CPF, telefone — só admin vê completo.
- **XSS**: Tailwind + React já escapam por padrão. Sanitização explícita via OWASP Java Encoder se houver renderização de HTML vindo do banco (ex.: descrição de produto rica).
- **Vulnerable Dependencies**: OWASP Dependency Check no CI (Maven plugin), Trivy no Docker image, Renovate ou Dependabot abrindo PRs automáticos.
- **Insufficient Logging**: já coberto pelo audit log + Sentry (T17).

---

## 14. Confiabilidade, Operação e Engenharia

Confiabilidade não é só código bom — é processo. Estes itens transformam um software entregue em sistema operável.

### 14.1 Backup e recuperação de dados

- **Backup automatizado do PostgreSQL** com `pg_dump` em script `scripts/backup.sh`:
  - Diário às 03:00 BRT (retenção 30 dias)
  - Semanal aos domingos (retenção 12 semanas)
  - Mensal no dia 1 (retenção 12 meses)
- **Off-site obrigatório**: backups enviados para S3 ou Backblaze B2 (~R$ 30/mês). Bucket com versionamento e lock de exclusão. Chave de criptografia separada da chave de aplicação.
- **Teste de restore mensal automatizado**: GitHub Actions agendado executa restore num ambiente isolado, valida integridade (queries de smoke), reporta resultado. Backup que ninguém testa não é backup, é falsa esperança.
- **Metas iniciais**: RPO 24h (perda máxima de 1 dia em desastre absoluto), RTO 4h (tempo máximo pra restaurar serviço).
- Documentar processo completo em `docs/backup-restore.md`.

### 14.2 Disaster Recovery plan

Documento `docs/disaster-recovery.md` com cenários e procedimentos passo-a-passo:

- Cenário A: banco corrompido — restore do último backup, perda do dia.
- Cenário B: servidor inacessível — recriar VM, restaurar backup, reapontar DNS.
- Cenário C: exclusão acidental em massa por usuário — ponto-no-tempo via WAL archiving (configurar em produção).
- Cenário D: provedor cloud inteiro fora do ar — procedimento de provisionamento em provedor alternativo.
- Cenário E: ransomware ou comprometimento — contenção, restore de backup off-site, rotação de chaves, comunicação ao cliente.

Cada cenário tem: gatilho, contatos, comandos exatos, validação pós-restore, comunicação interna.

**Teste do plano**: pelo menos uma simulação completa antes do go-live, repetido trimestralmente.

### 14.3 Health checks profundos

- `GET /actuator/health/liveness` — processo está rodando? (resposta < 100ms)
- `GET /actuator/health/readiness` — pronto pra atender? (banco responde, migrations aplicadas, fila de eventos abaixo do limite).
- Indicadores customizados:
  - `DatabaseLatencyHealthIndicator` — latência média do banco nos últimos 5min < 200ms.
  - `MigrationsHealthIndicator` — todas as migrations Flyway aplicadas.
  - `AlertQueueHealthIndicator` — fila de avaliação de alertas com lag < 30s.
  - `ConsistencyHealthIndicator` — última conciliação saldo materializado vs movimentações sem divergência.
- Endpoints expostos sem autenticação somente em rede interna (load balancer, Kubernetes probes); externamente sob auth.

### 14.4 Rate limiting e resiliência

- **Rate limiting** com Bucket4j: limites diferenciados:
  - Anônimo (rotas de login): 10 req/min por IP.
  - Autenticado: 200 req/min por usuário.
  - Endpoint de exclusão LGPD: 5 req/dia por usuário.
- **Circuit breaker** com Resilience4j em todas as chamadas para APIs externas (preparação para canais de venda em onda 1.2):
  - Retry exponencial: 3 tentativas com backoff 1s, 3s, 9s.
  - Circuit breaker abre após 50% de falhas em janela de 20 chamadas.
  - Fallback gracioso: persiste a operação numa fila de retry assíncrono em vez de falhar a requisição do usuário.
- **Timeouts explícitos** em toda chamada externa: 5s connect, 30s read.

### 14.5 Status page interna

Tela `/admin/status` (perfil ADMIN ou GERENTE) com:
- Saúde de cada componente (banco, fila, jobs agendados) com cor (verde/amarelo/vermelho).
- Últimas 20 movimentações registradas em todas as filiais.
- Alertas disparados não resolvidos.
- Transferências em trânsito.
- Métricas-resumo: requisições/min, latência p95, taxa de erro 5xx última hora.
- Versão da aplicação, hash do commit, data do último deploy.

Não substitui Grafana (T17), mas é o primeiro lugar que o suporte abre quando cliente liga reclamando.

### 14.6 Processos de engenharia

**ADRs (Architecture Decision Records)** em `docs/adr/`:
- Formato: arquivo numerado `0001-titulo-curto.md` com seções `Status`, `Contexto`, `Decisão`, `Consequências`.
- Toda decisão arquitetural relevante vira ADR antes da implementação.
- Imutável após aprovado: mudanças geram nova ADR que supersedes a anterior.
- ADRs iniciais a criar na T00: `0001-modular-monolith`, `0002-postgres-como-banco-principal`, `0003-jwt-com-refresh-rotation`, `0004-fefo-como-estrategia-de-saida`, `0005-versionamento-de-receita-via-snapshot`.

**Runbooks operacionais** em `docs/runbooks/`:
- `postgres-lento.md` — diagnóstico e ações.
- `restore-backup.md` — procedimento de restore validado.
- `novo-usuario-admin.md` — criar usuário ADMIN com 2FA e auditoria.
- `migracao-manual-de-dados.md` — quando migration Flyway não basta.
- `alerta-falso-positivo.md` — desativar config errônea.
- `feature-flag-rollback.md` — desligar funcionalidade em produção sem deploy.
- Cresce com a operação. Atualizado após cada incidente.

**Gestão de incidentes**:
- Severity levels:
  - **P0** — sistema indisponível ou perda de dados. Resposta em 30min, 24/7.
  - **P1** — funcionalidade crítica indisponível (não consegue lançar venda, transferência travada). Resposta em 2h em horário comercial.
  - **P2** — bug não-crítico ou degradação. Resposta no próximo dia útil.
  - **P3** — melhoria ou bug cosmético. Backlog.
- **Post-mortem blameless** após cada P0 e P1: o que aconteceu, linha do tempo, causa raiz, ações de prevenção. Em prosa de 1 página, em `docs/post-mortems/`. Cultura: focar em sistema, não em pessoa.

**SLO/SLI definidos contratualmente**:
- Disponibilidade: 99.5% mensal (3.6h de downtime permitido — realista para arquitetura single-region sem alta disponibilidade).
- Latência p95 < 1s nas operações principais (login, listagem de saldo, lançamento de movimentação).
- Error rate < 0.5% em janela de 5 minutos.
- Medido por Prometheus (T17), exposto em dashboard Grafana acessível ao cliente.

**Code review checklist** (atualizar `.github/pull_request_template.md`):
- [ ] Testes adicionados cobrindo o novo comportamento.
- [ ] Cobertura JaCoCo mantida ou aumentada.
- [ ] ADR criado se decisão arquitetural significativa.
- [ ] Migration Flyway revisada por outro dev.
- [ ] Sem campos sensíveis em logs ou DTOs de resposta inadequados.
- [ ] Documentação atualizada (README, runbook, OpenAPI).
- [ ] Sem secrets commitados (verificação automática via git-secrets ou similar).

SLA interno de revisão: 24h úteis para primeira resposta.

---

## 15. Observabilidade e Notificações Internas

A comunicação com o usuário no MVP 1.0 acontece **dentro do sistema** (notificações in-app + dashboards) e **nos logs centralizados** (para a equipe de operação). Email e WhatsApp ficam reservados para evolução posterior — a arquitetura prevê os pontos de extensão, mas nenhum está ativo.

### 15.1 Error tracking — Sentry

- **Sentry SDK** integrado no backend (sentry-spring-boot-starter) e no frontend (@sentry/react).
- Toda exception não tratada e todo erro de JS no frontend cai no Sentry com stack trace, contexto do usuário (anonimizado para LGPD — só ID, nunca CPF), request payload sanitizado.
- Free tier do Sentry cobre o início; revisar quando volume crescer.
- **Source maps** do frontend enviados no build pra stack trace fazer sentido.
- **Release tracking**: cada deploy reporta versão pro Sentry, permite ver em que versão um bug apareceu.
- Filtros: erros conhecidos e ignoráveis (cliente cancelando request) em `BeforeSendCallback` para não poluir.

### 15.2 Métricas — Prometheus + Grafana

- **Spring Boot Actuator** + **micrometer-registry-prometheus** expõe `/actuator/prometheus` automaticamente.
- **Grafana Cloud free tier** (10k séries, 14 dias retenção) cobre o início. Migra para self-hosted depois se justificar.
- Dashboards iniciais:
  - **Operacional**: requisições/min por endpoint, latência p50/p95/p99, taxa de erro 4xx/5xx, uso de conexões do pool HikariCP.
  - **Banco**: queries lentas top 10, locks ativos, cache hit ratio, tamanho das tabelas.
  - **Negócio**: total de movimentações por dia, distribuição por tipo, saldo total por filial, alertas disparados por categoria.
- Alertas operacionais via Grafana Alerting — notificação interna no sistema (entrega via webhook que cria notificação na tabela `notificacoes_usuario`).

### 15.3 Tracing distribuído — OpenTelemetry

- **OpenTelemetry SDK** em modo básico no MVP 1.0 (apenas traces locais, sem export externo). Justifica investir em backend de tracing (Tempo, Jaeger) na onda 1.2 quando integrações de canais aumentarem complexidade.
- **Correlation ID** em toda request via filtro Spring que adiciona `traceId` no MDC do Logback. Logs estruturados carregam o ID, permite rastrear request inteira nos logs.
- Auto-instrumentação de chamadas JDBC, HTTP cliente e endpoints REST.

### 15.4 Notificações internas — sistema próprio

Toda comunicação operacional com o usuário acontece **dentro do sistema** no MVP 1.0.

**Modelo de dados**:
```
notificacoes_usuario (
  id uuid PK,
  usuario_id uuid FK,
  tipo varchar,           -- ALERTA_DISPARADO, TRANSFERENCIA_APROVADA,
                          -- DIVERGENCIA_INVENTARIO, LOGIN_NOVO_IP, etc
  prioridade varchar,     -- INFO, AVISO, CRITICA
  titulo varchar,
  mensagem text,
  link_acao varchar,      -- ex: /alertas/42 — pra clicar e ir direto pro contexto
  metadata jsonb,         -- dados estruturados específicos do tipo
  criada_em timestamp,
  lida_em timestamp,      -- null = não lida
  arquivada_em timestamp  -- null = ativa
)
```

**Backend**:
- `NotificacaoInternaService` recebe eventos de domínio (via `@EventListener` Spring) e cria registros: alerta disparado, transferência mudou de status, divergência detectada, login de IP novo, exercício de direito LGPD.
- API REST:
  - `GET /api/v1/notificacoes` — paginado, com filtros (lida, tipo, período).
  - `GET /api/v1/notificacoes/contagem-nao-lidas` — leve, otimizado pra polling.
  - `POST /api/v1/notificacoes/{id}/marcar-lida`
  - `POST /api/v1/notificacoes/marcar-todas-lidas`
  - `POST /api/v1/notificacoes/{id}/arquivar`

**Frontend**:
- Badge no header com contagem de não lidas (atualizada por polling a cada 30s — em onda futura, migrar para Server-Sent Events ou WebSocket).
- Página `/notificacoes` com lista paginada, filtros, ações em massa.
- Toast no canto da tela quando notificação CRITICA chega em tempo real.
- Som opcional configurável por usuário (preferência salva).

**Pontos de extensão para o futuro**:
- Interface `CanalNotificacao` no backend; implementação atual `CanalInterno` que grava na tabela. Implementações futuras (`CanalEmail`, `CanalWhatsApp`) plugam sem refatorar consumidores. Cada notificação tem campo `canais_destino` (jsonb) que decide para onde despachar — hoje sempre `["INTERNO"]`, no futuro pode ser `["INTERNO", "EMAIL"]`.

### 15.5 Logs estruturados centralizados

- **Logback configurado em duas modalidades**:
  - Dev: console colorido legível.
  - Produção: JSON estruturado em stdout. Cada linha contém `timestamp`, `level`, `logger`, `traceId`, `usuario_id`, `filial_id` (quando disponível), `message`, `stack_trace` (se erro).
- **Agregação centralizada**: Loki (Grafana Cloud free tier) ou Promtail enviando para stack ELK self-hosted, conforme decisão do cliente sobre orçamento.
- **Correlation ID** propagado via MDC: a mesma request gera dezenas de linhas de log que podem ser agrupadas pelo `traceId`.
- **Filtro de campos sensíveis**: appender Logback customizado que mascara CPF, senha, token JWT, números de cartão (mesmo que nunca devam aparecer em log, defesa em profundidade).
- **Retenção**: 30 dias hot, 90 dias cold (gzip em S3). Audit log via tabela própria tem retenção 2 anos.
- Padrão de mensagens:
  - Log estruturado preferencial: `log.info("Movimentacao registrada", kv("movimentacaoId", id), kv("tipo", tipo), kv("filialId", filialId))` usando StructuredArguments do logstash-logback-encoder.
  - Texto livre só em situações onde estrutura não agrega.

### 15.6 Email e WhatsApp — fora do escopo do MVP 1.0

Reservado para roadmap pós-go-live. Quando entrar:
- **Email**: Amazon SES recomendado pelo custo (R$ 0,50 por 1000 emails). Templates Thymeleaf renderizados server-side. Casos de uso: recuperação de senha, alerta CRITICA opcional, relatório semanal de gestor.
- **WhatsApp**: avaliação caso-a-caso entre API oficial Meta Business (caro mas estável) e Baileys (não-oficial, padrão SM Recuperadora — viável mas exige operação dedicada). Dado que o Jeff já tem expertise com Baileys no projeto SM, pode-se replicar a arquitetura.

A interface `CanalNotificacao` na seção 15.4 garante que adicionar essas implementações no futuro não exige reescrita.

---

## Novas Tarefas

### T16 — Hardening de segurança e LGPD

**Objetivo:** elevar o sistema do nível "funciona" para o nível "passa em auditoria". Cobre seções 13.1–13.5.

**Pré-requisitos:** T09 (API consolidada), T10 (infra de testes).

**Entregáveis:**
- Migration `V0XX__create_audit_log_and_lgpd_schemas.sql` com tabelas `audit_log`, `tokens_revogados`, `aceites_termos`, `tentativas_login`, `usuarios_2fa`.
- Hibernate Envers configurado em todas entidades de domínio.
- `AuditLogService` para eventos não-CRUD (login, alteração de permissão, etc).
- `PoliticaSenhaValidator` (Bean Validation custom) e `HistoricoSenhaService` impedindo reuso.
- `BruteForceProtectionFilter` com bloqueio progressivo.
- Setup de 2FA TOTP: endpoints `POST /api/v1/auth/2fa/setup`, `POST /api/v1/auth/2fa/confirmar`, geração de QR Code, backup codes.
- JWT refresh token rotation com detecção de replay.
- Token blacklist em logout.
- `CamposSensiveis` JPA AttributeConverter usando BouncyCastle ou pgcrypto. Aplicado em CPF, CNPJ-PF, telefone pessoal, endereço residencial.
- Configuração de chave mestra em `application-prod.yml` via env var, documentado em `docs/secrets-management.md`.
- Spring Security configurado com headers de segurança completos.
- CSP no frontend via meta tag e header HTTP.
- OWASP Java Encoder em qualquer renderização de string vinda do banco.
- OWASP Dependency Check no Maven, executado no workflow PR (falha se CVE crítico).
- Endpoints LGPD: `GET /api/v1/lgpd/meus-dados`, `POST /api/v1/lgpd/correcao`, `DELETE /api/v1/lgpd/exclusao`.
- Job `@Scheduled` mensal de anonimização programada.
- Documentos: `docs/lgpd-compliance.md`, `docs/lgpd-mapping.md`, `docs/lgpd-ropa.md`, `docs/secrets-management.md`.

**Critérios de aceitação:**
- Tentativa de SQL injection em qualquer endpoint retorna 400 ou é bloqueada (teste explícito).
- Sequência de 5 logins falhos bloqueia conta por 1h (teste integração).
- Token JWT após logout retorna 401 (mesmo dentro da validade).
- Refresh token usado duas vezes invalida toda a família (teste integração).
- ADMIN sem 2FA configurado não consegue acessar funcionalidades sensíveis.
- CPF gravado no banco não é legível em consulta direta (criptografado).
- OWASP Dependency Check verde no CI.
- Documento LGPD revisado e assinado pelo responsável (Jeff ou Ewerton).

---

### T17 — Observabilidade e notificações internas

**Objetivo:** instrumentar a aplicação e implementar o canal de comunicação interno com o usuário. Cobre seções 15.1–15.5.

**Pré-requisitos:** T16.

**Entregáveis:**
- Sentry SDK integrado (backend Spring Boot starter, frontend `@sentry/react`).
- Source maps do build do frontend enviados ao Sentry no workflow main.
- `BeforeSendCallback` filtrando dados sensíveis e erros conhecidos.
- micrometer-registry-prometheus + endpoint `/actuator/prometheus`.
- Configuração de scrape do Prometheus em `docs/observability/prometheus-config.md`.
- Dashboards Grafana exportados em JSON em `docs/observability/dashboards/` (operacional, banco, negócio).
- OpenTelemetry SDK em modo básico, com correlation ID em MDC.
- Filtro Spring que adiciona `traceId`, `usuarioId`, `filialId` ao MDC em toda request autenticada.
- Logback config: console colorido em dev, JSON estruturado em prod.
- Appender customizado de mascaramento de campos sensíveis.
- Migration `V0XX__create_notificacoes_internas.sql`.
- `NotificacaoInternaService` + `CanalNotificacao` interface + implementação `CanalInterno`.
- `@EventListener` em eventos críticos: `AlertaDisparadoEvent`, `TransferenciaStatusAlteradoEvent`, `DivergenciaInventarioEvent`, `LoginNovoIPEvent`, `LgpdDireitoExercidoEvent`.
- API REST de notificações (5 endpoints listados em 15.4).
- Frontend: badge no header, página `/notificacoes`, toast para CRITICA, polling a cada 30s.
- Webhook do Grafana Alerting → notificação interna pra perfil ADMIN.

**Critérios de aceitação:**
- Exception não tratada cai no Sentry com stack trace completo, contexto sanitizado.
- Dashboard operacional do Grafana exibe requisições, latência, erro em tempo real.
- Log de produção é parseável como JSON e tem `traceId` consistente em request inteira.
- Alerta de estoque mínimo dispara → registro em `alertas_disparados` + entrada em `notificacoes_usuario` + badge incrementa no frontend em < 5s.
- CPF presente em variável de log é mascarado na saída.
- Página `/notificacoes` paginada, com filtros, com ações em massa.

---

### T18 — Backup, Disaster Recovery e Runbooks

**Objetivo:** garantir que o sistema sobrevive a desastres e que a operação tem playbooks confiáveis. Cobre seções 14.1, 14.2, 14.6.

**Pré-requisitos:** T17.

**Entregáveis:**
- `scripts/backup.sh` com `pg_dump`, criptografia GPG, upload pra S3 ou Backblaze B2.
- Cron job no servidor de produção (ou GitHub Actions agendado com SSH) executando backup diário às 03:00 BRT.
- Política de retenção configurada no bucket: 30 backups diários, 12 semanais, 12 mensais via lifecycle rules.
- `scripts/restore.sh` com download, descriptografia, restore validado por queries de smoke.
- Workflow `.github/workflows/backup-restore-test.yml` mensal: provisiona ambiente isolado, restaura último backup, executa smoke tests, derruba ambiente, reporta resultado.
- ADR `0010-backup-strategy.md` documentando decisões.
- Documentos:
  - `docs/disaster-recovery.md` com 5 cenários e procedimentos.
  - `docs/backup-restore.md` com procedimento operacional.
  - `docs/runbooks/postgres-lento.md`
  - `docs/runbooks/restore-backup.md`
  - `docs/runbooks/novo-usuario-admin.md`
  - `docs/runbooks/migracao-manual-de-dados.md`
  - `docs/runbooks/alerta-falso-positivo.md`
  - `docs/runbooks/feature-flag-rollback.md`
- `docs/post-mortems/template.md` (estrutura para futuros post-mortems).
- Tabela `feature_flags` (placeholder, sem UI ainda) + classe `FeatureFlagService` + uso em pelo menos uma rota crítica como prova de conceito.
- Atualização de `.github/pull_request_template.md` com checklist completo da seção 14.6.
- Atualização de `STATUS.md` com seção "ADRs criados" listando todos.
- Simulação de DR registrada em `docs/post-mortems/simulacao-dr-pre-golive.md`.

**Critérios de aceitação:**
- Workflow de teste de restore passa verde em ambiente isolado.
- Tempo medido de restore < RTO definido (4h).
- Backup diário gera arquivo criptografado no bucket off-site.
- ADRs 0001 a 0010 escritos e versionados.
- Pelo menos 6 runbooks na pasta, todos com procedimento testado.
- PR template atualizado com checklist completo.
- Simulação de DR completa executada com Ewerton e Jeff em uma sessão de 2–3h, documento de post-mortem da simulação produzido.
- Tag `v1.0.0` gerada após T18 verde, GitHub Release publicada com binário, source maps, CHANGELOG, e link para documentação.

---

## Atualização do STATUS.md

Adicione estas três linhas na tabela do `STATUS.md`, **após** a linha da T15:

```md
| T16 | Hardening de segurança e LGPD (audit log, 2FA, criptografia, brute force, headers, OWASP, endpoints LGPD) | pendente | — | — | — |
| T17 | Observabilidade e notificações internas (Sentry, Prometheus, Grafana, OpenTelemetry, logs estruturados, sistema de notificações in-app) | pendente | — | — | — |
| T18 | Backup, Disaster Recovery e Runbooks (pg_dump automatizado, restore validado, ADRs, runbooks operacionais, simulação DR) | pendente | — | — | — |
```

---

## Recalibração de esforço e cobrança

Atualizar a estimativa do MVP 1.0:

| Onda | Antes | Depois | Diferença |
|------|-------|--------|-----------|
| MVP 1.0 (T00–T15) | ~660h | ~660h | — |
| **T16 — Hardening segurança e LGPD** | — | **+80h** | novo |
| **T17 — Observabilidade e notificações** | — | **+60h** | novo |
| **T18 — Backup, DR e runbooks** | — | **+40h** | novo |
| **MVP 1.0 atualizado (T00–T18)** | ~660h | **~840h** | +180h |

**Cobrança recalibrada (faixa mediana, R$ 200/h):**

- MVP 1.0 anterior: R$ 132.000
- MVP 1.0 com hardening: **R$ 168.000** (+R$ 36.000)
- MVP 1.1 (NF-e): R$ 24.000
- MVP 1.2 (canais): R$ 60.000
- V2 (mobile): R$ 30.000
- **Total atualizado: R$ 282.000**

Note que a faixa mediana original era R$ 318k–405k para o pacote completo. Com a recalibração, o MVP 1.0 atualizado fica em R$ 168k (vs R$ 180–220k da projeção original) — abaixo do range esperado, **a favor do cliente**, porque a estimativa anterior já tinha gordura de risco. Vale apresentar ao cliente como "investimento extra de R$ 36k em hardening que protege contra risco regulatório (LGPD), perda de dados (DR) e degradação silenciosa (observabilidade)".

---

**Fim do adendo.** Após colar no documento principal, próxima ação é atualizar `STATUS.md` e iniciar a T00 normalmente — o protocolo da seção 0 do prompt original assume o controle a partir daí.
