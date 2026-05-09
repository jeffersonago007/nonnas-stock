package com.nonnas.alerts.domain;

public enum TipoAlerta {
    /** Saldo cai abaixo de X% do estoque máximo do insumo na filial. Threshold em [0, 100]. */
    ESTOQUE_MINIMO_PERCENTUAL,
    /** Saldo cai abaixo de X unidades base. Threshold &gt; 0. */
    ESTOQUE_MINIMO_ABSOLUTO,
    /** Algum lote vai vencer em &le; N dias. Threshold = dias (inteiro). */
    VENCIMENTO_PROXIMO_DIAS,
    /** Saldo zerado ou negativo. Não usa threshold. */
    RUPTURA;

    public boolean precisaThreshold() { return this != RUPTURA; }

    public boolean ehEstoque() {
        return this == ESTOQUE_MINIMO_PERCENTUAL
                || this == ESTOQUE_MINIMO_ABSOLUTO
                || this == RUPTURA;
    }

    public boolean ehVencimento() { return this == VENCIMENTO_PROXIMO_DIAS; }
}
