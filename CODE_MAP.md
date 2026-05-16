# Code Map — Nonnas Stock

Índice navegável do codebase. **Antes de explorar com Grep/Glob, consulte aqui primeiro.**
Estado: pós-T20 + T-LOT-09 (MVP 1.0 em produção interna) + T-CANAL-00..05 (MVP 1.2 canais completo — fundação + adapter genérico + pipeline de baixa de estoque + CRUD APIs + UI + scheduler ativo de processamento).

- [Arquitetura em uma página](#arquitetura-em-uma-página)
- [Bounded contexts (backend)](#bounded-contexts-backend)
- [Módulos transversais](#módulos-transversais)
- [Frontend](#frontend)
- [Migrations Flyway](#migrations-flyway)
- [Convenções: onde colocar X](#convenções-onde-colocar-x)
- [Convenções de UX (frontend)](#convenções-de-ux-frontend)
- [Scripts operacionais](#scripts-operacionais)
- [Perguntas frequentes ("onde fica Y?")](#perguntas-frequentes)
- [Referências cruzadas](#referências-cruzadas)

---

## Arquitetura em uma página

**Modular monolith** (ADR 0001). 7 bounded contexts em Java/Spring Boot 3.5.12 + Postgres 16 + frontend Vite/React/TS/Tailwind/shadcn. Schema único `public` (não `catalog.*` / `identity.*`). Comunicação inter-módulo: dependência Maven direta (ex.: `recipes → inventory-core`, ADR 0008) **ou** evento via `shared-kernel/events/*` (ex.: `AlertaDisparadoEvent`) **ou** SQL nativo cross-context (ex.: reporting, ADR 0010).

**Pilha de testes**: Zonky embedded Postgres (ADR 0007/0011) + ArchUnit em `quality-tests/` + Playwright E2E em `e2e/`.

Para o "porquê" de cada decisão, ver [docs/adr/](docs/adr/).

---

## Bounded contexts (backend)

Cada módulo segue layout idêntico (memória `feedback_module_patterns.md`):
```
<modulo>/src/main/java/com/nonnas/<modulo>/
├── domain/          ← entidades, value objects, regras puras
├── application/     ← use cases + ports (interfaces de repo)
└── infrastructure/  ← JPA entities, mappers, listeners, configs
└── interfaces/rest/ ← controllers + DTOs
```

| Módulo | Responsabilidade | Entry points REST | Conceitos centrais |
|---|---|---|---|
| [identity/](identity/) | Empresa, Filial, Usuário, Auth JWT, 2FA, LGPD, notificações internas | [AuthController](identity/src/main/java/com/nonnas/identity/interfaces/rest/AuthController.java), [FilialController](identity/src/main/java/com/nonnas/identity/interfaces/rest/FilialController.java), [UsuarioController](identity/src/main/java/com/nonnas/identity/interfaces/rest/UsuarioController.java), [EmpresaController](identity/src/main/java/com/nonnas/identity/interfaces/rest/EmpresaController.java), [NotificacaoController](identity/src/main/java/com/nonnas/identity/interfaces/rest/NotificacaoController.java), [TwoFaController](identity/src/main/java/com/nonnas/identity/interfaces/rest/TwoFaController.java), [LgpdController](identity/src/main/java/com/nonnas/identity/interfaces/rest/LgpdController.java) | `Usuario`, `Perfil` (ADMIN/GERENTE/OPERADOR/CONSULTA), `Filial`, `RefreshToken`, `AlertaDisparadoListener` |
| [catalog/](catalog/) | Insumo, Categoria, Unidade de medida, Conversão, Fornecedor | [InsumoController](catalog/src/main/java/com/nonnas/catalog/interfaces/rest/InsumoController.java), [CategoriaInsumoController](catalog/src/main/java/com/nonnas/catalog/interfaces/rest/CategoriaInsumoController.java), [UnidadeMedidaController](catalog/src/main/java/com/nonnas/catalog/interfaces/rest/UnidadeMedidaController.java), [FornecedorController](catalog/src/main/java/com/nonnas/catalog/interfaces/rest/FornecedorController.java), [ConversaoUnidadeController](catalog/src/main/java/com/nonnas/catalog/interfaces/rest/ConversaoUnidadeController.java) | `Insumo` (com flag `controlaValidade` + `diasAlertaVencimento`), `UnidadeMedida`, `ConversorUnidadeService` (cascata) |
| [inventory-core/](inventory-core/) | Lote, SaldoLote, Movimentação (FEFO + agregador) | [SaldoController](inventory-core/src/main/java/com/nonnas/inventory/interfaces/rest/SaldoController.java), [MovimentacaoController](inventory-core/src/main/java/com/nonnas/inventory/interfaces/rest/MovimentacaoController.java) | `Lote` (RASTREADO/AGREGADOR — ADR 0014), `SaldoLote` (PK composta), `Movimentacao` (imutável), `SelecionarLotesParaSaidaService` (polimórfico FEFO/agregador) |
| [recipes/](recipes/) | Produto vendável (FABRICADO/REVENDA), Ficha técnica versionada, Venda simulada, Cardápio unificado | [ProdutoVendavelController](recipes/src/main/java/com/nonnas/recipes/interfaces/rest/ProdutoVendavelController.java), [FichaTecnicaController](recipes/src/main/java/com/nonnas/recipes/interfaces/rest/FichaTecnicaController.java), [VendaSimuladaController](recipes/src/main/java/com/nonnas/recipes/interfaces/rest/VendaSimuladaController.java), [CardapioController](recipes/src/main/java/com/nonnas/recipes/interfaces/rest/CardapioController.java) | `ProdutoVendavel` com `tipo` FABRICADO/REVENDA (ADR 0015), `FichaTecnica` (versionada via partial unique index), `PreviewVendaSimuladaUseCase` (T-LOT-06) bifurca por tipo, `ListarCardapioUseCase` + `VenderInsumoOrfaoUseCase` (auto-promove insumo a REVENDA na 1ª venda) |
| [operations/](operations/) | Transferência (state machine), Ajuste, Carga inicial, Nota fiscal | [TransferenciaController](operations/src/main/java/com/nonnas/operations/interfaces/rest/TransferenciaController.java), [AjusteEstoqueController](operations/src/main/java/com/nonnas/operations/interfaces/rest/AjusteEstoqueController.java), [CargaInicialController](operations/src/main/java/com/nonnas/operations/interfaces/rest/CargaInicialController.java) | `Transferencia` (5 transições), `NotaFiscal` (persistida aqui; parseamento em nfe-importer), `FornecedorInsumoDePara` |
| [alerts/](alerts/) | Avaliador de alertas (estoque + vencimento) | [AlertaConfigController](alerts/src/main/java/com/nonnas/alerts/interfaces/rest/AlertaConfigController.java), [AlertaDisparadoController](alerts/src/main/java/com/nonnas/alerts/interfaces/rest/AlertaDisparadoController.java) | `AlertaConfig` (escopo flexível), `AlertaDisparado`, `AvaliadorAlertasService`, `VencimentoScheduledJob` (cron 06h BRT + endpoint manual `POST /alertas-disparados/avaliar-vencimentos`) |
| [reporting/](reporting/) | 6 relatórios read-only via SQL nativo cross-schema | [RelatoriosController](reporting/src/main/java/com/nonnas/reporting/interfaces/rest/RelatoriosController.java) | `RelatorioQueriesJdbc` (ADR 0010). `mv_curva_abc` materializada (90d rolling, refresh cron 30min). `mv_ruptura_iminente` virou VIEW comum em V023 → tempo real. |
| [sales-channels-api/](sales-channels-api/) | **MVP 1.2** — canais de venda (iFood/99Food/Keeta) via padrão Open Delivery (ADR 0016). T-CANAL-00..05 entregou: persistência + domain + contrato canônico + adapter genérico com polling + pipeline de baixa de estoque + de-para `externalCode→ProdutoVendavel` + 4 controllers CRUD + UI completa + scheduler ativo de processamento. | [CredencialCanalController](sales-channels-api/src/main/java/com/nonnas/saleschannels/interfaces/rest/CredencialCanalController.java), [CanalProdutoDeParaController](sales-channels-api/src/main/java/com/nonnas/saleschannels/interfaces/rest/CanalProdutoDeParaController.java), [PedidoCanalController](sales-channels-api/src/main/java/com/nonnas/saleschannels/interfaces/rest/PedidoCanalController.java), [CanalPollingController](sales-channels-api/src/main/java/com/nonnas/saleschannels/interfaces/rest/CanalPollingController.java) | `CanalTipo`, `PedidoCanal` (state machine RECEBIDO→CONFIRMADO_ESTOQUE→CONCLUIDO), `CredencialCanal` (segredo cifrado via [`SegredoCifrador`](sales-channels-api/src/main/java/com/nonnas/saleschannels/application/ports/SegredoCifrador.java) — placeholder Base64; AES-256-GCM via T16 CryptoService quando credencial real chegar), `EventoCanal` (idempotência por `event_id_externo`), `CanalProdutoDePara` (lookup global vs filial-específico), `PedidoVendaCanonico` subset Open Delivery v1.0.1 em [application/opendelivery/](sales-channels-api/src/main/java/com/nonnas/saleschannels/application/opendelivery/), [`CanalAdapter`](sales-channels-api/src/main/java/com/nonnas/saleschannels/application/ports/CanalAdapter.java) port (polling + ack + state actions), [`OpenDeliveryAdapter`](sales-channels-api/src/main/java/com/nonnas/saleschannels/infrastructure/adapter/opendelivery/OpenDeliveryAdapter.java) impl genérica, [`CanalPollingScheduler`](sales-channels-api/src/main/java/com/nonnas/saleschannels/infrastructure/schedule/CanalPollingScheduler.java) (consome eventos do canal), [`ProcessarPedidosScheduler`](sales-channels-api/src/main/java/com/nonnas/saleschannels/infrastructure/schedule/ProcessarPedidosScheduler.java) (baixa estoque + confirma no canal), [`ProcessarPedidoCanalUseCase`](sales-channels-api/src/main/java/com/nonnas/saleschannels/application/ProcessarPedidoCanalUseCase.java) |

**Regras de isolamento** (validadas por ArchUnit em `quality-tests/`):
- Nenhum bounded context importa classes Java de outro **exceto** as deps Maven declaradas em `pom.xml` (`recipes → inventory-core`, `operations → catalog + inventory-core`, `alerts → catalog + inventory-core`, `nfe-importer → catalog + operations`).
- `sales-channels-api` depende de recipes + inventory-core via Maven (libera tudo de Open Delivery → baixa de estoque); ArchUnit `salesChannels_soDependeDeRecipesEInventoryCore` bloqueia imports de identity/catalog/operations/alerts/reporting/nfeimporter (catalog é acessado **indiretamente** via recipes).
- Cross-context sem dep direta: SQL nativo via `NamedParameterJdbcTemplate` (padrão ADR 0010).

---

## Módulos transversais

| Módulo | Papel |
|---|---|
| [shared-kernel/](shared-kernel/) | `Money`, `Quantity`, `EntityId`, `Result` sealed, `DomainException` sealed. Events (`AlertaDisparadoEvent`). Distribui `AbstractIntegrationTest` + `Builders` via **test-jar** (ADR 0011). Zero deps de Spring/JPA. |
| [web-commons/](web-commons/) | `GlobalExceptionHandler` único (RFC 7807). Substitui handlers locais. |
| [app/](app/) | Agrega 7 bounded contexts + actuator + springdoc. `NonnasStockApplication`, `application*.yml` (defaults/dev/test/prod), `SecurityConfig`, `RateLimitFilter`, `MdcRequestFilter`, `SentryConfig`. **Único módulo executável** (`./mvnw -pl app spring-boot:run`). |
| [nfe-importer/](nfe-importer/) | Parser NF-e modelo 55 (DOM/XPath, XXE-blocked). [NotaFiscalController](nfe-importer/src/main/java/com/nonnas/nfeimporter/interfaces/rest/NotaFiscalController.java) com `/preview-xml` (enriquecido com `matchStatus` por item: DEPARA/COLISAO/LIVRE — ver [PreviewNotaFiscalUseCase](nfe-importer/src/main/java/com/nonnas/nfeimporter/application/PreviewNotaFiscalUseCase.java)), `/lancar`. Resolve insumo via de-para `(fornecedor, cProd)` primeiro; nunca reusa por código global. |
| ~~`sales-channels-api/` placeholder~~ | Promovido a bounded context na tabela acima (T-CANAL-00..02, 2026-05-15). |
| [quality-tests/](quality-tests/) | 13 regras ArchUnit + JaCoCo aggregate + perfil `nightly` com PIT mutation testing. |
| [e2e/](e2e/) | Playwright Java + Page Objects + `SmokeE2ETest` com 8+ cenários encadeados. Profile `e2e` (fora do `mvn verify` default). |

---

## Frontend

Vite 5 + React 18 + TS 5.6 + Tailwind 3.4 + shadcn/ui + Zustand + TanStack Query + react-router. Estrutura **por feature** em [frontend/src/features/](frontend/src/features/):

| Feature | Páginas principais |
|---|---|
| `auth/` | [LoginPage](frontend/src/features/auth/LoginPage.tsx), `store.ts` (Zustand) |
| `dashboard/` | [DashboardPage](frontend/src/features/dashboard/DashboardPage.tsx) (Recharts) |
| `cadastros/` | [FiliaisPage](frontend/src/features/cadastros/filiais/FiliaisPage.tsx) + [CargaInicialPage](frontend/src/features/cadastros/filiais/CargaInicialPage.tsx), [InsumosPage](frontend/src/features/cadastros/insumos/InsumosPage.tsx), [FornecedoresPage](frontend/src/features/cadastros/fornecedores/FornecedoresPage.tsx), [ProdutosPage](frontend/src/features/cadastros/produtos/ProdutosPage.tsx) (Cardápio) |
| `receitas/` | [FichasTecnicasPage](frontend/src/features/receitas/FichasTecnicasPage.tsx) |
| `operacoes/` | [EstoquePage](frontend/src/features/operacoes/EstoquePage.tsx), [MovimentacoesPage](frontend/src/features/operacoes/MovimentacoesPage.tsx), [TransferenciasPage](frontend/src/features/operacoes/TransferenciasPage.tsx), [NotasFiscaisPage](frontend/src/features/operacoes/notas-fiscais/NotasFiscaisPage.tsx) + [LancarNotaFiscalPage](frontend/src/features/operacoes/notas-fiscais/LancarNotaFiscalPage.tsx) |
| `vendas/` | [VendasPage](frontend/src/features/vendas/VendasPage.tsx) (POS MVP) |
| `alertas/` | [AlertasPage](frontend/src/features/alertas/AlertasPage.tsx) (tabs Disparados/Configurações) |
| `notificacoes/` | [NotificacoesPage](frontend/src/features/notificacoes/NotificacoesPage.tsx) + badge no header |
| `relatorios/` | [RelatoriosPage](frontend/src/features/relatorios/RelatoriosPage.tsx) (6 relatórios sobre endpoints T08) |
| `admin/` | [CategoriasPage](frontend/src/features/admin/categorias/CategoriasPage.tsx), [UnidadesPage](frontend/src/features/admin/unidades/UnidadesPage.tsx), [EmpresasPage](frontend/src/features/admin/empresas/EmpresasPage.tsx), [UsuariosPage](frontend/src/features/admin/usuarios/UsuariosPage.tsx) — gated por `RoleGuard` |
| `canais/` | [PedidosCanaisPage](frontend/src/features/canais/PedidosCanaisPage.tsx) (operacional, depende de filial global), [CredenciaisPage](frontend/src/features/canais/CredenciaisPage.tsx) (ADMIN), [DeparaPage](frontend/src/features/canais/DeparaPage.tsx) (ADMIN, com lookup inline de colisão), [DetalhesPedidoDialog](frontend/src/features/canais/DetalhesPedidoDialog.tsx). [api.ts](frontend/src/features/canais/api.ts) compartilhado por todas as 3 páginas |

**Infra do frontend**:
- [src/lib/api.ts](frontend/src/lib/api.ts) — Axios singleton, JWT injection, 401 → redirect
- [src/lib/tokenStorage.ts](frontend/src/lib/tokenStorage.ts) + [userStorage.ts](frontend/src/lib/userStorage.ts) — localStorage (TODO: migrar pra httpOnly cookie em onda futura)
- [src/lib/toastError.ts](frontend/src/lib/toastError.ts) — parser RFC 7807 + fallback Spring default
- [src/routes/AppRouter.tsx](frontend/src/routes/AppRouter.tsx) — `createBrowserRouter` + `ProtectedRoute` + `RoleGuard`

---

## Migrations Flyway

**Versão única no classpath consolidado**: módulos diferentes não podem ter o mesmo `Vnnn` (alerts importa catalog+inventory-core no classpath e o Flyway rejeita colisão). Por isso V015/V020 foram renumerados na história.

| V | Módulo | Conteúdo |
|---|---|---|
| V001 | identity | Empresa, Filial, Usuário, RefreshToken |
| V003 | catalog | Categoria, UnidadeMedida, Fornecedor, Insumo, InsumoFilial, ConversaoUnidade |
| V004 | catalog | Seed unidades (G/KG/ML/L/UN/CX/PORCAO) + conversões globais |
| V005 | inventory-core | Lote, SaldoLote (PK composta), Movimentacao + ItemMovimentacao |
| V006 | recipes | ProdutoVendavel, FichaTecnica, ItemFichaTecnica |
| V007 | operations | Transferencia, AjusteEstoque, CargaInicial |
| V008 | alerts | AlertaConfig, AlertaDisparado (+ partial unique index ATIVO) |
| V009 | reporting | Schema `reporting` + 2 MVs (curva ABC, ruptura iminente) |
| V010 | identity | T16 — audit_log, tokens_revogados, aceites_termos, usuarios_2fa |
| V011 | identity | T17 — notificacoes_usuario |
| V012 | identity | T18 — feature_flags |
| V013 | identity | Filiais permite CNPJ duplicado (filiais da mesma empresa) |
| V015 | catalog | Seed categoria "A classificar" (UUID determinístico) — usada por nfe-importer |
| V016 | operations | T20 — notas_fiscais, itens_nota_fiscal, fornecedor_insumo_depara |
| V017 | catalog | Normaliza nomes em uppercase |
| V018 | recipes | Normaliza produtos em uppercase |
| V019 | catalog | fornecedores_contatos |
| V020 | catalog | `insumos.dias_alerta_vencimento` (T-LOT-02) |
| V021 | inventory-core | `lotes.tipo` AGREGADOR/RASTREADO + unique partial (T-LOT-01..09, ADR 0014) |
| V022 | recipes | `produtos_vendaveis.tipo` FABRICADO/REVENDA + `insumo_revenda_id` (ADR 0015) |
| V023 | reporting | `mv_ruptura_iminente` virou VIEW comum (real-time, eliminando atraso de 30min no dashboard) |
| V024 | identity | T-RBAC-01 — `usuarios.filial_id` obrigatória pra não-ADMIN |
| V025 | sales-channels-api | T-CANAL-01 — `canais_credenciais` (partial unique `(canal, filial) WHERE ativa`) |
| V026 | sales-channels-api | T-CANAL-01 — `pedidos_canais` + `itens_pedido_canal` (UNIQUE `(canal, pedido_externo_id)` para idempotência; JSONB de payload canônico + bruto) |
| V027 | sales-channels-api | T-CANAL-01 — `eventos_canais` (UNIQUE `(canal, event_id_externo)` para idempotência de polling) |
| V028 | sales-channels-api | T-CANAL-04 — `canal_produto_depara` com 2 partial unique indexes: `uq_canal_produto_depara_global` (filial IS NULL) e `uq_canal_produto_depara_com_filial` (filial IS NOT NULL). Sem FK física pra produtos_vendaveis (ADR 0010 — validação em runtime) |

---

## Convenções: onde colocar X

- **Novo endpoint REST** → criar use case em `<modulo>/application/<area>/`, DTO em `<modulo>/interfaces/rest/dto/`, controller em `<modulo>/interfaces/rest/`. `@PreAuthorize` apropriado (PR template lembra). Validação no DTO via Bean Validation.
- **Nova migration** → `<modulo>/src/main/resources/db/migration/Vnnn__nome.sql` — sempre verificar [STATUS.md](STATUS.md) pra próxima versão disponível no **classpath inteiro**.
- **Novo evento de domínio** → record em `shared-kernel/src/main/java/com/nonnas/sharedkernel/events/`. Listener com `@TransactionalEventListener(AFTER_COMMIT)`.
- **Cross-context sem dep Maven** → SQL nativo via `NamedParameterJdbcTemplate` (padrão de [reporting/.../RelatorioQueriesJdbc.java](reporting/src/main/java/com/nonnas/reporting/infrastructure/persistence/RelatorioQueriesJdbc.java)).
- **Nova ADR** → `docs/adr/00XX-titulo.md`, atualizar tabela em [STATUS.md](STATUS.md).
- **Nova feature frontend** → `frontend/src/features/<area>/` com `api.ts` + `<X>Page.tsx` (+ `<X>FormDialog.tsx` se CRUD).
- **Novo runbook** → `docs/runbooks/<problema>.md`. PR template tem checklist de operação.

---

## Convenções de UX (frontend)

Regras transversais aplicadas em todas as telas (especialmente CRUDs):

| Convenção | Onde aplicar | Como implementar |
|---|---|---|
| **Inativos sempre ao fim das listagens** | Toda tabela com flag `ativo`/`ativa` (Usuários, Unidades, Categorias, Insumos/Produtos, Fornecedores, Cardápio, Filiais, Empresas) | `.sort((a,b) => a.ativo !== b.ativo ? (a.ativo ? -1 : 1) : <critério secundário>)` no `useMemo` que monta o array para `DataTable`. Critério secundário: `localeCompare(x, 'pt-BR', { sensitivity: 'base' })` para texto. Em Usuários, antes do nome vem o perfil (ADMIN → GERENTE → OPERADOR → CONSULTA). |
| **`step=1` em campos numéricos de quantidade** | Inputs de quantidade, ajuste, transferência | `<Input type="number" step="1" ...>` — operador da pizzaria não trabalha com fração de unidade. Exceção: campos de receita (ficha técnica) onde `step="0.001"`. |
| **Mensagens de erro com label visível** | Validação de form | Mensagem cita o label do campo ("Quantidade deve ser positiva"), não o nome interno (`quantidade`). |
| **Toast com consequência mensurável** | Pós-ação | "Saída registrada: -3 kg de Mussarela" > "Saída registrada". Operador precisa saber o impacto. |
| **"Produto" como termo único na UI** | Tela operacional | O domínio separa `Insumo` (matéria-prima) de `ProdutoVendavel` (item do cardápio), mas para o operador é tudo "produto". Reservar "insumo" pra docs técnicas. |

Detalhe e histórico de cada regra está nas memórias `feedback_inativos_no_final` e `feedback_ux_jefferson` (vivem em `~/.claude/projects/.../memory/`, fora do repo).

---

## Scripts operacionais

`scripts/db/` — scripts SQL ad-hoc (não-Flyway) para administração do banco em dev/homologação. Cada operação é envolvida em `BEGIN/COMMIT` e deve rodar com `psql -v ON_ERROR_STOP=1`.

| Script | Função |
|---|---|
| [scripts/db/reset_para_base_limpa.sql](scripts/db/reset_para_base_limpa.sql) | Zera transações e cadastros preservando empresa principal, 2 filiais, admin, categoria seed "A classificar" e as 7 unidades-base seed. |
| [scripts/db/seed_categorias_restaurante.sql](scripts/db/seed_categorias_restaurante.sql) | Insere 21 categorias padrão de restaurante (alimentos, bebidas, operacional). |

Ver [scripts/db/README.md](scripts/db/README.md) para regras de uso e diferença vs. Flyway.

---

## Perguntas frequentes

| Pergunta | Resposta direta |
|---|---|
| **Onde está a config de segurança?** | [app/.../config/SecurityConfig.java](app/src/main/java/com/nonnas/app/config/SecurityConfig.java) — JWT, CORS, rate limit, security headers |
| **Onde está o filtro JWT?** | [identity/.../infrastructure/security/JwtAuthenticationFilter.java](identity/src/main/java/com/nonnas/identity/infrastructure/security/JwtAuthenticationFilter.java) |
| **Como rodar local?** | `.\tasks.ps1 run` (backend) + `cd frontend && npm run dev` (frontend). Postgres precisa estar em pé na porta 5432. Memória `reference_local_env.md`. |
| **Login default de dev?** | `admin@nonnas.com` / `AdminNonnas2026!` — criado por [AdminBootstrap.java](identity/src/main/java/com/nonnas/identity/application/bootstrap/AdminBootstrap.java) |
| **Como disparar alerta de vencimento agora?** | `POST /api/v1/alertas-disparados/avaliar-vencimentos` (ADMIN/GERENTE). Cron normal é diário 06:00 BRT. Ver memória `reference_alertas_vencimento_trigger.md`. |
| **Como adicionar unidade de medida?** | UI: `/admin/un-medida`. Para NF-e: alias map em [frontend/.../nfeUnidadeAliases.ts](frontend/src/features/operacoes/notas-fiscais/nfeUnidadeAliases.ts). |
| **Como funciona a tela Vendas?** | Consome `GET /api/v1/cardapio?filialId=X` que devolve união: produtos vendáveis ativos (FABRICADO + REVENDA) + insumos com saldo > 0 que ainda não têm produto REVENDA vinculado. Itens são ordenados por vendas dos últimos 30 dias (DESC) + nome; insumos órfãos no fim alfabéticos. Vender insumo órfão chama `POST /api/v1/cardapio/vender-insumo` que cria produto REVENDA automaticamente. |
| **Como o import de NF-e evita criar insumo duplicado?** | `PreviewNotaFiscalUseCase` calcula `matchStatus` por item: `MATCH_DEPARA` (já há de-para fornecedor+cProd → sugere vincular), `COLISAO_CODIGO` (cProd colide com código de outro insumo → operador escolhe vincular ou criar separado), `LIVRE` (sem conflito). Frontend mostra banner per-item; `lancar` envia `insumo.id` se vincular ou `{codigo, nome, unidadeBaseId}` se criar. Backend sufixa código com CNPJ se ainda houver colisão. |
| **Como funciona o regime de lote?** | Ver [ADR 0014](docs/adr/0014-lote-opcional-via-controla-validade.md) + [docs/adendo-lote-opcional.md](docs/adendo-lote-opcional.md). Resumo: insumo com `controla_validade=true` cria lote RASTREADO em cada entrada; `false` reusa um único lote AGREGADOR por insumo. |
| **Onde está a categoria default "A classificar"?** | UUID fixo `00000000-0000-0000-0000-000000000001`, seed [V015](catalog/src/main/resources/db/migration/V015__seed_categoria_a_classificar.sql). |
| **Onde estão os hooks/cron?** | `@Scheduled`: [VencimentoScheduledJob](alerts/src/main/java/com/nonnas/alerts/infrastructure/schedule/VencimentoScheduledJob.java) (06h), [RefreshViewsUseCase](reporting/src/main/java/com/nonnas/reporting/application/RefreshViewsUseCase.java) (30min). Schedule habilitado em `NonnasStockApplication` via `@EnableScheduling`. |
| **CI/CD?** | `.github/workflows/`: `pr.yml` (verify + Trivy + SBOM), `main.yml` (verify + Docker + release), `e2e.yml`, `nightly.yml`, `backup-restore-test.yml`, `security-deep.yml`. |
| **Como zerar o banco para testar do zero?** | `psql -v ON_ERROR_STOP=1 -f scripts/db/reset_para_base_limpa.sql`. Mantém empresa principal + 2 filiais + admin + categoria seed + 7 unidades seed. Ver [scripts/db/README.md](scripts/db/README.md). |
| **Em que ordem as listagens devem aparecer?** | Convenção global: ativos primeiro, inativos no fim. Sort secundário alfabético (pt-BR) ou específico da tela (ex.: Usuários ordena por perfil dentro do grupo ativos). Ver seção [Convenções de UX](#convenções-de-ux-frontend). |
| **Onde estão as categorias padrão de restaurante?** | Inseridas via [scripts/db/seed_categorias_restaurante.sql](scripts/db/seed_categorias_restaurante.sql) — 21 categorias baseadas em ERPs de mercado (Linx Food, Consinco, Sankhya). Não confundir com a categoria seed "A classificar" do Flyway V015 (usada pelo NF-e importer). |
| **Como funciona a integração com iFood/99Food/Keeta?** | Os 3 canais convergiram para o padrão Open Delivery (Abrasel) em 2025. Usamos um contrato canônico interno (`PedidoVendaCanonico`, subset Open Delivery v1.0.1) — cada canal implementa o port [`CanalAdapter`](sales-channels-api/src/main/java/com/nonnas/saleschannels/application/ports/CanalAdapter.java) traduzindo seu payload nativo pro canônico. **T-CANAL-00..02** entregou a fundação (persistência + domain + contrato + tests). **T-CANAL-03** entregou [`OpenDeliveryAdapter`](sales-channels-api/src/main/java/com/nonnas/saleschannels/infrastructure/adapter/opendelivery/OpenDeliveryAdapter.java) genérico (qualquer canal Open Delivery v1.0.1, sem auth) + [`CanalPollingScheduler`](sales-channels-api/src/main/java/com/nonnas/saleschannels/infrastructure/schedule/CanalPollingScheduler.java) (idempotência via schema). **T-CANAL-04** entregou [`ProcessarPedidoCanalUseCase`](sales-channels-api/src/main/java/com/nonnas/saleschannels/application/ProcessarPedidoCanalUseCase.java) (evento → materializa pedido → resolve via [`CanalProdutoDePara`](sales-channels-api/src/main/java/com/nonnas/saleschannels/domain/CanalProdutoDePara.java) → baixa estoque via recipes → confirma no canal) + 4 controllers CRUD. **T-CANAL-05** entregou as 3 UIs (`/canais/pedidos` operacional, `/admin/canais/credenciais` e `/admin/canais/depara` admin) + [`ProcessarPedidosScheduler`](sales-channels-api/src/main/java/com/nonnas/saleschannels/infrastructure/schedule/ProcessarPedidosScheduler.java) ativo (consome eventos pendentes e dispara o use case em loop). 2 flags independentes em `application.yml`: `nonnas.canais.polling.enabled` (puxa eventos) e `nonnas.canais.processar.enabled` (baixa estoque); ambas default `false` em prod (env `NONNAS_CANAIS_POLLING_ENABLED` / `NONNAS_CANAIS_PROCESSAR_ENABLED`). Endpoints manuais `POST /api/v1/canais/{tipo}/poll-now` (ADMIN) e `POST /api/v1/canais/processar-pendentes` (ADMIN/GERENTE) seguem como retry pela UI. POC roda contra mock Prism (script `scripts/dev/start-mock-canal.ps1`) — sem precisar de credencial real. Falta `IfoodAdapter` específico com OAuth2 (quando credencial real chegar) e webhook como alternativa ao polling. Ver [ADR 0016](docs/adr/0016-open-delivery-como-contrato-canonico.md). |

---

## Referências cruzadas

- **Tarefas** (T00→T20, T-LOT-01..09): [STATUS.md](STATUS.md)
- **Documento mestre**: [PROMPT_CLAUDE_CODE.md](PROMPT_CLAUDE_CODE.md) + [PROMPT_CLAUDE_CODE_ADENDO.md](PROMPT_CLAUDE_CODE_ADENDO.md)
- **ADRs** (14 decisões arquiteturais): [docs/adr/](docs/adr/) — ver [README](docs/adr/README.md)
- **Runbooks operacionais**: [docs/runbooks/](docs/runbooks/)
- **Domínio**: [docs/domain-model.md](docs/domain-model.md)
- **DR / Backup**: [docs/disaster-recovery.md](docs/disaster-recovery.md), [docs/backup-restore.md](docs/backup-restore.md)
- **LGPD**: [docs/lgpd-compliance.md](docs/lgpd-compliance.md), [docs/lgpd-mapping.md](docs/lgpd-mapping.md), [docs/lgpd-ropa.md](docs/lgpd-ropa.md)
- **Onboarding**: [docs/onboarding.md](docs/onboarding.md)
- **Observabilidade**: [docs/observability/](docs/observability/)
- **Adendo lote opcional**: [docs/adendo-lote-opcional.md](docs/adendo-lote-opcional.md)
- **Changelog**: [CHANGELOG.md](CHANGELOG.md)

---

**Manutenção deste documento**: atualize quando (1) um bounded context novo for criado, (2) um controller/feature de UI principal mudar de lugar, (3) uma migration nova for adicionada, (4) uma convenção mudar. Não preciso documentar cada arquivo — só pontos de entrada e regras gerais.
