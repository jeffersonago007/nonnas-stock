package com.nonnas.recipes.infrastructure.persistence;

import com.nonnas.recipes.application.ports.ProdutoVendavelRepository;
import com.nonnas.recipes.domain.ProdutoVendavel;
import com.nonnas.recipes.domain.ProdutoVendavelId;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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
}
