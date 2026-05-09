package com.nonnas.e2e.pageobjects;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public class AlertasPage {

    private final Page page;
    private final String baseUrl;

    public AlertasPage(Page page, String baseUrl) {
        this.page = page;
        this.baseUrl = baseUrl;
    }

    public AlertasPage abrir() {
        page.navigate(baseUrl + "/alertas");
        page.waitForSelector("h1:has-text('Alertas')");
        return this;
    }

    public AlertasPage abrirAbaConfiguracoes() {
        page.getByRole(AriaRole.TAB, new Page.GetByRoleOptions().setName("Configurações")).click();
        return this;
    }

    public AlertasPage abrirAbaDisparados() {
        page.getByRole(AriaRole.TAB, new Page.GetByRoleOptions().setName("Disparados")).click();
        return this;
    }

    public AlertasPage criarConfigRuptura(String prioridadeNome) {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Nova configuração")).click();
        page.waitForSelector("text=Nova configuração de alerta");
        // Tipo Ruptura (não exige threshold)
        page.locator("#tipo").click();
        page.locator("[role=option]:has-text('Ruptura')").click();
        page.locator("#prioridade").click();
        page.locator("[role=option]:has-text('" + prioridadeNome + "')").click();
        // Mantém escopo de rede (default __rede__) — válido para ruptura.
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Criar")).click();
        page.waitForSelector("text=Configuração criada");
        return this;
    }

    public boolean exibeConfigDoTipo(String tipoLabel) {
        return page.locator("td:has-text('" + tipoLabel + "')").count() > 0;
    }

    public boolean disparadoAtivoVisivel() {
        return page.locator("tr:has-text('Ativo')").count() > 0;
    }
}
