package com.nonnas.catalog.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface SpringDataConversaoUnidadeRepository extends JpaRepository<ConversaoUnidadeEntity, UUID> {

    @Query("""
        SELECT c FROM ConversaoUnidadeEntity c
        WHERE c.insumoId = :insumoId
          AND c.unidadeOrigemId = :origemId
          AND c.unidadeDestinoId = :destinoId
        """)
    Optional<ConversaoUnidadeEntity> findByInsumoEOrigemDestino(
            @Param("insumoId") UUID insumoId,
            @Param("origemId") UUID origemId,
            @Param("destinoId") UUID destinoId);

    @Query("""
        SELECT c FROM ConversaoUnidadeEntity c
        WHERE c.insumoId IS NULL
          AND c.unidadeOrigemId = :origemId
          AND c.unidadeDestinoId = :destinoId
        """)
    Optional<ConversaoUnidadeEntity> findGlobalPorOrigemDestino(
            @Param("origemId") UUID origemId,
            @Param("destinoId") UUID destinoId);
}
