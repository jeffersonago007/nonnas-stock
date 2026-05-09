package com.nonnas.identity.application.auth;

import com.nonnas.identity.application.audit.AuditEvent;
import com.nonnas.identity.application.audit.AuditLogService;
import com.nonnas.identity.application.ports.UsuarioRepository;
import com.nonnas.identity.domain.Email;
import com.nonnas.identity.domain.Usuario;
import com.nonnas.identity.infrastructure.security.JwtTokenProvider;
import com.nonnas.identity.infrastructure.security.RefreshTokenService;
import com.nonnas.sharedkernel.BusinessRuleException;
import com.nonnas.sharedkernel.ErrorCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Login flow:
 * <ol>
 *   <li>Look up user by email; uniform "credenciais inválidas" if not found
 *       (avoids user enumeration).</li>
 *   <li>Reject if account is locked (travada or bloqueado_ate &gt; now).</li>
 *   <li>Verify BCrypt password. On failure: increment counters per
 *       {@link Usuario#registrarLoginFalho(java.time.Instant)}, persist, deny.</li>
 *   <li>On success: reset counters, issue access + refresh tokens (new
 *       refresh family).</li>
 * </ol>
 */
@Service
public class AutenticarUseCase {

    private final UsuarioRepository usuarioRepo;
    private final PasswordEncoder encoder;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenService refreshTokens;
    private final AuditLogService auditLog;
    private final Clock clock;

    public AutenticarUseCase(UsuarioRepository usuarioRepo,
                             PasswordEncoder encoder,
                             JwtTokenProvider tokenProvider,
                             RefreshTokenService refreshTokens,
                             AuditLogService auditLog,
                             Clock clock) {
        this.usuarioRepo = usuarioRepo;
        this.encoder = encoder;
        this.tokenProvider = tokenProvider;
        this.refreshTokens = refreshTokens;
        this.auditLog = auditLog;
        this.clock = clock;
    }

    /**
     * {@code noRollbackFor = BusinessRuleException} é necessário porque a
     * incrementação do contador de tentativas falhas precisa <em>persistir</em>
     * mesmo quando lançamos a exceção que sinaliza credencial inválida — sem
     * isso o rollback default desfaz o save e a brute-force protection
     * nunca acumula contagem.
     */
    @Transactional(noRollbackFor = BusinessRuleException.class)
    public TokenPair execute(String emailRaw, String senhaPlaintext) {
        Email email;
        try {
            email = Email.of(emailRaw);
        } catch (Exception ex) {
            throw new BusinessRuleException(ErrorCode.UNAUTHORIZED, "Credenciais inválidas");
        }

        Usuario usuario = usuarioRepo.findByEmail(email)
                .orElseThrow(() -> {
                    auditLog.registrarTentativaLogin(AuditEvent.Types.LOGIN_FAILED, null, null, null,
                            "{\"motivo\":\"email_inexistente\",\"emailTentado\":\"" + emailRaw + "\"}");
                    return new BusinessRuleException(ErrorCode.UNAUTHORIZED, "Credenciais inválidas");
                });

        if (!usuario.ativo()) {
            auditLog.registrarTentativaLogin(AuditEvent.Types.LOGIN_BLOQUEADO, usuario, null, null,
                    "{\"motivo\":\"usuario_desativado\"}");
            throw new BusinessRuleException(ErrorCode.FORBIDDEN, "Usuário desativado");
        }

        if (usuario.travada()) {
            auditLog.registrarTentativaLogin(AuditEvent.Types.LOGIN_BLOQUEADO, usuario, null, null,
                    "{\"motivo\":\"conta_travada\"}");
            throw new BusinessRuleException(
                    ErrorCode.FORBIDDEN,
                    "Conta travada por excesso de tentativas. Contate um administrador.");
        }

        if (usuario.estaBloqueado(clock.instant())) {
            auditLog.registrarTentativaLogin(AuditEvent.Types.LOGIN_BLOQUEADO, usuario, null, null,
                    "{\"motivo\":\"bloqueado_temporariamente\"}");
            throw new BusinessRuleException(
                    ErrorCode.FORBIDDEN,
                    "Conta temporariamente bloqueada. Tente novamente mais tarde.");
        }

        boolean ok = encoder.matches(senhaPlaintext, usuario.senhaHash().value());
        if (!ok) {
            usuario.registrarLoginFalho(clock.instant());
            usuarioRepo.save(usuario);
            auditLog.registrarTentativaLogin(AuditEvent.Types.LOGIN_FAILED, usuario, null, null,
                    "{\"motivo\":\"senha_invalida\"}");
            throw new BusinessRuleException(ErrorCode.UNAUTHORIZED, "Credenciais inválidas");
        }

        usuario.registrarLoginSucesso(clock.instant());
        usuarioRepo.save(usuario);
        auditLog.registrarTentativaLogin(AuditEvent.Types.LOGIN_SUCCESS, usuario, null, null, null);

        JwtTokenProvider.IssuedToken access = tokenProvider.issueAccess(usuario);
        JwtTokenProvider.IssuedToken refresh = refreshTokens.issueNewFamily(usuario);
        return new TokenPair(access.value(), access.expiresAt(), refresh.value(), refresh.expiresAt());
    }
}
