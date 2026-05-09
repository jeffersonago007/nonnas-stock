package com.nonnas.identity.application.auth;

import com.nonnas.identity.application.ports.Usuario2faPort;
import com.nonnas.identity.application.ports.UsuarioRepository;
import com.nonnas.identity.domain.Usuario;
import com.nonnas.identity.domain.UsuarioId;
import com.nonnas.identity.infrastructure.crypto.CryptoService;
import com.nonnas.identity.infrastructure.security.TotpService;
import com.nonnas.sharedkernel.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Inicia o setup 2FA: gera secret base32 + URI otpauth para QR. Persiste
 * o secret cifrado com {@code confirmado=false} — só vira ativo após o
 * usuário confirmar o primeiro código (ver {@link Confirmar2faUseCase}).
 */
@Service
public class Iniciar2faUseCase {

    private static final String ISSUER = "Nonnas Stock";

    private final UsuarioRepository usuarios;
    private final Usuario2faPort port;
    private final TotpService totp;
    private final CryptoService crypto;

    public Iniciar2faUseCase(UsuarioRepository usuarios,
                             Usuario2faPort port,
                             TotpService totp,
                             CryptoService crypto) {
        this.usuarios = usuarios;
        this.port = port;
        this.totp = totp;
        this.crypto = crypto;
    }

    @Transactional
    public Resultado execute(UUID usuarioId) {
        Usuario usuario = usuarios.findById(UsuarioId.of(usuarioId))
                .orElseThrow(() -> new NotFoundException("Usuário", usuarioId));
        String secret = totp.generateSecretBase32();
        port.salvarSetup(usuarioId, crypto.encrypt(secret));
        String uri = totp.otpauthUri(secret, usuario.email().value(), ISSUER);
        return new Resultado(secret, uri);
    }

    /**
     * Devolve o secret em base32 + URI otpauth. Frontend usa o URI para
     * desenhar o QR; o secret em texto fica como fallback de digitação manual.
     * O usuário NÃO recebe esse secret de volta após {@code confirmar}.
     */
    public record Resultado(String secretBase32, String otpauthUri) {}
}
