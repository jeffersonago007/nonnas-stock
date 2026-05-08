package com.nonnas.catalog.application.unidademedida;

import com.nonnas.catalog.application.ports.UnidadeMedidaRepository;
import com.nonnas.catalog.domain.UnidadeMedida;
import com.nonnas.catalog.domain.UnidadeMedidaTipo;
import com.nonnas.sharedkernel.BusinessRuleException;
import com.nonnas.sharedkernel.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class CriarUnidadeMedidaUseCase {

    private final UnidadeMedidaRepository repository;
    private final Clock clock;

    public CriarUnidadeMedidaUseCase(UnidadeMedidaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public UnidadeMedida execute(String codigo, String nome, UnidadeMedidaTipo tipo) {
        if (repository.existsByCodigo(codigo.trim().toUpperCase())) {
            throw new BusinessRuleException(ErrorCode.CONFLICT,
                    "Já existe unidade de medida com código " + codigo);
        }
        return repository.save(UnidadeMedida.nova(codigo, nome, tipo, clock.instant()));
    }
}
