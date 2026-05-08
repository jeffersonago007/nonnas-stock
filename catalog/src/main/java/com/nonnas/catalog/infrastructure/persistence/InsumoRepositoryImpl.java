package com.nonnas.catalog.infrastructure.persistence;

import com.nonnas.catalog.application.ports.InsumoRepository;
import com.nonnas.catalog.domain.Insumo;
import com.nonnas.catalog.domain.InsumoId;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
class InsumoRepositoryImpl implements InsumoRepository {

    private final SpringDataInsumoRepository jpa;

    InsumoRepositoryImpl(SpringDataInsumoRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Insumo save(Insumo i) {
        return CatalogMappers.toDomain(jpa.save(CatalogMappers.toEntity(i)));
    }

    @Override
    public Optional<Insumo> findById(InsumoId id) {
        return jpa.findById(id.value()).map(CatalogMappers::toDomain);
    }

    @Override
    public Optional<Insumo> findByCodigo(String codigo) {
        return jpa.findByCodigo(codigo).map(CatalogMappers::toDomain);
    }

    @Override
    public boolean existsByCodigo(String codigo) {
        return jpa.existsByCodigo(codigo);
    }

    @Override
    public List<Insumo> findAll(int page, int size) {
        return jpa.findAll(PageRequest.of(page, size)).map(CatalogMappers::toDomain).getContent();
    }
}
