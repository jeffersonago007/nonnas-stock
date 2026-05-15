package com.nonnas.saleschannels.infrastructure.persistence;

import com.nonnas.saleschannels.domain.CanalTipo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataCredencialCanalRepository extends JpaRepository<CredencialCanalEntity, UUID> {

    Optional<CredencialCanalEntity> findByCanalTipoAndFilialIdAndAtivaIsTrue(CanalTipo canalTipo, UUID filialId);

    Optional<CredencialCanalEntity> findByCanalTipoAndMerchantExternoIdAndAtivaIsTrue(CanalTipo canalTipo, String merchantExternoId);

    List<CredencialCanalEntity> findByCanalTipo(CanalTipo canalTipo);
}
