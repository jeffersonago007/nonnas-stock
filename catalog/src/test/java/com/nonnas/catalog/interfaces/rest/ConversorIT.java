package com.nonnas.catalog.interfaces.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonnas.catalog.testsupport.AbstractCatalogIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica que o seed V004 (G, KG, ML, L + conversões globais KG→G e L→ML)
 * está acessível e que o endpoint POST /conversoes-unidade/converter chama
 * o {@code ConversorUnidadeService} corretamente.
 */
class ConversorIT extends AbstractCatalogIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void converterKgParaG_usandoSeedGlobal() throws Exception {
        UUID kgId = unidadeIdPorCodigo("KG");
        UUID gId  = unidadeIdPorCodigo("G");

        String body = """
                {
                  "valor": 2.5,
                  "origemId": "%s",
                  "destinoId": "%s"
                }
                """.formatted(kgId, gId);

        mvc.perform(post("/api/v1/conversoes-unidade/converter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorDestino").value(2500.0));
    }

    @Test
    void converterMlParaL_usandoConversaoInversaDerivada() throws Exception {
        UUID mlId = unidadeIdPorCodigo("ML");
        UUID lId  = unidadeIdPorCodigo("L");

        String body = """
                {
                  "valor": 750,
                  "origemId": "%s",
                  "destinoId": "%s"
                }
                """.formatted(mlId, lId);

        mvc.perform(post("/api/v1/conversoes-unidade/converter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorDestino").value(0.75));
    }

    @Test
    void converterEntreUnidadesSemConversao_retorna422() throws Exception {
        UUID kgId = unidadeIdPorCodigo("KG");
        UUID mlId = unidadeIdPorCodigo("ML");

        String body = """
                {
                  "valor": 1,
                  "origemId": "%s",
                  "destinoId": "%s"
                }
                """.formatted(kgId, mlId);

        // BusinessRuleException com ErrorCode.BUSINESS_RULE_VIOLATED → 422
        // (não 409, que seria reservado para conflito de estado tipo CNPJ duplicado).
        mvc.perform(post("/api/v1/conversoes-unidade/converter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(
                        org.hamcrest.Matchers.containsString("Não há conversão definida")));
    }

    private UUID unidadeIdPorCodigo(String codigo) throws Exception {
        MvcResult res = mvc.perform(get("/api/v1/unidades-medida"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode arr = json.readTree(res.getResponse().getContentAsString());
        for (JsonNode node : arr) {
            if (node.get("codigo").asText().equals(codigo)) {
                return UUID.fromString(node.get("id").asText());
            }
        }
        throw new IllegalStateException("Unidade " + codigo + " não encontrada no seed");
    }
}
