package com.nonnas.catalog.infrastructure.persistence;

import com.nonnas.catalog.application.ports.InsumoFilialRepository;
import com.nonnas.catalog.domain.InsumoFilial;
import com.nonnas.catalog.domain.InsumoFilialId;
import com.nonnas.catalog.domain.InsumoId;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class InsumoFilialRepositoryImpl implements InsumoFilialRepository {

    private final SpringDataInsumoFilialRepository jpa;

    InsumoFilialRepositoryImpl(SpringDataInsumoFilialRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public InsumoFilial save(InsumoFilial i) {
        return CatalogMappers.toDomain(jpa.save(CatalogMappers.toEntity(i)));
    }

    @Override
    public Optional<InsumoFilial> findById(InsumoFilialId id) {
        return jpa.findById(id.value()).map(CatalogMappers::toDomain);
    }

    @Override
    public boolean existsByInsumoEFilial(InsumoId insumoId, UUID filialId) {
        return jpa.existsByInsumoIdAndFilialId(insumoId.value(), filialId);
    }

    @Override
    public List<InsumoFilial> findAll(int page, int size) {
        return jpa.findAll(PageRequest.of(page, size)).map(CatalogMappers::toDomain).getContent();
    }
}
