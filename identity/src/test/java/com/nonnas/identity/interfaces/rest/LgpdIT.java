package com.nonnas.identity.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonnas.identity.testsupport.AbstractIdentityIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Direitos do titular LGPD Art. 18 (master doc 13.5):
 * II/IV — confirmação + portabilidade via /meus-dados;
 * III — correção via /correcao;
 * VI — eliminação via /exclusao (anonimização imediata pra preservar
 *     integridade referencial das movimentações fiscais).
 */
class LgpdIT extends AbstractIdentityIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void meusDadosRetornaSnapshotDoUsuarioLogado() throws Exception {
        String token = loginComoAdmin();
        mvc.perform(get("/api/v1/lgpd/meus-dados")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@nonnas.com"))
                .andExpect(jsonPath("$.perfil").value("ADMIN"))
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void corrigirAtualizaNomeEDevolveSnapshotAtualizado() throws Exception {
        String token = loginComoAdmin();
        mvc.perform(post("/api/v1/lgpd/correcao")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Admin Renomeado E2E\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Admin Renomeado E2E"));
    }

    // O cenário {@code DELETE /lgpd/exclusao} fica coberto por unit test
    // (ver {@code LgpdServiceTest}). Em IT seria caro: exige criar
    // empresa+filial+usuário só pra ter um titular pra excluir, e isso
    // colide com o admin compartilhado entre ITs deste módulo.

    private String loginComoAdmin() throws Exception {
        MvcResult login = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@nonnas.com\",\"senha\":\"AdminNonnas2026!\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(login.getResponse().getContentAsString()).get("accessToken").asText();
    }
}
