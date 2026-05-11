package com.nonnas.e2e.pageobjects;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

public class EstoquePage {

    private final Page page;
    private final String baseUrl;

    public EstoquePage(Page page, String baseUrl) {
        this.page = page;
        this.baseUrl = baseUrl;
    }

    public EstoquePage abrir() {
        page.navigate(baseUrl + "/estoque");
        page.waitForSelector("h1:has-text('Estoque')");
        return this;
    }

    public EstoquePage filtrarPorInsumo(String termo) {
        page.locator("input#filtro-busca").fill(termo);
        // A partir de 2026-05-10 o filtro só aplica ao clicar Pesquisar.
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Pesquisar")).click();
        return this;
    }

    public boolean linhaTemSaldo(String insumoNome, String saldoEsperado) {
        Locator linha = page.locator("tr:has-text('" + insumoNome + "')");
        return linha.count() > 0 && linha.first().textContent().contains(saldoEsperado);
    }

    public boolean possuiIndicadorDe(String insumoNome, String indicador) {
        return page.locator("tr:has-text('" + insumoNome + "') >> text=" + indicador).count() > 0;
    }
}
