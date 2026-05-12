package com.nonnas.recipes.application.ports;

import com.nonnas.recipes.domain.ProdutoVendavel;
import com.nonnas.recipes.domain.ProdutoVendavelId;
import com.nonnas.recipes.domain.TipoProdutoVendavel;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ProdutoVendavelRepository {
    ProdutoVendavel save(ProdutoVendavel p);
    Optional<ProdutoVendavel> findById(ProdutoVendavelId id);
    Optional<ProdutoVendavel> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);
    List<ProdutoVendavel> findAll(int page, int size);

    /** Conjunto dos {@code insumo_revenda_id} de produtos REVENDA — usado pra
     *  listar insumos órfãos (que não têm produto vendável ainda). */
    Set<UUID> listarInsumosVinculadosARevenda();

    /** Localiza o produto REVENDA vinculado a um insumo, priorizando o ativo
     *  e em seguida o mais recentemente atualizado. {@code Optional.empty()}
     *  se nenhum produto REVENDA aponta para esse insumo. */
    Optional<ProdutoVendavel> findRevendaPorInsumo(UUID insumoId);

    /**
     * @param categoria quando não-nula/não-vazia restringe à categoria informada (match exato).
     * @param ativo     quando não-nulo filtra pelo status ativo/inativo.
     * @param tipo      quando não-nulo restringe ao tipo FABRICADO ou REVENDA.
     * @param q         quando não-nulo/não-vazio aplica busca case-insensitive em nome ou código.
     */
    List<ProdutoVendavel> findFiltered(String categoria, Boolean ativo, TipoProdutoVendavel tipo,
                                       String q, int page, int size);

    /**
     * Categorias distintas em uso pelos produtos cadastrados, ordenadas
     * alfabeticamente. Usado para popular o combo de filtro/cadastro.
     * Não inclui categorias não-utilizadas — categoria é texto livre,
     * não há entidade própria.
     */
    List<String> listarCategoriasDistintas();
}
