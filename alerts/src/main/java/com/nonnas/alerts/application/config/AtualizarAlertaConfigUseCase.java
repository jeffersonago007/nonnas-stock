package com.nonnas.alerts.application.config;

import com.nonnas.alerts.application.ports.AlertaConfigRepository;
import com.nonnas.alerts.domain.AlertaConfig;
import com.nonnas.alerts.domain.AlertaConfigId;
import com.nonnas.sharedkernel.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;

@Service
public class AtualizarAlertaConfigUseCase {

    private final AlertaConfigRepository repo;
    private final Clock clock;

    public AtualizarAlertaConfigUseCase(AlertaConfigRepository repo, Clock clock) {
        this.repo = repo;
        this.clock = clock;
    }

    @Transactional
    public AlertaConfig execute(Comando cmd) {
        AlertaConfig c = repo.findById(AlertaConfigId.of(cmd.id))
                .orElseThrow(() -> new NotFoundException("Alerta config", cmd.id));
        c.atualizar(cmd.threshold, cmd.prioridade, cmd.observacao, clock.instant());
        if (cmd.ativo) c.ativar(clock.instant()); else c.desativar(clock.instant());
        return repo.save(c);
    }

    public record Comando(
            UUID id,
            BigDecimal threshold,
            int prioridade,
            String observacao,
            boolean ativo
    ) {}
}
