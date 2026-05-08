package com.nonnas.identity.application.auth;

import com.nonnas.identity.application.ports.UsuarioRepository;
import com.nonnas.identity.domain.Usuario;
import com.nonnas.identity.infrastructure.security.JwtTokenProvider;
import com.nonnas.identity.infrastructure.security.RefreshTokenService;
import com.nonnas.sharedkernel.BusinessRuleException;
import com.nonnas.sharedkernel.ErrorCode;
import io.jsonwebtoken.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenUseCase {

    private final UsuarioRepository usuarioRepo;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenService refreshTokens;

    public RefreshTokenUseCase(UsuarioRepository usuarioRepo,
                               JwtTokenProvider tokenProvider,
                               RefreshTokenService refreshTokens) {
        this.usuarioRepo = usuarioRepo;
        this.tokenProvider = tokenProvider;
        this.refreshTokens = refreshTokens;
    }

    /**
     * {@code noRollbackFor} é crítico aqui (e em
     * {@link com.nonnas.identity.infrastructure.security.RefreshTokenService#rotate}):
     * o outer transaction precisa <em>commitar</em> mesmo quando a rotação
     * detecta replay e lança 401, senão o {@code revokeFamily} é desfeito
     * pelo rollback default do Spring @Transactional.
     */
    @Transactional(noRollbackFor = BusinessRuleException.class)
    public TokenPair execute(String refreshTokenValue) {
        JwtTokenProvider.ParsedRefresh parsed;
        try {
            parsed = tokenProvider.parseRefresh(refreshTokenValue);
        } catch (JwtException ex) {
            throw new BusinessRuleException(ErrorCode.UNAUTHORIZED, "Refresh token inválido");
        }

        Usuario usuario = usuarioRepo.findById(parsed.usuarioId())
                .orElseThrow(() -> new BusinessRuleException(
                        ErrorCode.UNAUTHORIZED, "Usuário não encontrado"));

        if (!usuario.ativo() || usuario.travada()) {
            throw new BusinessRuleException(ErrorCode.FORBIDDEN, "Usuário não pode renovar token");
        }

        JwtTokenProvider.IssuedToken access = tokenProvider.issueAccess(usuario);
        JwtTokenProvider.IssuedToken newRefresh = refreshTokens.rotate(usuario, parsed);
        return new TokenPair(access.value(), access.expiresAt(), newRefresh.value(), newRefresh.expiresAt());
    }
}
