package com.nonnas.operations.infrastructure.persistence;

import com.nonnas.operations.application.ports.FornecedorInsumoDeParaRepository;
import com.nonnas.operations.domain.FornecedorInsumoDePara;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class FornecedorInsumoDeParaRepositoryImpl implements FornecedorInsumoDeParaRepository {

    private final SpringDataDeParaRepository jpa;

    FornecedorInsumoDeParaRepositoryImpl(SpringDataDeParaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public FornecedorInsumoDePara save(FornecedorInsumoDePara depara) {
        return OperationsMappers.toDomain(jpa.save(OperationsMappers.toEntity(depara)));
    }

    @Override
    public Optional<FornecedorInsumoDePara> findByFornecedorAndCodigo(UUID fornecedorId, String codigoFornecedor) {
        return jpa.findByFornecedorIdAndCodigoFornecedor(fornecedorId, codigoFornecedor)
                .map(OperationsMappers::toDomain);
    }

    @Override
    public List<FornecedorInsumoDePara> findByFornecedor(UUID fornecedorId) {
        return jpa.findByFornecedorId(fornecedorId).stream()
                .map(OperationsMappers::toDomain).toList();
    }

    @Override
    @Transactional
    public void deleteByFornecedorAndCodigo(UUID fornecedorId, String codigoFornecedor) {
        jpa.deleteByFornecedorIdAndCodigoFornecedor(fornecedorId, codigoFornecedor);
    }
}
