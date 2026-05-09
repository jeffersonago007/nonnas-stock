package com.nonnas.e2e.pageobjects;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public class InsumosPage {

    private final Page page;
    private final String baseUrl;

    public InsumosPage(Page page, String baseUrl) {
        this.page = page;
        this.baseUrl = baseUrl;
    }

    public InsumosPage abrir() {
        page.navigate(baseUrl + "/insumos");
        page.waitForSelector("h1:has-text('Insumos')");
        return this;
    }

    public InsumosPage abrirNovoInsumo() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Novo insumo")).click();
        page.waitForSelector("text=Novo insumo");
        return this;
    }

    public InsumosPage preencher(String codigo, String nome, String categoria, String unidade) {
        page.locator("input#codigo").fill(codigo);
        page.locator("input#nome").fill(nome);
        page.locator("[role=combobox]:near(:text('Categoria'))").first().click();
        page.locator("[role=option]:has-text('" + categoria + "')").click();
        page.locator("[role=combobox]:near(:text('Unidade base'))").first().click();
        page.locator("[role=option]:has-text('" + unidade + "')").click();
        return this;
    }

    public void confirmarCriacao() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Criar")).click();
        page.waitForSelector("text=Insumo criado");
    }

    public boolean linhaComNomeExiste(String nome) {
        return page.locator("td:has-text('" + nome + "')").count() > 0;
    }

    public InsumosPage filtrarPorBusca(String termo) {
        page.locator("input#filtro-busca").fill(termo);
        return this;
    }
}
