package com.nonnas.catalog.application.conversao;

import com.nonnas.catalog.domain.ConversorUnidadeService;
import com.nonnas.catalog.domain.InsumoId;
import com.nonnas.catalog.domain.UnidadeMedidaId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Read-only use case que expõe {@link ConversorUnidadeService} para o
 * controller. Justifica existir como camada de aplicação porque o service
 * de domínio é instanciado via {@code @Bean} sem {@code @Service}.
 */
@Service
public class ConverterUnidadeUseCase {

    private final ConversorUnidadeService conversor;

    public ConverterUnidadeUseCase(ConversorUnidadeService conversor) {
        this.conversor = conversor;
    }

    @Transactional(readOnly = true)
    public BigDecimal execute(BigDecimal valor, UUID origemId, UUID destinoId, UUID insumoId) {
        return conversor.converter(
                valor,
                UnidadeMedidaId.of(origemId),
                UnidadeMedidaId.of(destinoId),
                insumoId != null ? InsumoId.of(insumoId) : null);
    }
}
