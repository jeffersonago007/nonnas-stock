package com.nonnas.operations.infrastructure.persistence;

import com.nonnas.operations.application.ports.AjusteEstoqueRepository;
import com.nonnas.operations.domain.AjusteEstoque;
import com.nonnas.operations.domain.AjusteEstoqueId;
import com.nonnas.operations.domain.StatusAjuste;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class AjusteEstoqueRepositoryImpl implements AjusteEstoqueRepository {

    private final SpringDataAjusteEstoqueRepository jpa;

    AjusteEstoqueRepositoryImpl(SpringDataAjusteEstoqueRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public AjusteEstoque save(AjusteEstoque a) {
        return OperationsMappers.toDomain(jpa.save(OperationsMappers.toEntity(a)));
    }

    @Override
    public Optional<AjusteEstoque> findById(AjusteEstoqueId id) {
        return jpa.findById(id.value()).map(OperationsMappers::toDomain);
    }

    @Override
    public List<AjusteEstoque> findByFilialEStatus(UUID filialId, StatusAjuste status, int page, int size) {
        return jpa.findByFilialIdAndStatusOrderByDataSolicitacaoDesc(filialId, status, PageRequest.of(page, size))
                .stream().map(OperationsMappers::toDomain).toList();
    }

    @Override
    public List<AjusteEstoque> findPendentes(int page, int size) {
        return jpa.findByStatusOrderByDataSolicitacaoDesc(StatusAjuste.PENDENTE_APROVACAO, PageRequest.of(page, size))
                .stream().map(OperationsMappers::toDomain).toList();
    }
}
