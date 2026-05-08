package com.nonnas.identity.interfaces.rest;

import com.fasterxml.jackson.databind.JsonNode;
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
 * End-to-end: cria empresa → filial → usuário OPERADOR, todos como ADMIN,
 * depois lista filiais e usuarios. Cobre as use cases CriarFilial,
 * CriarUsuario, ListarFiliais e a lógica de validação que rejeita usuário
 * não-ADMIN sem filial.
 */
class FullSetupIT extends AbstractIdentityIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void fluxoCompleto_criaEmpresaFilialUsuarioEListaTudo() throws Exception {
        String adminToken = loginComoAdmin();

        // 1. Empresa
        MvcResult empresaResult = mvc.perform(post("/api/v1/empresas")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"razaoSocial\":\"Nonnas Setup Test\",\"cnpj\":\"27865757000102\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String empresaId = json.readTree(empresaResult.getResponse().getContentAsString())
                .get("id").asText();

        // 2. Filial dessa empresa
        String filialBody = "{\"empresaId\":\"" + empresaId + "\","
                + "\"nome\":\"Filial Centro\","
                + "\"cnpj\":\"19131243000197\","
                + "\"endereco\":\"Av. Paulista, 1000\"}";
        MvcResult filialResult = mvc.perform(post("/api/v1/filiais")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(filialBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Filial Centro"))
                .andExpect(jsonPath("$.cnpjFormatado").value("19.131.243/0001-97"))
                .andReturn();
        String filialId = json.readTree(filialResult.getResponse().getContentAsString())
                .get("id").asText();

        // 3. Usuário OPERADOR vinculado a essa filial
        String userBody = "{\"filialId\":\"" + filialId + "\","
                + "\"nome\":\"Operador Teste\","
                + "\"email\":\"operador@nonnas.com\","
                + "\"senha\":\"OperadorTeste2026!\","
                + "\"perfil\":\"OPERADOR\"}";
        mvc.perform(post("/api/v1/usuarios")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("operador@nonnas.com"))
                .andExpect(jsonPath("$.perfil").value("OPERADOR"));

        // 4. Listar filiais filtrando por empresa
        mvc.perform(get("/api/v1/filiais?empresaId=" + empresaId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Filial Centro"));

        // 5. Listar todos usuários (admin + operador)
        mvc.perform(get("/api/v1/usuarios")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email", org.hamcrest.Matchers.hasItem("operador@nonnas.com")));
    }

    @Test
    void rejeitaUsuarioNaoAdminSemFilial() throws Exception {
        String adminToken = loginComoAdmin();
        String userBody = "{\"nome\":\"Sem Filial\","
                + "\"email\":\"semfilial@nonnas.com\","
                + "\"senha\":\"SemFilial2026!\","
                + "\"perfil\":\"OPERADOR\"}";
        mvc.perform(post("/api/v1/usuarios")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejeitaSenhaFracaNaCriacaoDoUsuario() throws Exception {
        String adminToken = loginComoAdmin();
        String userBody = "{\"nome\":\"Senha Fraca\","
                + "\"email\":\"fraca@nonnas.com\","
                + "\"senha\":\"abc\","
                + "\"perfil\":\"ADMIN\"}";
        mvc.perform(post("/api/v1/usuarios")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void filialEmEmpresaInexistenteRetorna404() throws Exception {
        String adminToken = loginComoAdmin();
        String filialBody = "{\"empresaId\":\"00000000-0000-0000-0000-000000000000\","
                + "\"nome\":\"Filial Fantasma\","
                + "\"cnpj\":\"19131243000197\"}";
        mvc.perform(post("/api/v1/filiais")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(filialBody))
                .andExpect(status().isNotFound());
    }

    private String loginComoAdmin() throws Exception {
        MvcResult login = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@nonnas.com\",\"senha\":\"AdminNonnas2026!\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = json.readTree(login.getResponse().getContentAsString());
        return body.get("accessToken").asText();
    }
}
