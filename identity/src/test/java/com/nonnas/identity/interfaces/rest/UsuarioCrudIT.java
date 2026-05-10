package com.nonnas.identity.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonnas.identity.testsupport.AbstractIdentityIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UsuarioCrudIT extends AbstractIdentityIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void buscarPorIdRetornaUsuario() throws Exception {
        String token = loginComoAdmin();
        UUID id = criarUsuario(token, "Maria Get", "maria.get@nonnas.com");

        mvc.perform(get("/api/v1/usuarios/{id}", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Maria Get"))
                .andExpect(jsonPath("$.email").value("maria.get@nonnas.com"))
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void buscarPorIdInexistenteRetorna404() throws Exception {
        String token = loginComoAdmin();
        mvc.perform(get("/api/v1/usuarios/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void atualizarRenomeiaPreservandoEmail() throws Exception {
        String token = loginComoAdmin();
        UUID id = criarUsuario(token, "Carlos Antigo", "carlos.update@nonnas.com");

        mvc.perform(put("/api/v1/usuarios/{id}", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Carlos Renomeado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Carlos Renomeado"))
                .andExpect(jsonPath("$.email").value("carlos.update@nonnas.com"));
    }

    @Test
    void atualizarSemAuthRetorna401() throws Exception {
        mvc.perform(put("/api/v1/usuarios/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Foo\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void desativarEAtivarSemExclusaoFisica() throws Exception {
        String token = loginComoAdmin();
        UUID id = criarUsuario(token, "Rita Toggle", "rita.toggle@nonnas.com");

        mvc.perform(patch("/api/v1/usuarios/{id}/desativar", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));

        mvc.perform(get("/api/v1/usuarios/{id}", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));

        mvc.perform(patch("/api/v1/usuarios/{id}/ativar", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(true));
    }

    private UUID criarUsuario(String token, String nome, String email) throws Exception {
        // Usa perfil ADMIN porque a política de domínio exige filialId para
        // perfis não-ADMIN, e este IT exercita só o CRUD genérico (não o
        // vínculo com filial).
        String body = "{\"nome\":\"" + nome + "\",\"email\":\"" + email
                + "\",\"senha\":\"Senha@Teste1\",\"perfil\":\"ADMIN\"}";
        MvcResult res = mvc.perform(post("/api/v1/usuarios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(json.readTree(res.getResponse().getContentAsString()).get("id").asText());
    }

    private String loginComoAdmin() throws Exception {
        MvcResult login = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@nonnas.com\",\"senha\":\"AdminNonnas2026!\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(login.getResponse().getContentAsString()).get("accessToken").asText();
    }
}
