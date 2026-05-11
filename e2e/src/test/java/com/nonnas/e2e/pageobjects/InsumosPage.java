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
        // Página /insumos hoje exibe título "Produtos" (rename de UX 2026-05-10).
        page.waitForSelector("h1:has-text('Produtos')");
        return this;
    }

    public InsumosPage abrirNovoInsumo() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Novo produto")).click();
        // Aguarda o dialog modal estar visível (evita matching com o botão na lista).
        page.waitForSelector("[role=dialog]");
        return this;
    }

    public InsumosPage preencher(String codigo, String nome, String categoria, String unidade) {
        page.locator("input#codigo").fill(codigo);
        page.locator("input#nome").fill(nome);
        // IDs específicos do dialog evitam colidir com Selects de mesmo label
        // que existem no filtro da página.
        page.locator("#categoriaId").click();
        page.locator("[role=option]:has-text('" + categoria + "')").click();
        page.locator("#unidadeBaseId").click();
        page.locator("[role=option]:has-text('" + unidade + "')").click();
        return this;
    }

    public void confirmarCriacao() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Criar")).click();
        page.waitForSelector("text=Produto criado");
    }

    public boolean linhaComNomeExiste(String nome) {
        return page.locator("td:has-text('" + nome + "')").count() > 0;
    }

    public InsumosPage filtrarPorBusca(String termo) {
        page.locator("input#filtro-busca").fill(termo);
        // A partir de 2026-05-10 os filtros só aplicam após clicar Pesquisar.
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Pesquisar")).click();
        return this;
    }
}
