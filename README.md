# 🍅 Nonnas Stock

Sistema profissional de controle de estoque centralizado para a rede **Nonnas Paola** — churrascaria e pizzaria, multi-filial em São Paulo, multi-canal de venda (salão, iFood, Keeta, 99Food).

![Java](https://img.shields.io/badge/Java-21_LTS-007396?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.5-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791?logo=postgresql&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-multi--módulo-C71A36?logo=apachemaven&logoColor=white)
![Status](https://img.shields.io/badge/MVP_1.0-em_desenvolvimento-orange)
![License](https://img.shields.io/badge/license-Privado-red)

## Visão geral

Sistema centralizado que cobre os gaps operacionais do controle de estoque tradicional em rede de restaurantes:

- **Saldo separado por filial** (multi-tenant literal — cada loja vê seu estoque)
- **Rastreabilidade fina por lote** com data de fabricação e validade
- **FEFO automático** (First Expired, First Out) — sem perder mussarela na geladeira
- **Ficha técnica versionada** — alterações de receita não reescrevem o histórico de vendas
- **Alertas configuráveis** — estoque mínimo, vencimento próximo, ruptura
- **Transferência orquestrada** entre filiais com workflow de estados
- **API REST** documentada (OpenAPI/Swagger)
- **Frontend web** com identidade visual da marca (a partir de T12)

## Stack

| Camada | Tecnologia |
|---|---|
| Backend | Java 21 · Spring Boot 3.3.5 · Maven multi-módulo · PostgreSQL 16 · Flyway · JPA/Hibernate 6 · Spring Security · JWT (jjwt 0.12) |
| Testes | JUnit 5 · Mockito · AssertJ · Embedded Postgres (Zonky) · ArchUnit · JaCoCo · MockMvc |
| Frontend (T12+) | React 18 · Vite · TypeScript · Tailwind · shadcn/ui · TanStack Query |
| Infra | Docker (opcional) · GitHub Actions · Maven Wrapper |

## Arquitetura

**Modular Monolith** com bounded contexts isolados — um deployable, contextos comunicam via contratos públicos ou eventos de aplicação. ArchUnit valida a fronteira em todo build.

```
nonnas-stock/
├── shared-kernel/        Money, Quantity, EntityId, exceções base, Result
├── identity/             Empresa, Filial, Usuário, JWT com refresh rotation, brute force
├── catalog/              Insumo, Categoria, UnidadeMedida, Conversão, Fornecedor
├── inventory-core/       Lote, SaldoLote, Movimentação imutável, FEFO  ← núcleo
├── recipes/              ProdutoVendavel, FichaTecnica versionada
├── operations/           Transferência, Carga inicial, Ajuste
├── alerts/               AlertaConfig, avaliação reativa
├── reporting/            Dashboards e queries materializadas
├── sales-channels-api/   Contratos para iFood/Keeta/99Food (MVP 1.2)
├── nfe-importer/         Parser NF-e (MVP 1.1)
├── quality-tests/        ArchUnit transversal + JaCoCo agregado
├── app/                  Composição: main, configs, migrations Flyway unificadas
├── docs/
│   ├── adr/              Architecture Decision Records (0001+)
│   └── domain-model.md   Modelo de domínio incremental
└── frontend/             React + Vite (criado em T12)
```

Detalhes em [`docs/domain-model.md`](docs/domain-model.md) e [`docs/adr/`](docs/adr/).

## Status

| Tarefa | Módulo | Status |
|---|---|---|
| T00 | Fundação do repositório | ✅ |
| T01 | shared-kernel | ✅ |
| T02 | identity (Auth JWT, refresh rotation, brute force) | ✅ |
| T03 | catalog (ConversorUnidadeService) | ✅ |
| T04 | inventory-core (FEFO + saldo materializado) | ✅ |
| T05–T15 | recipes, operations, alerts, reporting, app, CI/CD, frontend | 🔜 |
| T16–T18 | hardening (LGPD, observability, DR) | 🔜 |

Roadmap completo: [`STATUS.md`](STATUS.md). Detalhamento de cada tarefa: [`PROMPT_CLAUDE_CODE.md`](PROMPT_CLAUDE_CODE.md) seção 10.

## Setup local

### Pré-requisitos

- **Java 21** (Temurin recomendado, ou JBR do Android Studio)
- **Docker Desktop** opcional — testes usam Postgres embedded (Zonky), produção usa Postgres standalone

> Maven não precisa estar instalado. O projeto usa **Maven Wrapper** (`./mvnw` / `mvnw.cmd`) que baixa Maven 3.9.9 sob demanda.

### Comandos

```powershell
# Windows / PowerShell
.\tasks.ps1 verify       # build + unit + integração + ArchUnit
.\tasks.ps1 run          # sobe a aplicação (após T09)
.\tasks.ps1 up           # Postgres via docker-compose (opcional)
```

```bash
# Linux / macOS / Git Bash
make verify
make run
make up
```

Equivalentes diretos: `./mvnw verify`, `./mvnw -pl app spring-boot:run`.

| Target | Make | PowerShell |
|---|---|---|
| Build + tests | `make verify` | `.\tasks.ps1 verify` |
| Unit tests | `make test` | `.\tasks.ps1 test` |
| Sobe Postgres | `make up` | `.\tasks.ps1 up` |
| Roda app | `make run` | `.\tasks.ps1 run` |
| Limpa | `make clean` | `.\tasks.ps1 clean` |

## Convenções

- **Commits**: Conventional Commits com escopo de módulo (`feat(catalog): ...`, `fix(inventory-core): ...`).
- **Branches**: `main` é a linha estável; trabalho em `feat/T0X-...` e `fix/T0X-...`.
- **Testes**: Postgres real via embedded-postgres (Zonky) — **nunca H2** (regra invariante).
- **Idioma**: logs e identificadores técnicos em inglês; UI, validações e mensagens ao usuário em **pt-BR**.
- **Arquitetura**: ports em `application.ports`, adapters em `infrastructure.persistence`, domain puro (sem Spring/JPA).

## Documentação

- 📄 [`PROMPT_CLAUDE_CODE.md`](PROMPT_CLAUDE_CODE.md) — documento mestre (escopo, regras, tarefas)
- 📄 [`STATUS.md`](STATUS.md) — estado atual das tarefas com hashes de commit
- 📄 [`docs/domain-model.md`](docs/domain-model.md) — modelo de domínio por bounded context
- 📁 [`docs/adr/`](docs/adr/) — Architecture Decision Records

## Equipe

- **Jefferson Pacheco Agostinho** — BA/QA, condução do projeto
- **Edwagney Luz** — desenvolvedor sênior Java

## Licença

🔒 **Privado** — código proprietário do projeto Nonnas Paola. Distribuição restrita aos colaboradores autorizados.
