package com.nonnas.identity.interfaces.rest;

import com.nonnas.identity.application.lgpd.LgpdService;
import com.nonnas.identity.infrastructure.security.AuthenticatedPrincipal;
import com.nonnas.identity.interfaces.rest.dto.LgpdDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/lgpd")
public class LgpdController {

    private final LgpdService lgpd;

    public LgpdController(LgpdService lgpd) {
        this.lgpd = lgpd;
    }

    @GetMapping("/meus-dados")
    public LgpdService.DadosPessoais meusDados(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return lgpd.meusDados(principal.usuarioId().value());
    }

    @PostMapping("/correcao")
    public LgpdService.DadosPessoais corrigir(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                                              @Valid @RequestBody LgpdDto.CorrigirRequest req) {
        return lgpd.corrigir(principal.usuarioId().value(), req.nome(), req.email());
    }

    @DeleteMapping("/exclusao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        lgpd.excluir(principal.usuarioId().value());
    }
}
