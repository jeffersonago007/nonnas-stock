package com.nonnas.saleschannels.application.ports;

import com.nonnas.saleschannels.domain.CanalTipo;
import com.nonnas.saleschannels.domain.PedidoCanal;
import com.nonnas.saleschannels.domain.PedidoCanalId;
import com.nonnas.saleschannels.domain.StatusPedidoCanal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PedidoCanalRepository {

    /**
     * Persiste um pedido novo. O payload canônico/bruto serializado é
     * passado de fora para evitar acoplamento do domínio com Jackson.
     */
    PedidoCanal salvarNovo(PedidoCanal pedido, String payloadCanonicoJson, String payloadBrutoJson);

    PedidoCanal atualizar(PedidoCanal pedido);

    Optional<PedidoCanal> findById(PedidoCanalId id);

    Optional<PedidoCanal> findByCanalEPedidoExterno(CanalTipo canalTipo, String pedidoExternoId);

    List<PedidoCanal> listarPorFilialEStatus(UUID filialId, StatusPedidoCanal status);

    List<PedidoCanal> listarPorFilial(UUID filialId);
}
