package com.nonnas.operations.application.ports;

import com.nonnas.operations.domain.CargaInicial;
import com.nonnas.operations.domain.CargaInicialId;

import java.util.Optional;

public interface CargaInicialRepository {
    CargaInicial save(CargaInicial c);
    Optional<CargaInicial> findById(CargaInicialId id);
    Optional<CargaInicial> findByHashPlanilha(String hash);
}
