package com.nonnas.identity.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataFilialRepository extends JpaRepository<FilialEntity, UUID> {

    Optional<FilialEntity> findByCnpj(String cnpj);

    boolean existsByCnpj(String cnpj);

    Page<FilialEntity> findByEmpresaId(UUID empresaId, Pageable pageable);
}
