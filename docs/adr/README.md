# Architecture Decision Records

Repositório de decisões arquiteturais do Nonnas Stock. Toda decisão relevante vira ADR antes (ou logo depois) da implementação.

**Formato:** arquivos numerados sequencialmente, imutáveis após aprovação. Mudanças geram nova ADR que supersedes a anterior, nunca edição retroativa.

**Estrutura padrão:** Status · Contexto · Decisão · Consequências.

## Índice

| # | Título | Status | Decidida em |
|---|---|---|---|
| [0001](0001-modular-monolith.md) | Modular Monolith como estilo arquitetural | Aceita | 2026-05-08 |
| [0002](0002-postgres-banco-principal.md) | PostgreSQL 16 como banco principal | Aceita | 2026-05-08 |
| [0003](0003-jwt-com-refresh-rotation.md) | JWT com refresh token rotation | Aceita | 2026-05-08 |
| [0004](0004-fefo-como-estrategia-de-saida.md) | FEFO como estratégia de seleção de lote em saída | Aceita | 2026-05-08 |
| [0005](0005-versionamento-de-receita-via-snapshot.md) | Versionamento de ficha técnica via snapshot na venda | Aceita | 2026-05-08 |
| [0006](0006-sequenciamento-pos-adendo.md) | Sequenciamento de execução pós-adendo (T16–T18) | Aceita | 2026-05-08 |
| [0007](0007-embedded-postgres-em-vez-de-testcontainers.md) | Embedded Postgres (Zonky) em vez de Testcontainers no MVP 1.0 | Aceita | 2026-05-08 |

ADRs 0008–0010 serão criadas em T18 (observability stack, deployment platform, secrets management) conforme necessidade. T10 reavalia se voltamos para Testcontainers.
