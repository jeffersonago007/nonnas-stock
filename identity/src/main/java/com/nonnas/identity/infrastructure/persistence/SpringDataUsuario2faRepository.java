package com.nonnas.identity.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface SpringDataUsuario2faRepository extends JpaRepository<Usuario2faEntity, UUID> {
    boolean existsByUsuarioIdAndConfirmadoTrue(UUID usuarioId);
}
