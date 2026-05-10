# PROMPT CLAUDE CODE — Nonnas Stock

**Sistema profissional de controle de estoque centralizado para a rede Nonnas Paola (churrascaria e pizzaria, multi-filial em São Paulo, multi-canal de venda).**

Documento mestre de orientação para desenvolvimento assistido. Lido por inteiro antes de cada tarefa. Atualizado a cada entrega.

- **Repositório**: `C:\dev\nonnas-stock`
- **Modelo comercial**: preço fechado por entregáveis, hora-base R$ 200/h
- **Equipe**: Jefferson Pacheco Agostinho (BA/QA, condução do projeto) + Ewerton Carreira (dev sênior Java)

---

## 0. Protocolo de execução do Claude Code

Antes de qualquer ação, ler este documento inteiro. Em cada nova sessão:

1. Ler `STATUS.md` na raiz para identificar a tarefa corrente (formato `T00`, `T01`, ...).
2. Confirmar que todas as tarefas anteriores estão marcadas como `concluída` em `STATUS.md`. Se não estiverem, **não pular**: terminar primeiro a anterior.
3. Ler integralmente a seção da tarefa nesse documento (seção 11) e aplicar os critérios de aceitação.
4. Antes de gerar código, **listar para o usuário** as decisões técnicas que vai tomar dentro da tarefa (escolha de biblioteca, padrão de pacote, nome de tabela). Pedir confirmação se houver ambiguidade.
5. Implementar, escrever testes correspondentes, rodar `mvn verify` (ou `npm test` no frontend) e garantir tudo verde antes de finalizar.
6. Fazer commit no padrão Conventional Commits (`feat(catalog): adiciona entidade Insumo`, `test(inventory): cobre cenário FEFO multi-lote`).
7. Atualizar `STATUS.md` marcando a tarefa como concluída, com data, hash do commit e nota curta do que foi entregue.
8. Parar e perguntar ao usuário antes de iniciar a próxima tarefa. Não emendar tarefas em sequência sem aprovação.

Regras invariantes:

- Nunca apagar ou regravar este documento. Atualizações vêm via PR e revisão humana.
- Nunca implementar uma funcionalidade sem teste correspondente.
- Nunca subir código com testes falhando ou pulados (`@Disabled`).
- Nunca commitar com `mvn verify` falhando.
- Nunca usar `H2` em testes que tocam banco. Sempre Postgres via Testcontainers.
- Mensagens de log, comentários no código e nomes técnicos em **inglês**. Strings de UI, validação e comunicação com usuário em **português brasileiro**.

---

## 1. Contexto do projeto

### 1.1 O cliente

Nonnas Paola é uma rede de restaurantes em São Paulo (churrascaria e pizzaria), com múltiplas unidades e três canais simultâneos de venda: salão presencial, iFood, Keeta e 99Food. Hoje o estoque é controlado de forma fragmentada por filial, sem visão consolidada e sem rastreabilidade fina de validade — em ramo de alimentação esse gap representa risco sanitário, perda financeira (vencimento na geladeira) e ruptura operacional (loja sem mussarela enquanto outra tem caixa fechada).

### 1.2 O que é o MVP 1.0

Um sistema web centralizado, com saldos separados por filial, que cobre:

- Cadastro de empresa, filiais, usuários com perfis, fornecedores e canais de venda.
- Catálogo de insumos com categorias, unidades de medida padronizadas e conversões.
- Ficha técnica (receita) por produto vendável, versionada.
- Movimentação completa: entrada manual, saída manual, transferência entre filiais, ajuste, carga inicial.
- Controle de lote com data de fabricação e validade, com saída automática por **FEFO** (First Expired, First Out).
- Alertas configuráveis por administrador (estoque mínimo percentual ou absoluto, vencimento próximo, ruptura).
- Dashboard e relatórios essenciais (posição, curva ABC, ruptura, vencimento próximo).
- API REST documentada com OpenAPI.
- Frontend web responsivo com a identidade visual da marca.

**Fora do MVP 1.0** (entram em ondas posteriores):

- Importador de NF-e (onda 1.1).
- Integrações com canais de venda — iFood, PDV salão, 99Food, Keeta (onda 1.2).
- Aplicativo mobile (V2).

### 1.3 Princípios arquiteturais não-negociáveis

1. **Modular Monolith** com bounded contexts independentes. Cada módulo é um Maven module separado, com camadas internas próprias. Acoplamento entre módulos só por contratos públicos (`api` package) ou eventos de aplicação.
2. **Saldo é projeção, movimentação é a verdade.** Toda alteração de estoque é uma `Movimentacao` imutável. O saldo materializado é cache, sempre reconstrutível.
3. **Saldo por `(lote, filial)`**. Granularidade fina é obrigatória — sem isso, FEFO é impossível.
4. **Unidade base canônica por insumo**. Toda matemática interna em unidade base. Conversão acontece nas bordas (entrada e exibição).
5. **Multi-filial é literal**. Saldo separado, transferência orquestrada com estados, relatórios sempre filtráveis por filial.
6. **Ficha técnica versionada**. Mudança de receita não afeta histórico. Venda registrada captura snapshot.
7. **Testes desde o primeiro commit.** Nenhum módulo nasce sem suíte unitária; nenhum repositório sem teste de integração com Postgres real via Testcontainers.

---

## 2. Stack tecnológica

### 2.1 Backend

- **Java 21 LTS** (record, sealed, pattern matching, virtual threads onde fizer sentido).
- **Spring Boot 3.3+**.
- **Maven** multi-módulo (parent + 12 módulos).
- **PostgreSQL 16**.
- **Flyway** para versionamento de schema.
- **Spring Data JPA** + **Hibernate 6**.
- **Spring Security** + **JWT** (jjwt 0.12+).
- **MapStruct** 1.5+ (DTOs).
- **Lombok** (com moderação — `@Value`, `@Builder`, `@RequiredArgsConstructor`. **Evitar** `@Data` em entidades JPA).
- **springdoc-openapi** 2.5+ (OpenAPI 3 + Swagger UI).
- **Spring Application Events** para comunicação interna entre módulos (síncrona, mesma transação).
- **Spring Boot Actuator** para health, info, metrics.
- **Logback** com encoder JSON em produção.

### 2.2 Testes

- **JUnit 5 (Jupiter)**.
- **Mockito** + **Mockito-Inline**.
- **AssertJ** (asserções fluentes — preferir sobre Hamcrest).
- **Testcontainers** (Postgres real, sem H2).
- **WireMock** (mock de APIs externas).
- **ArchUnit** (regras estruturais).
- **Spring Boot Test** (`@SpringBootTest`, `@DataJpaTest`, `@WebMvcTest`).
- **JaCoCo** (cobertura, threshold no Maven).
- **PIT (Pitest)** (mutation testing — pipeline semanal).
- **Playwright Java** (E2E pré-merge e nightly).
- **Gatling** (carga, projeto separado).

### 2.3 Frontend

- **React 18+** com **Vite 5+** e **TypeScript 5+**.
- **TanStack Query v5** (server state, cache, mutations).
- **Zustand** (UI state mínimo, evitar quando TanStack Query basta).
- **React Router v6**.
- **React Hook Form** + **Zod** (formulários e validação tipada).
- **Axios** com interceptor JWT.
- **shadcn/ui** (componentes acessíveis sobre Radix UI).
- **Tailwind CSS** com tema customizado para a marca Nonnas Paola.
- **Lucide React** (ícones).
- **Recharts** (gráficos).
- **Vitest** + **React Testing Library** (testes unitários frontend).
- **Playwright** (E2E compartilhado com backend).

### 2.4 Infra e CI/CD

- **GitHub Actions** (workflows: PR, main, nightly).
- **Docker** (imagens slim baseadas em `eclipse-temurin:21-jre-alpine`).
- **Docker Compose** para desenvolvimento local (Postgres + serviço da app).
- Servidor de produção a definir com cliente — recomendação inicial: DigitalOcean Premium Droplet 4GB ou AWS Lightsail.
- **Renovate** ou **Dependabot** para atualizações automatizadas de dependências.

---

## 3. Estrutura de pastas

```
C:\dev\nonnas-stock\
├── pom.xml                          # parent POM
├── README.md
├── PROMPT_CLAUDE_CODE.md            # este documento
├── STATUS.md                        # estado das tarefas
├── CHANGELOG.md                     # gerado por semantic-release
├── .editorconfig
├── .gitignore
├── .gitattributes
├── docker-compose.yml               # postgres local
├── Makefile                         # atalhos: make test, make run, make e2e
├── .github/
│   └── workflows/
│       ├── pr.yml                   # build + unit + integration + arch
│       ├── main.yml                 # tudo do PR + smoke E2E + Docker
│       └── nightly.yml              # tudo + PIT + Gatling
├── docs/
│   ├── architecture.md
│   ├── domain-model.md
│   ├── coding-standards.md
│   ├── testing-strategy.md
│   ├── deployment.md
│   └── api-contracts/               # exports OpenAPI versionados
├── shared-kernel/                   # value objects, exceções base
├── identity/                        # Empresa, Filial, Usuário, Auth JWT
├── catalog/                         # Insumo, Categoria, UnidadeMedida, Conversão, Fornecedor
├── inventory-core/                  # Lote, SaldoLote, Movimentação (NÚCLEO)
├── recipes/                         # ProdutoVendavel, FichaTecnica
├── operations/                      # Transferência, Carga inicial, Ajuste
├── alerts/                          # AlertaConfig, AlertaDisparado, avaliador
├── reporting/                       # queries de relatórios e dashboards
├── sales-channels-api/              # contratos públicos de canais (preparação MVP 1.2)
├── nfe-importer/                    # placeholder, ativado em MVP 1.1
├── app/                             # composição: main, config, perfis Spring
│   └── src/main/resources/
│       ├── application.yml
│       ├── application-dev.yml
│       ├── application-test.yml
│       ├── application-prod.yml
│       └── db/migration/            # Flyway centralizado
└── frontend/
    ├── package.json
    ├── vite.config.ts
    ├── tailwind.config.ts
    ├── tsconfig.json
    └── src/
        ├── main.tsx
        ├── routes/
        ├── pages/
        ├── components/
        ├── stores/
        ├── api/
        ├── theme/
        └── lib/
```

Estrutura padrão de cada módulo backend:

```
{modulo}/
├── pom.xml
└── src/
    ├── main/java/com/nonnas/{modulo}/
    │   ├── domain/                  # entidades, value objects, regras puras
    │   ├── application/             # use cases, ports (interfaces)
    │   ├── infrastructure/          # JPA repos, adapters, integrações
    │   ├── interfaces/rest/         # controllers, DTOs, mappers
    │   └── api/                     # contratos públicos para outros módulos (eventos, queries somente leitura)
    └── test/java/com/nonnas/{modulo}/
        ├── domain/                  # unitários
        ├── application/             # unitários
        ├── infrastructure/          # integração com Testcontainers
        └── interfaces/              # integração HTTP com @WebMvcTest ou @SpringBootTest
```

---

## 4. Modelo de domínio

### 4.1 Bounded contexts

- **identity** — Empresa, Filial, Usuário, Perfil, autenticação.
- **catalog** — Categoria, Unidade de medida, Conversão, Insumo, Fornecedor, configuração por filial (`InsumoFilial`).
- **inventory-core** — Lote, Saldo por lote-filial, Movimentação, Item de movimentação. Coração do sistema.
- **recipes** — Produto vendável, Ficha técnica versionada, Item de ficha técnica.
- **operations** — Transferência entre filiais (com workflow de status), ajuste manual com aprovação, carga inicial.
- **alerts** — Configuração de alerta com escopo flexível, alerta disparado, avaliador.
- **reporting** — Queries materializadas e dashboards.
- **sales-channels-api** (preparação) — Contratos `PedidoVendaCanonico` e `CanalAdapter` que serão implementados em 1.2.

### 4.2 Entidades principais (referência rápida)

Modelagem completa em `docs/domain-model.md` (gerado em T03). Resumo:

**identity**
- `Empresa { id, razao_social, cnpj }`
- `Filial { id, empresa_id, nome, cnpj, endereco, ativa }`
- `Usuario { id, filial_id?, nome, email, senha_hash, perfil, ativo }`
- `Perfil` enum: `ADMIN`, `GERENTE`, `OPERADOR`, `CONSULTA`

**catalog**
- `CategoriaInsumo { id, categoria_pai_id?, nome }` (auto-relação opcional)
- `UnidadeMedida { id, codigo, nome, tipo }` — tipo em `PESO`, `VOLUME`, `UNIDADE`
- `ConversaoUnidade { id, insumo_id?, unidade_origem_id, unidade_destino_id, fator }` — `insumo_id` null para conversão global
- `Insumo { id, codigo, nome, categoria_id, unidade_base_id, controla_lote, controla_validade, ativo }`
- `InsumoFilial { id, insumo_id, filial_id, estoque_minimo, estoque_maximo, ponto_pedido, ativo }`
- `Fornecedor { id, razao_social, cnpj, ativo }`

**inventory-core**
- `Lote { id, insumo_id, fornecedor_id?, nota_fiscal_id?, numero_lote, data_fabricacao?, data_validade?, valor_unitario }`
- `SaldoLote { lote_id (PK), filial_id (PK), quantidade_base, atualizado_em }` — chave composta
- `Movimentacao { id, filial_id, usuario_id, tipo, data_movimentacao, documento_origem_tipo, documento_origem_id, observacao }` — **imutável**
- `ItemMovimentacao { id, movimentacao_id, insumo_id, lote_id, unidade_lancamento_id, quantidade_lancada, quantidade_base, valor_unitario }`
- `TipoMovimentacao` enum: `ENTRADA_NF`, `ENTRADA_AJUSTE`, `ENTRADA_TRANSFERENCIA`, `ENTRADA_DEVOLUCAO_CLIENTE`, `ENTRADA_CARGA_INICIAL`, `SAIDA_VENDA`, `SAIDA_AJUSTE`, `SAIDA_TRANSFERENCIA`, `SAIDA_PERDA`, `SAIDA_QUEBRA`, `SAIDA_VENCIMENTO`

**recipes**
- `ProdutoVendavel { id, codigo, nome, categoria, ativo }`
- `FichaTecnica { id, produto_vendavel_id, versao, vigente_desde, vigente_ate?, ativa }`
- `ItemFichaTecnica { id, ficha_tecnica_id, insumo_id, unidade_id, quantidade }`

**operations**
- `Transferencia { id, filial_origem_id, filial_destino_id, mov_saida_id?, mov_entrada_id?, usuario_solicitante_id, status, data_solicitacao, data_envio?, data_recebimento? }`
- `StatusTransferencia` enum: `SOLICITADA`, `APROVADA`, `EM_TRANSITO`, `RECEBIDA`, `CANCELADA`

**alerts**
- `AlertaConfig { id, nome, tipo, escopo, insumo_id?, categoria_id?, filial_id?, threshold_valor, threshold_unidade, destinatarios (jsonb), ativo, criado_por }`
- `AlertaDisparado { id, config_id, insumo_id, filial_id, lote_id?, valor_observado, mensagem, disparado_em, visualizado_em?, resolvido_em? }`
- `TipoAlerta` enum: `ESTOQUE_MINIMO_PERCENTUAL`, `ESTOQUE_MINIMO_ABSOLUTO`, `VENCIMENTO_PROXIMO_DIAS`, `RUPTURA`
- `EscopoAlerta` enum: `GLOBAL`, `INSUMO`, `CATEGORIA`, `FILIAL`, `INSUMO_FILIAL`

---

## 5. Regras de negócio críticas

### 5.1 Conversão de unidade

Cada insumo tem uma unidade base. Toda matemática interna usa essa unidade. Na entrada e exibição, converte.

Resolução de conversão, na ordem:
1. Conversão específica do insumo (ex.: `CX → G` fator 5000 para mussarela em caixa de 5kg).
2. Conversão global entre unidades de mesmo tipo (ex.: `KG → G` fator 1000).
3. Erro `UnidadeNaoConversivelException` se nenhuma definida.

`item_movimentacao` grava sempre `quantidade_lancada` (na unidade que o operador escolheu) e `quantidade_base` (após conversão). Saldo opera só em base.

### 5.2 FEFO na saída

Ao gerar movimentação de saída para um insumo numa filial:

```sql
SELECT sl.lote_id, sl.quantidade_base, l.data_validade
  FROM saldo_lote sl
  JOIN lote l ON l.id = sl.lote_id
 WHERE sl.filial_id = :filialId
   AND l.insumo_id = :insumoId
   AND sl.quantidade_base > 0
 ORDER BY l.data_validade NULLS LAST, l.id ASC
```

Itera consumindo até completar a quantidade requerida. Pode gerar múltiplos `item_movimentacao` (um por lote). Lotes sem validade ficam por último (FIFO entre eles).

Se a soma dos lotes não bastar, a regra do MVP é **permitir saldo negativo** para o último lote consumido, sinalizar com flag `gerou_negativo = true` na movimentação e disparar alerta de ruptura. Restaurante prefere registrar a venda mesmo sem estoque a bloquear o pedido.

### 5.3 Transferência entre filiais

Workflow:

1. Usuário cria `Transferencia` com origem, destino, lista de insumos+quantidades. Status `SOLICITADA`.
2. Aprovador (perfil `GERENTE` ou `ADMIN`) aprova. Status `APROVADA`.
3. Operador da origem registra envio: cria `Movimentacao` `SAIDA_TRANSFERENCIA` na origem, decrementa `saldo_lote(lote, origem)`, status `EM_TRANSITO`, `mov_saida_id` preenchido.
4. Operador do destino registra recebimento: cria `Movimentacao` `ENTRADA_TRANSFERENCIA` no destino, **mantém o mesmo `lote_id`**, incrementa `saldo_lote(lote, destino)`, status `RECEBIDA`, `mov_entrada_id` preenchido.
5. Se quantidade recebida diferente da enviada, gerar ajuste automático de `SAIDA_QUEBRA` na origem ou `ENTRADA_AJUSTE` no destino, com observação rastreável à transferência.

Estoque em trânsito = soma de `SaidaTransferencia` cuja `Transferencia.status = EM_TRANSITO`. Consultável a qualquer momento.

### 5.4 Avaliação de alerta

Reativa: após cada `Movimentacao` de tipo `SAIDA_*`, dispara `AvaliadorAlertasService.avaliar(insumoId, filialId)`. Busca configs aplicáveis ao escopo `(insumo, filial)` (ordenadas do mais específico ao mais genérico, mais específico ganha), calcula saldo atual, compara threshold, insere `AlertaDisparado` se cruzou, despacha notificação.

Programada: job Spring `@Scheduled` diário às 06:00 avalia `VENCIMENTO_PROXIMO_DIAS` para todos os lotes ativos.

Resolução automática: quando saldo volta acima do threshold por nova entrada, alerta correspondente é marcado `resolvido_em = now()` automaticamente.

### 5.5 Carga inicial

Ao criar uma filial, a tela inclui etapa "Carga inicial". Dois modos:
- Manual: form linha a linha (insumo, lote, validade, quantidade, unidade, valor).
- Planilha: upload CSV/XLSX com colunas padronizadas. Sistema valida, exibe preview, confirma.

Cada linha gera `LOTE` (ou reutiliza se já existir lote idêntico) + `MOVIMENTACAO` tipo `ENTRADA_CARGA_INICIAL`. Permite múltiplas cargas, mas marca a primeira como "carga de abertura".

### 5.6 Ficha técnica versionada com snapshot

Quando ficha técnica é alterada:
1. A versão atual recebe `vigente_ate = now()` e `ativa = false`.
2. Nova `FichaTecnica` é criada com `versao = anterior + 1`, `vigente_desde = now()`, `vigente_ate = null`, `ativa = true`.
3. Itens são copiados ou ajustados conforme edição.

Quando uma venda é registrada (`PedidoVenda` em onda 1.2, `Movimentacao` `SAIDA_VENDA` no MVP via lançamento manual), o `item_pedido_venda` (ou equivalente) referencia o `ficha_tecnica_id` da versão **vigente naquele momento**. Esse é o snapshot. Mudanças futuras na receita não alteram o histórico.

---

## 6. Convenções de código

### 6.1 Java

- Pacote raiz: `com.nonnas.{modulo}` (ex.: `com.nonnas.catalog.domain`).
- Entidades JPA em `infrastructure.persistence`. **Nunca expor entidade JPA fora do módulo** — mappers convertem para domínio puro.
- Domain layer **livre de Spring** (sem `@Component`, sem `@Autowired`). Lógica pura, instanciável em teste sem container.
- Use cases em `application` recebem ports (interfaces) por construtor. Implementações dos ports vivem em `infrastructure`.
- Identifier types: usar `record` ou classe selada para IDs (`InsumoId`, `FilialId`) — evita confundir UUIDs entre entidades.
- Datas e horas: `Instant` para timestamps, `LocalDate` para datas civis. **Nunca** `Date` legado.
- Dinheiro: `BigDecimal` com escala definida (4 casas para valor unitário, 2 para total). Arredondamento `HALF_EVEN`.
- Quantidades: `BigDecimal` com escala 4 (suficiente para "0.0050 KG" = 5g).
- Exceções: estender `DomainException` (em shared-kernel) com mensagens ricas. Nunca `RuntimeException` genérica.
- Logs: SLF4J com placeholders (`log.info("Lote {} criado para insumo {}", loteId, insumoId)`). Nunca concatenação.
- Não usar `null` como valor de retorno em métodos públicos. Preferir `Optional`.

### 6.2 SQL e schema

- Tabelas em `snake_case` plural (`insumos`, `lotes`, `saldos_lotes`).
- Colunas em `snake_case` singular.
- IDs sempre `UUID` (PostgreSQL `uuid` nativo, geração via `gen_random_uuid()`).
- Foreign keys com nome explícito: `fk_lote_insumo`, `fk_movimentacao_filial`.
- Índices em colunas de busca: `idx_lote_validade`, `idx_movimentacao_filial_data`.
- Migrations Flyway: `V{number}__{description}.sql` (ex.: `V001__create_identity_schema.sql`). Nunca editar migration aplicada — sempre nova.
- Constraints documentadas: `CHECK (quantidade_base > 0)`, `CHECK (status IN ('SOLICITADA', 'APROVADA', ...))`.
- Soft delete via `ativo BOOLEAN` ou `desativado_em TIMESTAMP`. **Não** deletar fisicamente.

### 6.3 REST API

- Versionamento na URL: `/api/v1/...`.
- Substantivos plurais: `/api/v1/insumos`, `/api/v1/filiais`, `/api/v1/movimentacoes`.
- Verbos HTTP semânticos. Nada de `/api/v1/insumos/criar` (POST em `/api/v1/insumos` já significa criar).
- DTOs separados de entidades. `InsumoCreateRequest`, `InsumoResponse`, `InsumoUpdateRequest`.
- Validação com Jakarta Bean Validation (`@NotBlank`, `@Positive`, `@Size`).
- Erros padronizados (RFC 7807 — Problem Details): `{ type, title, status, detail, instance, errors[] }`.
- Paginação: `?page=0&size=20&sort=nome,asc`. Retorno `Page<T>` do Spring.
- Filtros via query string: `/api/v1/insumos?categoriaId=...&ativo=true`.
- Respostas em português: mensagens de validação, descrições OpenAPI. Nomes de campos em português também (`quantidade`, `dataValidade`).
- OpenAPI documentado por anotação Swagger em controllers — gerado em `/v3/api-docs` e visualizável em `/swagger-ui.html` em ambiente dev.

### 6.4 Frontend

- Pastas por feature, não por tipo: `pages/filiais/`, `pages/insumos/`, em vez de `components/` gigante.
- Componentes em `PascalCase`, hooks em `camelCase` com prefixo `use`.
- Server state via `@tanstack/react-query` com query keys hierárquicas (`['insumos']`, `['insumos', id]`, `['saldos', filialId]`).
- Forms com React Hook Form + Zod schemas. Schemas em arquivo separado, reaproveitáveis.
- API client centralizado em `src/api/`, com Axios interceptado por JWT. Tipos gerados a partir do OpenAPI (script `npm run generate-types`).
- Strings de UI em arquivo `pt-BR.ts` em `src/i18n/`. Preparar para i18n futura mesmo sem ativar.

### 6.5 Git e PRs

- **Conventional Commits** obrigatório (`feat`, `fix`, `test`, `docs`, `refactor`, `chore`, `perf`, `build`, `ci`).
- Escopo do commit = nome do módulo (`feat(catalog): ...`).
- Branches: `main` protegida; trabalho em `feat/T03-identity-jwt`, `fix/T05-fefo-edge-case`.
- PR obrigatório, mínimo 1 aprovação, status checks verdes.
- PR template em `.github/pull_request_template.md` com seções: Objetivo, Mudanças, Testes adicionados, Como validar.
- Squash merge na main, com mensagem no padrão Conventional.
- Tags semânticas (`v1.0.0`, `v1.1.0`). `semantic-release` automatiza após cada merge na main.

---

## 7. Estratégia de testes

Pirâmide de três níveis com camadas auxiliares.

### 7.1 Unitários (base, ~800 testes alvo MVP)

- Cobertura mínima: **85% domain, 75% application** (verificada por JaCoCo no PR).
- Sem Spring, sem banco, sem rede.
- Frameworks: JUnit 5, Mockito, AssertJ.
- Builders e fixtures em `src/test/java/.../testsupport/`.
- Cada classe de domínio nasce com seu teste irmão antes do código de produção (TDD encorajado, não imposto).
- Roda em segundos a cada `mvn test`.

### 7.2 Integração (meio, ~150 testes alvo MVP)

- Postgres real via Testcontainers em todos os testes que tocam banco. **Nunca H2.**
- `@DataJpaTest` com `@Testcontainers` para repositórios.
- `@SpringBootTest(webEnvironment = RANDOM_PORT)` para testes ponta-a-ponta de use cases via REST.
- WireMock para simular APIs externas (preparação para canais de venda).
- Container reutilizado entre testes via `@Container static` para velocidade.
- Cleanup explícito via `@Sql` ou `@Transactional` rollback.
- Roda em 1–2 min em CI, em todo PR.

### 7.3 E2E (topo, ~20 testes alvo MVP)

- Playwright Java em projeto separado dentro do monorepo.
- Page Object Model.
- Headless em CI, headful local.
- Screenshots e vídeo em falhas.
- Cenários cobertos: login, cadastrar filial com carga inicial, cadastrar insumo + ficha técnica, lançar entrada/saída manual e validar saldo, transferir entre filiais, configurar alerta e disparar, visualizar relatório de posição.
- Pré-merge (smoke, 5 cenários críticos) e nightly (suíte completa).

### 7.4 Arquitetura (transversal)

- ArchUnit em projeto raiz com regras:
  - Domain de qualquer módulo não depende de Spring, JPA, ou outros módulos.
  - Application só depende de Domain e API públicas.
  - Infrastructure pode depender de tudo, mas não pode ser depended-upon por Domain.
  - Controllers só chamam Use Cases via interface, nunca repositórios.
  - Entidades JPA só vivem em `infrastructure.persistence`.
- Roda como teste normal em todo `mvn verify`.

### 7.5 Mutação (semanal)

- PIT configurado no parent POM, executado em workflow `nightly.yml` na sexta-feira.
- Threshold inicial: mutation score > 70%.
- Score em queda → item de débito técnico no backlog.

### 7.6 Carga (sob demanda)

- Gatling em projeto separado (`load-tests/`).
- Cenários: 50 vendas/min em 5 filiais simultâneas (pico almoço), 1000 ajustes em batch (carga inicial).
- Roda em ambiente de staging, gera relatório HTML, comparável entre versões.

---

## 8. CI/CD

### 8.1 Workflows

**`.github/workflows/pr.yml`** — gatilho: pull_request, qualquer branch para main.

```
- checkout
- setup Java 21
- cache Maven
- mvn verify (unit + integration + arch + jacoco)
- check coverage threshold (falha se abaixo)
- comment com cobertura no PR
```

Tempo alvo: 8 min.

**`.github/workflows/main.yml`** — gatilho: push em main.

```
- tudo do PR
- build Docker image
- push para GitHub Container Registry com tag short SHA + latest
- deploy automático em staging (script SSH ou GitHub Environments)
- smoke E2E contra staging
- semantic-release: gera tag, atualiza CHANGELOG, cria GitHub Release
```

Tempo alvo: 15 min.

**`.github/workflows/nightly.yml`** — gatilho: cron às 03:00 UTC (00:00 BRT).

```
- E2E completo contra staging
- PIT mutation testing (sexta-feira)
- Gatling load tests contra staging (sábado)
- relatórios publicados em GitHub Pages
```

### 8.2 Branch protection

- `main` exige PR.
- 1 aprovação mínima.
- Status checks verdes obrigatórios.
- Sem force-push.
- Sem merge direto (só via PR).
- Linear history (squash ou rebase).

### 8.3 Versionamento

- `semantic-release` lê commits Conventional desde a última tag e gera próxima versão.
- `feat:` → minor. `fix:` → patch. `BREAKING CHANGE:` → major.
- Tag criada automaticamente, CHANGELOG.md atualizado, GitHub Release publicada.

---

## 9. Identidade visual

Marca: vermelho italiano + verde italiano sobre branco. Logo com mascote da nonna. Sistema usa as cores com parcimônia para não cansar quem opera o dia inteiro.

### 9.1 Paleta (Tailwind config)

```ts
// tailwind.config.ts
colors: {
  brand: {
    red: '#D62828',          // primary, header, botão CTA
    'red-hover': '#B91D1D',
    green: '#2A9D3F',        // sucesso, indicadores positivos
    'green-hover': '#1F7A30',
  },
  neutral: {
    bg: '#FAFAF7',           // fundo da tela
    surface: '#FFFFFF',      // cards, modais
    border: '#E5E7EB',       // divisores, tabelas
    text: '#1F2937',         // texto principal
    'text-muted': '#6B7280', // texto secundário, labels
  },
  semantic: {
    danger: '#DC2626',       // saldo negativo crítico
    warning: '#D97706',      // estoque mínimo, vencimento próximo
    info: '#2563EB',         // notificações neutras
  },
}
```

### 9.2 Tipografia

- **Inter** (Google Fonts) — UI principal: tabelas, formulários, dashboards. Legível em densidade alta.
- **Playfair Display** (Google Fonts) — display em momentos de marca: tela de login, cabeçalho de relatórios impressos. Limitado, complementa o ar italiano sem competir com o logo.
- A fonte script do logo **não** é usada como tipografia da UI. Permanece como asset gráfico do logo.

### 9.3 Logo e ícones

- Logo PNG completo em `frontend/src/assets/logo-nonnas.png`.
- Ícone reduzido (tomate/bandeirinha) em `frontend/src/assets/icon-nonnas.svg` — usado como favicon e loading spinner.
- Ícones funcionais: Lucide React.

---

## 10. Tarefas sequenciais

Cada tarefa: objetivo, pré-requisitos, entregáveis, critérios de aceitação. Claude Code executa **uma por vez**, com confirmação humana entre uma e outra.

### T00 — Fundação do repositório

**Objetivo:** Estrutura base do monorepo Maven multi-módulo, configuração de qualidade, infra local.

**Pré-requisitos:** nenhum.

**Entregáveis:**
- `git init`, primeiro commit.
- `pom.xml` parent com Java 21, Spring Boot 3.3+ BOM, dependências de teste, plugins (Surefire, Failsafe, JaCoCo, Maven Compiler).
- `pom.xml` por módulo (placeholder em todos os 12 módulos listados).
- `.gitignore` (Java + Node + IDE).
- `.editorconfig`.
- `.gitattributes` (LF para `.java`, `.yml`, etc).
- `README.md` com instruções de setup local.
- `STATUS.md` com lista de tarefas e estado.
- `docker-compose.yml` com Postgres 16.
- `Makefile` com targets: `up`, `down`, `test`, `run`, `clean`.
- `.github/workflows/pr.yml` (esqueleto inicial).
- `.github/pull_request_template.md`.

**Critérios de aceitação:**
- `mvn validate` passa em todos os módulos.
- `docker-compose up -d postgres` sobe banco em `localhost:5432`.
- `make test` retorna 0 (mesmo sem testes, retorna OK).
- `git log` mostra primeiro commit `chore(repo): initial setup with maven multi-module structure`.

---

### T01 — shared-kernel

**Objetivo:** Value objects e exceções base reutilizadas por todos os módulos.

**Pré-requisitos:** T00.

**Entregáveis:**
- `Money` — value object com `BigDecimal` valor + `Currency` (default BRL), com operações `add`, `subtract`, `multiply`, `divide`. Imutável. `equals`/`hashCode`/`toString` definidos.
- `Quantity` — `BigDecimal` valor + `UnidadeMedida` referência (apenas o id por enquanto, módulo catalog define o resto). Imutável.
- `EntityId<T>` — sealed type para IDs tipados.
- `DomainException`, `ValidationException`, `BusinessRuleException`.
- `Result<T>` — wrapper para retornos de uso casos com sucesso/falha (alternativa a exceções para casos previstos).
- Testes unitários cobrindo todos value objects (mínimo 90% cobertura).

**Critérios de aceitação:**
- `mvn -pl shared-kernel test` passa.
- JaCoCo report > 90% cobertura no módulo.
- ArchUnit (mesmo que ainda básico) confirma que shared-kernel não depende de Spring.

---

### T02 — identity (Empresa, Filial, Usuário, Auth JWT)

**Objetivo:** Cadastros de empresa/filial/usuário e fluxo de autenticação JWT.

**Pré-requisitos:** T00, T01.

**Entregáveis:**
- Migration Flyway `V001__create_identity_schema.sql` com tabelas `empresas`, `filiais`, `usuarios`, `perfis`.
- Entidades JPA em `identity.infrastructure.persistence`.
- Domain entities (`Empresa`, `Filial`, `Usuario`) puros, em `identity.domain`.
- Use cases: `CriarEmpresa`, `CriarFilial`, `CriarUsuario`, `Autenticar`, `RefreshToken`.
- Controllers REST: `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`, `GET /api/v1/empresas`, `POST /api/v1/empresas`, `GET /api/v1/filiais`, `POST /api/v1/filiais`, `GET /api/v1/usuarios`, `POST /api/v1/usuarios`.
- Spring Security com filtro JWT, `JwtAuthenticationFilter`, `JwtTokenProvider`.
- Hash de senha com BCrypt.
- Seed inicial em `V002__seed_initial_admin.sql` com usuário `admin@nonnas.com` (senha em variável de ambiente).
- Testes unitários para domain e application.
- Testes de integração com Testcontainers para repositórios e endpoints REST.

**Critérios de aceitação:**
- `mvn -pl identity verify` passa.
- POST `/api/v1/auth/login` com credenciais válidas retorna JWT.
- GET `/api/v1/usuarios` sem JWT retorna 401.
- GET `/api/v1/usuarios` com JWT válido retorna lista paginada.
- Cobertura JaCoCo: domain > 85%, application > 75%.

---

### T03 — catalog (Insumo, Categoria, Unidade, Conversão, Fornecedor)

**Objetivo:** Catálogo de insumos com unidades padronizadas e conversões.

**Pré-requisitos:** T02.

**Entregáveis:**
- Migration `V003__create_catalog_schema.sql`.
- Entidades: `CategoriaInsumo`, `UnidadeMedida`, `ConversaoUnidade`, `Insumo`, `InsumoFilial`, `Fornecedor`.
- `ConversorUnidadeService` em `catalog.domain`, com a lógica de resolução em cascata (específica → global → erro).
- Seed `V004__seed_unidades_padrao.sql` com unidades comuns (G, KG, ML, L, UN, CX, PORCAO) e conversões globais entre G/KG e ML/L.
- Use cases CRUD para todas entidades.
- Controllers REST.
- Testes unitários cobrindo conversão (incluindo casos de erro: unidade incompatível sem conversão definida).

**Critérios de aceitação:**
- `mvn -pl catalog verify` passa.
- Teste `ConversorUnidadeServiceTest` cobre 8+ cenários (global, específica do insumo, ausência, ciclo).
- POST de Insumo com `unidadeBaseId` inválida retorna 400 com mensagem clara.
- `docs/domain-model.md` é gerado com seção do catalog (Claude Code escreve esse documento incrementalmente).

---

### T04 — inventory-core (Lote, Saldo, Movimentação, FEFO)

**Objetivo:** Núcleo do sistema. Lote, saldo por lote-filial, movimentação imutável, lógica FEFO.

**Pré-requisitos:** T03.

**Entregáveis:**
- Migration `V005__create_inventory_schema.sql`.
- Entidades: `Lote`, `SaldoLote` (com PK composta), `Movimentacao`, `ItemMovimentacao`.
- `RegistrarMovimentacaoUseCase` (entrada e saída).
- `CalcularSaldoUseCase` (saldo atual por insumo+filial somando os lotes).
- `SelecionarLotesPorFefoService` (algoritmo de seleção de lotes para saída).
- Trigger ou listener Spring `@EventListener` que atualiza `SaldoLote` materializado a cada nova `ItemMovimentacao`.
- Job de conciliação `@Scheduled` que compara saldo materializado com soma das movimentações (alerta em divergência).
- Use cases REST: `POST /api/v1/movimentacoes/entrada-manual`, `POST /api/v1/movimentacoes/saida-manual`, `GET /api/v1/saldos?filialId=...`, `GET /api/v1/saldos/{insumoId}/lotes?filialId=...`.
- Testes unitários e de integração cobrindo:
  - FEFO com 1 lote, com múltiplos lotes, com lote sem validade, com lote vencido, com quantidade que esgota lotes.
  - Saldo materializado vs soma calculada (consistência).
  - Movimentação imutável (tentativa de update lança erro).
  - Concorrência: duas saídas simultâneas no mesmo lote (lock otimista ou pessimista — escolher e testar).

**Critérios de aceitação:**
- `mvn -pl inventory-core verify` passa.
- 30+ testes unitários, 15+ testes de integração só nesse módulo.
- Cobertura JaCoCo > 90% domain.

---

### T05 — recipes (Ficha técnica versionada)

**Objetivo:** Produto vendável e ficha técnica com versionamento e snapshot.

**Pré-requisitos:** T04.

**Entregáveis:**
- Migration `V006__create_recipes_schema.sql`.
- Entidades: `ProdutoVendavel`, `FichaTecnica`, `ItemFichaTecnica`.
- `CriarFichaTecnicaUseCase`, `AtualizarFichaTecnicaUseCase` (que cria nova versão e desativa antiga), `BuscarFichaTecnicaVigenteUseCase`.
- `RegistrarVendaSimuladaUseCase` — para testar fluxo end-to-end no MVP sem canais reais. Recebe `produtoVendavelId` e `quantidade`, busca ficha vigente, gera `Movimentacao` `SAIDA_VENDA` com `ItemMovimentacao` para cada insumo da receita, aplica FEFO.
- Controllers REST.
- Testes cobrindo:
  - Versionamento: criar v1, editar gera v2, v1 fica como histórico.
  - Snapshot: venda registra `ficha_tecnica_id` da versão vigente; mudança posterior não altera histórico.
  - Venda integral com múltiplos insumos baixados via FEFO simultaneamente.

**Critérios de aceitação:**
- `mvn -pl recipes verify` passa.
- Teste de integração `vendaSimulada_baixaInsumosViaFefo_atualizaSaldoCorretamente` passa.

---

### T06 — operations (Transferência, Carga inicial, Ajuste manual)

**Objetivo:** Workflows operacionais com estados.

**Pré-requisitos:** T04.

**Entregáveis:**
- Migration `V007__create_operations_schema.sql`.
- Entidades: `Transferencia`, `ItemTransferencia`, `AjusteEstoque`, `CargaInicial`.
- State machine para `Transferencia` (`SOLICITADA → APROVADA → EM_TRANSITO → RECEBIDA / CANCELADA`).
- Use cases: `SolicitarTransferencia`, `AprovarTransferencia`, `RegistrarEnvioTransferencia`, `RegistrarRecebimentoTransferencia`, `CancelarTransferencia`, `LancarAjusteManual` (com aprovação obrigatória se quantidade > X), `ProcessarCargaInicial` (recebe lista ou planilha).
- Controllers REST.
- Endpoint `GET /api/v1/transferencias/em-transito` que retorna soma de saídas com status `EM_TRANSITO`.
- Importador de planilha (XLSX e CSV) para carga inicial — Apache POI ou OpenCSV.
- Testes cobrindo todos os caminhos do state machine, divergência no recebimento gera ajuste, carga inicial idempotente.

**Critérios de aceitação:**
- `mvn -pl operations verify` passa.
- Teste E2E (ainda em integração com `@SpringBootTest`) cobre transferência inteira: solicita, aprova, envia, recebe, valida saldos das duas filiais.

---

### T07 — alerts (Alertas configuráveis)

**Objetivo:** Sistema de alertas com escopo flexível.

**Pré-requisitos:** T04.

**Entregáveis:**
- Migration `V008__create_alerts_schema.sql`.
- Entidades: `AlertaConfig`, `AlertaDisparado`.
- `AvaliadorAlertasService` com algoritmo de match de escopo (mais específico primeiro).
- `@EventListener` reativo escutando `MovimentacaoSaidaCriada` (evento de domínio publicado pelo inventory-core).
- `@Scheduled` diário às 06:00 BRT que avalia alertas de tipo `VENCIMENTO_PROXIMO_DIAS`.
- Lógica de auto-resolução quando saldo volta acima do threshold.
- Use cases: `CriarAlertaConfig`, `AtualizarAlertaConfig`, `ListarAlertasDisparados` (com filtros), `MarcarAlertaResolvido`, `MarcarAlertaVisualizado`.
- Notificação inicial: apenas inserção em `alertas_disparados` (frontend lê por polling). Email e push ficam para ondas futuras.
- Testes cobrindo cada combinação de escopo, cada tipo de alerta, auto-resolução.

**Critérios de aceitação:**
- `mvn -pl alerts verify` passa.
- Teste integrado: cria config "estoque mínimo 20% para Mussarela na Filial Centro", lança saídas até cruzar, valida que `AlertaDisparado` é criado.

---

### T08 — reporting (Dashboards e relatórios)

**Objetivo:** Queries otimizadas para visões agregadas.

**Pré-requisitos:** T07.

**Entregáveis:**
- Migration `V009__create_reporting_views.sql` com views materializadas para queries pesadas (curva ABC, ruptura).
- Use cases (read-only): `PosicaoEstoquePorFilial`, `CurvaABC`, `RupturaIminente`, `VencimentoProximo`, `MovimentacaoPorPeriodo`, `DivergenciaInventario`.
- Refresh agendado das views materializadas (a cada 30 min ou sob demanda).
- Controllers REST somente leitura.
- Testes verificando ordenação correta, filtros, performance (consultar 10k movimentações em < 500ms — teste com Gatling micro).

**Critérios de aceitação:**
- `mvn -pl reporting verify` passa.
- `GET /api/v1/relatorios/posicao?filialId=...` retorna em < 500ms para dataset de 1000 lotes.

---

### T09 — Consolidação API REST + OpenAPI

**Objetivo:** API unificada, documentada, com tratamento global de erros.

**Pré-requisitos:** T02–T08.

**Entregáveis:**
- `app/` agrega todos os módulos como dependência.
- `NonnasStockApplication.java` (main, com `@SpringBootApplication`, `@EnableScheduling`, `@EnableJpaAuditing`).
- `application.yml`, perfis `dev`, `test`, `prod`.
- `GlobalExceptionHandler` com `@ControllerAdvice` traduzindo todas exceções em RFC 7807 (Problem Details).
- springdoc-openapi configurado com tags por módulo, descrições em português, exemplos de request/response.
- Swagger UI acessível em `/swagger-ui.html` no perfil dev.
- Health checks: `/actuator/health` com indicadores customizados (banco, Flyway, latência média de queries).
- Logs estruturados em JSON em produção, em texto colorido em dev.
- CORS configurado para o frontend.
- Rate limiting básico (Bucket4j) — 100 req/min por IP no MVP.

**Critérios de aceitação:**
- `mvn verify` na raiz passa.
- App sobe com `mvn -pl app spring-boot:run`.
- Swagger UI lista todos endpoints.
- Erro de validação retorna 400 no formato Problem Details.
- ArchUnit valida que app/ é o único módulo a importar de todos os outros.

---

### T10 — Infra de testes consolidada (ArchUnit, JaCoCo, fixtures)

**Objetivo:** Estrutura de qualidade que sobrevive ao crescimento do projeto.

**Pré-requisitos:** T09.

**Entregáveis:**
- Módulo `quality-tests/` (não publicado, só roda em CI) com regras ArchUnit completas.
- JaCoCo agregado: report consolidado de todos os módulos, threshold mínimo configurado por módulo.
- Test fixtures compartilhados em `shared-kernel/src/test/java/.../testsupport/` (builders para entidades comuns).
- Configuração de Testcontainers reusável (`AbstractIntegrationTest` com Postgres compartilhado entre testes).
- PIT configurado mas não rodando ainda (workflow nightly).

**Critérios de aceitação:**
- `mvn verify` produz `target/site/jacoco-aggregate/index.html`.
- ArchUnit quebra build se alguém criar dependência ilegal (testar manualmente).
- Tempo total de `mvn verify` (raiz, todos módulos) < 5 min em máquina dev.

---

### T11 — CI/CD com GitHub Actions

**Objetivo:** Pipelines completos rodando.

**Pré-requisitos:** T10.

**Entregáveis:**
- `.github/workflows/pr.yml` finalizado.
- `.github/workflows/main.yml` com build de Docker image e push (registry a definir com cliente; usar GitHub Container Registry no início).
- `.github/workflows/nightly.yml` com PIT e placeholder Gatling.
- `Dockerfile` multi-stage (build com Maven, runtime com `eclipse-temurin:21-jre-alpine`).
- `docker-compose.prod.yml` (referência, deploy real é manual no MVP).
- `semantic-release` configurado.
- README com badges de build, cobertura, versão.

**Critérios de aceitação:**
- PR fictício passa todos os checks em < 10 min.
- Push em main constrói Docker image e publica no registry.
- Tag semântica é gerada automaticamente após primeiro merge com `feat:`.

---

### T12 — Frontend setup + tema Nonnas

**Objetivo:** Esqueleto do frontend com identidade visual aplicada.

**Pré-requisitos:** T11.

**Entregáveis:**
- `frontend/` inicializado com Vite + React + TypeScript.
- Tailwind configurado com paleta Nonnas (seção 9.1).
- shadcn/ui instalado e tematizado.
- Layout base: sidebar fixa, header com user info, área principal.
- Tela de login com a identidade da marca (logo, fonte Playfair no título, cores).
- Roteamento React Router com placeholders para todas as páginas.
- Cliente API Axios com interceptor JWT, tipos gerados a partir do OpenAPI exportado em T09.
- Storage seguro do JWT (httpOnly cookie ou storage criptografado — escolher e documentar).
- TanStack Query Provider, Zustand store mínimo (auth state).
- Tests setup (Vitest + RTL).

**Critérios de aceitação:**
- `npm run dev` roda em `localhost:5173`.
- Tela de login responde a credenciais inválidas/válidas, salvando JWT e redirecionando.
- Componente `Button` da shadcn renderiza com cor `brand.red`.
- `npm test` retorna 0 (mesmo com poucos testes ainda).

---

### T13 — Frontend cadastros (Filial com carga inicial, Insumo, Fornecedor, Ficha Técnica)

**Objetivo:** CRUDs principais com formulários ricos.

**Pré-requisitos:** T12.

**Entregáveis:**
- Página `/filiais` (listar, novo, editar, desativar).
- Página `/filiais/:id/carga-inicial` com dois modos: linha-a-linha e upload de planilha (preview antes de confirmar).
- Página `/insumos` (listar com filtros por categoria/ativo, novo, editar).
- Página `/fornecedores`.
- Página `/produtos` (produtos vendáveis).
- Página `/fichas-tecnicas` com editor de receita (insumos + quantidades + unidades), exibindo histórico de versões.
- Validação client-side com Zod, server-side via mensagens 400 do backend.
- Toast notifications (sucess/error) padronizadas.
- Loading states e empty states bem desenhados.

**Critérios de aceitação:**
- Todos os CRUDs funcionais com backend real.
- Carga inicial via planilha cobre cenário completo: upload, preview, validação, importação, saldo refletido.
- Acessibilidade básica: labels associadas, navegação por teclado, contraste adequado.

---

### T14 — Frontend operações (Movimentação, Transferência, Saldos, Alertas)

**Objetivo:** Operação diária do sistema.

**Pré-requisitos:** T13.

**Entregáveis:**
- Página `/estoque` com saldo por filial, filtros, indicadores visuais (ruptura, vencimento próximo).
- Página `/movimentacoes` com lançamento manual de entrada e saída, escolha de lote, conversão de unidade visual.
- Página `/transferencias` com workflow visual (kanban ou tabela com status), ações por estado.
- Página `/alertas` com aba "Configurações" e aba "Disparados", criar/editar/desativar configs, marcar disparado como resolvido.
- Dashboard `/` com cards de resumo (total de filiais, alertas ativos, transferências em trânsito, ruptura iminente) e gráficos Recharts.
- Filtro global de filial no header (afeta todas as views).

**Critérios de aceitação:**
- Operador consegue lançar uma entrada manual e ver saldo atualizar em tempo real (TanStack Query invalidate).
- Configurar alerta, simular venda que cruza threshold, alerta aparece na lista em < 5s.
- Transferência completa fim a fim pela UI (todas as 5 transições de estado).

---

### T15 — E2E + Hardening final

**Objetivo:** Smoke tests automatizados e prontidão para entrega.

**Pré-requisitos:** T14.

**Entregáveis:**
- Projeto `e2e/` com Playwright Java.
- Page Objects para login, filiais, insumos, estoque, transferências, alertas.
- 8 cenários smoke listados na seção 7.3.
- Workflow `main.yml` atualizado para rodar smoke E2E após deploy em staging.
- `docs/deployment.md` com guia passo-a-passo de deploy em produção (manual no MVP, automatizar depois).
- `docs/operations-runbook.md` com procedimentos de troubleshooting comuns (banco lento, alerta falso, rollback de migração).
- Audit completo de segurança: dependências (Trivy ou Snyk no CI), headers HTTP, validação de inputs, rate limiting.
- Performance: garantir que dashboard carrega em < 2s com 50 filiais e 5000 insumos.
- Backup automático configurado no banco de produção (script `pg_dump` agendado).
- Onboarding guide em `docs/onboarding.md` para novos devs entrarem no projeto em < 1 dia.

**Critérios de aceitação:**
- Suíte E2E roda verde em CI.
- Trivy scan sem CVEs críticas ou altas.
- Documento de deploy completo com cliente conseguindo seguir e colocar no ar.
- Tag `v1.0.0` gerada e GitHub Release publicada com binários e changelog.

---

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

### T19 — Telas administrativas (Empresa, Usuário, Categoria, Unidade)

**Objetivo:** fechar o gap do MVP 1.0 (seção 1.2) entregando telas administrativas para
as 4 entidades que têm dado/endpoint mas nunca ganharam UI: Empresa, Usuário,
Categoria de Insumo, Unidade de Medida. Sem essas telas, dropdowns dos formulários de
Filial e Insumo nascem vazios e travam o operador.

**Pré-requisitos:** T18 (v1.0.0 entregue).

**Entregáveis:**

Backend (`catalog` + `identity`):
- 4 use cases novos por entidade (Buscar/Atualizar/Ativar/Desativar) — total 16 use cases
  seguindo padrão estabelecido em T13.
- 4 endpoints novos por entidade — total 16: `GET /{id}`, `PUT /{id}`,
  `PATCH /{id}/ativar`, `PATCH /{id}/desativar`.
- `UpdateRequest` em cada DTO restringido aos campos editáveis:
  - `CategoriaInsumo`: `nome` (categoria_pai_id imutável após criação).
  - `UnidadeMedida`: `nome` (codigo e tipo imutáveis — codigo é UNIQUE).
  - `Empresa`: `razao_social` (cnpj imutável — UNIQUE).
  - `Usuario`: `nome` (email e senha em fluxos separados, fora deste T-task).
- `@PreAuthorize` consistente em todos os endpoints novos:
  - `categorias-insumo` e `unidades-medida`: `hasAnyRole('ADMIN','GERENTE')`.
  - `empresas`: `hasRole('ADMIN')`.
  - `usuarios`: `hasAnyRole('ADMIN','GERENTE')`.
- Desativar é soft-delete (`ativo=false`/`ativa=false`), nunca DELETE físico.
  Ativar é idempotente.
- IT por entidade cobrindo: criar+listar (já existe), buscar/{id}, editar (campo
  editável e bloqueio dos imutáveis), ativar/desativar, 401 sem JWT, 403 com role
  insuficiente.

Frontend (`frontend`):
- 4 features novas em `frontend/src/features/admin/{categorias,unidades,empresas,usuarios}/`,
  cada uma com `api.ts` + `<Entity>Page.tsx` + `<Entity>FormDialog.tsx` espelhando o
  padrão dos CRUDs T13.
- Sidebar ganha separador + seção "Administração" abaixo de "Relatórios" com 4 itens
  (icons lucide: Tag, Ruler, Building, Users).
- `RoleGuard` componente e hook (`useHasRole`) que ocultam itens do menu e bloqueiam
  acesso direto a rotas sem permissão (redirect a `/dashboard` + toast).
- 4 rotas novas em `AppRouter.tsx`, todas envoltas por `ProtectedRoute` + `RoleGuard`.
- Combo "Empresa" no `FilialFormDialog` passa a ter dados disponíveis a partir do
  primeiro cadastro feito na nova tela — sem alteração do componente.

E2E (`e2e`):
- Estender `SmokeE2ETest` com cenário admin: ADMIN cria empresa → cria filial vinculada
  → cria categoria → cria insumo usando essa categoria. Reusa `LoginPage` e
  `ApiClient`. Cenário roda antes do fluxo de carga inicial.

Documentação:
- `STATUS.md` atualizado, "Ordem de execução decidida" estendida com T19.
- Não cria ADR — não há decisão arquitetural nova; o padrão é o mesmo do T13.

**Critérios de aceitação:**
- Reactor `mvn verify` verde, 14/14 SUCCESS, ArchUnit sem violações novas.
- Cobertura mantida: ≥85% domain, ≥75% application, ≥70% global.
- 4 telas administrativas acessíveis em `/admin/{categorias,unidades,empresas,usuarios}`.
- Sidebar mostra seção "Administração" só para usuários com role apropriado;
  para `OPERADOR`/`CONSULTA` a seção não aparece.
- Acesso direto a `/admin/empresas` sem role `ADMIN` redireciona para `/dashboard`
  com toast informativo.
- Modal "Nova Filial" passa a listar empresas cadastradas. Modal "Novo Insumo" passa
  a listar categorias cadastradas pela tela. Sem regressão nos modais existentes.
- Edição parcial respeita imutáveis: tentar editar `cnpj` da empresa, `email` do
  usuário, `codigo` da unidade ou `categoria_pai_id` da categoria via PUT retorna
  o registro inalterado nesses campos.
- Smoke E2E verde, incluindo o novo cenário admin.
- `npm run build` e `npm test` verdes.
- Commit no padrão Conventional Commits (`feat(admin): T19 — telas administrativas...`).
- `STATUS.md` atualizado com data, hash e nota.

**Não-escopo (registrado para ondas futuras):**
- Hierarquia `categoria_pai_id` na UI (combo "Categoria pai").
- Página de Conversões de Unidade (ortogonal ao CRUD de Unidade).
- Reset/troca de senha de usuário (precisa fluxo de auth dedicado).
- UI de feature flags (T18 deixou seed e service; UI fica para depois).
- Página de canais de venda (MVP 1.2).
- Wizard 2FA pelo próprio usuário (gap T16 já registrado).

---

## 11. Definition of Done

### Por tarefa
- Todos os entregáveis listados foram criados.
- Todos os critérios de aceitação foram verificados manualmente ou por teste automatizado.
- `mvn verify` (e `npm test` no frontend) verde.
- Cobertura mínima respeitada (85% domain, 75% application, 70% global).
- ArchUnit verde.
- Commits no padrão Conventional, escopo correto.
- `STATUS.md` atualizado.
- PR aprovado e mergeado.

### Por release (versão)
- Tag semântica gerada por `semantic-release`.
- CHANGELOG.md atualizado.
- GitHub Release publicada.
- Imagem Docker disponível no registry.
- Smoke E2E verde em staging.
- Documentação atualizada.

---

## 12. Anexos

### Anexo A: Comando inicial pro Claude Code

Quando o usuário disser "inicie", o Claude Code deve responder:

> Lendo o documento PROMPT_CLAUDE_CODE.md por completo. Próxima tarefa pendente identificada em STATUS.md: [TXX]. Antes de executar, vou listar as decisões técnicas que pretendo tomar dentro dela.

E aí lista, espera confirmação, e executa.

### Anexo B: Template de STATUS.md inicial

```md
# Status das Tarefas — Nonnas Stock

| Tarefa | Estado | Data | Commit | Nota |
|--------|--------|------|--------|------|
| T00 | pendente | — | — | — |
| T01 | pendente | — | — | — |
| T02 | pendente | — | — | — |
| T03 | pendente | — | — | — |
| T04 | pendente | — | — | — |
| T05 | pendente | — | — | — |
| T06 | pendente | — | — | — |
| T07 | pendente | — | — | — |
| T08 | pendente | — | — | — |
| T09 | pendente | — | — | — |
| T10 | pendente | — | — | — |
| T11 | pendente | — | — | — |
| T12 | pendente | — | — | — |
| T13 | pendente | — | — | — |
| T14 | pendente | — | — | — |
| T15 | pendente | — | — | — |
```

### Anexo C: Prompts úteis durante o desenvolvimento

- **Validar arquitetura:** "Rode mvn verify e me mostre o relatório de ArchUnit. Há violações?"
- **Refatorar com segurança:** "Antes de refatorar X, liste todos os testes que cobrem essa área. Se cobertura < 80%, escreva mais testes primeiro."
- **Investigar bug:** "O endpoint Y está retornando 500. Liste a stack trace, identifique a causa raiz, escreva um teste que reproduz, depois corrija."
- **Adicionar feature:** "Quero adicionar campo Z em Insumo. Liste todas as camadas afetadas (migration, entity, DTO, mapper, validation, frontend), implemente uma por uma, com teste em cada."
- **Performance:** "Query da página de saldos está lenta. Rode EXPLAIN ANALYZE, identifique gargalo, proponha índice ou refatoração, valide com benchmark antes/depois."

### Anexo D: Ondas futuras (não escopo do MVP 1.0)

**MVP 1.1 — Importador NF-e** (após go-live do MVP 1.0)
- Parser XML NF-e modelo 55.
- Engine de de-para fornecedor-produto-insumo.
- Fila de pendências para itens novos.
- Conversão de unidade automática.
- Idempotência por chave de acesso.

**MVP 1.2 — Integrações de canais**
- Adapter pattern em `sales-channels-api`.
- iFood for Restaurants (webhook + OAuth).
- PDV salão (depende do sistema usado pelo cliente — descoberta técnica).
- 99Food (descoberta técnica).
- Keeta (descoberta técnica).
- Conciliação cruzada de pedidos vs movimentações.

**V2 — Mobile**
- React Native + Expo.
- Telas: consulta de saldo por filial, lançamento de ajuste com foto, notificações push.
- Backend: extensões para push (Firebase Cloud Messaging) e autenticação mobile (refresh tokens longos).

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

**Fim do documento.**

Última atualização: integração do adendo de profissionalização (T16, T17, T18 + seções 13–15).
Versão: 1.1
Autor: Jefferson Pacheco Agostinho com auxílio do Claude.
