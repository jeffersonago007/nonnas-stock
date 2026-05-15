package com.nonnas.e2e.pageobjects;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public class FiliaisPage {

    private final Page page;
    private final String baseUrl;

    public FiliaisPage(Page page, String baseUrl) {
        this.page = page;
        this.baseUrl = baseUrl;
    }

    public FiliaisPage abrir() {
        page.navigate(baseUrl + "/filiais");
        page.waitForSelector("h1:has-text('Filiais')");
        return this;
    }

    public FiliaisPage abrirNovaFilial() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Nova filial")).click();
        page.waitForSelector("text=Nova filial", new Page.WaitForSelectorOptions().setTimeout(5_000));
        return this;
    }

    public FiliaisPage preencherFilial(String empresaNome, String nome, String cnpj, String endereco) {
        page.locator("[role=combobox]:near(:text('Empresa'))").first().click();
        page.locator("[role=option]:has-text('" + empresaNome + "')").click();
        page.locator("input#nome").fill(nome);
        page.locator("input#cnpj").fill(cnpj);
        page.locator("textarea#endereco").fill(endereco);
        return this;
    }

    public void confirmarCriacao() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Criar")).click();
        page.waitForSelector("text=Filial criada");
    }

    public boolean linhaComNomeExiste(String nome) {
        // Após criar, react-query invalida e refaz a query — pode haver uma janela
        // entre o toast "Filial criada" e a linha aparecer na tabela. Wait generoso
        // (10s) acomoda refetch lento sem mascarar falhas reais.
        try {
            page.waitForSelector("td:has-text('" + nome + "')",
                    new Page.WaitForSelectorOptions().setTimeout(10_000));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public CargaInicialPage abrirCargaInicialDe(String nomeFilial) {
        page.locator("tr:has-text('" + nomeFilial + "') >> a:has-text('Carga inicial')").click();
        page.waitForURL("**/carga-inicial");
        return new CargaInicialPage(page);
    }
}
