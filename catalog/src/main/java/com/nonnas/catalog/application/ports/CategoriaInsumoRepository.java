package com.nonnas.catalog.application.ports;

import com.nonnas.catalog.domain.CategoriaInsumo;
import com.nonnas.catalog.domain.CategoriaInsumoId;

import java.util.List;
import java.util.Optional;

public interface CategoriaInsumoRepository {
    CategoriaInsumo save(CategoriaInsumo c);
    Optional<CategoriaInsumo> findById(CategoriaInsumoId id);
    List<CategoriaInsumo> findAll(int page, int size);
    long count();
}
