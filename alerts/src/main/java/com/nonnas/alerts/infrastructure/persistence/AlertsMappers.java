package com.nonnas.alerts.infrastructure.persistence;

import com.nonnas.alerts.domain.AlertaConfig;
import com.nonnas.alerts.domain.AlertaConfigId;
import com.nonnas.alerts.domain.AlertaDisparado;
import com.nonnas.alerts.domain.AlertaDisparadoId;

final class AlertsMappers {

    private AlertsMappers() {}

    // AlertaConfig
    static AlertaConfigEntity toEntity(AlertaConfig c) {
        AlertaConfigEntity e = new AlertaConfigEntity();
        e.setId(c.id().value());
        e.setTipo(c.tipo());
        e.setInsumoId(c.insumoIdOpt().orElse(null));
        e.setFilialId(c.filialIdOpt().orElse(null));
        e.setThreshold(c.thresholdOpt().orElse(null));
        e.setAtivo(c.ativo());
        e.setPrioridade(c.prioridade());
        e.setObservacao(c.observacaoOpt().orElse(null));
        e.setCreatedAt(c.createdAt());
        e.setUpdatedAt(c.updatedAt());
        return e;
    }

    static AlertaConfig toDomain(AlertaConfigEntity e) {
        return new AlertaConfig(
                AlertaConfigId.of(e.getId()), e.getTipo(),
                e.getInsumoId(), e.getFilialId(),
                e.getThreshold(), e.isAtivo(), e.getPrioridade(), e.getObservacao(),
                e.getCreatedAt(), e.getUpdatedAt());
    }

    // AlertaDisparado
    static AlertaDisparadoEntity toEntity(AlertaDisparado a) {
        AlertaDisparadoEntity e = new AlertaDisparadoEntity();
        e.setId(a.id().value());
        e.setConfigId(a.configId().value());
        e.setTipo(a.tipo());
        e.setInsumoId(a.insumoId());
        e.setFilialId(a.filialId());
        e.setLoteId(a.loteIdOpt().orElse(null));
        e.setStatus(a.status());
        e.setSaldoNoDisparo(a.saldoNoDisparoOpt().orElse(null));
        e.setDetalhe(a.detalheOpt().orElse(null));
        e.setDataDisparo(a.dataDisparo());
        e.setDataResolucao(a.dataResolucaoOpt().orElse(null));
        e.setVisualizadoEm(a.visualizadoEmOpt().orElse(null));
        e.setVisualizadoPor(a.visualizadoPorOpt().orElse(null));
        e.setResolvidoPor(a.resolvidoPorOpt().orElse(null));
        return e;
    }

    static AlertaDisparado toDomain(AlertaDisparadoEntity e) {
        return new AlertaDisparado(
                AlertaDisparadoId.of(e.getId()),
                AlertaConfigId.of(e.getConfigId()),
                e.getTipo(), e.getInsumoId(), e.getFilialId(), e.getLoteId(),
                e.getStatus(), e.getSaldoNoDisparo(), e.getDetalhe(),
                e.getDataDisparo(), e.getDataResolucao(),
                e.getVisualizadoEm(), e.getVisualizadoPor(), e.getResolvidoPor());
    }
}
