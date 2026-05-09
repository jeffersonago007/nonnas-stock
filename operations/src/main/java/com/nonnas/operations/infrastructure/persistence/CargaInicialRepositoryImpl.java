package com.nonnas.operations.infrastructure.persistence;

import com.nonnas.operations.application.ports.CargaInicialRepository;
import com.nonnas.operations.domain.CargaInicial;
import com.nonnas.operations.domain.CargaInicialId;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
class CargaInicialRepositoryImpl implements CargaInicialRepository {

    private final SpringDataCargaInicialRepository jpa;

    CargaInicialRepositoryImpl(SpringDataCargaInicialRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public CargaInicial save(CargaInicial c) {
        return OperationsMappers.toDomain(jpa.save(OperationsMappers.toEntity(c)));
    }

    @Override
    public Optional<CargaInicial> findById(CargaInicialId id) {
        return jpa.findById(id.value()).map(OperationsMappers::toDomain);
    }

    @Override
    public Optional<CargaInicial> findByHashPlanilha(String hash) {
        return jpa.findByHashPlanilha(hash).map(OperationsMappers::toDomain);
    }
}
