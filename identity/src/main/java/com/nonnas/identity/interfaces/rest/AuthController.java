package com.nonnas.identity.interfaces.rest;

import com.nonnas.identity.application.auth.AutenticarUseCase;
import com.nonnas.identity.application.auth.RefreshTokenUseCase;
import com.nonnas.identity.application.auth.TokenPair;
import com.nonnas.identity.interfaces.rest.dto.AuthDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AutenticarUseCase autenticar;
    private final RefreshTokenUseCase refresh;

    public AuthController(AutenticarUseCase autenticar, RefreshTokenUseCase refresh) {
        this.autenticar = autenticar;
        this.refresh = refresh;
    }

    @PostMapping("/login")
    public AuthDto.TokenResponse login(@Valid @RequestBody AuthDto.LoginRequest req) {
        TokenPair pair = autenticar.execute(req.email(), req.senha());
        return AuthDto.TokenResponse.from(pair);
    }

    @PostMapping("/refresh")
    public AuthDto.TokenResponse refresh(@Valid @RequestBody AuthDto.RefreshRequest req) {
        TokenPair pair = refresh.execute(req.refreshToken());
        return AuthDto.TokenResponse.from(pair);
    }
}
