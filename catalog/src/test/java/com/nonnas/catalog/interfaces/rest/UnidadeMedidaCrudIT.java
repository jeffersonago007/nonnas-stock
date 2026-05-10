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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UnidadeMedidaCrudIT extends AbstractCatalogIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void buscarPorIdRetornaUnidade() throws Exception {
        UUID id = unidadeIdSeed("KG");

        mvc.perform(get("/api/v1/unidades-medida/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("KG"))
                .andExpect(jsonPath("$.tipo").value("PESO"));
    }

    @Test
    void buscarPorIdInexistenteRetorna404() throws Exception {
        mvc.perform(get("/api/v1/unidades-medida/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void atualizarRenomeiaUnidadeMantendoCodigo() throws Exception {
        UUID id = unidadeIdSeed("KG");

        mvc.perform(put("/api/v1/unidades-medida/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Quilograma (massa)\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("KG"))
                .andExpect(jsonPath("$.nome").value("Quilograma (massa)"));
    }

    @Test
    void atualizarComNomeVazioRetorna400() throws Exception {
        UUID id = unidadeIdSeed("KG");

        mvc.perform(put("/api/v1/unidades-medida/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void atualizarIdInexistenteRetorna404() throws Exception {
        mvc.perform(put("/api/v1/unidades-medida/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Qualquer\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void desativarSetaAtivaFalsoEAtivarReverteSemExclusaoFisica() throws Exception {
        UUID id = criarUnidade("CX99", "Caixa Teste 99", "UNIDADE");

        mvc.perform(patch("/api/v1/unidades-medida/{id}/desativar", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativa").value(false));

        mvc.perform(get("/api/v1/unidades-medida/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativa").value(false));

        mvc.perform(patch("/api/v1/unidades-medida/{id}/ativar", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativa").value(true));
    }

    private UUID unidadeIdSeed(String codigo) throws Exception {
        MvcResult res = mvc.perform(get("/api/v1/unidades-medida"))
                .andExpect(status().isOk()).andReturn();
        JsonNode arr = json.readTree(res.getResponse().getContentAsString());
        for (JsonNode node : arr) {
            if (node.get("codigo").asText().equals(codigo)) {
                return UUID.fromString(node.get("id").asText());
            }
        }
        throw new IllegalStateException(codigo + " não encontrada no seed");
    }

    private UUID criarUnidade(String codigo, String nome, String tipo) throws Exception {
        String body = "{\"codigo\":\"" + codigo + "\",\"nome\":\"" + nome + "\",\"tipo\":\"" + tipo + "\"}";
        MvcResult res = mvc.perform(post("/api/v1/unidades-medida")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        return UUID.fromString(json.readTree(res.getResponse().getContentAsString()).get("id").asText());
    }
}
