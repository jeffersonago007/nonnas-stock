package com.nonnas.identity.interfaces.rest;

import com.nonnas.identity.application.auth.Confirmar2faUseCase;
import com.nonnas.identity.application.auth.Iniciar2faUseCase;
import com.nonnas.identity.infrastructure.security.AuthenticatedPrincipal;
import com.nonnas.identity.interfaces.rest.dto.TwoFaDto;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/2fa")
public class TwoFaController {

    private final Iniciar2faUseCase iniciar;
    private final Confirmar2faUseCase confirmar;

    public TwoFaController(Iniciar2faUseCase iniciar, Confirmar2faUseCase confirmar) {
        this.iniciar = iniciar;
        this.confirmar = confirmar;
    }

    @PostMapping("/setup")
    public TwoFaDto.SetupResponse setup(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        Iniciar2faUseCase.Resultado r = iniciar.execute(principal.usuarioId().value());
        return new TwoFaDto.SetupResponse(r.secretBase32(), r.otpauthUri());
    }

    @PostMapping("/confirmar")
    public TwoFaDto.ConfirmarResponse confirmar(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                                                @Valid @RequestBody TwoFaDto.ConfirmarRequest req) {
        Confirmar2faUseCase.Resultado r = confirmar.execute(principal.usuarioId().value(), req.codigo());
        return new TwoFaDto.ConfirmarResponse(r.backupCodes());
    }
}
