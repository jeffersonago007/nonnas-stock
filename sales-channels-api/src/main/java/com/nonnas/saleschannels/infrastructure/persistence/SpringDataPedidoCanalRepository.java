package com.nonnas.saleschannels.infrastructure.persistence;

import com.nonnas.saleschannels.domain.CanalTipo;
import com.nonnas.saleschannels.domain.StatusPedidoCanal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataPedidoCanalRepository extends JpaRepository<PedidoCanalEntity, UUID> {

    Optional<PedidoCanalEntity> findByCanalTipoAndPedidoExternoId(CanalTipo canalTipo, String pedidoExternoId);

    List<PedidoCanalEntity> findByFilialIdAndStatusOrderByRecebidoEmDesc(UUID filialId, StatusPedidoCanal status);

    List<PedidoCanalEntity> findByFilialIdOrderByRecebidoEmDesc(UUID filialId);
}
