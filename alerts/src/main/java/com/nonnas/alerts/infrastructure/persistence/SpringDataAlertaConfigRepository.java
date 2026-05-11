package com.nonnas.alerts.infrastructure.persistence;

import com.nonnas.alerts.domain.TipoAlerta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SpringDataAlertaConfigRepository extends JpaRepository<AlertaConfigEntity, UUID> {

    @Query("""
        SELECT c FROM AlertaConfigEntity c
        WHERE c.ativo = true
          AND c.tipo = :tipo
          AND (c.insumoId IS NULL OR c.insumoId = :insumoId)
          AND (c.filialId IS NULL OR c.filialId = :filialId)
        """)
    List<AlertaConfigEntity> findAtivasParaEscopo(@Param("tipo") TipoAlerta tipo,
                                                   @Param("insumoId") UUID insumoId,
                                                   @Param("filialId") UUID filialId);

    List<AlertaConfigEntity> findByTipoAndAtivoIsTrue(TipoAlerta tipo);
}
