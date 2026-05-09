package com.nonnas.operations.interfaces.rest.dto;

import com.nonnas.operations.application.carga.PlanilhaCargaInicial;
import com.nonnas.operations.domain.CargaInicial;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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

    public record PreviewResponse(
            String hashPlanilha,
            String nomeArquivo,
            int totalLinhas,
            List<LinhaPreview> linhas
    ) {
        public static PreviewResponse from(PlanilhaCargaInicial plan) {
            List<LinhaPreview> linhas = plan.linhas().stream()
                    .map(LinhaPreview::from)
                    .toList();
            return new PreviewResponse(plan.hashSha256(), plan.nomeArquivo(), linhas.size(), linhas);
        }
    }

    public record LinhaPreview(
            int numeroLinha,
            UUID insumoId,
            UUID unidadeId,
            String numeroLote,
            BigDecimal quantidade,
            BigDecimal valorUnitario,
            LocalDate dataFabricacao,
            LocalDate dataValidade
    ) {
        static LinhaPreview from(PlanilhaCargaInicial.Linha l) {
            return new LinhaPreview(l.numeroLinha(), l.insumoId(), l.unidadeId(),
                    l.numeroLote(), l.quantidade(), l.valorUnitario(),
                    l.dataFabricacao(), l.dataValidade());
        }
    }

    private CargaInicialDto() {}
}
