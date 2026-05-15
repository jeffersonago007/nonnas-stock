package com.nonnas.saleschannels.application.ports;

import com.nonnas.saleschannels.domain.CanalTipo;
import com.nonnas.saleschannels.domain.CredencialCanal;
import com.nonnas.saleschannels.domain.CredencialCanalId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CredencialCanalRepository {

    CredencialCanal save(CredencialCanal credencial);

    Optional<CredencialCanal> findById(CredencialCanalId id);

    Optional<CredencialCanal> findAtivaByCanalEFilial(CanalTipo canalTipo, UUID filialId);

    Optional<CredencialCanal> findAtivaByMerchantExterno(CanalTipo canalTipo, String merchantExternoId);

    List<CredencialCanal> listarPorCanal(CanalTipo canalTipo);

    List<CredencialCanal> listarTodas();
}
