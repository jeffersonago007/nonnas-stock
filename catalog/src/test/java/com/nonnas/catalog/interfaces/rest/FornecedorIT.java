package com.nonnas.catalog.interfaces.rest;

import com.nonnas.catalog.testsupport.AbstractCatalogIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FornecedorIT extends AbstractCatalogIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void criarFornecedorEListar() throws Exception {
        mvc.perform(post("/api/v1/fornecedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"razaoSocial\":\"Atacado SP Ltda\",\"cnpj\":\"11444777000161\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cnpjFormatado").value("11.444.777/0001-61"));

        mvc.perform(get("/api/v1/fornecedores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cnpj").value("11444777000161"));
    }

    @Test
    void criarFornecedorComCnpjDuplicado_retorna409() throws Exception {
        String body = "{\"razaoSocial\":\"Atacado X\",\"cnpj\":\"19131243000197\"}";
        mvc.perform(post("/api/v1/fornecedores").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/fornecedores").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void criarFornecedorComCnpjInvalido_retorna400() throws Exception {
        mvc.perform(post("/api/v1/fornecedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"razaoSocial\":\"X\",\"cnpj\":\"11111111111111\"}"))
                .andExpect(status().isBadRequest());
    }
}
