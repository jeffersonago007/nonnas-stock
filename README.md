# Nonnas Stock

Sistema profissional de controle de estoque centralizado para a rede **Nonnas Paola** (churrascaria e pizzaria, multi-filial em São Paulo, multi-canal de venda).

> Documento mestre de desenvolvimento: [`PROMPT_CLAUDE_CODE.md`](PROMPT_CLAUDE_CODE.md)
> Estado das tarefas: [`STATUS.md`](STATUS.md)

---

## Stack

- **Backend:** Java 21 · Spring Boot 3.3.5 · Maven multi-módulo · PostgreSQL 16 · Flyway · JPA/Hibernate 6 · JWT
- **Testes:** JUnit 5 · Mockito · AssertJ · Testcontainers · ArchUnit · JaCoCo
- **Frontend (T12+):** React 18 · Vite · TypeScript · Tailwind · shadcn/ui · TanStack Query
- **Infra:** Docker · GitHub Actions

## Estrutura

```
nonnas-stock/
├── shared-kernel/           value objects, exceções base
├── identity/                empresa, filial, usuário, JWT
├── catalog/                 insumos, unidades, conversões
├── inventory-core/          lote, saldo, movimentação, FEFO
├── recipes/                 ficha técnica versionada
├── operations/              transferência, ajuste, carga inicial
├── alerts/                  alertas configuráveis
├── reporting/               dashboards e relatórios
├── sales-channels-api/      contratos para canais (MVP 1.2)
├── nfe-importer/            importador de NF-e (MVP 1.1)
├── quality-tests/           ArchUnit + JaCoCo agregado
├── app/                     composição: main, configs, migrations
└── frontend/                React + Vite (T12+)
```

## Pré-requisitos

- **Java 21** (Temurin recomendado)
- **Docker Desktop** (para Postgres local)
- **Node.js 20+** (apenas a partir da T12)

> Maven não precisa estar instalado: o projeto usa o **Maven Wrapper** (`./mvnw` / `mvnw.cmd`) e baixa o Maven 3.9.9 sob demanda na primeira execução.

## Setup local

```bash
# 1. Subir o Postgres
make up                       # ou: .\tasks.ps1 up   (Windows)

# 2. Validar build (primeira execução baixa o Maven 3.9.9)
make verify                   # ou: .\tasks.ps1 verify
# Equivalente direto: ./mvnw verify  (Linux/Mac)  ou  .\mvnw.cmd verify  (Windows)

# 3. Rodar a aplicação (após T09)
make run                      # ou: .\tasks.ps1 run
```

### Variáveis de ambiente

| Variável            | Default       | Uso                         |
|---------------------|---------------|-----------------------------|
| `POSTGRES_PASSWORD` | `nonnas_dev`  | Senha do Postgres (compose) |

## Comandos úteis

| Make           | PowerShell                | Descrição                        |
|----------------|---------------------------|----------------------------------|
| `make up`      | `.\tasks.ps1 up`          | Sobe Postgres                    |
| `make down`    | `.\tasks.ps1 down`        | Para Postgres                    |
| `make logs`    | `.\tasks.ps1 logs`        | Tail de logs                     |
| `make test`    | `.\tasks.ps1 test`        | Apenas testes unitários          |
| `make verify`  | `.\tasks.ps1 verify`      | Build + integração + ArchUnit    |
| `make run`     | `.\tasks.ps1 run`         | Sobe Spring Boot (módulo `app`)  |
| `make clean`   | `.\tasks.ps1 clean`       | Limpa artefatos                  |
| `make rebuild` | `.\tasks.ps1 rebuild`     | clean + verify                   |

## Convenções

- **Commits:** Conventional Commits, escopo = nome do módulo (`feat(catalog): adiciona entidade Insumo`).
- **Branches:** `main` protegida; trabalho em `feat/T03-...`, `fix/T05-...`.
- **Testes:** Postgres real via Testcontainers — **nunca H2**.
- **Idioma:** logs e código em inglês; UI, validações e mensagens ao usuário em pt-BR.

## Tarefas e roadmap

Ver [`STATUS.md`](STATUS.md) para o estado atual e [`PROMPT_CLAUDE_CODE.md`](PROMPT_CLAUDE_CODE.md) seção 10 para o detalhamento de cada tarefa T00–T15.

## Licença

Privado — Nonnas Paola.
