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
     * Lista notas com filtros opcionais. Cada filtro nulo/vazio é ignorado.
     * Ordenado por data de emissão decrescente.
     */
    List<NotaFiscal> findFiltered(Filtros filtros, int page, int size);

    record Filtros(
            UUID filialId,
            UUID fornecedorId,
            String numero,             // match case-insensitive parcial
            String chaveNfe,           // match parcial
            Instant emissaoDe,
            Instant emissaoAte,
            Instant lancamentoDe,
            Instant lancamentoAte
    ) {}
}
