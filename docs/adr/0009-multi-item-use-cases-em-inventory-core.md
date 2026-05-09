# ADR 0009 — Use cases multi-item em `inventory-core` para eventos atômicos N-insumo

- **Status:** Aceita
- **Data:** 2026-05-08
- **Contexto da decisão:** T05 introduziu `RegistrarSaidaMultiItemUseCase`; T06 introduz `RegistrarEntradaMultiItemUseCase`. Antes que outros módulos tomem decisão ad-hoc, formaliza-se o padrão.

## Contexto

`inventory-core` originalmente expôs use cases single-item:
- `RegistrarEntradaManualUseCase` — 1 lote, 1 movimentação. Pensado para a UI de entrada manual (operador escolhe um insumo por vez).
- `RegistrarSaidaManualUseCase` — 1 insumo, N lotes (FEFO), 1 movimentação. UI de saída manual.

Conforme módulos consumidores foram ganhando casos de uso atômicos N-insumos:
- **T05 recipes** — `RegistrarVendaSimuladaUseCase`: 1 venda baixa N insumos da ficha técnica simultaneamente. Loop de N saídas single-item criaria N movimentações `SAIDA_VENDA` que parecem vendas independentes (errado: foi UMA venda).
- **T06 operations transferência** — recebimento gera N lotes na filial destino (um por insumo recebido). Loop de N entradas single-item criaria N movimentações `ENTRADA_TRANSFERENCIA` (errado: foi UM recebimento).
- **T06 operations carga inicial** — abertura de filial pode ter centenas de linhas de planilha. Uma única movimentação consolida o evento "filial X foi aberta com este inventário" (errado seria N movimentações individuais).

Ambas as opções resolvem o problema de saldo — a soma é a mesma. Mas a **trilha de auditoria** difere:
- N movimentações: difícil reconstruir "qual evento de negócio originou estas N entradas?"
- 1 movimentação: relação 1:1 entre evento de negócio e movimentação. `documento_origem_tipo` + `documento_origem_id` apontam direto para a venda/transferência/carga.

## Decisão

**Sempre que um evento de negócio afetar N insumos atomicamente, expor um use case multi-item dedicado em `inventory-core` que produz UMA movimentação consolidada com N `ItemMovimentacao`.**

Use cases multi-item até o momento:
- `RegistrarSaidaMultiItemUseCase` — N insumos, FEFO por insumo, 1 `Movimentacao` (T05).
- `RegistrarEntradaMultiItemUseCase` — N lotes novos, 1 `Movimentacao` (T06).

Critério de decisão para futuros módulos:
- **Use case multi-item** — quando o que está sendo registrado é UM evento (uma venda, um recebimento, uma carga). A movimentação consolidada captura corretamente a unidade da transação.
- **Loop single-item** — quando são N eventos verdadeiramente independentes (operador faz 5 ajustes manuais de 5 insumos diferentes ao longo da manhã, sem unidade de negócio entre eles).

Os use cases single-item originais **permanecem** — UI de entrada/saída manual continua funcionando. Não há plano de removê-los.

Implementação:
- Use case multi-item recebe `List<ItemEntrada/ItemSaida>` no Comando.
- `@Transactional`: falha em qualquer item rola back todos os lotes/movimentações.
- Publica **um único** `MovimentacaoCriadaEvent` (não um por item).
- Listeners reativos (T07 alerts, T08 reporting) recebem a movimentação consolidada — economiza disparos.

## Consequências

**Positivas:**
- Trilha de auditoria 1:1 entre evento de negócio e movimentação.
- Atomicidade transacional natural — o conceito de evento atômico é refletido no banco.
- Listeners (`@EventListener`) processam um evento por venda/transferência/carga, não N — menos overhead em T07/T08.
- Linguagem ubíqua reforçada: o domínio fala "uma venda baixou os insumos da pizza", não "cinco saídas independentes que por acaso aconteceram juntas".

**Negativas:**
- Duplicação leve entre single-item e multi-item (lógica FEFO compartilhada via `SelecionarLotesPorFefoService`, mas montagem de `ItemMovimentacao` se repete).
- API surface de `inventory-core` mais larga: 4 use cases em vez de 2.
- Consumidores precisam decidir qual usar — ADR é o guia.

**Mitigações:**
- ArchUnit (T13) pode adicionar regra de detecção: "use case que faz `for { saidaManual.execute(...) }` é antipattern — usar multi-item".
- Code review: revisores conhecem este ADR e cobram o padrão.
- Documentação inline em cada use case linka esta ADR.

## Referências

- ADR 0001 — Modular Monolith (princípio: módulos comunicam via contratos públicos, não SQL cross-schema).
- ADR 0004 — FEFO como estratégia de saída (algoritmo é compartilhado entre single e multi-item).
- ADR 0008 — `recipes` depende de `inventory-core` em escopo `compile` (mecanismo da consumação).
- PROMPT_CLAUDE_CODE.md seção 4 (princípio "saldo é projeção, movimentação é a verdade").
