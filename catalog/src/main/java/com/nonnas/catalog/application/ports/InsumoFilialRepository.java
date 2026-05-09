package com.nonnas.catalog.application.ports;

import com.nonnas.catalog.domain.InsumoFilial;
import com.nonnas.catalog.domain.InsumoFilialId;
import com.nonnas.catalog.domain.InsumoId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InsumoFilialRepository {
    InsumoFilial save(InsumoFilial i);
    Optional<InsumoFilial> findById(InsumoFilialId id);
    Optional<InsumoFilial> findByInsumoEFilial(InsumoId insumoId, UUID filialId);
    boolean existsByInsumoEFilial(InsumoId insumoId, UUID filialId);
    List<InsumoFilial> findAll(int page, int size);
}
