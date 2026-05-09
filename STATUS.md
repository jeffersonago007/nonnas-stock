# Status das Tarefas — Nonnas Stock

| Tarefa | Estado     | Data | Commit | Nota |
|--------|------------|------|--------|------|
| T00    | concluída  | 2026-05-08 | `dbe2173` | Monorepo Maven multi-módulo com 12 módulos placeholder, Maven Wrapper 3.3.2 + Maven 3.9.9, docker-compose Postgres 16, Makefile/tasks.ps1, CI esqueleto. `./mvnw validate` e `./mvnw test` verdes. |
| T01    | concluída  | 2026-05-08 | `42fc27c` | shared-kernel: Money, Quantity, EntityId, Result sealed (Success/Failure), DomainException sealed (Validation/BusinessRule/NotFound), ErrorCode. 68 testes, cobertura 99% linhas / 94% branches. ArchUnit local valida zero deps Spring/JPA/Lombok/Date legado. |
| T02    | concluída  | 2026-05-08 | `6e50311` | identity (Empresa, Filial, Usuário, Auth JWT). Refresh rotation com replay detection (ADR 0003), brute force progressivo (3→15min, 5→1h, 10→travada) e PoliticaSenhaValidator antecipados de T16 (ADR 0006 D). AdminBootstrap idempotente substitui V002 seed. Embedded Postgres via Zonky (ADR 0007) — sem Docker. 12 testes integração + ~30 unit, cobertura 89% linhas / 76% branches. |
| T03    | concluída  | 2026-05-08 | `a8149c1` | catalog (CategoriaInsumo, UnidadeMedida, ConversaoUnidade, Fornecedor, Insumo, InsumoFilial). `ConversorUnidadeService` domain-puro com cascata específica→global→inversa→erro (9 cenários cobertos no unit test). Seed V004 com unidades padrão e conversões globais KG→G, L→ML (inversa derivada automaticamente). `InsumoFilial.filial_id` sem FK física para identity.filiais (consolidação em T09). 5 IT + ~50 unit tests, cobertura 84% linhas / 80% branches. `docs/domain-model.md` criado. |
| T04    | concluída  | 2026-05-08 | `fd56360` | inventory-core (Lote, SaldoLote PK composta, Movimentacao imutável, ItemMovimentacao, FEFO). `SelecionarLotesPorFefoService` cobre 7 cenários (master doc exige 5+). Lock pessimista (PESSIMISTIC_WRITE) no path de saída. Saldo materializado por `@EventListener` Propagation.MANDATORY na mesma transação. Saldo negativo permitido com flag `gerouNegativo` (master doc 5.2). 5 IT + 18 unit tests, cobertura 85% linhas / 66% branches. |
| T05    | concluída  | 2026-05-08 | `bc4bffc` | recipes (ProdutoVendavel, FichaTecnica versionada, ItemFichaTecnica). 5 use cases — destaque `AtualizarFichaTecnica` que faz UPDATE+INSERT na mesma transação via `saveAndFlush` para não violar o partial unique index `uq_fichas_ativa_por_produto`. ADR 0008 formaliza dependência Maven `recipes → inventory-core`; `RegistrarVendaSimuladaUseCase` delega para novo `RegistrarSaidaMultiItemUseCase` em inventory-core (uma `Movimentacao SAIDA_VENDA` consolidada com N insumos via FEFO, atômica). Snapshot por referência: `documento_origem_tipo='FICHA_TECNICA'` + `documento_origem_id=fichaVigente.id`. 18 unit + 10 IT recipes (cobertura 90.2% linhas / 71.2% branches) + 3 IT em inventory-core. Reactor verify 13/13 SUCCESS em 02:20. |
| T06    | concluída  | 2026-05-08 | `d26f689` | operations (Transferencia state machine SOLICITADA→APROVADA→EM_TRANSITO→RECEBIDA/CANCELADA com cancel bloqueado após envio, ItemTransferencia, AjusteEstoque PENDENTE_APROVACAO/APROVADO/REJEITADO, CargaInicial idempotente via SHA-256). 8 use cases — destaque `RegistrarRecebimentoTransferencia` que cria AjusteEstoque APROVADO sem mov para auditoria de divergências, e `LancarAjusteManual` com threshold parametrizável (default 50, via `@ConfigurationProperties`) que separa criação direta de movimentação vs fila de aprovação. Importador planilha XLSX (Apache POI 5.3.0) + CSV (OpenCSV 5.9, detecta `,`/`;` por header) com schema fixo de 7 colunas. ADR 0009 formaliza padrão multi-item em inventory-core; novo `RegistrarEntradaMultiItemUseCase` espelha o de saída de T05. Endpoint `GET /api/v1/transferencias/em-transito` agrega qtd por insumo. 32 unit + 9 IT recipes (cobertura 73.7% linhas / 56.2% branches) + 4 IT em inventory-core. Reactor verify 13/13 SUCCESS. |
| T07    | concluída  | 2026-05-08 | `78df4c2` | alerts (AlertaConfig, AlertaDisparado, AvaliadorAlertasService). 4 tipos de alerta (PERCENTUAL/ABSOLUTO/VENCIMENTO/RUPTURA) com escopo flexível e match "mais específico primeiro" + desempate por prioridade. Threshold polimórfico em coluna única — semântica encodada no tipo, validação no domínio. `MovimentacaoAlertaListener` com `@TransactionalEventListener(AFTER_COMMIT)`+`REQUIRES_NEW`: alerts é observabilidade, falhas não rolam back movimentação. `VencimentoScheduledJob` cron diário 06:00 BRT, idempotente via partial unique index. Auto-resolução: saldo voltando ao normal resolve estoque ativo; lote zerado resolve vencimento. 5 use cases. Estendeu inventory-core (`findLotesVencendoComSaldoAte`) e catalog (`findByInsumoEFilial`). Primeiro módulo a depender de 2 bounded contexts simultâneos — `alertsClock @Primary` resolve ambiguidade Clock. 21 unit + 6 IT (cobertura 78.3% linhas / 64.0% branches). Reactor verify 13/13 SUCCESS. |
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
