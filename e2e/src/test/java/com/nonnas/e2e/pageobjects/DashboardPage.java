package com.nonnas.e2e.pageobjects;

import com.microsoft.playwright.Page;

public class DashboardPage {

    private final Page page;
    private final String baseUrl;

    public DashboardPage(Page page, String baseUrl) {
        this.page = page;
        this.baseUrl = baseUrl;
    }

    public DashboardPage abrir() {
        page.navigate(baseUrl + "/dashboard");
        page.waitForSelector("h1:has-text('Dashboard')");
        return this;
    }

    public boolean cardsResumoVisiveis() {
        // Valida os 4 cards padrão do master doc 9.1
        return page.locator("text=Filiais ativas").count() > 0
                && page.locator("text=Alertas ativos").count() > 0
                && page.locator("text=Transferências em trânsito").count() > 0
                && page.locator("text=Itens em ruptura").count() > 0;
    }

    public DashboardPage selecionarFilialNoHeader(String nomeFilial) {
        page.locator("header [role=combobox]").first().click();
        page.locator("[role=option]:has-text('" + nomeFilial + "')").click();
        return this;
    }
}
