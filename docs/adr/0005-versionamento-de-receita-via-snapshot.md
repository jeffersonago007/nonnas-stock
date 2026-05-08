# ADR 0005 — Versionamento de ficha técnica via snapshot na venda

- **Status:** Aceita
- **Data:** 2026-05-08
- **Contexto da decisão:** Adendo seção 14.6, T00 backfill

## Contexto

A ficha técnica define os insumos consumidos por unidade de produto vendável (ex.: "Pizza Margherita usa 200g mussarela, 150g molho, 1 manjericão"). Receitas mudam ao longo do tempo: novo fornecedor reduz a porção, chef ajusta a fórmula, sazonalidade troca um ingrediente por outro.

Se uma venda registrada hoje aponta para `FichaTecnica.id = X` e amanhã o chef edita a receita, o que acontece com o histórico?
- Se a venda referencia o ID e a edição altera o registro: **histórico é reescrito retroativamente**. Relatório de consumo de mussarela dos últimos 6 meses passa a mostrar dados que nunca foram verdade. Inaceitável para auditoria fiscal e gerencial.
- Se a venda copia os itens da receita inline: **duplicação de dados** em milhões de linhas. Não-normalizado, gigantesco, consultas pesadas.
- Se a edição cria nova versão e a anterior fica imutável: **versionamento via snapshot**. Histórico preservado, sem duplicação massiva.

## Decisão

Adotamos **versionamento via snapshot por referência à versão vigente no momento da venda**:

1. `FichaTecnica` tem campos `versao`, `vigente_desde`, `vigente_ate` (nullable), `ativa` (boolean). Apenas uma versão é ativa por vez para cada `produto_vendavel_id`.
2. Edição da receita **não altera** a versão atual. O fluxo é:
   - Versão atual recebe `vigente_ate = now()` e `ativa = false`.
   - Nova `FichaTecnica` é inserida com `versao = anterior + 1`, `vigente_desde = now()`, `vigente_ate = null`, `ativa = true`.
   - `ItemFichaTecnica` da nova versão é populado com a receita editada.
3. Cada venda registrada (`Movimentacao` `SAIDA_VENDA` no MVP, futuro `PedidoVenda` em onda 1.2) referencia o `ficha_tecnica_id` da versão **vigente naquele momento** — esse é o snapshot por referência.
4. Mudanças posteriores na receita **não alteram** o histórico, pois apontam para a versão antiga, imutável.

**Trade-off rejeitado:** snapshot inline (copiar itens para `item_pedido_venda`). Para o porte do cliente (estimativa: 5–10k vendas/dia × 5 insumos = 25–50k linhas/dia), 5 anos de histórico daria ~70M linhas. Versionamento por referência mantém ~tens de milhares de linhas em `ficha_tecnica` independente do volume de vendas.

## Consequências

**Positivas:**
- Auditoria gerencial e fiscal íntegra: relatório de consumo histórico nunca é alterado por edição de receita.
- Footprint pequeno: cada edição de receita gera ~10 linhas, não milhões.
- Suporta análise "qual o consumo de mussarela ANTES/DEPOIS de mudarmos a receita" diretamente via versão.

**Negativas:**
- Consulta de "consumo total por insumo no período" precisa joinar venda → ficha_tecnica → item_ficha_tecnica. Três joins; mitigado por views materializadas em reporting (T08).
- UI precisa expor o versionamento (em T13: "Histórico de versões da receita") sem confundir o usuário comum.

**Mitigações:**
- View materializada `vw_consumo_historico` agregada por insumo+filial+período, refresh a cada 30min, evita join cascata em queries online.
- T13 incluirá UX do versionamento: dropdown "ver versão X de Y", visualização de diff entre versões.

## Referências

- PROMPT_CLAUDE_CODE.md seção 5.6 (ficha técnica versionada com snapshot), seção 4.2 (entidades recipes), seção 7.2 (testes de snapshot).
