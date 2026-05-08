package com.nonnas.identity.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HistoricoSenhaJpaRepository extends JpaRepository<HistoricoSenhaEntity, UUID> {

    List<HistoricoSenhaEntity> findByUsuarioIdOrderByCriadaEmDesc(UUID usuarioId, Pageable pageable);
}
