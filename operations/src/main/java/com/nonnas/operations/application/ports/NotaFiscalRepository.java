package com.nonnas.operations.application.ports;

import com.nonnas.operations.domain.NotaFiscal;
import com.nonnas.operations.domain.NotaFiscalId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotaFiscalRepository {

    NotaFiscal save(NotaFiscal nota);

    Optional<NotaFiscal> findById(NotaFiscalId id);

    Optional<NotaFiscal> findByChaveNfe(String chaveNfe);

    boolean existsByChaveNfe(String chaveNfe);

    /**
     * Lista notas com filtros opcionais (filial e período de emissão),
     * ordenado por data de emissão decrescente.
     */
    List<NotaFiscal> findFiltered(UUID filialId,
                                  Instant emissaoDe,
                                  Instant emissaoAte,
                                  int page, int size);
}
