package com.nonnas.recipes.infrastructure.persistence;

import com.nonnas.recipes.application.ports.ProdutoVendavelRepository;
import com.nonnas.recipes.domain.ProdutoVendavel;
import com.nonnas.recipes.domain.ProdutoVendavelId;
import com.nonnas.recipes.domain.TipoProdutoVendavel;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
class ProdutoVendavelRepositoryImpl implements ProdutoVendavelRepository {

    private final SpringDataProdutoVendavelRepository jpa;

    ProdutoVendavelRepositoryImpl(SpringDataProdutoVendavelRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public ProdutoVendavel save(ProdutoVendavel p) {
        return RecipesMappers.toDomain(jpa.save(RecipesMappers.toEntity(p)));
    }

    @Override
    public Optional<ProdutoVendavel> findById(ProdutoVendavelId id) {
        return jpa.findById(id.value()).map(RecipesMappers::toDomain);
    }

    @Override
    public Optional<ProdutoVendavel> findByCodigo(String codigo) {
        return jpa.findByCodigo(codigo).map(RecipesMappers::toDomain);
    }

    @Override
    public boolean existsByCodigo(String codigo) {
        return jpa.existsByCodigo(codigo);
    }

    @Override
    public List<ProdutoVendavel> findAll(int page, int size) {
        return jpa.findAll(PageRequest.of(page, size)).map(RecipesMappers::toDomain).getContent();
    }

    @Override
    public List<ProdutoVendavel> findFiltered(String categoria, Boolean ativo, TipoProdutoVendavel tipo,
                                              String q, int page, int size) {
        String normalizedCat = (categoria == null || categoria.isBlank()) ? null : categoria.trim();
        String pattern = (q == null || q.isBlank()) ? null : "%" + q.trim().toLowerCase() + "%";
        String tipoStr = tipo == null ? null : tipo.name();
        return jpa.findFiltered(normalizedCat, ativo, tipoStr, pattern, PageRequest.of(page, size))
                .map(RecipesMappers::toDomain)
                .getContent();
    }

    @Override
    public List<String> listarCategoriasDistintas() {
        return jpa.findDistinctCategorias();
    }

    @Override
    public Set<UUID> listarInsumosVinculadosARevenda() {
        return new HashSet<>(jpa.findAllInsumoRevendaIds());
    }

    @Override
    public Optional<ProdutoVendavel> findRevendaPorInsumo(UUID insumoId) {
        return jpa.findRevendasByInsumoId(insumoId).stream()
                .findFirst()
                .map(RecipesMappers::toDomain);
    }
}
