package com.nonnas.recipes.interfaces.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonnas.recipes.testsupport.AbstractRecipesIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RecipesRestIT extends AbstractRecipesIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;

    @Test
    void fluxoCompleto_criarProdutoFichaEAtualizarVersao() throws Exception {
        // 1. Cria produto
        var produtoBody = """
                {"codigo":"REST-PIZ-MARG","nome":"Pizza Margherita","categoria":"Pizza"}
                """;
        var produtoRes = mvc.perform(post("/api/v1/produtos-vendaveis")
                        .contentType(MediaType.APPLICATION_JSON).content(produtoBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("REST-PIZ-MARG"))
                .andExpect(jsonPath("$.ativo").value(true))
                .andReturn();
        UUID produtoId = UUID.fromString(json.readTree(
                produtoRes.getResponse().getContentAsString(StandardCharsets.UTF_8)).get("id").asText());

        // 2. Cria ficha v1
        UUID insumoA = UUID.randomUUID();
        UUID insumoB = UUID.randomUUID();
        UUID unidade = UUID.randomUUID();
        String fichaBody = """
                {"itens":[
                    {"insumoId":"%s","unidadeId":"%s","quantidade":0.2},
                    {"insumoId":"%s","unidadeId":"%s","quantidade":0.1}
                ]}
                """.formatted(insumoA, unidade, insumoB, unidade);

        mvc.perform(post("/api/v1/produtos-vendaveis/{id}/fichas", produtoId)
                        .contentType(MediaType.APPLICATION_JSON).content(fichaBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versao").value(1))
                .andExpect(jsonPath("$.ativa").value(true))
                .andExpect(jsonPath("$.itens.length()").value(2));

        // 3. Atualizar gera v2
        String fichaV2 = """
                {"itens":[
                    {"insumoId":"%s","unidadeId":"%s","quantidade":0.25}
                ]}
                """.formatted(insumoA, unidade);

        mvc.perform(put("/api/v1/produtos-vendaveis/{id}/fichas/vigente", produtoId)
                        .contentType(MediaType.APPLICATION_JSON).content(fichaV2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versao").value(2))
                .andExpect(jsonPath("$.ativa").value(true))
                .andExpect(jsonPath("$.itens.length()").value(1));

        // 4. GET vigente devolve a v2
        mvc.perform(get("/api/v1/produtos-vendaveis/{id}/fichas/vigente", produtoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versao").value(2));
    }

    @Test
    void criarProduto_codigoDuplicado_retorna409() throws Exception {
        String body = """
                {"codigo":"DUP-001","nome":"Produto Duplicado","categoria":"Generico"}
                """;
        mvc.perform(post("/api/v1/produtos-vendaveis")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        var res = mvc.perform(post("/api/v1/produtos-vendaveis")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andReturn();
        assertThat(res.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .contains("Já existe");
    }

    @Test
    void criarProduto_camposEmBranco_retorna400() throws Exception {
        mvc.perform(post("/api/v1/produtos-vendaveis")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void criarFicha_produtoInexistente_retorna404() throws Exception {
        UUID inexistente = UUID.randomUUID();
        UUID insumo = UUID.randomUUID();
        UUID unidade = UUID.randomUUID();
        String body = """
                {"itens":[{"insumoId":"%s","unidadeId":"%s","quantidade":1}]}
                """.formatted(insumo, unidade);

        mvc.perform(post("/api/v1/produtos-vendaveis/{id}/fichas", inexistente)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void buscarFichaVigente_semFicha_retorna404() throws Exception {
        String produtoBody = """
                {"codigo":"NEW-FICHA","nome":"Novo","categoria":"Generico"}
                """;
        var res = mvc.perform(post("/api/v1/produtos-vendaveis")
                        .contentType(MediaType.APPLICATION_JSON).content(produtoBody))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode tree = json.readTree(res.getResponse().getContentAsString(StandardCharsets.UTF_8));
        UUID id = UUID.fromString(tree.get("id").asText());

        mvc.perform(get("/api/v1/produtos-vendaveis/{id}/fichas/vigente", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void buscarProduto_inexistente_retorna404() throws Exception {
        mvc.perform(get("/api/v1/produtos-vendaveis/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
