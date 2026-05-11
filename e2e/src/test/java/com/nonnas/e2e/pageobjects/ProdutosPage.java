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
        // Rename UX 2026-05-10: /produtos virou "Cardápio".
        page.waitForSelector("h1:has-text('Cardápio')");
        return this;
    }

    public ProdutosPage criar(String codigo, String nome, String categoria) {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Novo item")).click();
        // Dialog title pra criação continua "Novo produto vendável" (frontend mantém).
        page.waitForSelector("text=Novo produto vendável");
        page.locator("input#codigo").fill(codigo);
        page.locator("input#nome").fill(nome);
        // O campo categoria pode aparecer como Select (quando há categorias) ou como
        // Input direto (quando lista vazia). Se Select estiver presente, abrimos
        // e escolhemos "+ Nova categoria…" pra entrar em modo nova.
        var categoriaInput = page.locator("input#categoria");
        if (categoriaInput.count() == 0) {
            // Modo Select — abre dropdown e escolhe "+ Nova categoria…".
            page.locator("[role=combobox]#categoria").click();
            page.locator("[role=option]:has-text('Nova categoria')").click();
        }
        page.locator("input#categoria").fill(categoria);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Criar")).click();
        page.waitForSelector("text=Item do cardápio criado");
        return this;
    }

    public FichaTecnicaPage abrirFichaDe(String nomeProduto) {
        page.locator("tr:has-text('" + nomeProduto + "') >> a:has-text('Ficha técnica')").click();
        page.waitForURL("**/fichas-tecnicas**");
        return new FichaTecnicaPage(page);
    }
}
