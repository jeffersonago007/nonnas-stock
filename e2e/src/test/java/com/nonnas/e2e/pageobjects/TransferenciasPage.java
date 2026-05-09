package com.nonnas.e2e.pageobjects;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public class TransferenciasPage {

    private final Page page;
    private final String baseUrl;

    public TransferenciasPage(Page page, String baseUrl) {
        this.page = page;
        this.baseUrl = baseUrl;
    }

    public TransferenciasPage abrir() {
        page.navigate(baseUrl + "/transferencias");
        page.waitForSelector("h1:has-text('Transferências')");
        return this;
    }

    public TransferenciasPage abrirNovaTransferencia() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Nova transferência")).click();
        page.waitForSelector("text=Nova transferência");
        return this;
    }

    public TransferenciasPage preencherRota(String origemNome, String destinoNome) {
        page.locator("[role=combobox]:near(:text('Filial origem'))").first().click();
        page.locator("[role=option]:has-text('" + origemNome + "')").click();
        page.locator("[role=combobox]:near(:text('Filial destino'))").first().click();
        page.locator("[role=option]:has-text('" + destinoNome + "')").click();
        return this;
    }

    public TransferenciasPage preencherPrimeiroItem(String insumoNome, String unidade, String quantidade) {
        page.locator("[role=combobox]:has-text('Insumo')").first().click();
        page.locator("[role=option]:has-text('" + insumoNome + "')").click();
        page.locator("[role=combobox]:has-text('Unidade')").first().click();
        page.locator("[role=option]:has-text('" + unidade + "')").click();
        page.locator("input[placeholder=Qtde]").fill(quantidade);
        return this;
    }

    public void confirmarSolicitacao() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Solicitar transferência")).click();
        page.waitForSelector("text=Transferência solicitada");
    }

    public TransferenciasPage aprovarPrimeira() {
        page.locator("tr:has-text('Solicitada') >> button:has-text('Aprovar')").first().click();
        page.waitForSelector("text=Transferência aprovada");
        return this;
    }

    public TransferenciasPage despacharPrimeira() {
        page.locator("tr:has-text('Aprovada') >> button:has-text('Despachar')").first().click();
        page.waitForSelector("text=Envio registrado");
        return this;
    }

    public TransferenciasPage abrirRecebimentoDaPrimeira() {
        page.locator("tr:has-text('Em trânsito') >> button:has-text('Receber')").first().click();
        page.waitForSelector("text=Receber transferência");
        return this;
    }

    public void confirmarRecebimento() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Confirmar recebimento")).click();
        page.waitForSelector("text=Recebimento registrado");
    }

    public boolean possuiTransferenciaComStatus(String status) {
        return page.locator("tr:has-text('" + status + "')").count() > 0;
    }
}
