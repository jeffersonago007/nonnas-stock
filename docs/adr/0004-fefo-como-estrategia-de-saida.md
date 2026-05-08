# ADR 0004 — FEFO como estratégia de seleção de lote em saída

- **Status:** Aceita
- **Data:** 2026-05-08
- **Contexto da decisão:** Adendo seção 14.6, T00 backfill

## Contexto

Restaurante trabalha com perecíveis (mussarela, carne, hortifrúti). Vencimento na geladeira é perda direta, e mais grave: produto vencido servido ao cliente é risco sanitário e jurídico. A estratégia errada de consumo pode esconder problemas até virarem prejuízo: FIFO clássico (primeiro a entrar, primeiro a sair) ignora validade — um lote novo de validade curta consumido depois de um lote antigo de validade longa perde-se. LIFO é proibitivo nesse contexto.

Existem três regras possíveis para perecíveis:
- **FIFO** (First In, First Out) — sai o que entrou primeiro. Razoável para insumos não-perecíveis (sal, tempero seco).
- **FEFO** (First Expired, First Out) — sai o que vence antes. Correto para perecíveis.
- **LIFO** (Last In, First Out) — só faz sentido em contextos contábeis específicos. Não aplicável aqui.

## Decisão

**FEFO é a estratégia padrão e única para saída automática** no MVP 1.0. Implementada em `SelecionarLotesPorFefoService` (módulo inventory-core, T04).

Algoritmo:

```sql
SELECT sl.lote_id, sl.quantidade_base, l.data_validade
  FROM saldo_lote sl
  JOIN lote l ON l.id = sl.lote_id
 WHERE sl.filial_id = :filialId
   AND l.insumo_id = :insumoId
   AND sl.quantidade_base > 0
 ORDER BY l.data_validade NULLS LAST, l.id ASC
```

Iteração consome lotes na ordem retornada até completar a quantidade requerida. Pode gerar múltiplos `ItemMovimentacao` (um por lote consumido). Lotes sem validade ficam por último (FIFO entre eles via `l.id ASC`).

**Caso de saldo insuficiente:** a regra explícita do MVP é **permitir saldo negativo** no último lote consumido, sinalizar com flag `gerou_negativo = true` na `Movimentacao` e disparar alerta de ruptura. Restaurante prefere registrar a venda mesmo sem estoque a bloquear o pedido.

## Consequências

**Positivas:**
- Vencimento minimizado por consumo natural — perda só ocorre quando recebimento excede consumo previsível, não por má rotação.
- Auditoria clara: cada `ItemMovimentacao` referencia o lote consumido com sua data de validade no momento da saída.
- Operação não pensa em qual lote pegar — sistema decide. Reduz erro humano em ambiente de pressão (almoço).

**Negativas:**
- Lotes podem ser fragmentados em várias movimentações pequenas. Consultas precisam considerar isso (não há `lote_unico` em `Movimentacao`).
- Saldo negativo introduz complexidade no relatório de posição (precisa flag visual).

**Mitigações:**
- Índice composto em `saldo_lote(filial_id, lote_id)` e em `lote(insumo_id, data_validade)` garante que a query FEFO é O(log n) por insumo+filial.
- Alerta `RUPTURA` automático quando saldo negativo (T07).
- Job de conciliação detecta divergências entre saldo materializado e soma de movimentações (T04).

## Referências

- PROMPT_CLAUDE_CODE.md seção 5.2 (FEFO na saída).
- ADR 0005 — Versionamento de receita (snapshot de venda usa o FEFO no momento).
