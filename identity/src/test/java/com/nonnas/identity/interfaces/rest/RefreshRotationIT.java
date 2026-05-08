package com.nonnas.identity.interfaces.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonnas.identity.testsupport.AbstractIdentityIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ADR 0003 — refresh token rotation:
 * <ul>
 *   <li>Primeiro refresh emite novo par e invalida o anterior.</li>
 *   <li>Reuso do refresh já consumido é detectado como replay e retorna 401;
 *       toda a família é revogada.</li>
 *   <li>O segundo refresh, mesmo válido por TTL, também passa a ser inválido
 *       depois do replay.</li>
 * </ul>
 */
class RefreshRotationIT extends AbstractIdentityIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void rotacaoEmiteNovoParEReusoDoRefreshAnteriorEhRejeitado() throws Exception {
        // 1. Login inicial
        MvcResult login = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@nonnas.com\",\"senha\":\"AdminNonnas2026!\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String firstRefresh = json.readTree(login.getResponse().getContentAsString())
                .get("refreshToken").asText();

        // 2. Primeiro refresh
        MvcResult r1 = mvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + firstRefresh + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String secondRefresh = json.readTree(r1.getResponse().getContentAsString())
                .get("refreshToken").asText();
        assertThat(secondRefresh).isNotEqualTo(firstRefresh);

        // 3. Reuso do firstRefresh — replay → 401
        mvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + firstRefresh + "\"}"))
                .andExpect(status().isUnauthorized());

        // 4. Após replay detectado, o segundo refresh também foi revogado
        mvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + secondRefresh + "\"}"))
                .andExpect(status().isUnauthorized());
    }
}
