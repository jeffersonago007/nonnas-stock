package com.nonnas.operations.interfaces.rest.dto;

import com.nonnas.operations.domain.CargaInicial;

import java.time.Instant;
import java.util.UUID;

public final class CargaInicialDto {

    public record Response(
            UUID id,
            UUID filialId,
            String hashPlanilha,
            String nomeArquivo,
            int registrosProcessados,
            int registrosFalhos,
            UUID solicitadoPor,
            Instant createdAt
    ) {
        public static Response from(CargaInicial c) {
            return new Response(c.id().value(), c.filialId(), c.hashPlanilha(),
                    c.nomeArquivo(), c.registrosProcessados(), c.registrosFalhos(),
                    c.solicitadoPor(), c.createdAt());
        }
    }

    private CargaInicialDto() {}
}
