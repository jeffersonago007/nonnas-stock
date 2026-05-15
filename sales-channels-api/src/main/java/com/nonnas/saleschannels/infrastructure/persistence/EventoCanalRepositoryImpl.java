package com.nonnas.saleschannels.infrastructure.persistence;

import com.nonnas.saleschannels.application.ports.EventoCanalRepository;
import com.nonnas.saleschannels.domain.CanalTipo;
import com.nonnas.saleschannels.domain.EventoCanal;
import com.nonnas.saleschannels.domain.EventoCanalId;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
class EventoCanalRepositoryImpl implements EventoCanalRepository {

    private final SpringDataEventoCanalRepository jpa;

    EventoCanalRepositoryImpl(SpringDataEventoCanalRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<EventoCanal> salvarSeNovo(EventoCanal evento) {
        // Pre-check: evita exception em caso comum de duplicado (polling reprocessando).
        if (jpa.findByCanalTipoAndEventIdExterno(evento.canalTipo(), evento.eventIdExterno()).isPresent()) {
            return Optional.empty();
        }
        try {
            return Optional.of(SalesChannelsMappers.toDomain(jpa.save(SalesChannelsMappers.toEntity(evento))));
        } catch (DataIntegrityViolationException race) {
            // Race condition: outra thread inseriu primeiro entre o pre-check e o save.
            return Optional.empty();
        }
    }

    @Override
    public EventoCanal atualizar(EventoCanal evento) {
        return SalesChannelsMappers.toDomain(jpa.save(SalesChannelsMappers.toEntity(evento)));
    }

    @Override
    public Optional<EventoCanal> findById(EventoCanalId id) {
        return jpa.findById(id.value()).map(SalesChannelsMappers::toDomain);
    }

    @Override
    public Optional<EventoCanal> findByCanalEEventIdExterno(CanalTipo canalTipo, String eventIdExterno) {
        return jpa.findByCanalTipoAndEventIdExterno(canalTipo, eventIdExterno)
                .map(SalesChannelsMappers::toDomain);
    }

    @Override
    public List<EventoCanal> listarNaoProcessados(int limite) {
        return jpa.findNaoProcessados(PageRequest.of(0, limite)).stream()
                .map(SalesChannelsMappers::toDomain).toList();
    }
}
