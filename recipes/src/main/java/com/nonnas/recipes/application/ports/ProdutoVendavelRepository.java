package com.nonnas.recipes.application.ports;

import com.nonnas.recipes.domain.ProdutoVendavel;
import com.nonnas.recipes.domain.ProdutoVendavelId;

import java.util.List;
import java.util.Optional;

public interface ProdutoVendavelRepository {
    ProdutoVendavel save(ProdutoVendavel p);
    Optional<ProdutoVendavel> findById(ProdutoVendavelId id);
    Optional<ProdutoVendavel> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);
    List<ProdutoVendavel> findAll(int page, int size);
}
