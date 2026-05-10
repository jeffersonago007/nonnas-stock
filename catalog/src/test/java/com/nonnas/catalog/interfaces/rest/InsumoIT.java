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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InsumoIT extends AbstractCatalogIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void postInsumoComUnidadeBaseInvalida_retorna404ComMensagemClara() throws Exception {
        // Cria uma categoria primeiro (para isolar o erro à unidade inválida).
        UUID categoriaId = criarCategoria("Laticínios");
        UUID unidadeFantasma = UUID.randomUUID();

        String body = """
                {
                  "codigo": "INS-001",
                  "nome": "Mussarela",
                  "categoriaId": "%s",
                  "unidadeBaseId": "%s",
                  "controlaLote": true,
                  "controlaValidade": true
                }
                """.formatted(categoriaId, unidadeFantasma);

        mvc.perform(post("/api/v1/insumos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Unidade de medida")));
    }

    @Test
    void postInsumoComUnidadeBaseValida_e_categoriaValida_retorna201() throws Exception {
        UUID categoriaId = criarCategoria("Massas");
        UUID unidadeKgId = unidadeIdPorCodigo("KG");

        String body = """
                {
                  "codigo": "INS-002",
                  "nome": "Farinha de trigo",
                  "categoriaId": "%s",
                  "unidadeBaseId": "%s",
                  "controlaLote": true,
                  "controlaValidade": true
                }
                """.formatted(categoriaId, unidadeKgId);

        mvc.perform(post("/api/v1/insumos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("INS-002"))
                .andExpect(jsonPath("$.unidadeBaseId").value(unidadeKgId.toString()));
    }

    @Test
    void postInsumoComCodigoDuplicado_retorna409() throws Exception {
        UUID categoriaId = criarCategoria("Bebidas");
        UUID unidadeLId = unidadeIdPorCodigo("L");

        String body = """
                {
                  "codigo": "INS-DUP",
                  "nome": "Suco de Laranja",
                  "categoriaId": "%s",
                  "unidadeBaseId": "%s"
                }
                """.formatted(categoriaId, unidadeLId);

        mvc.perform(post("/api/v1/insumos").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/insumos").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void postInsumoSemCamposObrigatorios_retorna400() throws Exception {
        mvc.perform(post("/api/v1/insumos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void putInsumoComDiasAlertaVencimento_persisteValor() throws Exception {
        UUID categoriaId = criarCategoria("Carnes");
        UUID unidadeKg = unidadeIdPorCodigo("KG");
        UUID insumoId = criarInsumo("INS-PUT-OK", "Picanha", categoriaId, unidadeKg);

        String putBody = """
                {
                  "nome": "Picanha Premium",
                  "diasAlertaVencimento": 7
                }
                """;
        mvc.perform(put("/api/v1/insumos/" + insumoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(putBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("PICANHA PREMIUM"))
                .andExpect(jsonPath("$.diasAlertaVencimento").value(7));
    }

    @Test
    void putInsumoComDiasAlertaForaDoIntervalo_retorna400() throws Exception {
        UUID categoriaId = criarCategoria("Hortaliças");
        UUID unidadeKg = unidadeIdPorCodigo("KG");
        UUID insumoId = criarInsumo("INS-PUT-RANGE", "Tomate", categoriaId, unidadeKg);

        String putBody = """
                {
                  "nome": "Tomate Italiano",
                  "diasAlertaVencimento": 91
                }
                """;
        mvc.perform(put("/api/v1/insumos/" + insumoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(putBody))
                .andExpect(status().isBadRequest());
    }

    private UUID criarInsumo(String codigo, String nome, UUID categoriaId, UUID unidadeId) throws Exception {
        String body = """
                {
                  "codigo": "%s",
                  "nome": "%s",
                  "categoriaId": "%s",
                  "unidadeBaseId": "%s"
                }
                """.formatted(codigo, nome, categoriaId, unidadeId);
        MvcResult res = mvc.perform(post("/api/v1/insumos")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(json.readTree(res.getResponse().getContentAsString()).get("id").asText());
    }

    private UUID criarCategoria(String nome) throws Exception {
        MvcResult res = mvc.perform(post("/api/v1/categorias-insumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"" + nome + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(json.readTree(res.getResponse().getContentAsString()).get("id").asText());
    }

    /** Lookup das unidades padrão seedadas em V004. */
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
