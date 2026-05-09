package com.nonnas.alerts.application.ports;

import com.nonnas.alerts.domain.AlertaConfig;
import com.nonnas.alerts.domain.AlertaConfigId;
import com.nonnas.alerts.domain.TipoAlerta;

import java.util.List;
import java.util.Optional;

public interface AlertaConfigRepository {
    AlertaConfig save(AlertaConfig c);
    Optional<AlertaConfig> findById(AlertaConfigId id);

    /**
     * Configurações ativas que se aplicam ao escopo dado. Inclui configs com
     * {@code insumo_id IS NULL} ou {@code filial_id IS NULL} (escopo global).
     * Filtra por {@code tipo}.
     */
    List<AlertaConfig> findAtivasParaEscopo(TipoAlerta tipo,
                                            java.util.UUID insumoId,
                                            java.util.UUID filialId);

    List<AlertaConfig> findAtivasPorTipo(TipoAlerta tipo);

    List<AlertaConfig> findAll(int page, int size);
}
