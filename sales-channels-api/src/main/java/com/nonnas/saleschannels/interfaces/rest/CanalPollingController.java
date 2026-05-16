package com.nonnas.saleschannels.interfaces.rest;

import com.nonnas.saleschannels.domain.CanalTipo;
import com.nonnas.saleschannels.infrastructure.schedule.CanalPollingScheduler;
import com.nonnas.sharedkernel.NotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoints administrativos para operação dos canais. Por enquanto só o
 * trigger manual de polling — útil em dev (sem cron) e em CI para forçar
 * sincronização sob demanda.
 */
@RestController
@RequestMapping("/api/v1/canais")
class CanalPollingController {

    private final CanalPollingScheduler scheduler;

    CanalPollingController(CanalPollingScheduler scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * Dispara polling sob demanda para um canal específico. Retorna a
     * quantidade de eventos novos persistidos (idempotente — chamar 2x
     * em sequência não duplica eventos).
     */
    @PostMapping("/{tipo}/poll-now")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<Map<String, Object>> pollNow(@PathVariable("tipo") String tipo) {
        CanalTipo canal;
        try {
            canal = CanalTipo.valueOf(tipo.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new NotFoundException("Canal desconhecido: " + tipo);
        }
        if (!scheduler.suportaCanal(canal)) {
            throw new NotFoundException("Nenhum adapter registrado para " + canal);
        }
        int novos = scheduler.pollCanal(canal);
        return ResponseEntity.ok(Map.of(
                "canal", canal.name(),
                "eventosNovos", novos));
    }
}
