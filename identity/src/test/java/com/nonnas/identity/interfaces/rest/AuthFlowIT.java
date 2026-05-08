package com.nonnas.identity.interfaces.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonnas.identity.testsupport.AbstractIdentityIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthFlowIT extends AbstractIdentityIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void loginEmiteTokensEEndpointProtegidoAceitaAccessToken() throws Exception {
        MvcResult login = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@nonnas.com\",\"senha\":\"AdminNonnas2026!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();

        JsonNode body = json.readTree(login.getResponse().getContentAsString(StandardCharsets.UTF_8));
        String accessToken = body.get("accessToken").asText();

        // Sem auth → 401
        mvc.perform(get("/api/v1/usuarios"))
                .andExpect(status().isUnauthorized());

        // Com token válido → 200, contém o admin seedado
        mvc.perform(get("/api/v1/usuarios?page=0&size=10")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email", org.hamcrest.Matchers.hasItem("admin@nonnas.com")));
    }

    @Test
    void loginComCredenciaisInvalidasRetorna401() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@nonnas.com\",\"senha\":\"SenhaErrada123!\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginComEmailInexistenteRetorna401SemEnumeration() throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"naoexiste@nonnas.com\",\"senha\":\"QualquerCoisa1!\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        JsonNode body = json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        // mensagem genérica — não vaza informação
        assertThat(body.get("detail").asText()).isEqualTo("Credenciais inválidas");
    }
}
