package com.nonnas.inventory.application.events;

import com.nonnas.inventory.domain.Movimentacao;

/**
 * Disparado quando uma movimentação é persistida. O {@code SaldoLoteListener}
 * consome para atualizar o saldo materializado (within same transaction).
 * Outros módulos (alerts em T07) também escutarão para avaliação reativa.
 */
public record MovimentacaoCriadaEvent(Movimentacao movimentacao) {}
