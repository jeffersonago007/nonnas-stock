package com.nonnas.identity.interfaces.rest;

import com.nonnas.identity.application.notifications.NotificacaoInternaService;
import com.nonnas.identity.infrastructure.security.AuthenticatedPrincipal;
import com.nonnas.identity.interfaces.rest.dto.NotificacaoDto;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notificacoes")
public class NotificacaoController {

    private final NotificacaoInternaService service;

    public NotificacaoController(NotificacaoInternaService service) {
        this.service = service;
    }

    @GetMapping
    public List<NotificacaoDto.Response> listar(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @RequestParam(required = false) String tipo,
            @RequestParam(defaultValue = "false") boolean incluirArquivadas,
            @RequestParam(defaultValue = "false") boolean somenteNaoLidas,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listar(principal.usuarioId().value(), tipo, incluirArquivadas,
                        somenteNaoLidas, page, size).stream()
                .map(NotificacaoDto.Response::from)
                .toList();
    }

    /**
     * Endpoint leve, otimizado para polling do badge no header (30s).
     * Retorna apenas o count — UI não precisa do payload completo.
     */
    @GetMapping("/contagem-nao-lidas")
    public NotificacaoDto.ContagemResponse contagem(
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return new NotificacaoDto.ContagemResponse(
                service.contarNaoLidas(principal.usuarioId().value()));
    }

    @PostMapping("/{id}/marcar-lida")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarLida(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                           @PathVariable UUID id) {
        service.marcarLida(id, principal.usuarioId().value());
    }

    @PostMapping("/marcar-todas-lidas")
    public NotificacaoDto.ContagemResponse marcarTodasLidas(
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        int marcadas = service.marcarTodasLidas(principal.usuarioId().value());
        return new NotificacaoDto.ContagemResponse(marcadas);
    }

    @PostMapping("/{id}/arquivar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void arquivar(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                         @PathVariable UUID id) {
        service.arquivar(id, principal.usuarioId().value());
    }
}
