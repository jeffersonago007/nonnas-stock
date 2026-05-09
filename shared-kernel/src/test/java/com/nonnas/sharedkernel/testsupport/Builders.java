package com.nonnas.sharedkernel.testsupport;

import com.nonnas.sharedkernel.Money;
import com.nonnas.sharedkernel.Quantity;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Construtores enxutos para valores compartilhados nos testes.
 *
 * <p>Não substituem as fábricas estáticas em {@link Money}/{@link Quantity}
 * para uso em produção; servem apenas para encurtar setup de testes
 * (ex.: gerar UUIDs aleatórios para a unidade de medida quando o teste
 * não se importa com qual é).
 */
public final class Builders {

    private Builders() {}

    /** Money(BRL) com escala unitária. */
    public static Money brl(String amount) {
        return Money.brl(new BigDecimal(amount));
    }

    /** Quantity numa unidade aleatória (UUID descartável). */
    public static Quantity quantity(String value) {
        return Quantity.of(new BigDecimal(value), UUID.randomUUID());
    }

    /** Quantity numa unidade específica. */
    public static Quantity quantity(String value, UUID unidadeMedidaId) {
        return Quantity.of(new BigDecimal(value), unidadeMedidaId);
    }

    /** UUID aleatório, alias para legibilidade em setup de testes. */
    public static UUID newId() {
        return UUID.randomUUID();
    }
}
