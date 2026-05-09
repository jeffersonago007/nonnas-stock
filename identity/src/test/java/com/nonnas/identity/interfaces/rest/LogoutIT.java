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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cobre o critério de aceitação do master doc T16:
 * "Token JWT após logout retorna 401 (mesmo dentro da validade)".
 */
class LogoutIT extends AbstractIdentityIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void tokenAposLogoutNaoAutenticaMaisRequests() throws Exception {
        // Login → access token válido
        MvcResult login = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@nonnas.com\",\"senha\":\"AdminNonnas2026!\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String token = json.readTree(login.getResponse().getContentAsString())
                .get("accessToken").asText();

        // Antes do logout: endpoint autenticado responde 200
        mvc.perform(get("/api/v1/lgpd/meus-dados")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Logout
        mvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNoContent());

        // Pós-logout: o MESMO token agora é rejeitado (blacklist)
        mvc.perform(get("/api/v1/lgpd/meus-dados")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
}
