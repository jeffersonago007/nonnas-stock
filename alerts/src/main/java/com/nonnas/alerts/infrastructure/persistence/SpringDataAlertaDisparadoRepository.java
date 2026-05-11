package com.nonnas.alerts.infrastructure.persistence;

import com.nonnas.alerts.domain.StatusAlerta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataAlertaDisparadoRepository extends JpaRepository<AlertaDisparadoEntity, UUID> {

    @Query("""
        SELECT a FROM AlertaDisparadoEntity a
        WHERE a.status = 'ATIVO'
          AND a.configId = :configId
          AND a.insumoId = :insumoId
          AND a.filialId = :filialId
          AND a.loteId IS NULL
        """)
    Optional<AlertaDisparadoEntity> findAtivoSemLote(@Param("configId") UUID configId,
                                                      @Param("insumoId") UUID insumoId,
                                                      @Param("filialId") UUID filialId);

    @Query("""
        SELECT a FROM AlertaDisparadoEntity a
        WHERE a.status = 'ATIVO'
          AND a.configId = :configId
          AND a.loteId = :loteId
        """)
    Optional<AlertaDisparadoEntity> findAtivoPorLote(@Param("configId") UUID configId,
                                                      @Param("loteId") UUID loteId);

    List<AlertaDisparadoEntity> findByStatusAndInsumoIdAndFilialId(
            StatusAlerta status, UUID insumoId, UUID filialId);

    List<AlertaDisparadoEntity> findByStatusAndLoteId(StatusAlerta status, UUID loteId);
}
