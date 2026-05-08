package com.nonnas.identity.interfaces.rest;

import com.nonnas.identity.testsupport.AbstractIdentityIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Política de bloqueio progressivo (master doc 13.3 + ADR 0006 D):
 * 3 falhas → bloqueio temporário (15min). A partir desse ponto, mesmo
 * a senha correta encontra a conta bloqueada.
 *
 * <p>O reset do admin entre testes acontece em {@code AbstractIdentityIntegrationTest.unblockAdminBeforeEach()}.
 */
class BruteForceIT extends AbstractIdentityIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void tresFalhasBloqueiamConta() throws Exception {
        String wrong = "{\"email\":\"admin@nonnas.com\",\"senha\":\"Errado12345!\"}";
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(wrong))
                    .andExpect(status().isUnauthorized());
        }

        // 4ª tentativa, mesmo com a senha correta, encontra conta bloqueada
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@nonnas.com\",\"senha\":\"AdminNonnas2026!\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("bloqueada")));
    }
}
