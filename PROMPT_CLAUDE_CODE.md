# PROMPT CLAUDE CODE — Nonnas Stock

**Sistema profissional de controle de estoque centralizado para a rede Nonnas Paola (churrascaria e pizzaria, multi-filial em São Paulo, multi-canal de venda).**

Documento mestre de orientação para desenvolvimento assistido. Lido por inteiro antes de cada tarefa. Atualizado a cada entrega.

- **Repositório**: `C:\dev\nonnas-stock`
- **Modelo comercial**: preço fechado por entregáveis, hora-base R$ 200/h
- **Equipe**: Jefferson Pacheco Agostinho (BA/QA, condução do projeto) + Edwagney Luz (dev sênior Java)

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

**Fim do documento.**

Última atualização: setup inicial do projeto.
Versão: 1.0
Autor: Jefferson Pacheco Agostinho com auxílio do Claude.
