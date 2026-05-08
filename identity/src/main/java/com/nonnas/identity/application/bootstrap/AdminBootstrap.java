package com.nonnas.identity.application.bootstrap;

import com.nonnas.identity.application.password.HistoricoSenhaService;
import com.nonnas.identity.application.ports.UsuarioRepository;
import com.nonnas.identity.domain.Email;
import com.nonnas.identity.domain.Perfil;
import com.nonnas.identity.domain.SenhaHash;
import com.nonnas.identity.domain.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * Idempotent admin seed (replaces the original V002 Flyway seed planned in
 * the master document — see commit message for ADR rationale: BCrypt hashes
 * are non-deterministic, so committing a fixed hash via Flyway placeholder
 * adds friction without security benefit).
 *
 * <p>On startup, if no user exists with email {@code admin@nonnas.com}, one
 * is created with the password from environment variable
 * {@code NONNAS_INITIAL_ADMIN_PASSWORD} (default {@code AdminNonnas2026!} —
 * dev only; production MUST override and rotate after first login).
 */
@Component
public class AdminBootstrap implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);
    private static final Email ADMIN_EMAIL = Email.of("admin@nonnas.com");

    private final UsuarioRepository usuarioRepo;
    private final PasswordEncoder encoder;
    private final HistoricoSenhaService historico;
    private final Clock clock;
    private final String initialPassword;

    public AdminBootstrap(UsuarioRepository usuarioRepo,
                          PasswordEncoder encoder,
                          HistoricoSenhaService historico,
                          Clock clock,
                          @Value("${nonnas.security.initial-admin-password:AdminNonnas2026!}")
                          String initialPassword) {
        this.usuarioRepo = usuarioRepo;
        this.encoder = encoder;
        this.historico = historico;
        this.clock = clock;
        this.initialPassword = initialPassword;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepo.findByEmail(ADMIN_EMAIL).isPresent()) {
            log.debug("Admin user already exists — skipping bootstrap");
            return;
        }
        SenhaHash hash = SenhaHash.of(encoder.encode(initialPassword));
        Usuario admin = Usuario.novo(null, "Administrador Nonnas", ADMIN_EMAIL, hash, Perfil.ADMIN, clock.instant());
        Usuario saved = usuarioRepo.save(admin);
        historico.registrarSenha(saved.id(), hash.value());
        log.warn("AdminBootstrap criou usuário admin@nonnas.com com senha do env (default troca obrigatória após primeiro login)");
    }
}
