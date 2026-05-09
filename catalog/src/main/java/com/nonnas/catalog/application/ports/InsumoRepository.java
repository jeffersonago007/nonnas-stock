package com.nonnas.catalog.application.ports;

import com.nonnas.catalog.domain.Insumo;
import com.nonnas.catalog.domain.InsumoId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InsumoRepository {
    Insumo save(Insumo i);
    Optional<Insumo> findById(InsumoId id);
    Optional<Insumo> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);
    List<Insumo> findAll(int page, int size);

    /**
     * @param categoriaId quando não-nulo restringe à categoria informada.
     * @param ativo       quando não-nulo filtra pelo status ativo/inativo.
     * @param q           quando não-nulo/não-vazio aplica busca case-insensitive em nome ou código.
     */
    List<Insumo> findFiltered(UUID categoriaId, Boolean ativo, String q, int page, int size);
}
