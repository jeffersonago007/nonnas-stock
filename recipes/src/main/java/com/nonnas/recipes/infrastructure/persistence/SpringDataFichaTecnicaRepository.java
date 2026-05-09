package com.nonnas.recipes.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataFichaTecnicaRepository extends JpaRepository<FichaTecnicaEntity, UUID> {

    Optional<FichaTecnicaEntity> findByProdutoVendavelIdAndAtivaIsTrue(UUID produtoVendavelId);

    List<FichaTecnicaEntity> findByProdutoVendavelIdOrderByVersaoDesc(UUID produtoVendavelId);
}
