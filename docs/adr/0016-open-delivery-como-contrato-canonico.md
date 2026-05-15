# ADR 0016 — Open Delivery como contrato canônico de canais de venda

- **Status:** Aceita
- **Data:** 2026-05-15
- **Contexto da decisão:** MVP 1.2 prevê integração com iFood, 99Food e Keeta (master doc 1.2 / seção 25.MVP 1.2). Os três canais convergiram em 2025 para o padrão **Open Delivery** (iniciativa Abrasel) — REST API pública, com spec OpenAPI versionada no [repositório público da Abrasel](https://github.com/Abrasel-Nacional). Construir 3 adapters específicos é desperdício; construir 1 contrato canônico Open Delivery torna cada canal um plug.

## Contexto

Cenário em 2026-05:

| Canal | API pública? | Sandbox self-service? |
|---|---|---|
| iFood | Sim — Merchant API + Order API documentadas; **OAuth2 client-credentials** | Sim — `developer.ifood.com.br` (em 2026-05-15 o cadastro estava intermitente) |
| 99Food | **Apenas via Open Delivery** (integração concluída em nov/2025) | Não, sem portal próprio |
| Keeta | **Apenas via Open Delivery** (integração concluída em nov/2025) | Não, sem portal próprio |

Open Delivery v1.0.1 é a versão estável; v1.7.0-rc.2 em release candidate (adiciona campos opcionais, retro-compatível). O conjunto mínimo necessário pra "receber pedido + baixar estoque" é estável desde v1.0.0.

iFood implementa Open Delivery **adicionalmente** à sua API proprietária; campos extras (cancelamento com motivo enumerado, eventos de entregador) ficam fora do padrão mas o `Order` central é o mesmo.

## Decisão

1. **Contrato canônico interno = subset Open Delivery v1.0.1**, capturado em `PedidoVendaCanonico` (record Java) + DTOs auxiliares em `sales-channels-api/application/opendelivery/`.
2. **Cada canal implementa `CanalAdapter`** (port em `application/ports/CanalAdapter.java`) — a tradução do payload nativo do canal para `PedidoVendaCanonico` é responsabilidade exclusiva do adapter.
3. **Adapter iFood específico** é uma instância de `CanalAdapter` que sabe fazer OAuth2 client-credentials + entender campos exclusivos iFood. **NÃO inventa um contrato paralelo** — converte tudo pra Open Delivery internamente.
4. **Polling em vez de webhook** no POC (T-CANAL-03), por não exigir URL pública (ngrok/cloudflared). Open Delivery suporta tanto polling quanto webhook com a mesma semântica de eventos; trocar é mudar 1 classe.

## Estrutura do módulo (T-CANAL-00..02)

```
sales-channels-api/
├── domain/                            ← agnóstico do contrato externo
│   ├── CanalTipo  (IFOOD, NOVENTANOVE_FOOD, KEETA, OPEN_DELIVERY_GENERICO)
│   ├── StatusPedidoCanal (state machine)
│   ├── TipoEventoCanal
│   ├── CredencialCanal  (segredo cifrado AES-256-GCM via CryptoService T16)
│   ├── PedidoCanal      (aggregate root; itens + transições)
│   ├── ItemPedidoCanal
│   └── EventoCanal      (idempotência por event_id_externo)
├── application/
│   ├── ports/
│   │   ├── CanalAdapter                  ← implementado por cada canal externo
│   │   ├── CredencialCanalRepository
│   │   ├── PedidoCanalRepository
│   │   └── EventoCanalRepository
│   └── opendelivery/                     ← CONTRATO CANÔNICO
│       ├── PedidoVendaCanonico  (record top-level)
│       ├── OpenDeliveryItem / Merchant / Customer / Total / Price / Option
│       ├── OpenDeliveryOrderType / OpenDeliveryUnit
│       ├── EventoBruto
│       └── PedidoCanonicoMapper          ← Canônico → domain (stateless puro)
├── infrastructure/persistence/           ← JPA: 4 entities + 3 repos + mappers
└── resources/db/migration/
    ├── V025__create_canais_credenciais.sql
    ├── V026__create_pedidos_canais.sql
    └── V027__create_eventos_canais.sql
```

## Por que polling > webhook no POC

| Critério | Polling (escolhido) | Webhook |
|---|---|---|
| Roda local (dev) | Sim | Não — exige ngrok/cloudflared |
| Roda em CI | Sim | Não |
| Cenários determinísticos | Sim | Não (depende do canal pushar) |
| Latência típica | 30s (config) | <1s |
| Custo de migrar para o outro | Trocar `IFoodPollingScheduler` por `IFoodWebhookController` | — |

Open Delivery especifica os dois modos com o mesmo modelo de eventos (PLC/CFM/DSP/CON/CAN). A migração é uma classe nova, mantendo o mapper e domínio idênticos. Para o POC focar em validar a baixa de estoque ponta-a-ponta, polling é menor risco.

## Idempotência

Duas chaves de idempotência protegem reprocessamento (essencial em polling):

1. **`uq_pedidos_canais_externo`** em `(canal_tipo, pedido_externo_id)` — bloqueia criar o mesmo pedido 2 vezes.
2. **`uq_eventos_canais_externo`** em `(canal_tipo, event_id_externo)` — bloqueia reprocessar o mesmo evento.

`EventoCanalRepository.salvarSeNovo` faz pre-check + try/catch em `DataIntegrityViolationException` para tratar race condition entre polling threads.

## Subset cobrido pela canônica (POC)

Capturados em `PedidoVendaCanonico` + tipos auxiliares:

- `id`, `displayId`, `type`, `salesChannel`, `createdAt`, `extraInfo`
- `merchant` (id, name)
- `customer` (id, name, phone) — **sem** documentNumber/email (LGPD)
- `items[]` (id, index, externalCode, name, quantity, unit, unitPrice, totalPrice, observations, options[])
- `total` (itemsPrice, otherFees, discount, orderAmount, currency)

**Fora do POC** (capturados em comentário no record para futuro):
- `payments` (não afeta baixa de estoque)
- `delivery` (logística)
- `schedule` (pedidos agendados)
- `indoor` (atendimento presencial em mesa)

Adicionar campos novos é estender o record — não quebra adapters existentes.

## Por que não usar OpenAPI codegen direto?

A spec Open Delivery está em YAML e poderíamos gerar DTOs via `openapi-generator`. Decidimos não fazer isso por agora:

1. **Subset POC tem 9 classes** — manual é tão rápido quanto configurar codegen.
2. **Codegen acopla nossa build à versão do plugin** + lifecycle de regen; perde-se controle de naming/anotações.
3. **Versão da spec é alvo móvel** (v1.0.1 → v1.7.0-rc.2). Subset estável manual sobrevive a evolução; codegen do RC quebraria.

Quando o subset crescer (T-CANAL-06+, cardápio outbound, cancelamento rico), reavaliamos.

## Implicações de segurança

- `client_secret_cifrado` em `canais_credenciais` é AES-256-GCM via `CryptoService` (T16) — chave mestra `NONNAS_MASTER_KEY`. Domínio nunca vê o segredo em claro.
- `payload_bruto_json` (JSONB) pode conter dados pessoais do cliente (nome, telefone). Política de retenção fica em ADR futuro (provavelmente 90d com purga via cron, alinhado a LGPD).
- Endpoints de credencial (`/api/v1/canais/credenciais`) terão `@PreAuthorize("hasRole('ADMIN')")` (T-CANAL-05).
- Validação de origem (HMAC do payload do canal) entra com o adapter iFood concreto (T-CANAL-03).

## Não-escopo desta ADR (gaps registrados)

- **`CanalAdapter` concreto** — só interface foi definida em T-CANAL-02; implementação iFood/genérica vem em T-CANAL-03.
- **`ProcessarPedidoCanalUseCase`** — orquestrador que baixa estoque via `recipes`/`inventory-core` vem em T-CANAL-04. Hoje sales-channels-api é standalone (ArchUnit `salesChannels_isStandaloneAteTCANAL04`).
- **De-para `externalCode → ProdutoVendavel`** — vai virar tabela `canal_produto_depara` (T-CANAL-04).
- **Cardápio outbound** (enviar produtos pro canal) — não é POC. Fica em T-CANAL-06+.
- **Cancelamento com motivo enumerado** — Open Delivery v1.7.0-rc.2 adiciona; quando virar estável, atualizamos.

## Consequências positivas

- 1 adapter Open Delivery genérico cobre 99Food + Keeta + qualquer canal futuro que aderir ao padrão. iFood ganha adapter específico pra OAuth/eventos exclusivos, mas o miolo (`Order → PedidoVendaCanonico`) é compartilhado.
- POC roda contra mock server local (Prism + spec pública Open Delivery YAML) sem credencial. Quando a credencial iFood sair, troca de baseURL + injeção de OAuth.
- `payload_bruto_json` preservado garante forensics: se o mapper acusar divergência, temos o pedido original.

## Consequências negativas

- Open Delivery v1.0.1 cobre o caso comum, mas campos iFood-only (ex.: eventos `RPO`/`RPR` de entregador) precisam de extensão própria — perde-se parte da portabilidade quando o adapter iFood cresce.
- Subset POC reflete decisão de hoje; adicionar pagamento/delivery posteriormente exigirá migration para enriquecer o JSONB salvo (ou aceitamos que pedidos antigos não têm esses dados).
- Polling tem ~30s de latência ante webhook quase imediato. Aceitável pra dev/POC; produção pode querer migrar (apontado em T-CANAL-03 follow-up).

## Alternativas consideradas

### A — Adapter específico por canal sem contrato canônico interno
Cada canal vira um módulo (`sales-channels-ifood`, `sales-channels-99food`, ...). Use cases consomem DTOs específicos. **Rejeitada**: triplica código de baixa de estoque e cria 3 fontes de verdade de "pedido recebido".

### B — Adotar Open Delivery v1.7.0-rc.2 como base
Mais campos, mais futuro-proof. **Rejeitada por agora**: RC pode mudar; subset v1.0.1 cobre o POC. Migrar é aditivo (campos novos), sem breaking change esperado.

### C — Webhook desde o POC
Mais elegante operacionalmente. **Rejeitada por agora**: exige ngrok ou deploy em URL pública só pra POC, complicando dev local e CI sem ganho funcional. Migração é trivial quando deploy de produção existir.

### D — Codegen via openapi-generator
Avaliada acima. **Rejeitada** pelo trade-off de controle vs. economia (~50 linhas).
