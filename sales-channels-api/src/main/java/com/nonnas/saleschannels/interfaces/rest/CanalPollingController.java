package com.nonnas.saleschannels.interfaces.rest;

import com.nonnas.saleschannels.application.ProcessarPedidoCanalUseCase;
import com.nonnas.saleschannels.application.ports.EventoCanalRepository;
import com.nonnas.saleschannels.domain.CanalTipo;
import com.nonnas.saleschannels.domain.EventoCanal;
import com.nonnas.saleschannels.infrastructure.schedule.CanalPollingScheduler;
import com.nonnas.sharedkernel.NotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Endpoints administrativos para operação dos canais — trigger manual
 * de polling (puxa eventos do canal) e de processamento (consome eventos
 * pendentes → baixa estoque → confirma no canal). Em dev sem cron, são
 * a forma de testar o pipeline.
 */
@RestController
@RequestMapping("/api/v1/canais")
class CanalPollingController {

    private final CanalPollingScheduler scheduler;
    private final ProcessarPedidoCanalUseCase processarUseCase;
    private final EventoCanalRepository eventos;

    CanalPollingController(CanalPollingScheduler scheduler,
                           ProcessarPedidoCanalUseCase processarUseCase,
                           EventoCanalRepository eventos) {
        this.scheduler = scheduler;
        this.processarUseCase = processarUseCase;
        this.eventos = eventos;
    }

    /**
     * Polling sob demanda para um canal específico. Retorna a quantidade
     * de eventos novos persistidos (idempotente — chamar 2x em sequência
     * não duplica eventos).
     */
    @PostMapping("/{tipo}/poll-now")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<Map<String, Object>> pollNow(@PathVariable("tipo") String tipo) {
        CanalTipo canal = parseCanalOuFalha(tipo);
        if (!scheduler.suportaCanal(canal)) {
            throw new NotFoundException("Nenhum adapter registrado para " + canal);
        }
        int novos = scheduler.pollCanal(canal);
        return ResponseEntity.ok(Map.of(
                "canal", canal.name(),
                "eventosNovos", novos));
    }

    /**
     * Processa todos os eventos pendentes (não-processados). Cada evento
     * gera 1 pedido (se PEDIDO_CRIADO), 1 baixa multi-item, 1 confirmação
     * no canal. Limite default 50 — alinhado ao batch-size do polling.
     */
    @PostMapping("/processar-pendentes")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    ResponseEntity<Map<String, Object>> processarPendentes(@AuthenticationPrincipal UserDetails user) {
        List<EventoCanal> pendentes = eventos.listarNaoProcessados(50);
        UUID usuarioSistemaId = UUID.nameUUIDFromBytes(user.getUsername().getBytes());

        int sucesso = 0;
        int falha = 0;
        for (EventoCanal e : pendentes) {
            try {
                processarUseCase.processarEventoNaoProcessado(e.id().value(), usuarioSistemaId);
                sucesso++;
            } catch (RuntimeException ex) {
                falha++;
                // Erro já gravado no evento via use case — só contamos aqui.
            }
        }
        return ResponseEntity.ok(Map.of(
                "processadosSucesso", sucesso,
                "processadosFalha", falha,
                "totalPendentes", pendentes.size()));
    }

    private static CanalTipo parseCanalOuFalha(String tipo) {
        try {
            return CanalTipo.valueOf(tipo.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new NotFoundException("Canal desconhecido: " + tipo);
        }
    }
}
