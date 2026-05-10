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

    /**
     * @param categoria quando não-nula/não-vazia restringe à categoria informada (match exato).
     * @param ativo     quando não-nulo filtra pelo status ativo/inativo.
     * @param q         quando não-nulo/não-vazio aplica busca case-insensitive em nome ou código.
     */
    List<ProdutoVendavel> findFiltered(String categoria, Boolean ativo, String q, int page, int size);

    /**
     * Categorias distintas em uso pelos produtos cadastrados, ordenadas
     * alfabeticamente. Usado para popular o combo de filtro/cadastro.
     * Não inclui categorias não-utilizadas — categoria é texto livre,
     * não há entidade própria.
     */
    List<String> listarCategoriasDistintas();
}
