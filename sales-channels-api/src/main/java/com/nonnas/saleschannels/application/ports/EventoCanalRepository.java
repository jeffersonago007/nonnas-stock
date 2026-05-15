package com.nonnas.saleschannels.application.ports;

import com.nonnas.saleschannels.domain.CanalTipo;
import com.nonnas.saleschannels.domain.EventoCanal;
import com.nonnas.saleschannels.domain.EventoCanalId;

import java.util.List;
import java.util.Optional;

public interface EventoCanalRepository {

    /**
     * Persiste um evento bruto. Devolve {@link Optional#empty()} quando o
     * {@code event_id} já existe para o canal — a chamada deve então ser
     * ignorada (idempotência). Não lança em vez de retornar empty para
     * deixar o caller decidir como logar/contar.
     */
    Optional<EventoCanal> salvarSeNovo(EventoCanal evento);

    EventoCanal atualizar(EventoCanal evento);

    Optional<EventoCanal> findById(EventoCanalId id);

    Optional<EventoCanal> findByCanalEEventIdExterno(CanalTipo canalTipo, String eventIdExterno);

    List<EventoCanal> listarNaoProcessados(int limite);
}
