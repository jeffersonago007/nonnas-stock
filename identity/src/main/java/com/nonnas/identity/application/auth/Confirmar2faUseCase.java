package com.nonnas.identity.application.auth;

import com.nonnas.identity.application.audit.AuditEvent;
import com.nonnas.identity.application.audit.AuditLogService;
import com.nonnas.identity.application.ports.Usuario2faPort;
import com.nonnas.identity.application.ports.UsuarioRepository;
import com.nonnas.identity.domain.Usuario;
import com.nonnas.identity.domain.UsuarioId;
import com.nonnas.identity.infrastructure.crypto.CryptoService;
import com.nonnas.identity.infrastructure.security.TotpService;
import com.nonnas.sharedkernel.BusinessRuleException;
import com.nonnas.sharedkernel.ErrorCode;
import com.nonnas.sharedkernel.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * Confirma o setup 2FA verificando o primeiro código TOTP. Em sucesso:
 *   - marca o registro como {@code confirmado=true};
 *   - gera 8 backup codes one-shot, devolve em texto puro pra o usuário,
 *     persiste apenas hashes SHA-256.
 * O usuário deve guardar os backup codes (master doc 13.1).
 */
@Service
public class Confirmar2faUseCase {

    private static final int BACKUP_CODES_QUANTIDADE = 8;
    private static final int BACKUP_CODE_BYTES = 6; // -> 12 hex chars

    private final UsuarioRepository usuarios;
    private final Usuario2faPort port;
    private final TotpService totp;
    private final CryptoService crypto;
    private final AuditLogService auditLog;
    private final Clock clock;
    private final SecureRandom rng = new SecureRandom();

    public Confirmar2faUseCase(UsuarioRepository usuarios,
                               Usuario2faPort port,
                               TotpService totp,
                               CryptoService crypto,
                               AuditLogService auditLog,
                               Clock clock) {
        this.usuarios = usuarios;
        this.port = port;
        this.totp = totp;
        this.crypto = crypto;
        this.auditLog = auditLog;
        this.clock = clock;
    }

    @Transactional
    public Resultado execute(UUID usuarioId, String codigo) {
        Usuario usuario = usuarios.findById(UsuarioId.of(usuarioId))
                .orElseThrow(() -> new NotFoundException("Usuário", usuarioId));
        Usuario2faPort.Snapshot snap = port.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new BusinessRuleException(
                        ErrorCode.CONFLICT, "Setup 2FA não iniciado — chame /setup primeiro"));

        if (snap.confirmado()) {
            throw new BusinessRuleException(ErrorCode.CONFLICT, "2FA já confirmado para esse usuário");
        }

        String secret = crypto.decrypt(snap.secretCifrado());
        if (!totp.verifyCode(secret, codigo)) {
            throw new BusinessRuleException(ErrorCode.UNAUTHORIZED, "Código TOTP inválido");
        }

        List<String> backupTexts = new ArrayList<>(BACKUP_CODES_QUANTIDADE);
        List<String> backupHashes = new ArrayList<>(BACKUP_CODES_QUANTIDADE);
        for (int i = 0; i < BACKUP_CODES_QUANTIDADE; i++) {
            byte[] raw = new byte[BACKUP_CODE_BYTES];
            rng.nextBytes(raw);
            String code = HexFormat.of().formatHex(raw);
            backupTexts.add(code);
            backupHashes.add(sha256Hex(code));
        }

        port.confirmar(usuarioId, backupHashes, clock.instant());
        auditLog.registrarTentativaLogin(AuditEvent.Types.TWO_FA_HABILITADO, usuario, null, null, null);

        return new Resultado(backupTexts);
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 indisponível?", ex);
        }
    }

    /**
     * Backup codes em texto puro — devolvidos UMA VEZ ao usuário. Frontend
     * deve forçar leitura/cópia antes de fechar o wizard.
     */
    public record Resultado(List<String> backupCodes) {}
}
