package com.nonnas.saleschannels.infrastructure.persistence;

import com.nonnas.saleschannels.domain.CanalTipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataCanalProdutoDeParaRepository extends JpaRepository<CanalProdutoDeParaEntity, UUID> {

    Optional<CanalProdutoDeParaEntity> findByCanalTipoAndExternalCodeAndFilialId(
            CanalTipo canalTipo, String externalCode, UUID filialId);

    @Query("""
        SELECT d FROM CanalProdutoDeParaEntity d
        WHERE d.canalTipo = :canal AND d.externalCode = :code AND d.filialId IS NULL
        """)
    Optional<CanalProdutoDeParaEntity> findGlobal(@Param("canal") CanalTipo canal,
                                                   @Param("code") String externalCode);

    List<CanalProdutoDeParaEntity> findByCanalTipoOrderByExternalCodeAsc(CanalTipo canalTipo);
}
