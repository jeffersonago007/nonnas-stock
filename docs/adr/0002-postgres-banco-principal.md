# ADR 0002 — PostgreSQL 16 como banco principal

- **Status:** Aceita
- **Data:** 2026-05-08
- **Contexto da decisão:** Adendo seção 14.6, T00 backfill

## Contexto

O sistema precisa de:
- Transações ACID rigorosas (saldo materializado precisa convergir com soma de movimentações).
- Consultas relacionais ricas (FEFO ordena lotes por validade joinando com saldo; relatórios fazem agregações por filial/categoria/período).
- Tipos especializados: `uuid` nativo (chaves primárias), `jsonb` (metadata de notificações, dados antes/depois em audit log), `inet` (IP em audit log), `tsrange` (futura indisponibilidade de lote).
- Extensões para criptografia (`pgcrypto`) e busca textual (eventual `pg_trgm`).
- Replicação física para backup off-site e ponto-no-tempo via WAL archiving.

Alternativas consideradas: MySQL 8 (familiar à equipe Java mas tipos relacionais menos ricos, JSON menos performático), Oracle (custo proibitivo no orçamento), MongoDB (modelo documental não casa com integridade transacional do estoque).

## Decisão

**PostgreSQL 16** como banco transacional único, gerenciado via:
- **Flyway** para versionamento de schema (migrations imutáveis em `app/src/main/resources/db/migration/`).
- **Spring Data JPA + Hibernate 6** como camada de persistência.
- **Testcontainers** com Postgres real em todos os testes que tocam banco — **nunca H2 ou outro substituto in-memory**, conforme regra invariante da seção 0 do master doc.
- **HikariCP** como pool de conexões.

Versão fixada em 16-alpine no `docker-compose.yml` (dev) e na configuração de produção. Upgrades para versões maiores acontecem como ADR independente.

## Consequências

**Positivas:**
- Modelo de domínio mapeia diretamente com integridade referencial e CHECK constraints.
- Transações garantem que `Movimentacao` + `ItemMovimentacao` + atualização de `SaldoLote` materializado são atômicas — sem janela onde o saldo está inconsistente.
- Custos baixos: PostgreSQL é open-source, hospedável em qualquer cloud por R$ 30–100/mês para o porte do cliente.
- Habilidades amplamente disponíveis no mercado brasileiro.

**Negativas:**
- Single point of failure no MVP 1.0 (não há réplica). RTO de 4h via restore do backup off-site (T18).
- Operações em massa (carga inicial > 50k linhas) exigem batching cuidadoso para não estourar lock.

**Mitigações:**
- Backup automatizado off-site com teste de restore mensal (T18).
- Health checks de latência e migrations aplicadas (T17).
- Réplica read-only opcional na onda 1.2, se relatórios começarem a competir com operação.

## Referências

- PROMPT_CLAUDE_CODE.md seção 2.1 (stack backend), seção 6.2 (convenções SQL), seção 14.1 (backup).
