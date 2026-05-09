package com.nonnas.app;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Smoke test do app: sobe o contexto completo (todos os bounded contexts +
 * actuator + springdoc + security) e valida wire-up básico:
 * <ul>
 *   <li>{@code /actuator/health} retorna UP</li>
 *   <li>{@code /v3/api-docs} lista as tags e os endpoints dos módulos</li>
 *   <li>Erro de validação retorna 400 no formato Problem Details (RFC 7807)</li>
 * </ul>
 *
 * O profile {@code app-it} ativa swagger/api-docs no perfil de teste; sem ele,
 * {@code application-test.yml} desabilita o springdoc.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles({"test", "app-it"})
@AutoConfigureEmbeddedDatabase(provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY)
class AppContextIT {

    @Autowired private MockMvc mvc;

    @Test
    void actuatorHealth_retornaUp() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void openApiDocs_listaTagsDosModulos() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Nonnas Stock API"))
                .andExpect(jsonPath("$.tags[?(@.name == 'Identidade')]").exists())
                .andExpect(jsonPath("$.tags[?(@.name == 'Catálogo')]").exists())
                .andExpect(jsonPath("$.tags[?(@.name == 'Estoque')]").exists())
                .andExpect(jsonPath("$.tags[?(@.name == 'Receitas')]").exists())
                .andExpect(jsonPath("$.tags[?(@.name == 'Operações')]").exists())
                .andExpect(jsonPath("$.tags[?(@.name == 'Alertas')]").exists())
                .andExpect(jsonPath("$.tags[?(@.name == 'Relatórios')]").exists());
    }

    @Test
    void erroDeValidacao_retornaProblemDetail400() throws Exception {
        // POST /api/v1/auth/login com body vazio → @Valid falha → MethodArgumentNotValidException
        mvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.errors").exists());
    }
}
