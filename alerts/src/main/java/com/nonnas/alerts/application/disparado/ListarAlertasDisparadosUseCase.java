package com.nonnas.alerts.application.disparado;

import com.nonnas.alerts.application.ports.AlertaDisparadoRepository;
import com.nonnas.alerts.domain.AlertaDisparado;
import com.nonnas.alerts.domain.StatusAlerta;
import com.nonnas.alerts.domain.TipoAlerta;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ListarAlertasDisparadosUseCase {

    private final AlertaDisparadoRepository repo;

    public ListarAlertasDisparadosUseCase(AlertaDisparadoRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<AlertaDisparado> execute(Filtros filtros, int page, int size) {
        var f = new AlertaDisparadoRepository.Filtros(
                filtros.status, filtros.filialId, filtros.insumoId, filtros.tipo,
                filtros.dataDisparoDe, filtros.dataDisparoAte);
        return repo.listar(f, page, size);
    }

    public record Filtros(
            StatusAlerta status,
            UUID filialId,
            UUID insumoId,
            TipoAlerta tipo,
            Instant dataDisparoDe,
            Instant dataDisparoAte
    ) {}
}
