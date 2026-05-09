package com.nonnas.e2e.pageobjects;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public class ProdutosPage {

    private final Page page;
    private final String baseUrl;

    public ProdutosPage(Page page, String baseUrl) {
        this.page = page;
        this.baseUrl = baseUrl;
    }

    public ProdutosPage abrir() {
        page.navigate(baseUrl + "/produtos");
        page.waitForSelector("h1:has-text('Produtos')");
        return this;
    }

    public ProdutosPage criar(String codigo, String nome, String categoria) {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Novo produto")).click();
        page.waitForSelector("text=Novo produto vendável");
        page.locator("input#codigo").fill(codigo);
        page.locator("input#nome").fill(nome);
        page.locator("input#categoria").fill(categoria);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Criar")).click();
        page.waitForSelector("text=Produto criado");
        return this;
    }

    public FichaTecnicaPage abrirFichaDe(String nomeProduto) {
        page.locator("tr:has-text('" + nomeProduto + "') >> a:has-text('Ficha técnica')").click();
        page.waitForURL("**/fichas-tecnicas**");
        return new FichaTecnicaPage(page, baseUrl);
    }
}
