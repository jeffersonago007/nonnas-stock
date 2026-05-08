package com.nonnas.identity.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonnas.identity.testsupport.AbstractIdentityIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EmpresaCrudIT extends AbstractIdentityIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void criarEmpresaComoAdminECnpjValido() throws Exception {
        String token = loginComoAdmin();

        mvc.perform(post("/api/v1/empresas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"razaoSocial\":\"Nonnas Paola Pizzaria SA\",\"cnpj\":\"11444777000161\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.razaoSocial").value("Nonnas Paola Pizzaria SA"))
                .andExpect(jsonPath("$.cnpj").value("11444777000161"))
                .andExpect(jsonPath("$.cnpjFormatado").value("11.444.777/0001-61"))
                .andExpect(jsonPath("$.ativa").value(true));
    }

    @Test
    void criarEmpresaComCnpjInvalidoRetorna400() throws Exception {
        String token = loginComoAdmin();
        mvc.perform(post("/api/v1/empresas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"razaoSocial\":\"Foo\",\"cnpj\":\"11111111111111\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void criarEmpresaSemAuthRetorna401() throws Exception {
        mvc.perform(post("/api/v1/empresas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"razaoSocial\":\"Foo\",\"cnpj\":\"11444777000161\"}"))
                .andExpect(status().isUnauthorized());
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
