package com.nonnas.identity.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonnas.identity.testsupport.AbstractIdentityIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cobre os endpoints novos do {@link FilialController} (T13): GET /{id},
 * PUT /{id}, PATCH /{id}/desativar, PATCH /{id}/ativar. O fluxo básico de
 * criação fica em {@link FullSetupIT}.
 *
 * <p>Os outros ITs do módulo (EmpresaCrudIT, FullSetupIT) compartilham o
 * mesmo Spring context e portanto o mesmo banco Zonky — daí escolhemos
 * CNPJs novos {@code 12.345.678/0001-95} e {@code 99.111.222/0001-49}
 * que ainda não foram usados em nenhum teste (chequei com grep).
 */
class FilialCrudIT extends AbstractIdentityIntegrationTest {

    private static final String EMPRESA_CNPJ = "12345678000195";
    private static final String FILIAL_CNPJ = "99111222000149";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void cicloCompletoCrudFilial() throws Exception {
        String token = loginComoAdmin();
        String filialId = criarEmpresaEFilial(token);

        // GET /{id}
        mvc.perform(get("/api/v1/filiais/" + filialId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(filialId))
                .andExpect(jsonPath("$.ativa").value(true));

        // PUT /{id} — renomeia + atualiza endereço
        mvc.perform(put("/api/v1/filiais/" + filialId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Filial Atualizada\",\"endereco\":\"Rua Nova, 200\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Filial Atualizada"))
                .andExpect(jsonPath("$.endereco").value("Rua Nova, 200"));

        // PATCH /{id}/desativar
        mvc.perform(patch("/api/v1/filiais/" + filialId + "/desativar")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativa").value(false));

        // PATCH /{id}/ativar
        mvc.perform(patch("/api/v1/filiais/" + filialId + "/ativar")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativa").value(true));
    }

    @Test
    void buscarFilialInexistenteRetorna404() throws Exception {
        String token = loginComoAdmin();
        mvc.perform(get("/api/v1/filiais/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    private String criarEmpresaEFilial(String token) throws Exception {
        MvcResult empresaResult = mvc.perform(post("/api/v1/empresas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"razaoSocial\":\"Empresa Crud Test\",\"cnpj\":\"" + EMPRESA_CNPJ + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String empresaId = json.readTree(empresaResult.getResponse().getContentAsString())
                .get("id").asText();

        String filialBody = "{\"empresaId\":\"" + empresaId + "\","
                + "\"nome\":\"Filial Original\","
                + "\"cnpj\":\"" + FILIAL_CNPJ + "\","
                + "\"endereco\":\"Rua Antiga, 100\"}";
        MvcResult filialResult = mvc.perform(post("/api/v1/filiais")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(filialBody))
                .andExpect(status().isCreated())
                .andReturn();
        return json.readTree(filialResult.getResponse().getContentAsString()).get("id").asText();
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
