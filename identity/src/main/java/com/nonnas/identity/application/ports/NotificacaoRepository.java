package com.nonnas.identity.application.ports;

import com.nonnas.identity.application.notifications.Notificacao;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificacaoRepository {

    Notificacao save(Notificacao n);

    Optional<Notificacao> findById(UUID id);

    /**
     * Listagem paginada com filtros. Todos os filtros são opcionais.
     *
     * @param incluirArquivadas se {@code false}, exclui arquivadas (UI default).
     * @param somenteNaoLidas   se {@code true}, exclui lidas.
     */
    List<Notificacao> findFiltered(UUID usuarioId, String tipo, boolean incluirArquivadas,
                                   boolean somenteNaoLidas, int page, int size);

    long countNaoLidas(UUID usuarioId);

    /** Marca como lida em {@code agora}. Idempotente. */
    void marcarLida(UUID id, Instant agora);

    /** Marca todas as não-lidas do usuário em {@code agora}. */
    int marcarTodasLidas(UUID usuarioId, Instant agora);

    /** Arquiva — some da lista default. */
    void arquivar(UUID id, Instant agora);
}
