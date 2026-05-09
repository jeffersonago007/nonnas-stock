package com.nonnas.alerts.application.ports;

import com.nonnas.alerts.domain.AlertaConfigId;
import com.nonnas.alerts.domain.AlertaDisparado;
import com.nonnas.alerts.domain.AlertaDisparadoId;
import com.nonnas.alerts.domain.StatusAlerta;
import com.nonnas.alerts.domain.TipoAlerta;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertaDisparadoRepository {
    AlertaDisparado save(AlertaDisparado a);

    /**
     * Como {@link #save(AlertaDisparado)}, mas para um disparo novo.
     * Implementação publica {@code AlertaDisparadoEvent} (master doc T17 /
     * 15.4) — listeners materializam notificações internas e podem
     * incrementar métricas. Use apenas no caminho de criação; updates
     * (ex.: auto-resolução) seguem usando {@code save}.
     */
    AlertaDisparado salvarNovo(AlertaDisparado a);

    Optional<AlertaDisparado> findById(AlertaDisparadoId id);

    /** Idempotência para alertas sem lote (estoque). */
    Optional<AlertaDisparado> findAtivoSemLote(AlertaConfigId configId, UUID insumoId, UUID filialId);

    /** Idempotência para alertas de vencimento (com lote). */
    Optional<AlertaDisparado> findAtivoPorLote(AlertaConfigId configId, UUID loteId);

    /** Ativos do escopo (insumo, filial), tipo opcional. Usado em auto-resolução. */
    List<AlertaDisparado> findAtivosPorEscopo(UUID insumoId, UUID filialId, TipoAlerta tipoOpt);

    /** Ativos para um lote específico — usado em auto-resolução de vencimento. */
    List<AlertaDisparado> findAtivosPorLote(UUID loteId);

    /** Listagem com filtros para o use case ListarAlertasDisparados. */
    List<AlertaDisparado> listar(Filtros filtros, int page, int size);

    record Filtros(
            StatusAlerta status,
            UUID filialId,
            UUID insumoId,
            TipoAlerta tipo,
            Instant dataDisparoDe,
            Instant dataDisparoAte
    ) {}
}
