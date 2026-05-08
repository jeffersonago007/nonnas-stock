package com.nonnas.inventory.domain;

public enum TipoMovimentacao {
    ENTRADA_NF, ENTRADA_AJUSTE, ENTRADA_TRANSFERENCIA, ENTRADA_DEVOLUCAO_CLIENTE, ENTRADA_CARGA_INICIAL,
    SAIDA_VENDA, SAIDA_AJUSTE, SAIDA_TRANSFERENCIA, SAIDA_PERDA, SAIDA_QUEBRA, SAIDA_VENCIMENTO;

    public boolean isEntrada() { return name().startsWith("ENTRADA_"); }
    public boolean isSaida()   { return name().startsWith("SAIDA_"); }
}
