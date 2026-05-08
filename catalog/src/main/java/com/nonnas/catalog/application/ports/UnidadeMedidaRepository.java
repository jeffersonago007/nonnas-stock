package com.nonnas.catalog.application.ports;

import com.nonnas.catalog.domain.UnidadeMedida;
import com.nonnas.catalog.domain.UnidadeMedidaId;

import java.util.List;
import java.util.Optional;

public interface UnidadeMedidaRepository {
    UnidadeMedida save(UnidadeMedida u);
    Optional<UnidadeMedida> findById(UnidadeMedidaId id);
    Optional<UnidadeMedida> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);
    List<UnidadeMedida> findAll(int page, int size);
}
