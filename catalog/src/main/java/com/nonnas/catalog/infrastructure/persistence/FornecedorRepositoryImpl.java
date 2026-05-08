package com.nonnas.catalog.infrastructure.persistence;

import com.nonnas.catalog.application.ports.FornecedorRepository;
import com.nonnas.catalog.domain.Cnpj;
import com.nonnas.catalog.domain.Fornecedor;
import com.nonnas.catalog.domain.FornecedorId;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
class FornecedorRepositoryImpl implements FornecedorRepository {

    private final SpringDataFornecedorRepository jpa;

    FornecedorRepositoryImpl(SpringDataFornecedorRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Fornecedor save(Fornecedor f) {
        return CatalogMappers.toDomain(jpa.save(CatalogMappers.toEntity(f)));
    }

    @Override
    public Optional<Fornecedor> findById(FornecedorId id) {
        return jpa.findById(id.value()).map(CatalogMappers::toDomain);
    }

    @Override
    public Optional<Fornecedor> findByCnpj(Cnpj cnpj) {
        return jpa.findByCnpj(cnpj.value()).map(CatalogMappers::toDomain);
    }

    @Override
    public boolean existsByCnpj(Cnpj cnpj) {
        return jpa.existsByCnpj(cnpj.value());
    }

    @Override
    public List<Fornecedor> findAll(int page, int size) {
        return jpa.findAll(PageRequest.of(page, size)).map(CatalogMappers::toDomain).getContent();
    }
}
