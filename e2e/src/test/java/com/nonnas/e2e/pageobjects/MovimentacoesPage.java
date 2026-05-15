package com.nonnas.e2e.pageobjects;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public class MovimentacoesPage {

    private final Page page;
    private final String baseUrl;

    public MovimentacoesPage(Page page, String baseUrl) {
        this.page = page;
        this.baseUrl = baseUrl;
    }

    public MovimentacoesPage abrir() {
        page.navigate(baseUrl + "/movimentacoes");
        page.waitForSelector("h1:has-text('Movimentações')");
        return this;
    }

    public MovimentacoesPage abrirAbaEntrada() {
        page.getByRole(AriaRole.TAB, new Page.GetByRoleOptions().setName("Entrada")).click();
        page.waitForSelector("text=Lançar entrada manual");
        return this;
    }

    public MovimentacoesPage abrirAbaSaida() {
        page.getByRole(AriaRole.TAB, new Page.GetByRoleOptions().setName("Saída")).click();
        page.waitForSelector("text=Lançar saída manual");
        return this;
    }

    public MovimentacoesPage preencherEntrada(String insumo, String unidade, String qtdLancada,
                                               String qtdBase, String valorUnitario) {
        page.locator("[role=combobox]:near(:text('Insumo'))").first().click();
        page.locator("[role=option]:has-text('" + insumo + "')").first().click();
        page.locator("[role=combobox]:near(:text('Unidade lançada'))").first().click();
        page.locator("[role=option]:has-text('" + unidade + "')").first().click();
        page.locator("text=Quantidade lançada").locator("xpath=../input").fill(qtdLancada);
        page.locator("text=Quantidade base").locator("xpath=../input").fill(qtdBase);
        page.locator("text=Valor unitário").locator("xpath=../input").fill(valorUnitario);
        // Backend exige numero de lote + validade pra insumos com controla_lote=true
        // (caso padrão de Farinha cadastrada nos cenários anteriores).
        page.locator("input#numeroLote").fill("LOTE-E2E");
        page.locator("input#dataValidade").fill("2027-12-31");
        return this;
    }

    public void confirmarEntrada() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Registrar entrada")).click();
        page.waitForSelector("text=Entrada registrada");
    }

    public MovimentacoesPage preencherSaida(String insumo, String unidade, String qtdBase) {
        return preencherSaida(insumo, unidade, qtdBase, null);
    }

    /**
     * Variante que também preenche a observação (motivo livre). Útil pra registros
     * de "erro operacional", "perda", etc. que precisam de rastro no histórico.
     */
    public MovimentacoesPage preencherSaida(String insumo, String unidade, String qtdBase,
                                            String observacao) {
        page.locator("[role=combobox]:near(:text('Insumo'))").first().click();
        page.locator("[role=option]:has-text('" + insumo + "')").first().click();
        page.locator("[role=combobox]:near(:text('Unidade'))").first().click();
        page.locator("[role=option]:has-text('" + unidade + "')").first().click();
        page.locator("text=Quantidade base").locator("xpath=../input").fill(qtdBase);
        if (observacao != null && !observacao.isBlank()) {
            // Campo textarea com label "Observação" — preenche se o componente expor.
            var obsField = page.locator("textarea[id*=observacao], textarea[name*=observacao]");
            if (obsField.count() > 0) {
                obsField.first().fill(observacao);
            }
        }
        return this;
    }

    public void confirmarSaida() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Registrar saída")).click();
        page.waitForSelector("text=Saída registrada");
    }
}
