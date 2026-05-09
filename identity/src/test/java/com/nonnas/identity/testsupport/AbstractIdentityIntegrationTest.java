package com.nonnas.identity.testsupport;

import com.nonnas.identity.IdentityTestApplication;
import com.nonnas.identity.application.ports.UsuarioRepository;
import com.nonnas.identity.domain.Email;
import com.nonnas.identity.domain.Usuario;
import com.nonnas.sharedkernel.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Clock;

/**
 * Base for identity integration tests. Herda de
 * {@link AbstractIntegrationTest} (shared-kernel) o setup comum de Zonky +
 * profile {@code test} + MockMvc, e adiciona o {@code @SpringBootTest} com
 * {@link IdentityTestApplication} específico do módulo.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = IdentityTestApplication.class)
public abstract class AbstractIdentityIntegrationTest extends AbstractIntegrationTest {

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
