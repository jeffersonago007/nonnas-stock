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

    @Test
    void buscarPorIdRetornaEmpresa() throws Exception {
        String token = loginComoAdmin();
        UUID id = criarEmpresaAndReturnId(token, "Empresa Get", "47960950000121");

        mvc.perform(get("/api/v1/empresas/{id}", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.razaoSocial").value("Empresa Get"))
                .andExpect(jsonPath("$.cnpj").value("47960950000121"));
    }

    @Test
    void buscarPorIdInexistenteRetorna404() throws Exception {
        String token = loginComoAdmin();
        mvc.perform(get("/api/v1/empresas/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void atualizarRenomeiaPreservandoCnpj() throws Exception {
        String token = loginComoAdmin();
        UUID id = criarEmpresaAndReturnId(token, "Empresa Original", "11222333000181");

        mvc.perform(put("/api/v1/empresas/{id}", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"razaoSocial\":\"Empresa Renomeada SA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.razaoSocial").value("Empresa Renomeada SA"))
                .andExpect(jsonPath("$.cnpj").value("11222333000181"));
    }

    @Test
    void atualizarSemAuthRetorna401() throws Exception {
        mvc.perform(put("/api/v1/empresas/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"razaoSocial\":\"Foo\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void desativarEAtivarSemExclusaoFisica() throws Exception {
        String token = loginComoAdmin();
        UUID id = criarEmpresaAndReturnId(token, "Empresa Toggle", "06990590000123");

        mvc.perform(patch("/api/v1/empresas/{id}/desativar", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativa").value(false));

        mvc.perform(get("/api/v1/empresas/{id}", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativa").value(false));

        mvc.perform(patch("/api/v1/empresas/{id}/ativar", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativa").value(true));
    }

    private UUID criarEmpresaAndReturnId(String token, String razao, String cnpj) throws Exception {
        MvcResult res = mvc.perform(post("/api/v1/empresas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"razaoSocial\":\"" + razao + "\",\"cnpj\":\"" + cnpj + "\"}"))
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
