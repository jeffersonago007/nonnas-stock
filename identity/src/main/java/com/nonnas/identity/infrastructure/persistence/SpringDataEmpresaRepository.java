package com.nonnas.identity.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface SpringDataEmpresaRepository extends JpaRepository<EmpresaEntity, UUID> {

    Optional<EmpresaEntity> findByCnpj(String cnpj);

    boolean existsByCnpj(String cnpj);
}
