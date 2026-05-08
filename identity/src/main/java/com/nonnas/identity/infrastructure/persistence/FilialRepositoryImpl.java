package com.nonnas.identity.infrastructure.persistence;

import com.nonnas.identity.application.ports.FilialRepository;
import com.nonnas.identity.domain.Cnpj;
import com.nonnas.identity.domain.EmpresaId;
import com.nonnas.identity.domain.Filial;
import com.nonnas.identity.domain.FilialId;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
class FilialRepositoryImpl implements FilialRepository {

    private final SpringDataFilialRepository jpa;

    FilialRepositoryImpl(SpringDataFilialRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Filial save(Filial filial) {
        FilialEntity saved = jpa.save(IdentityMappers.toEntity(filial));
        return IdentityMappers.toDomain(saved);
    }

    @Override
    public Optional<Filial> findById(FilialId id) {
        return jpa.findById(id.value()).map(IdentityMappers::toDomain);
    }

    @Override
    public Optional<Filial> findByCnpj(Cnpj cnpj) {
        return jpa.findByCnpj(cnpj.value()).map(IdentityMappers::toDomain);
    }

    @Override
    public boolean existsByCnpj(Cnpj cnpj) {
        return jpa.existsByCnpj(cnpj.value());
    }

    @Override
    public List<Filial> findByEmpresa(EmpresaId empresaId, int page, int size) {
        return jpa.findByEmpresaId(empresaId.value(), PageRequest.of(page, size))
                .map(IdentityMappers::toDomain)
                .getContent();
    }

    @Override
    public List<Filial> findAll(int page, int size) {
        return jpa.findAll(PageRequest.of(page, size))
                .map(IdentityMappers::toDomain)
                .getContent();
    }

    @Override
    public long count() {
        return jpa.count();
    }
}
