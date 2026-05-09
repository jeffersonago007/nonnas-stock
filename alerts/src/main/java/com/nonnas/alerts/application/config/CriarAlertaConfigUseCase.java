package com.nonnas.alerts.application.config;

import com.nonnas.alerts.application.ports.AlertaConfigRepository;
import com.nonnas.alerts.domain.AlertaConfig;
import com.nonnas.alerts.domain.TipoAlerta;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;

@Service
public class CriarAlertaConfigUseCase {

    private final AlertaConfigRepository repo;
    private final Clock clock;

    public CriarAlertaConfigUseCase(AlertaConfigRepository repo, Clock clock) {
        this.repo = repo;
        this.clock = clock;
    }

    @Transactional
    public AlertaConfig execute(Comando cmd) {
        AlertaConfig c = AlertaConfig.novo(cmd.tipo, cmd.insumoId, cmd.filialId,
                cmd.threshold, cmd.prioridade, cmd.observacao, clock.instant());
        return repo.save(c);
    }

    public record Comando(
            TipoAlerta tipo,
            UUID insumoId,
            UUID filialId,
            BigDecimal threshold,
            int prioridade,
            String observacao
    ) {}
}
