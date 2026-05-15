package com.nonnas.saleschannels.domain;

/**
 * State machine de um pedido recebido por canal.
 *
 * <pre>
 *   RECEBIDO ─── ProcessarPedidoCanal ───▶ EM_PROCESSAMENTO
 *                                                │
 *               sucesso (baixa estoque) ◀────────┤
 *                       │                        │
 *                       ▼                        ▼
 *               CONFIRMADO_ESTOQUE          FALHA (erro detalhado)
 *                       │
 *               ack do canal (CFM/CON)
 *                       │
 *                       ▼
 *                  CONCLUIDO
 *
 *   Cancelamento pode chegar a qualquer momento: CANCELADO.
 * </pre>
 *
 * Os estados {@link #CONFIRMADO_ESTOQUE} → {@link #CONCLUIDO} são separados
 * porque a baixa de estoque é local (atômica) mas o ack para o canal é
 * remoto (pode falhar). Em T-CANAL-04 o use case lida com essa transição.
 */
public enum StatusPedidoCanal {
    RECEBIDO,
    EM_PROCESSAMENTO,
    CONFIRMADO_ESTOQUE,
    CONCLUIDO,
    CANCELADO,
    FALHA
}
