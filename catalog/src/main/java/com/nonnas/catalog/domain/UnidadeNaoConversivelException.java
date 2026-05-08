package com.nonnas.catalog.domain;

import com.nonnas.sharedkernel.BusinessRuleException;
import com.nonnas.sharedkernel.ErrorCode;

import java.util.Optional;

/**
 * Lançada quando o {@link ConversorUnidadeService} não encontra uma
 * conversão entre as unidades pedidas — nem específica do insumo, nem
 * global, nem inversa.
 */
public final class UnidadeNaoConversivelException extends BusinessRuleException {

    private final UnidadeMedidaId origemId;
    private final UnidadeMedidaId destinoId;
    private final InsumoId insumoId;

    public UnidadeNaoConversivelException(UnidadeMedidaId origem, UnidadeMedidaId destino, InsumoId insumoId) {
        super(ErrorCode.BUSINESS_RULE_VIOLATED, buildMessage(origem, destino, insumoId));
        this.origemId = origem;
        this.destinoId = destino;
        this.insumoId = insumoId;
    }

    private static String buildMessage(UnidadeMedidaId origem, UnidadeMedidaId destino, InsumoId insumoId) {
        if (insumoId == null) {
            return "Não há conversão definida entre as unidades %s e %s".formatted(origem.value(), destino.value());
        }
        return "Não há conversão definida entre %s e %s para o insumo %s (nem específica nem global)"
                .formatted(origem.value(), destino.value(), insumoId.value());
    }

    public UnidadeMedidaId origemId() { return origemId; }
    public UnidadeMedidaId destinoId() { return destinoId; }
    public Optional<InsumoId> insumoId() { return Optional.ofNullable(insumoId); }
}
