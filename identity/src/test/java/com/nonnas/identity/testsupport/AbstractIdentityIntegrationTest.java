package com.nonnas.identity.testsupport;

import com.nonnas.identity.IdentityTestApplication;
import com.nonnas.identity.application.ports.UsuarioRepository;
import com.nonnas.identity.domain.Email;
import com.nonnas.identity.domain.Usuario;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;

/**
 * Base for identity integration tests. Starts the full Spring Boot context
 * with embedded Postgres provided by Zonky (no Docker — see ADR 0007), and
 * uses MockMvc instead of a real HTTP transport. MockMvc exercises the
 * full Spring MVC + Security + use-case + JPA stack without going through
 * the JDK's HttpURLConnection, which proved fragile when handling 401 on
 * streaming POST bodies.
 *
 * <p>Boot time ~3–5s once Postgres binary is cached in {@code ~/.embedpostgresql/}.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = IdentityTestApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@AutoConfigureEmbeddedDatabase(provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY)
public abstract class AbstractIdentityIntegrationTest {

    @Autowired
    private UsuarioRepository adminResetRepo;

    @Autowired
    private Clock adminResetClock;

    /**
     * Garante que o admin esteja destravado e com contador zerado antes de
     * cada teste. Necessário porque testes de brute force deixam o admin
     * bloqueado e o banco zonky é compartilhado entre classes na mesma JVM.
     */
    @BeforeEach
    void unblockAdminBeforeEach() {
        Email adminEmail = Email.of("admin@nonnas.com");
        adminResetRepo.findByEmail(adminEmail).ifPresent((Usuario u) -> {
            if (u.travada() || u.tentativasFalhas() > 0 || u.bloqueadoAte().isPresent()) {
                u.liberar(adminResetClock.instant());
                adminResetRepo.save(u);
            }
        });
    }
}
