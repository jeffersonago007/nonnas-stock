# ADR 0014 — Lote opcional via flag `controla_validade`

- **Status:** Aceita
- **Data:** 2026-05-10
- **Contexto da decisão:** O master doc (princípio 3, §1.3) cravava "saldo por (lote, filial), saída FEFO obrigatória". Operação real do restaurante mostrou que isso é overhead pra insumos que não controlam validade (descartáveis, embalagens, materiais de limpeza). O adendo técnico [`docs/adendo-lote-opcional.md`](../adendo-lote-opcional.md) propôs um regime opcional, e este ADR registra como ficou implementado em T-LOT-01..09.

## Contexto

Insumos do MVP eram universalmente rastreados: cada entrada cria um lote físico com numero + validade. A saída é FEFO (First Expired, First Out). Bom pra mussarela e tomate pelado; trabalho desnecessário pra guardanapo, papel filme, sabão de panela.

A pressão veio do uso real:
- Operador estava preenchendo "validade qualquer" só pra conseguir lançar guardanapo no estoque.
- Relatório de vencimento próximo tinha lixo (lotes de descartáveis "vencendo" em 2030).
- Cadastro de fornecedor com lista cheia de itens forçava número de lote inventado.

## Regime

Cada `Insumo` já tem `controla_validade boolean` (V003, default `true`). T-LOT formaliza o que esse flag significa:

- `controla_validade = true` → **regime RASTREADO**: cada entrada cria um `Lote` novo com `numero_lote` e (idealmente) `data_validade`. Saída por FEFO. Comportamento original.
- `controla_validade = false` → **regime AGREGADOR**: existe um lote único por insumo (`tipo = 'AGREGADOR'` em `lotes`), criado lazy. Toda entrada some saldo nesse lote (na filial onde caiu). Saída direta — não passa por FEFO. Sem datas, sem numero.

O `Lote` é a fronteira do regime, não o `Insumo` — porque inventory-core não importa catalog (ArchUnit) e a decisão tem que ser tomada lá dentro também.

## Decisões

### Decisão 1 — Lote universal vs por filial

Opções:
- **A.** Lote universal (uma linha por insumo, no caso AGREGADOR); saldo por filial vive em `saldos_lotes(lote_id, filial_id)`. **Escolhida.**
- B. Lote por `(insumo, filial)` como o adendo literalmente propõe.

**Por quê A:** a tabela `lotes` (T04) é universal há 6+ tasks. Migrar pra `(insumo, filial)` exigiria reformular `saldos_lotes` PK, `ItemMovimentacao.lote_id`, FEFO query, vencimento job — refator pesado pra um ajuste que o modelo atual já suporta. O resultado é equivalente: AGREGADOR único por insumo + `saldos_lotes(lote_id, filial_id)` dá a mesma granularidade "saldo agregado por filial".

Migração: `inventory-core/V020` adiciona coluna `tipo` (default `RASTREADO` preserva legacy), drop NOT NULL em `numero_lote`, 3 CHECKs garantindo que AGREGADOR é vazio em numero_lote/data_validade/data_fabricacao, UNIQUE parcial `(insumo_id) WHERE tipo='AGREGADOR'`.

### Decisão 2 — Criação lazy vs eager do agregador

Opções:
- **A.** Lazy: `BuscarOuCriarLoteAgregadorUseCase` cria no primeiro uso. **Escolhida.**
- B. Eager: ao virar `controla_validade=false`, migration cria agregador.

**Por quê A:** flag pode oscilar; eager produziria lote inútil se o insumo voltar pra RASTREADO antes de ter entrada. Lazy garante que só existe agregador quando há saldo de fato. Idempotência protegida pelo unique partial index (corrida entre transações: a perdedora recebe `DataIntegrityViolationException` no commit, rollback completo é aceitável porque o caller já está numa transação maior).

### Decisão 3 — Onde a decisão de regime vive

Opções:
- A. Use case consulta `Insumo.controla_validade` antes de criar lote — exige inventory-core importar catalog, viola ArchUnit.
- **B.** Caller passa `controlaValidade boolean` em `ItemEntrada`; inventory-core decide localmente. **Escolhida.**
- C. Resolver pelo estado do lote (se existe agregador, é AGREGADOR) — fechamento sobre dados, mas exige lookup em todo recebimento.

**Por quê B:** mantém o invariante arquitetural intacto. Cada caller de `RegistrarEntradaMultiItemUseCase` é responsável por preencher a flag:
- `nfe-importer/ProcessarNotaFiscalUseCase`: fetch do `Insumo` resolvido (já consulta catalog para validar id) — usa `insumo.controlaValidade()`.
- `operations/RegistrarRecebimentoTransferenciaUseCase`: operations não vê catalog. Decisão pragmática: `loteRepo.findAgregadorByInsumo(insumoId).isPresent()` → se já tem agregador, esse insumo é AGREGADOR; senão, RASTREADO. Aproveita estado do banco em vez de consulta cross-context (variante da Opção C, restrita a transferência).
- `operations/ProcessarCargaInicialUseCase`: sempre `true` (RASTREADO). Carga inicial é momento zero — gap aceito: se o insumo virar AGREGADOR depois, o lote da carga fica órfão. Não justifica complexidade.

### Decisão 4 — Polimorfismo de saída

Opções:
- A. Duas use cases distintas para saída (uma FEFO, uma agregador). Caller decide qual chamar.
- **B.** Um único `SelecionarLotesParaSaidaService` que delega para FEFO ou retorna alocação única do agregador. **Escolhida.**

**Por quê B:** caller (`RegistrarSaidaMultiItemUseCase`, `RegistrarSaidaManualUseCase`, `RegistrarVendaSimuladaUseCase` em recipes) não muda. O service decide via `LoteRepository.findAgregadorByInsumo`: se existe → toda quantidade pra ele (saldo pode ficar negativo, mantém invariante "venda não bloqueia"); senão → FEFO.

### Decisão 5 — Onde vive a constraint "RASTREADO precisa de número e validade"

Opções:
- A. Schema com CHECK estrito (NOT NULL em numero_lote quando tipo=RASTREADO, NOT NULL em data_validade quando RASTREADO).
- **B.** Schema só impede o que claramente é inválido pra AGREGADOR (3 CHECKs de "é null"); invariante "RASTREADO precisa de número" vive no factory `Lote.novoRastreado`. **Escolhida.**

**Por quê B:** lotes RASTREADO legacy podem ter sido criados sem `data_validade` em carga inicial antiga (o flag default era `true` mas o schema nunca obrigou validade no lote — só na criação via UI). Schema estrito quebra migration retroativa. Factory protege novos lotes; schema protege a invariante de AGREGADOR.

## Consequências

**Boas:**
- Operador para de inventar validade pra guardanapo.
- Relatórios de vencimento ficam limpos (lotes AGREGADOR não têm validade).
- Movimentação histórica preservada: agregador vai pra `documento_origem_*` igual ao rastreado.
- ArchUnit nova rule `loteAgregador_soAcessadoViaUseCase` (T-LOT-09) protege contra alguém chamar `Lote.novoAgregador` fora do use case dedicado.

**Ruins / aceitas:**
- Custo médio do agregador zerado: lote AGREGADOR sempre tem `valor_unitario=0`. Custo real vive no `ItemMovimentacao.valor_unitario` por entrada — relatório de custo precisa fazer agregação manual. Custo médio ponderado fica para uma onda futura.
- Carga inicial não consulta regime do insumo (sempre cria RASTREADO). Lotes de carga inicial podem ficar órfãos se o insumo virar AGREGADOR depois. Migration retroativa fica fora de escopo.
- `RegistrarEntradaManualUseCase` (saída single-item via REST `POST /movimentacoes/entrada`) não recebe o flag — sempre cria RASTREADO. UI atual não conhece `controla_validade` por insumo no ato da chamada; corrigir exige mudança de contrato. Aceito como gap dado que o caminho quente operacional é via NF-e (T20).
- Validação "não permitir desativar `controla_validade` se há lotes rastreados com saldo > 0" (sugerida pelo adendo) **não foi implementada**: operações cross-context (catalog precisa consultar inventory-core) violam ArchUnit. Aceito como gap — admin pode fazer manualmente via SQL se necessário. ADR poderá ser revisitado quando houver port catalog→inventory-core para uso pontual.
- Telas afetadas pelo adendo que **não existem** ainda foram pulados:
  - T-LOT-06 (tela de Vendas): tela não existe no MVP.
  - T-LOT-07 (modal Estoque com lotes opcionais): não há modal de detalhamento de saldo por lote.
  - T-LOT-08 (demo seed): não há seed de demo para atualizar.

## Quando revisitar

- Quando surgir requisito de custo médio ponderado por insumo → revisitar Decisão 5 sobre valor_unitario do agregador (provavelmente Movimentacao Reader + agregação SQL).
- Quando tela de Vendas (T-LOT-06) existir → expor `controla_validade` no listing pra UX condicional.
- Quando admin precisar mudar regime de insumo com saldo → desenhar use case `MudarRegimeInsumoUseCase` que move saldos rastreados→agregador (ou inverso) atômico.
