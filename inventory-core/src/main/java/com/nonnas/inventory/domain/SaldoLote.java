package com.nonnas.inventory.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Saldo materializado para um lote em uma filial. Esse valor é uma
 * <em>projeção</em> — a verdade são as movimentações. O saldo é atualizado
 * por @EventListener a cada {@link Movimentacao} criada e auditado por job
 * de conciliação que compara com a soma dos itens.
 *
 * <p>Permite quantidade negativa (master doc 5.2): em pico de venda,
 * preferimos registrar a saída e disparar alerta de ruptura a bloquear o
 * pedido.
 */
public record SaldoLote(
        LoteId loteId,
        UUID filialId,
        BigDecimal quantidadeBase,
        Instant atualizadoEm
) {
    public SaldoLote {
        Objects.requireNonNull(loteId);
        Objects.requireNonNull(filialId);
        Objects.requireNonNull(quantidadeBase);
        Objects.requireNonNull(atualizadoEm);
    }

    public static SaldoLote zero(LoteId loteId, UUID filialId, Instant agora) {
        return new SaldoLote(loteId, filialId, BigDecimal.ZERO, agora);
    }

    public SaldoLote acrescentar(BigDecimal qtd, Instant agora) {
        return new SaldoLote(loteId, filialId, quantidadeBase.add(qtd), agora);
    }

    public SaldoLote subtrair(BigDecimal qtd, Instant agora) {
        return new SaldoLote(loteId, filialId, quantidadeBase.subtract(qtd), agora);
    }

    public boolean isPositivo() { return quantidadeBase.signum() > 0; }
    public boolean isNegativo() { return quantidadeBase.signum() < 0; }
}
