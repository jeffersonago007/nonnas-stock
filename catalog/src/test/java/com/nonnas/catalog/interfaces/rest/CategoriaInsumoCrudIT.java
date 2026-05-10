package com.nonnas.catalog.interfaces.rest;

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

class CategoriaInsumoCrudIT extends AbstractCatalogIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void criarBuscarPorIdRetornaMesmaCategoria() throws Exception {
        UUID id = criarCategoria("Padaria");

        mvc.perform(get("/api/v1/categorias-insumo/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.nome").value("Padaria"))
                .andExpect(jsonPath("$.ativa").value(true));
    }

    @Test
    void buscarPorIdInexistenteRetorna404() throws Exception {
        mvc.perform(get("/api/v1/categorias-insumo/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void atualizarRenomeiaCategoria() throws Exception {
        UUID id = criarCategoria("Frios");

        mvc.perform(put("/api/v1/categorias-insumo/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Frios e Embutidos\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Frios e Embutidos"));
    }

    @Test
    void atualizarComNomeVazioRetorna400() throws Exception {
        UUID id = criarCategoria("Doces");

        mvc.perform(put("/api/v1/categorias-insumo/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void atualizarIdInexistenteRetorna404() throws Exception {
        mvc.perform(put("/api/v1/categorias-insumo/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Qualquer\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void desativarSetaAtivaFalsoEAtivarReverteSemExclusaoFisica() throws Exception {
        UUID id = criarCategoria("Bebidas Quentes");

        mvc.perform(patch("/api/v1/categorias-insumo/{id}/desativar", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativa").value(false));

        // Continua existindo (busca retorna 200, não 404).
        mvc.perform(get("/api/v1/categorias-insumo/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativa").value(false));

        mvc.perform(patch("/api/v1/categorias-insumo/{id}/ativar", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativa").value(true));
    }

    @Test
    void ativarIdempotenteEmCategoriaJaAtiva() throws Exception {
        UUID id = criarCategoria("Higiene");

        mvc.perform(patch("/api/v1/categorias-insumo/{id}/ativar", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativa").value(true));
    }

    private UUID criarCategoria(String nome) throws Exception {
        MvcResult res = mvc.perform(post("/api/v1/categorias-insumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"" + nome + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(json.readTree(res.getResponse().getContentAsString()).get("id").asText());
    }
}
