package com.nonnas.identity.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataTokenRevogadoRepository extends JpaRepository<TokenRevogadoEntity, String> {
    boolean existsByJti(String jti);
}
