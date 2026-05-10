package com.nonnas.operations.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataDeParaRepository extends JpaRepository<FornecedorInsumoDeParaEntity, UUID> {

    Optional<FornecedorInsumoDeParaEntity> findByFornecedorIdAndCodigoFornecedor(
            UUID fornecedorId, String codigoFornecedor);

    List<FornecedorInsumoDeParaEntity> findByFornecedorId(UUID fornecedorId);
}
