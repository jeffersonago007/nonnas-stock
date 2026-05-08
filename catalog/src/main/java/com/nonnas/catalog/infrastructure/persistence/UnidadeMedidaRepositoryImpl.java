package com.nonnas.catalog.infrastructure.persistence;

import com.nonnas.catalog.application.ports.UnidadeMedidaRepository;
import com.nonnas.catalog.domain.UnidadeMedida;
import com.nonnas.catalog.domain.UnidadeMedidaId;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
class UnidadeMedidaRepositoryImpl implements UnidadeMedidaRepository {

    private final SpringDataUnidadeMedidaRepository jpa;

    UnidadeMedidaRepositoryImpl(SpringDataUnidadeMedidaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public UnidadeMedida save(UnidadeMedida u) {
        return CatalogMappers.toDomain(jpa.save(CatalogMappers.toEntity(u)));
    }

    @Override
    public Optional<UnidadeMedida> findById(UnidadeMedidaId id) {
        return jpa.findById(id.value()).map(CatalogMappers::toDomain);
    }

    @Override
    public Optional<UnidadeMedida> findByCodigo(String codigo) {
        return jpa.findByCodigo(codigo.toUpperCase()).map(CatalogMappers::toDomain);
    }

    @Override
    public boolean existsByCodigo(String codigo) {
        return jpa.existsByCodigo(codigo.toUpperCase());
    }

    @Override
    public List<UnidadeMedida> findAll(int page, int size) {
        return jpa.findAll(PageRequest.of(page, size)).map(CatalogMappers::toDomain).getContent();
    }
}
