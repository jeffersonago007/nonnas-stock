# ADR 0001 — Modular Monolith como estilo arquitetural

- **Status:** Aceita
- **Data:** 2026-05-08
- **Contexto da decisão:** Adendo seção 14.6, T00 backfill

## Contexto

Estamos construindo um sistema de controle de estoque multi-filial, multi-canal, com aproximadamente 8 contextos de negócio bem delimitados (identity, catalog, inventory-core, recipes, operations, alerts, reporting, sales-channels-api). O cliente é uma rede de restaurantes de porte médio em São Paulo, sem necessidade imediata de escala horizontal massiva. A equipe de desenvolvimento é pequena (1 dev sênior + 1 BA/QA). O orçamento é limitado e o time-to-market importa.

Três alternativas reais foram consideradas: monolito tradicional (uma camada por tipo), microsserviços (um deployable por contexto) e modular monolith (um deployable, contextos isolados como módulos Maven com camadas internas).

## Decisão

Adotamos **Modular Monolith**. Cada bounded context é um módulo Maven separado com camadas internas próprias (`domain`, `application`, `infrastructure`, `interfaces/rest`, `api`). Acoplamento entre módulos só por contratos públicos (pacote `api`) ou eventos de aplicação Spring síncronos (mesma transação).

Regras estruturais aplicadas via ArchUnit (T10):
- Domain de qualquer módulo não depende de Spring, JPA ou outros módulos.
- Application só depende de Domain e API públicas.
- Entidades JPA só vivem em `infrastructure.persistence`.
- App é o único módulo que depende de todos os outros.

## Consequências

**Positivas:**
- Custo operacional baixo: um Postgres, um deployable, um pipeline.
- Refactoring entre contextos é seguro (compilador valida assinaturas).
- ArchUnit garante que a fronteira do módulo não vaza com o tempo.
- Migração para microsserviços, se um dia justificar, é incremental: extrair um módulo bem-isolado custa muito menos que partir de monolito tradicional.

**Negativas:**
- Sem isolamento de falha: bug em alerts pode derrubar inventory-core.
- Escala vertical apenas: limite eventual no servidor único.
- Disciplina exigida no review: contornar a fronteira do módulo é fácil em código.

**Mitigações:**
- Health checks profundos por componente (T17) detectam degradação isolada.
- Backup + DR plan (T18) cobrem o cenário de "monolito caiu".
- ArchUnit é executado em todo PR, falha se a fronteira é violada.

## Referências

- PROMPT_CLAUDE_CODE.md seção 1.3 (princípios arquiteturais inegociáveis), seção 3 (estrutura de pastas), seção 7.4 (regras ArchUnit).
- Sam Newman, *Monolith to Microservices* — capítulo "When to use modular monoliths".
