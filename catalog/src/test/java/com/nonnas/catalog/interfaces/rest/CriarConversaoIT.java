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

class CriarConversaoIT extends AbstractCatalogIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void criarConversaoGlobalEntreUnidadesExistentes() throws Exception {
        UUID kgId = unidadeId("KG");
        UUID unId = unidadeId("UN");

        // Conversão global hipotética: 1 UN = 0.05 KG (ex.: ovo médio).
        String body = "{\"origemId\":\"" + unId + "\",\"destinoId\":\"" + kgId + "\",\"fator\":0.05}";
        mvc.perform(post("/api/v1/conversoes-unidade")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.global").value(true))
                .andExpect(jsonPath("$.insumoId").doesNotExist());
    }

    @Test
    void criarConversaoEspecificaPorInsumo() throws Exception {
        UUID categoria = criarCategoria("Laticínios");
        UUID kgId = unidadeId("KG");
        UUID cxId = unidadeId("CX");
        UUID insumo = criarInsumo("MUSS-CX", "Mussarela em caixa 5kg", categoria, kgId);

        String body = """
                {
                  "origemId": "%s",
                  "destinoId": "%s",
                  "fator": 5,
                  "insumoId": "%s"
                }
                """.formatted(cxId, kgId, insumo);

        mvc.perform(post("/api/v1/conversoes-unidade")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.global").value(false))
                .andExpect(jsonPath("$.insumoId").value(insumo.toString()));
    }

    @Test
    void criarConversaoComUnidadeInexistenteRetorna404() throws Exception {
        UUID inexistente = UUID.randomUUID();
        UUID kgId = unidadeId("KG");
        String body = "{\"origemId\":\"" + inexistente + "\",\"destinoId\":\"" + kgId + "\",\"fator\":1}";
        mvc.perform(post("/api/v1/conversoes-unidade")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void criarConversaoComInsumoInexistenteRetorna404() throws Exception {
        UUID inexistente = UUID.randomUUID();
        UUID kgId = unidadeId("KG");
        UUID gId  = unidadeId("G");
        String body = """
                {
                  "origemId": "%s",
                  "destinoId": "%s",
                  "fator": 100,
                  "insumoId": "%s"
                }
                """.formatted(kgId, gId, inexistente);
        mvc.perform(post("/api/v1/conversoes-unidade")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    private UUID unidadeId(String codigo) throws Exception {
        MvcResult res = mvc.perform(get("/api/v1/unidades-medida"))
                .andExpect(status().isOk()).andReturn();
        JsonNode arr = json.readTree(res.getResponse().getContentAsString());
        for (JsonNode node : arr) {
            if (node.get("codigo").asText().equals(codigo)) {
                return UUID.fromString(node.get("id").asText());
            }
        }
        throw new IllegalStateException(codigo + " não encontrada");
    }

    private UUID criarCategoria(String nome) throws Exception {
        MvcResult res = mvc.perform(post("/api/v1/categorias-insumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"" + nome + "\"}"))
                .andExpect(status().isCreated()).andReturn();
        return UUID.fromString(json.readTree(res.getResponse().getContentAsString()).get("id").asText());
    }

    private UUID criarInsumo(String codigo, String nome, UUID categoria, UUID unidadeBase) throws Exception {
        String body = """
                {
                  "codigo": "%s",
                  "nome": "%s",
                  "categoriaId": "%s",
                  "unidadeBaseId": "%s"
                }
                """.formatted(codigo, nome, categoria, unidadeBase);
        MvcResult res = mvc.perform(post("/api/v1/insumos")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        return UUID.fromString(json.readTree(res.getResponse().getContentAsString()).get("id").asText());
    }
}
