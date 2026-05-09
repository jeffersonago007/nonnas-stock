package com.nonnas.operations.domain;

import java.util.Set;

/**
 * State machine da transferência:
 * <pre>
 *   SOLICITADA → APROVADA → EM_TRANSITO → RECEBIDA
 *        ↓          ↓
 *    CANCELADA  CANCELADA
 * </pre>
 *
 * Cancelamento bloqueado após {@code EM_TRANSITO} (já há movimentação de saída).
 */
public enum StatusTransferencia {
    SOLICITADA,
    APROVADA,
    EM_TRANSITO,
    RECEBIDA,
    CANCELADA;

    private static final Set<StatusTransferencia> CANCELAVEIS = Set.of(SOLICITADA, APROVADA);

    public boolean podeAprovar()    { return this == SOLICITADA; }
    public boolean podeEnviar()     { return this == APROVADA; }
    public boolean podeReceber()    { return this == EM_TRANSITO; }
    public boolean podeCancelar()   { return CANCELAVEIS.contains(this); }
    public boolean isFinal()        { return this == RECEBIDA || this == CANCELADA; }
}
