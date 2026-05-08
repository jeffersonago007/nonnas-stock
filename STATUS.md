# Status das Tarefas — Nonnas Stock

| Tarefa | Estado     | Data | Commit | Nota |
|--------|------------|------|--------|------|
| T00    | concluída  | 2026-05-08 | `dbe2173` | Monorepo Maven multi-módulo com 12 módulos placeholder, Maven Wrapper 3.3.2 + Maven 3.9.9, docker-compose Postgres 16, Makefile/tasks.ps1, CI esqueleto. `./mvnw validate` e `./mvnw test` verdes. |
| T01    | concluída  | 2026-05-08 | `42fc27c` | shared-kernel: Money, Quantity, EntityId, Result sealed (Success/Failure), DomainException sealed (Validation/BusinessRule/NotFound), ErrorCode. 68 testes, cobertura 99% linhas / 94% branches. ArchUnit local valida zero deps Spring/JPA/Lombok/Date legado. |
| T02    | pendente   | —    | —      | — |
| T03    | pendente   | —    | —      | — |
| T04    | pendente   | —    | —      | — |
| T05    | pendente   | —    | —      | — |
| T06    | pendente   | —    | —      | — |
| T07    | pendente   | —    | —      | — |
| T08    | pendente   | —    | —      | — |
| T09    | pendente   | —    | —      | — |
| T10    | pendente   | —    | —      | — |
| T11    | pendente   | —    | —      | — |
| T12    | pendente   | —    | —      | — |
| T13    | pendente   | —    | —      | — |
| T14    | pendente   | —    | —      | — |
| T15    | pendente   | —    | —      | — |
| T16    | pendente   | —    | —      | Hardening de segurança e LGPD: audit log, 2FA TOTP, criptografia de campos, brute force progressivo, headers, OWASP, endpoints LGPD. |
| T17    | pendente   | —    | —      | Observabilidade e notificações internas: Sentry, Prometheus + Grafana, OpenTelemetry, logs estruturados em JSON, sistema de notificações in-app. |
| T18    | pendente   | —    | —      | Backup, Disaster Recovery e Runbooks: pg_dump automatizado off-site, restore validado, ADRs 0001–0010, runbooks operacionais, simulação DR. |

## Decisões de execução (ADRs)

Decisões arquiteturais e de sequenciamento ficam em `docs/adr/`. ADRs imutáveis após aprovadas.

| # | Título | Status |
|---|---|---|
| 0001 | Modular Monolith como estilo arquitetural | Aceita |
| 0002 | PostgreSQL 16 como banco principal | Aceita |
| 0003 | JWT com refresh token rotation (implementado em T02) | Aceita |
| 0004 | FEFO como estratégia de seleção de lote em saída | Aceita |
| 0005 | Versionamento de ficha técnica via snapshot | Aceita |
| 0006 | Sequenciamento pós-adendo (T15→rc.1, T18→v1.0.0, auth antecipada para T02) | Aceita |
| 0007–0010 | Pendentes (criadas em T18) | Pendente |

## Ordem de execução decidida

T01 → T02 (escopo expandido conforme ADR 0003) → T03 → T04 → T05 → T06 → T07 → T08 → T09 → T10 → T11 → T12 → T13 → T14 → **T15 (`v1.0.0-rc.1`)** → T16 → T17 → **T18 (`v1.0.0`)**.

ADR 0006 detalha o racional. T15 será deduplicada quando executada (itens redundantes com T16/T18 saem); T02 absorve refresh rotation + brute force + política de senha (originalmente em T16).
