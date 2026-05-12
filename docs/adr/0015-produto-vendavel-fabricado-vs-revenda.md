# ADR 0015 — Produto vendável: FABRICADO vs REVENDA

- **Status:** Aceita
- **Data:** 2026-05-12
- **Contexto da decisão:** Operação real numa pizzaria/churrascaria mostrou que o MVP forçava ficha técnica para **todo** produto vendável. Itens de revenda (Coca-Cola, sorvete fechado, balas) não fabricados internamente passaram a exigir uma ficha técnica artificial "1 produto = 1 unidade do insumo X", que é ruído puro pro operador e polui a tela `/fichas-técnicas`.

## Contexto

Modelo original (T05, ADR 0005 + 0008):
- `ProdutoVendavel` (recipes) é o item de cardápio com preço.
- `FichaTecnica` versionada conecta o produto a N insumos (catalog) com quantidades.
- `RegistrarVendaSimuladaUseCase` busca a ficha vigente e gera 1 `Movimentacao SAIDA_VENDA` multi-item via FEFO em inventory-core.

Esse modelo é certo pra pizza/lasanha/churrasco — produção real, com receita versionada e desvio que importa pra custo.

Não cabe pra Coca-Cola: comprou da Ambev, baixa 1 garrafa do estoque, fim. Ficha de 1 item agrega zero valor e polui o cadastro.

## Decisão

Introduzir o campo `tipo` em `ProdutoVendavel`:

| Tipo | Como vende | Quando usar |
|---|---|---|
| `FABRICADO` | Ficha técnica vigente, baixa N insumos via FEFO/AGREGADOR (regime atual) | Pizza, lasanha, churrasco — produção real com receita |
| `REVENDA` | Vincula direto a **um** insumo, baixa 1:1 do estoque, **sem ficha técnica** | Coca-Cola, sorvete fechado, balas, água — comprou da indústria, revende |

`tipo` é imutável após criação. Misturar caminhos no mesmo produto significaria reescrever histórico de venda.

## Implementação

Schema (`recipes/V022`):
- `produtos_vendaveis.tipo VARCHAR(20) NOT NULL DEFAULT 'FABRICADO'` — preserva produtos antigos.
- `produtos_vendaveis.insumo_revenda_id UUID` — sem FK física (cross-context é validado em runtime, padrão InsumoFilial T03).
- 3 CHECKs: `tipo IN ('FABRICADO','REVENDA')`, REVENDA exige insumo, FABRICADO proíbe insumo.
- Index parcial em `insumo_revenda_id WHERE NOT NULL`.

Domain (`ProdutoVendavel`):
- Construtor canonical aceita tipo + insumoRevendaId; valida coerência localmente.
- Factories `novoFabricado(...)` e `novoRevenda(...)`.
- `tipo` final, sem setter.

Validação cross-context na criação de REVENDA:
- Novo port `CatalogQueries.findUnidadeBaseDoInsumo(UUID)` em recipes/application/ports.
- Impl via `NamedParameterJdbcTemplate` (SQL nativo, ADR 0010) — recipes não importa catalog via Maven.
- `Optional.empty()` indica que o insumo não existe → `NotFoundException` na criação.

Use cases:
- `RegistrarVendaSimuladaUseCase` bifurca:
  - `FABRICADO`: caminho atual (ficha técnica, `documento_origem_tipo='FICHA_TECNICA'`).
  - `REVENDA`: 1 `ItemSaida` do insumo vinculado, `documento_origem_tipo='PRODUTO_REVENDA'`, `documento_origem_id=produto.id`.
- `PreviewVendaSimuladaUseCase` bifurca da mesma forma (mantém preview por trás de venda funcionando pros dois tipos).
- `CriarFichaTecnicaUseCase` rejeita produto REVENDA com `BUSINESS_RULE_VIOLATED`.

API:
- `ProdutoVendavelDto.Request` ganha `tipo` (default FABRICADO se omitido) e `insumoRevendaId`.
- `Response` expõe ambos.
- `GET /api/v1/produtos-vendaveis?tipo=FABRICADO` filtra para alimentar `/fichas-técnicas`.

## Decisões secundárias

### Por que sem FK física para `insumos`

Mesma justificativa de T03 `InsumoFilial.filial_id`: cross-context FK acopla bounded contexts no nível físico do banco, complicando refactor futuro. A validação em runtime via `CatalogQueries.findUnidadeBaseDoInsumo` é suficiente — falhas resultam em 404 explícito no momento da criação ou primeira venda.

### Por que `tipo` é imutável

Histórico de movimentações já gravado com `documento_origem_tipo='FICHA_TECNICA'` ou `PRODUTO_REVENDA` deixa de fazer sentido se o produto trocar de tipo retroativamente. Alternativa (permitir troca apenas se sem vendas) adiciona invariante que precisaria ser verificada — não compensa pro caso de uso. Operador que errar o tipo pode desativar o produto e criar outro.

### Por que default `FABRICADO`

Migração V022 default `'FABRICADO'` preserva todos os produtos existentes (que dependem de ficha técnica). Nada quebra retroativamente.

## Não-escopo (gaps registrados)

- **Preço de venda**: o domínio atual de ProdutoVendavel não armazena preço. POS MVP (commit `3746871`) também não — venda registra só a baixa de estoque. Adicionar preço é onda separada (sale receipt + emissão fiscal).
- **Edição de tipo**: sem caminho de upgrade FABRICADO ↔ REVENDA. Operador resolve via desativar+criar.
- **Múltiplos insumos por produto REVENDA**: por design um produto REVENDA tem **um** insumo. Combo de 2 produtos (kit) é FABRICADO com ficha de 2 itens.

## Consequências positivas

- Tela `/fichas-técnicas` para de poluir com itens triviais 1:1.
- Operador vê na criação do produto qual fluxo está escolhendo, em vez de descobrir depois.
- Vendas continuam invisíveis pro operador (tela `/vendas` lista tudo junto, conforme memória `feedback_ux_jefferson` "operador chama tudo de produto").

## Consequências negativas / Aceitas

- 1 novo port (`CatalogQueries`) em recipes — pequeno aumento de superfície, justificado pela validação explícita na criação.
- `RegistrarVendaSimuladaUseCase` agora depende de `ProdutoVendavelRepository` (antes só `FichaTecnicaRepository`). Trivial — produto já existia, faltava só consultar.
- ArchUnit não precisa de regra nova: SQL nativo cross-context já está no padrão ADR 0010.
