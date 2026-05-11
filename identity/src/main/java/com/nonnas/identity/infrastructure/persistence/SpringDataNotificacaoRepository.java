package com.nonnas.identity.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface SpringDataNotificacaoRepository extends JpaRepository<NotificacaoEntity, UUID> {

    @Query("""
            SELECT n FROM NotificacaoEntity n
            WHERE n.usuarioId = :usuarioId
              AND (:tipo IS NULL OR n.tipo = :tipo)
              AND (:incluirArquivadas = true OR n.arquivadaEm IS NULL)
              AND (:somenteNaoLidas = false OR n.lidaEm IS NULL)
            ORDER BY n.criadaEm DESC
            """)
    Page<NotificacaoEntity> findFiltered(@Param("usuarioId") UUID usuarioId,
                                         @Param("tipo") String tipo,
                                         @Param("incluirArquivadas") boolean incluirArquivadas,
                                         @Param("somenteNaoLidas") boolean somenteNaoLidas,
                                         Pageable pageable);

    @Query("""
            SELECT COUNT(n) FROM NotificacaoEntity n
            WHERE n.usuarioId = :usuarioId
              AND n.lidaEm IS NULL
              AND n.arquivadaEm IS NULL
            """)
    long countNaoLidas(@Param("usuarioId") UUID usuarioId);

    @Modifying
    @Query("""
            UPDATE NotificacaoEntity n
               SET n.lidaEm = :agora
             WHERE n.id = :id
               AND n.lidaEm IS NULL
            """)
    int marcarLida(@Param("id") UUID id, @Param("agora") Instant agora);

    @Modifying
    @Query("""
            UPDATE NotificacaoEntity n
               SET n.lidaEm = :agora
             WHERE n.usuarioId = :usuarioId
               AND n.lidaEm IS NULL
            """)
    int marcarTodasLidas(@Param("usuarioId") UUID usuarioId, @Param("agora") Instant agora);

    @Modifying
    @Query("""
            UPDATE NotificacaoEntity n
               SET n.arquivadaEm = :agora,
                   n.lidaEm = COALESCE(n.lidaEm, :agora)
             WHERE n.id = :id
               AND n.arquivadaEm IS NULL
            """)
    int arquivar(@Param("id") UUID id, @Param("agora") Instant agora);
}
