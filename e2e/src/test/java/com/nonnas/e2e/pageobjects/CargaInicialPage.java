package com.nonnas.e2e.pageobjects;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class CargaInicialPage {

    private final Page page;
    private final String baseUrl;

    public CargaInicialPage(Page page, String baseUrl) {
        this.page = page;
        this.baseUrl = baseUrl;
    }

    /**
     * Cria um arquivo CSV temporário no schema esperado pelo backend
     * (CsvParser): {@code insumo_id;unidade_id;numero_lote;quantidade;valor_unitario;data_fabricacao;data_validade}.
     */
    public Path criarCsvTemporario(String insumoId, String unidadeId, String lote,
                                   String quantidade, String valorUnitario,
                                   String dataFabricacao, String dataValidade) throws IOException {
        String header = "insumo_id;unidade_id;numero_lote;quantidade;valor_unitario;data_fabricacao;data_validade";
        String linha = String.join(";", insumoId, unidadeId, lote, quantidade, valorUnitario,
                dataFabricacao, dataValidade);
        String csv = header + "\n" + linha + "\n";
        Path tmp = Files.createTempFile("carga-inicial-e2e-", ".csv");
        Files.writeString(tmp, csv, StandardCharsets.UTF_8);
        return tmp;
    }

    public CargaInicialPage uploadPlanilha(Path csv) {
        page.locator("input#planilha").setInputFiles(csv);
        return this;
    }

    public CargaInicialPage gerarPreview() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Gerar preview")).click();
        page.waitForSelector("text=linha pronta", new Page.WaitForSelectorOptions().setTimeout(10_000));
        return this;
    }

    public boolean previewMostraLinhasParaImportacao() {
        return page.locator("text=prontas para importação").count() > 0
                || page.locator("text=pronta para importação").count() > 0;
    }

    public void confirmarImportacao() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Confirmar importação")).click();
        page.waitForSelector("text=Carga inicial concluída", new Page.WaitForSelectorOptions().setTimeout(15_000));
    }
}
