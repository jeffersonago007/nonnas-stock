package com.nonnas.catalog.infrastructure.persistence;

import com.nonnas.catalog.application.ports.CategoriaInsumoRepository;
import com.nonnas.catalog.domain.CategoriaInsumo;
import com.nonnas.catalog.domain.CategoriaInsumoId;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
class CategoriaInsumoRepositoryImpl implements CategoriaInsumoRepository {

    private final SpringDataCategoriaInsumoRepository jpa;

    CategoriaInsumoRepositoryImpl(SpringDataCategoriaInsumoRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public CategoriaInsumo save(CategoriaInsumo c) {
        return CatalogMappers.toDomain(jpa.save(CatalogMappers.toEntity(c)));
    }

    @Override
    public Optional<CategoriaInsumo> findById(CategoriaInsumoId id) {
        return jpa.findById(id.value()).map(CatalogMappers::toDomain);
    }

    @Override
    public List<CategoriaInsumo> findAll(int page, int size) {
        return jpa.findAll(PageRequest.of(page, size)).map(CatalogMappers::toDomain).getContent();
    }

    @Override
    public long count() { return jpa.count(); }
}
