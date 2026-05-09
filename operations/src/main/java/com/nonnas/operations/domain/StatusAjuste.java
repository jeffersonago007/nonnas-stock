package com.nonnas.operations.domain;

public enum StatusAjuste {
    PENDENTE_APROVACAO,
    APROVADO,
    REJEITADO;

    public boolean podeAprovar()  { return this == PENDENTE_APROVACAO; }
    public boolean podeRejeitar() { return this == PENDENTE_APROVACAO; }
    public boolean isFinal()      { return this == APROVADO || this == REJEITADO; }
}
