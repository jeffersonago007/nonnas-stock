package com.nonnas.alerts.application.disparado;

import com.nonnas.alerts.application.ports.AlertaDisparadoRepository;
import com.nonnas.alerts.domain.AlertaDisparado;
import com.nonnas.alerts.domain.AlertaDisparadoId;
import com.nonnas.sharedkernel.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
public class MarcarAlertaResolvidoUseCase {

    private final AlertaDisparadoRepository repo;
    private final Clock clock;

    public MarcarAlertaResolvidoUseCase(AlertaDisparadoRepository repo, Clock clock) {
        this.repo = repo;
        this.clock = clock;
    }

    @Transactional
    public AlertaDisparado execute(UUID alertaId, UUID resolvidoPor) {
        AlertaDisparado a = repo.findById(AlertaDisparadoId.of(alertaId))
                .orElseThrow(() -> new NotFoundException("Alerta disparado", alertaId));
        a.resolverManual(resolvidoPor, clock.instant());
        return repo.save(a);
    }
}
