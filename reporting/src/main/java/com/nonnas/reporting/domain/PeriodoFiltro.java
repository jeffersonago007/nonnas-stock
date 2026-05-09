package com.nonnas.reporting.domain;

import com.nonnas.sharedkernel.ValidationException;

import java.time.Instant;

public record PeriodoFiltro(Instant inicio, Instant fim) {

    public PeriodoFiltro {
        if (inicio == null || fim == null) {
            throw new ValidationException("Período inicio e fim são obrigatórios.");
        }
        if (fim.isBefore(inicio)) {
            throw new ValidationException("Fim do período não pode ser anterior ao início.");
        }
    }
}
