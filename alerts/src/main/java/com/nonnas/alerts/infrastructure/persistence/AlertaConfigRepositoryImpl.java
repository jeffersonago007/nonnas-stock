package com.nonnas.alerts.infrastructure.persistence;

import com.nonnas.alerts.application.ports.AlertaConfigRepository;
import com.nonnas.alerts.domain.AlertaConfig;
import com.nonnas.alerts.domain.AlertaConfigId;
import com.nonnas.alerts.domain.TipoAlerta;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class AlertaConfigRepositoryImpl implements AlertaConfigRepository {

    private final SpringDataAlertaConfigRepository jpa;

    AlertaConfigRepositoryImpl(SpringDataAlertaConfigRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public AlertaConfig save(AlertaConfig c) {
        return AlertsMappers.toDomain(jpa.save(AlertsMappers.toEntity(c)));
    }

    @Override
    public Optional<AlertaConfig> findById(AlertaConfigId id) {
        return jpa.findById(id.value()).map(AlertsMappers::toDomain);
    }

    @Override
    public List<AlertaConfig> findAtivasParaEscopo(TipoAlerta tipo, UUID insumoId, UUID filialId) {
        return jpa.findAtivasParaEscopo(tipo, insumoId, filialId).stream()
                .map(AlertsMappers::toDomain).toList();
    }

    @Override
    public List<AlertaConfig> findAtivasPorTipo(TipoAlerta tipo) {
        return jpa.findByTipoAndAtivoIsTrue(tipo).stream()
                .map(AlertsMappers::toDomain).toList();
    }

    @Override
    public List<AlertaConfig> findAll(int page, int size) {
        return jpa.findAll(PageRequest.of(page, size)).map(AlertsMappers::toDomain).getContent();
    }
}
