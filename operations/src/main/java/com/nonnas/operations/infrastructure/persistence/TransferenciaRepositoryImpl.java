package com.nonnas.operations.infrastructure.persistence;

import com.nonnas.operations.application.ports.TransferenciaRepository;
import com.nonnas.operations.domain.StatusTransferencia;
import com.nonnas.operations.domain.Transferencia;
import com.nonnas.operations.domain.TransferenciaId;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class TransferenciaRepositoryImpl implements TransferenciaRepository {

    private final SpringDataTransferenciaRepository jpa;

    TransferenciaRepositoryImpl(SpringDataTransferenciaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Transferencia save(Transferencia t) {
        return OperationsMappers.toDomain(jpa.save(OperationsMappers.toEntity(t)));
    }

    @Override
    public Optional<Transferencia> findById(TransferenciaId id) {
        return jpa.findById(id.value()).map(OperationsMappers::toDomain);
    }

    @Override
    public List<Transferencia> findByFilialOrigem(UUID filialOrigemId, int page, int size) {
        return jpa.findByFilialOrigemIdOrderByDataSolicitacaoDesc(filialOrigemId, PageRequest.of(page, size))
                .stream().map(OperationsMappers::toDomain).toList();
    }

    @Override
    public List<Transferencia> findByStatus(StatusTransferencia status, int page, int size) {
        return jpa.findByStatusOrderByDataSolicitacaoDesc(status, PageRequest.of(page, size))
                .stream().map(OperationsMappers::toDomain).toList();
    }

    @Override
    public List<EmTransitoPorInsumo> agregadoEmTransito(UUID filialDestinoIdOpt) {
        return jpa.agregadoEmTransito(filialDestinoIdOpt).stream()
                .map(r -> new EmTransitoPorInsumo(r.insumoId(), r.quantidadeEmTransito()))
                .toList();
    }
}
