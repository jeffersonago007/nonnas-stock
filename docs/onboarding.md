# Onboarding — Nonnas Stock

Guia para um dev novo entrar no projeto e ser produtivo em < 1 dia (master doc T15).

## 1. Pré-requisitos (instalar uma vez)

- **Java 21** (JDK). Recomendamos o JBR do Android Studio se já estiver instalado, ou Eclipse Temurin 21.
- **Maven** — usar o wrapper versionado (`./mvnw`), não o Maven do sistema.
- **Node.js 20+** + npm.
- **Docker** + Docker Compose v2 (necessário para Postgres em ITs específicos; o build padrão usa Zonky e dispensa).
- **Git** com SSH configurado pro GitHub.

Validação rápida:
```bash
java --version    # 21.x
node --version    # 20.x ou 22.x
./mvnw --version  # 3.9.9
```

## 2. Clone e build inicial

```bash
git clone git@github.com:jeffersonago007/nonnas-stock.git
cd nonnas-stock
./mvnw -T 1C verify           # 4-5 min, banco Zonky efêmero
cd frontend && npm ci && npm run build
```

`mvn verify` precisa estar verde antes de fazer qualquer mudança.

## 3. Rodar a aplicação localmente

**Terminal 1 — Backend:**
```bash
./mvnw -pl app spring-boot:run
# http://localhost:8080
# AdminBootstrap cria admin@nonnas.com / AdminNonnas2026! no primeiro start
```

**Terminal 2 — Frontend:**
```bash
cd frontend
npm run dev
# http://localhost:5173
# Vite proxy /api → :8080
```

Login: `admin@nonnas.com` / `AdminNonnas2026!`

## 4. Estrutura do repo

```
nonnas-stock/
├── shared-kernel/           # Domain primitives (zero deps Spring)
├── web-commons/             # GlobalExceptionHandler + RFC 7807
├── identity/                # Auth, empresa, filial, usuário
├── catalog/                 # Insumo, fornecedor, categoria, unidade
├── inventory-core/          # Saldos materializados
├── recipes/                 # Produto vendável + ficha técnica
├── operations/              # Movimentação, transferência, carga inicial
├── alerts/                  # Configs + disparados
├── reporting/               # MVs (curva ABC, ruptura, vencimento)
├── sales-channels-api/      # Stub para integração com canais (T17+)
├── nfe-importer/            # Stub para NF-e (T17+)
├── quality-tests/           # ArchUnit + JaCoCo aggregate
├── app/                     # Spring Boot main + actuator + OpenAPI
├── e2e/                     # Playwright Java smoke (T15)
├── frontend/                # Vite + React 18 + Tailwind + shadcn
└── docs/                    # ADRs + deployment + runbook + este
```

ADRs em `docs/adr/` — leia 0001 (modular monolith), 0007 (Zonky) e 0010 (reporting cross-context) antes de começar.

## 5. Convenções

**Commits**: Conventional Commits (`feat(modulo): ...`, `fix(modulo): ...`, `chore: ...`). Semantic-release usa isso pra calcular versão.

**Branches**: `main` é protegida, sempre via PR. Não force-push.

**PRs**: usam template em `.github/pull_request_template.md`. Cobertura mínima checada por workflow `pr.yml`: 85% domain, 75% application.

**Padrão de módulo**:
- `domain/` — entidades + VOs (zero deps frameworks).
- `application/` — use cases + ports.
- `infrastructure/` — adapters JPA, integrações externas.
- `interfaces/rest/` — controllers + DTOs.

ArchUnit em `quality-tests/` valida camadas — quebra de regra falha o build.

**Testes**:
- Unitário (`*Test.java`) — sem Spring, sem banco. Cobertura mínima rigorosa.
- Integração (`*IT.java`) — `AbstractIntegrationTest` do shared-kernel test-jar; Zonky Postgres efêmero. Roda no `verify` (failsafe).
- Smoke E2E (`e2e/`) — Playwright Java, profile `e2e`, executa contra app + frontend rodando.

## 6. Comandos do dia-a-dia

```bash
# Build rápido + testes unitários (~30s)
./mvnw test

# Build completo com ITs (~5min)
./mvnw -T 1C verify

# Apenas um módulo
./mvnw -pl operations -am test

# Frontend
cd frontend
npm run dev          # dev server
npm test             # vitest
npm run lint         # eslint
npm run build        # tsc + vite build

# E2E (precisa backend + frontend rodando)
./mvnw -pl e2e test -Pe2e
```

## 7. Onde achar o quê

| Pergunta                                          | Resposta                                                       |
|---------------------------------------------------|----------------------------------------------------------------|
| Qual o status atual das tarefas T01–T18?          | `STATUS.md`                                                    |
| Quais decisões arquiteturais foram tomadas e por? | `docs/adr/`                                                    |
| Qual o roadmap completo do projeto?               | `PROMPT_CLAUDE_CODE.md` (master doc — fonte de verdade)        |
| Como deployar em produção?                        | `docs/deployment.md`                                           |
| Banco lento, alerta falso, rollback de migration? | `docs/operations-runbook.md`                                   |
| Esquema das migrations?                           | `<modulo>/src/main/resources/db/migration/`                    |
| OpenAPI da API?                                   | `http://localhost:8080/swagger-ui/index.html` (com app rodando) |

## 8. Quem chamar

- **Jefferson Agostinho** — BA/QA, coordenação geral.
- **Ewerton (revisor sênior Java)** — code review final em PRs grandes.
- Bugs/feedback: GitHub Issues no repo.

## 9. Primeiro PR sugerido

Para começar com pé direito, pegue uma issue tagged `good first issue` (TODO criar essas tags no repo) ou:
- Adicione um teste unitário cobrindo um caso de borda em algum domain.
- Melhore uma mensagem de erro RFC 7807 de algum endpoint.
- Atualize uma ADR com info nova que descobriu.

Mantenha PR ≤ 400 linhas e commit message Conventional. Bem-vindo!
