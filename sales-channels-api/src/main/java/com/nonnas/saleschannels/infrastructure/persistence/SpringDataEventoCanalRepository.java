package com.nonnas.saleschannels.infrastructure.persistence;

import com.nonnas.saleschannels.domain.CanalTipo;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataEventoCanalRepository extends JpaRepository<EventoCanalEntity, UUID> {

    Optional<EventoCanalEntity> findByCanalTipoAndEventIdExterno(CanalTipo canalTipo, String eventIdExterno);

    @Query("""
        SELECT e FROM EventoCanalEntity e
        WHERE e.processadoEm IS NULL
        ORDER BY e.recebidoEm ASC
        """)
    List<EventoCanalEntity> findNaoProcessados(Pageable pageable);
}
