package com.nonnas.catalog.application.ports;

import com.nonnas.catalog.domain.Insumo;
import com.nonnas.catalog.domain.InsumoId;

import java.util.List;
import java.util.Optional;

public interface InsumoRepository {
    Insumo save(Insumo i);
    Optional<Insumo> findById(InsumoId id);
    Optional<Insumo> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);
    List<Insumo> findAll(int page, int size);
}
