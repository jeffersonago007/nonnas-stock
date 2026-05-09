package com.nonnas.alerts.infrastructure.schedule;

import com.nonnas.alerts.domain.AvaliadorAlertasService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Job diário (06:00 BRT) que avalia alertas de
 * {@link com.nonnas.alerts.domain.TipoAlerta#VENCIMENTO_PROXIMO_DIAS}.
 * Idempotente — disparos repetidos para o mesmo lote são bloqueados
 * pelo partial unique index do schema.
 */
@Component
public class VencimentoScheduledJob {

    private static final Logger log = LoggerFactory.getLogger(VencimentoScheduledJob.class);

    private final AvaliadorAlertasService avaliador;

    public VencimentoScheduledJob(AvaliadorAlertasService avaliador) {
        this.avaliador = avaliador;
    }

    @Scheduled(cron = "0 0 6 * * *", zone = "America/Sao_Paulo")
    @Transactional
    public void avaliarDiariamente() {
        try {
            int disparados = avaliador.avaliarVencimentos();
            log.info("Job de vencimento concluído — {} alertas disparados", disparados);
        } catch (RuntimeException e) {
            log.error("Falha no job de vencimento", e);
        }
    }
}
