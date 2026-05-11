package com.nonnas.e2e.pageobjects;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/**
 * Page-object para o fluxo de lançamento manual de Nota Fiscal
 * (rota {@code /notas-fiscais} + {@code /notas-fiscais/lancar}).
 *
 * <p>Cobre apenas a aba <strong>Manual</strong> — upload de XML é exercitado
 * por testes unitários do nfe-importer.
 */
public class NotaFiscalPage {

    private final Page page;
    private final String baseUrl;

    public NotaFiscalPage(Page page, String baseUrl) {
        this.page = page;
        this.baseUrl = baseUrl;
    }

    public NotaFiscalPage abrirLista() {
        page.navigate(baseUrl + "/notas-fiscais");
        page.waitForSelector("h1:has-text('Notas fiscais')");
        return this;
    }

    public NotaFiscalPage abrirLancamento() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Lançar nota")).first().click();
        page.waitForSelector("h1:has-text('Lançar nota fiscal')");
        return this;
    }

    public NotaFiscalPage trocarParaAbaManual() {
        page.getByRole(AriaRole.TAB, new Page.GetByRoleOptions().setName("Manual")).click();
        // Espera o texto introdutório da aba aparecer.
        page.waitForSelector("text=Preencha os campos abaixo manualmente");
        return this;
    }

    /**
     * Preenche os campos do cabeçalho da nota. {@code dataEmissao} pode ser
     * {@code null} pra manter o valor default (hoje, preenchido pelo
     * {@code useState(new Date()...)} do componente).
     */
    public NotaFiscalPage preencherCabecalho(String filialNome, String dataEmissao,
                                              String valorTotal, String numero,
                                              String serie, String chaveNfe) {
        // ID específico do form de NF — evita colidir com o filtro global de
        // filial no header (FilialFiltro component) que também tem label "Filial".
        page.locator("#filial").click();
        page.locator("[role=option]:has-text('" + filialNome + "')").click();

        if (dataEmissao != null) {
            page.locator("input#dataEmissao").fill(dataEmissao);
        }
        page.locator("input#valorTotal").fill(valorTotal);
        page.locator("input#numero").fill(numero);
        page.locator("input#serie").fill(serie);
        if (chaveNfe != null) {
            page.locator("input#chave").fill(chaveNfe);
        }
        return this;
    }

    public NotaFiscalPage preencherFornecedor(String cnpj, String razaoSocial) {
        page.locator("input#cnpj").fill(cnpj);
        page.locator("input#razao").fill(razaoSocial);
        return this;
    }

    /**
     * Preenche o primeiro (e único) item da tabela. O componente inicia com
     * uma linha em branco — basta preencher os campos.
     *
     * @param codigoInsumo   código que será de-para'd para o catálogo (se novo, cria insumo)
     * @param descricao      nome do insumo (campo "Descrição"; pra insumo novo vira nome dele)
     * @param quantidade     qtd na unidade comercial
     * @param unidadeCodigo  código da unidade (ex: "KG")
     * @param valorUnitario  valor unitário em reais
     */
    public NotaFiscalPage preencherPrimeiroItem(String codigoInsumo, String descricao,
                                                 String quantidade, String unidadeCodigo,
                                                 String valorUnitario) {
        // A tabela tem apenas uma linha inicial — alvo direto via tbody > tr.
        var linha = page.locator("table tbody tr").first();
        // Cód insumo: primeiro <input> da linha.
        linha.locator("td").nth(0).locator("input").fill(codigoInsumo);
        // Descrição: segundo <input> da linha.
        linha.locator("td").nth(1).locator("input").fill(descricao);
        // Qtd: terceiro <input> da linha (type=number).
        linha.locator("td").nth(2).locator("input").fill(quantidade);
        // Unidade: combobox dentro da quarta célula.
        linha.locator("td").nth(3).locator("[role=combobox]").click();
        page.locator("[role=option]:has-text('" + unidadeCodigo + "')").click();
        // Valor unit: quinta célula.
        linha.locator("td").nth(4).locator("input").fill(valorUnitario);
        return this;
    }

    public void confirmarLancamento() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Lançar nota")).last().click();
        page.waitForSelector("text=Nota fiscal lançada");
        // Redireciona pra /notas-fiscais.
        page.waitForSelector("h1:has-text('Notas fiscais')");
    }

    public boolean linhaComNumeroExiste(String numero) {
        return page.locator("tr:has-text('" + numero + "')").count() > 0;
    }
}
