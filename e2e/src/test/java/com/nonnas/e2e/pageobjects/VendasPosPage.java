package com.nonnas.e2e.pageobjects;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/**
 * Page-object para o POS de vendas (rota {@code /vendas}).
 *
 * <p>Nome {@code VendasPosPage} (vs. frontend {@code VendasPage.tsx}) é
 * intencional: evita confusão mental quando o page-object e o componente
 * convivem no mesmo refactor.
 */
public class VendasPosPage {

    private final Page page;
    private final String baseUrl;

    public VendasPosPage(Page page, String baseUrl) {
        this.page = page;
        this.baseUrl = baseUrl;
    }

    public VendasPosPage abrir() {
        page.navigate(baseUrl + "/vendas");
        page.waitForSelector("h1:has-text('Vendas')");
        return this;
    }

    /**
     * Aplica filtro de busca (Pesquisar). Passe vazio pra listar tudo.
     */
    public VendasPosPage pesquisar(String termo) {
        page.locator("input#busca-venda").fill(termo);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Pesquisar")).click();
        return this;
    }

    /**
     * Preenche a quantidade no card do produto e dispara "Vender" (abre o
     * dialog de preview).
     */
    public VendasPosPage preencherQtdEVender(String produtoNome, String quantidade) {
        // Card raiz: div que contém o <h3> com o nome do produto.
        var card = page.locator("div:has(> div > h3:has-text('" + produtoNome + "'))").first();
        // O input de qtd na unidade do produto é o único <input type=number> do card.
        card.locator("input[type=number]").fill(quantidade);
        card.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Vender")).click();
        // Aguarda o dialog abrir.
        page.waitForSelector("text=Confirmar venda");
        return this;
    }

    /**
     * Aguarda o cálculo do preview e valida que existe pelo menos uma
     * linha de insumo a debitar com a quantidade esperada.
     */
    public boolean previewMostraInsumoComQtd(String insumoNome, String qtdComUnidade) {
        // Aguarda o "Calculando baixa…" sumir.
        page.waitForSelector("text=Calculando baixa…", new Page.WaitForSelectorOptions().setState(
                com.microsoft.playwright.options.WaitForSelectorState.DETACHED));
        page.waitForSelector("text=Insumos a debitar via ficha técnica");
        return page.locator("li:has-text('" + insumoNome + "'):has-text('" + qtdComUnidade + "')").count() > 0;
    }

    /**
     * Confirma a venda e aguarda toast de sucesso. Espera que o dialog feche
     * (toast "Venda registrada" aparece via Sonner).
     */
    public void confirmarVenda() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Confirmar venda")).click();
        page.waitForSelector("text=Venda registrada");
    }
}
