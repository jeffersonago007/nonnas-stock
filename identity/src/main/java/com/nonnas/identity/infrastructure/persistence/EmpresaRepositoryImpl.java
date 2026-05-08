package com.nonnas.identity.infrastructure.persistence;

import com.nonnas.identity.application.ports.EmpresaRepository;
import com.nonnas.identity.domain.Cnpj;
import com.nonnas.identity.domain.Empresa;
import com.nonnas.identity.domain.EmpresaId;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
class EmpresaRepositoryImpl implements EmpresaRepository {

    private final SpringDataEmpresaRepository jpa;

    EmpresaRepositoryImpl(SpringDataEmpresaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Empresa save(Empresa empresa) {
        EmpresaEntity saved = jpa.save(IdentityMappers.toEntity(empresa));
        return IdentityMappers.toDomain(saved);
    }

    @Override
    public Optional<Empresa> findById(EmpresaId id) {
        return jpa.findById(id.value()).map(IdentityMappers::toDomain);
    }

    @Override
    public Optional<Empresa> findByCnpj(Cnpj cnpj) {
        return jpa.findByCnpj(cnpj.value()).map(IdentityMappers::toDomain);
    }

    @Override
    public boolean existsByCnpj(Cnpj cnpj) {
        return jpa.existsByCnpj(cnpj.value());
    }

    @Override
    public List<Empresa> findAll(int page, int size) {
        return jpa.findAll(PageRequest.of(page, size))
                .map(IdentityMappers::toDomain)
                .getContent();
    }

    @Override
    public long count() {
        return jpa.count();
    }
}
