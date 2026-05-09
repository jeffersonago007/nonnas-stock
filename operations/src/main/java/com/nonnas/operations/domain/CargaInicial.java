package com.nonnas.operations.domain;

import com.nonnas.sharedkernel.ValidationException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Registro de uma carga inicial processada. Imutável após criação — uma
 * carga é um evento histórico, não um estado contínuo. {@code hashPlanilha}
 * é único e fornece idempotência: re-upload da mesma planilha encontra o
 * registro anterior e não reprocessa.
 */
public record CargaInicial(
        CargaInicialId id,
        UUID filialId,
        String hashPlanilha,
        String nomeArquivo,
        int registrosProcessados,
        int registrosFalhos,
        UUID solicitadoPor,
        Instant createdAt
) {
    public CargaInicial {
        Objects.requireNonNull(id);
        Objects.requireNonNull(filialId);
        Objects.requireNonNull(hashPlanilha);
        Objects.requireNonNull(nomeArquivo);
        Objects.requireNonNull(solicitadoPor);
        Objects.requireNonNull(createdAt);
        if (hashPlanilha.length() != 64) {
            throw new ValidationException("Hash da planilha deve ter 64 caracteres (SHA-256 hex)");
        }
        if (nomeArquivo.isBlank()) {
            throw new ValidationException("Nome do arquivo é obrigatório");
        }
        if (registrosProcessados < 0 || registrosFalhos < 0) {
            throw new ValidationException("Contadores de registros não podem ser negativos");
        }
    }

    public static CargaInicial novo(UUID filialId, String hashPlanilha, String nomeArquivo,
                                    int registrosProcessados, int registrosFalhos,
                                    UUID solicitadoPor, Instant agora) {
        return new CargaInicial(CargaInicialId.generate(), filialId, hashPlanilha, nomeArquivo,
                registrosProcessados, registrosFalhos, solicitadoPor, agora);
    }
}
