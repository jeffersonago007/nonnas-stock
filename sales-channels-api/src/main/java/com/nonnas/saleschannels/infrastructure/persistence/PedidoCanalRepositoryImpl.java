package com.nonnas.saleschannels.infrastructure.persistence;

import com.nonnas.saleschannels.application.ports.PedidoCanalRepository;
import com.nonnas.saleschannels.domain.CanalTipo;
import com.nonnas.saleschannels.domain.PedidoCanal;
import com.nonnas.saleschannels.domain.PedidoCanalId;
import com.nonnas.saleschannels.domain.StatusPedidoCanal;
import com.nonnas.sharedkernel.NotFoundException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class PedidoCanalRepositoryImpl implements PedidoCanalRepository {

    private final SpringDataPedidoCanalRepository jpa;

    PedidoCanalRepositoryImpl(SpringDataPedidoCanalRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public PedidoCanal salvarNovo(PedidoCanal pedido, String payloadCanonicoJson, String payloadBrutoJson) {
        PedidoCanalEntity entity = SalesChannelsMappers.toEntity(pedido, payloadCanonicoJson, payloadBrutoJson);
        return SalesChannelsMappers.toDomain(jpa.save(entity));
    }

    @Override
    public PedidoCanal atualizar(PedidoCanal pedido) {
        PedidoCanalEntity entity = jpa.findById(pedido.id().value())
                .orElseThrow(() -> new NotFoundException("PedidoCanal " + pedido.id().value() + " não encontrado"));
        SalesChannelsMappers.aplicarMudancas(pedido, entity);
        return SalesChannelsMappers.toDomain(jpa.save(entity));
    }

    @Override
    public Optional<PedidoCanal> findById(PedidoCanalId id) {
        return jpa.findById(id.value()).map(SalesChannelsMappers::toDomain);
    }

    @Override
    public Optional<PedidoCanal> findByCanalEPedidoExterno(CanalTipo canalTipo, String pedidoExternoId) {
        return jpa.findByCanalTipoAndPedidoExternoId(canalTipo, pedidoExternoId)
                .map(SalesChannelsMappers::toDomain);
    }

    @Override
    public List<PedidoCanal> listarPorFilialEStatus(UUID filialId, StatusPedidoCanal status) {
        return jpa.findByFilialIdAndStatusOrderByRecebidoEmDesc(filialId, status).stream()
                .map(SalesChannelsMappers::toDomain).toList();
    }

    @Override
    public List<PedidoCanal> listarPorFilial(UUID filialId) {
        return jpa.findByFilialIdOrderByRecebidoEmDesc(filialId).stream()
                .map(SalesChannelsMappers::toDomain).toList();
    }
}
