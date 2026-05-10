package com.nonnas.identity.interfaces.rest;

import com.nonnas.identity.application.auth.AutenticarUseCase;
import com.nonnas.identity.application.auth.LoginResult;
import com.nonnas.identity.application.auth.LogoutUseCase;
import com.nonnas.identity.application.auth.RefreshTokenUseCase;
import com.nonnas.identity.application.auth.TokenPair;
import com.nonnas.identity.interfaces.rest.dto.AuthDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AutenticarUseCase autenticar;
    private final RefreshTokenUseCase refresh;
    private final LogoutUseCase logout;

    public AuthController(AutenticarUseCase autenticar, RefreshTokenUseCase refresh, LogoutUseCase logout) {
        this.autenticar = autenticar;
        this.refresh = refresh;
        this.logout = logout;
    }

    @PostMapping("/login")
    public AuthDto.LoginResponse login(@Valid @RequestBody AuthDto.LoginRequest req) {
        LoginResult result = autenticar.execute(req.email(), req.senha());
        return AuthDto.LoginResponse.from(result);
    }

    @PostMapping("/refresh")
    public AuthDto.TokenResponse refresh(@Valid @RequestBody AuthDto.RefreshRequest req) {
        TokenPair pair = refresh.execute(req.refreshToken());
        return AuthDto.TokenResponse.from(pair);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestBody(required = false) AuthDto.LogoutRequest req,
                       HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) return;
        String accessToken = header.substring("Bearer ".length());
        String refreshToken = req != null ? req.refreshToken() : null;
        logout.execute(accessToken, refreshToken);
    }
}
