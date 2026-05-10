package com.nonnas.e2e.pageobjects;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public class FichaTecnicaPage {

    private final Page page;

    public FichaTecnicaPage(Page page) {
        this.page = page;
    }

    public FichaTecnicaPage adicionarPrimeiroItem(String insumoNome, String unidade, String quantidade) {
        page.locator("[role=combobox]:near(:text('Insumo'))").first().click();
        page.locator("[role=option]:has-text('" + insumoNome + "')").click();
        page.locator("[role=combobox]:near(:text('Unidade'))").first().click();
        page.locator("[role=option]:has-text('" + unidade + "')").click();
        page.locator("input[type=number]").first().fill(quantidade);
        return this;
    }

    public void salvarPrimeiraVersao() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Criar ficha")).click();
        page.waitForSelector("text=Ficha técnica criada");
    }

    public boolean exibeVersaoVigenteV1() {
        return page.locator("text=Versão vigente — v1").count() > 0;
    }
}
