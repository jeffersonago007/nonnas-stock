package com.nonnas.e2e.smoke;

import com.nonnas.e2e.AbstractE2ETest;
import com.nonnas.e2e.fixtures.Cnpjs;
import com.nonnas.e2e.fixtures.TestUsers;
import com.nonnas.e2e.pageobjects.AlertasPage;
import com.nonnas.e2e.pageobjects.DashboardPage;
import com.nonnas.e2e.pageobjects.EstoquePage;
import com.nonnas.e2e.pageobjects.FichaTecnicaPage;
import com.nonnas.e2e.pageobjects.FiliaisPage;
import com.nonnas.e2e.pageobjects.InsumosPage;
import com.nonnas.e2e.pageobjects.LoginPage;
import com.nonnas.e2e.pageobjects.MovimentacoesPage;
import com.nonnas.e2e.pageobjects.NotaFiscalPage;
import com.nonnas.e2e.pageobjects.ProdutosPage;
import com.nonnas.e2e.pageobjects.TransferenciasPage;
import com.nonnas.e2e.pageobjects.VendasPosPage;
import com.nonnas.e2e.support.ApiClient;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Suíte smoke de 8 cenários (master doc 7.3). Os testes rodam em ordem
 * pré-fixada e PILHAM estado entre si — assume-se um banco fresco no início
 * (o caso comum em CI com Postgres ephemeral).
 *
 * <p>Setup compartilhado via {@link ApiClient}: cria empresa + categoria +
 * insumo + 2 filiais via REST, deixando a UI livre para exercer os fluxos
 * que importam.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SmokeE2ETest extends AbstractE2ETest {

    private static String adminToken;
    private static String empresaId;
    private static String filialPrincipalNome;
    private static String filialSecundariaId;
    private static String filialSecundariaNome;
    private static String categoriaNome;
    private static String unidadeKgId;
    private static String unidadeKgCodigo;
    private static String insumoId;
    private static String insumoNome;
    private static String insumoNovoNomeViaNF;
    private static String produtoVendavelNome;
    private static ApiClient api;

    private static String unique(String prefix) {
        return prefix + "-" + Long.toHexString(System.nanoTime() & 0xfffff);
    }

    @Test
    @Order(1)
    void cenario01_loginAdminCarregaDashboard() {
        var login = new LoginPage(page, BASE_URL).open();
        login.entrarCom(TestUsers.ADMIN_EMAIL, TestUsers.ADMIN_SENHA);
        var dashboard = new DashboardPage(page, BASE_URL);
        assertThat(dashboard.cardsResumoVisiveis()).isTrue();

        // Bootstrap API uma vez aqui — token já está autenticado.
        api = new ApiClient(API_URL);
        adminToken = api.loginComoAdmin(TestUsers.ADMIN_EMAIL, TestUsers.ADMIN_SENHA);
        empresaId = api.criarEmpresa(adminToken, "Nonnas E2E", Cnpjs.EMPRESA_E2E);
        unidadeKgId = api.idUnidadePorCodigo(adminToken, "KG");
        unidadeKgCodigo = "KG";
        categoriaNome = unique("Categoria E2E");
        api.criarCategoria(adminToken, categoriaNome);

        // T19 — admin section disponível e tela de categorias lista o que a API
        // criou. Validação sem PageObject novo, foca em sidebar + render.
        page.navigate(BASE_URL + "/admin/categorias");
        page.waitForLoadState();
        assertThat(page.locator("h1:has-text(\"Categorias de insumo\")").isVisible()).isTrue();
        assertThat(page.locator("text=" + categoriaNome).first().isVisible()).isTrue();
    }

    @Test
    @Order(2)
    void cenario02_cadastrarFilialPelaUI() {
        loginViaUI();
        filialPrincipalNome = unique("Filial Principal");
        var filiais = new FiliaisPage(page, BASE_URL).abrir().abrirNovaFilial();
        filiais.preencherFilial("Nonnas E2E", filialPrincipalNome,
                Cnpjs.FILIAL_E2E_PRINCIPAL, "Av. Nonnas, 100");
        filiais.confirmarCriacao();
        assertThat(filiais.linhaComNomeExiste(filialPrincipalNome)).isTrue();
    }

    @Test
    @Order(3)
    void cenario03_cadastrarInsumoEFichaTecnica() {
        loginViaUI();
        insumoNome = unique("Farinha 00 E2E");
        new InsumosPage(page, BASE_URL)
                .abrir()
                .abrirNovoInsumo()
                .preencher(unique("INS"), insumoNome, categoriaNome, unidadeKgCodigo)
                .confirmarCriacao();

        // Captura UUID do insumo recém-criado para uso nos cenários seguintes.
        insumoId = api.idInsumoPorNome(adminToken, insumoNome);

        // Produto vendável + ficha técnica.
        produtoVendavelNome = unique("Pizza Margherita E2E");
        var produtos = new ProdutosPage(page, BASE_URL).abrir();
        produtos.criar(unique("PROD"), produtoVendavelNome, "Pizzas E2E");
        FichaTecnicaPage ficha = produtos.abrirFichaDe(produtoVendavelNome);
        ficha.adicionarPrimeiroItem(insumoNome, unidadeKgCodigo, "0.250");
        ficha.salvarPrimeiraVersao();
        assertThat(ficha.exibeVersaoVigenteV1()).isTrue();
    }

    @Test
    @Order(4)
    void cenario04_cargaInicialPlanilhaPreviewEConfirma() throws Exception {
        loginViaUI();
        var filiais = new FiliaisPage(page, BASE_URL).abrir();
        var carga = filiais.abrirCargaInicialDe(filialPrincipalNome);
        Path csv = carga.criarCsvTemporario(insumoId, unidadeKgId, "LOTE-E2E-001",
                "100", "12.50", "2026-04-01", "2027-04-01");
        carga.uploadPlanilha(csv).gerarPreview();
        assertThat(carga.previewMostraLinhasParaImportacao()).isTrue();
        carga.confirmarImportacao();
    }

    @Test
    @Order(5)
    void cenario05_entradaManualAtualizaSaldo() {
        loginViaUI();
        new DashboardPage(page, BASE_URL).abrir().selecionarFilialNoHeader(filialPrincipalNome);

        new MovimentacoesPage(page, BASE_URL)
                .abrir()
                .abrirAbaEntrada()
                .preencherEntrada(insumoNome, unidadeKgCodigo, "50", "50", "11.00")
                .confirmarEntrada();

        var estoque = new EstoquePage(page, BASE_URL).abrir().filtrarPorInsumo(insumoNome);
        // Carga inicial 100 + entrada 50 = 150 (esperado se tests 4 + 5 executaram)
        assertThat(estoque.linhaTemSaldo(insumoNome, "150")).isTrue();
    }

    @Test
    @Order(6)
    void cenario06_saidaManualReduzSaldo() {
        loginViaUI();
        new DashboardPage(page, BASE_URL).abrir().selecionarFilialNoHeader(filialPrincipalNome);

        new MovimentacoesPage(page, BASE_URL)
                .abrir()
                .abrirAbaSaida()
                .preencherSaida(insumoNome, unidadeKgCodigo, "30")
                .confirmarSaida();

        var estoque = new EstoquePage(page, BASE_URL).abrir().filtrarPorInsumo(insumoNome);
        // 150 - 30 = 120
        assertThat(estoque.linhaTemSaldo(insumoNome, "120")).isTrue();
    }

    @Test
    @Order(7)
    void cenario07_transferenciaCompletaEntreFiliais() {
        loginViaUI();
        // Cria 2ª filial via API pra evitar dependência adicional na UI.
        if (filialSecundariaId == null) {
            filialSecundariaNome = unique("Filial Secundária");
            filialSecundariaId = api.criarFilial(adminToken, empresaId, filialSecundariaNome,
                    Cnpjs.FILIAL_E2E_SECUNDARIA);
        }

        var transferencias = new TransferenciasPage(page, BASE_URL).abrir().abrirNovaTransferencia();
        transferencias
                .preencherRota(filialPrincipalNome, filialSecundariaNome)
                .preencherPrimeiroItem(insumoNome, unidadeKgCodigo, "10");
        transferencias.confirmarSolicitacao();

        transferencias.abrir().aprovarPrimeira();
        transferencias.abrir().despacharPrimeira();
        transferencias.abrir().abrirRecebimentoDaPrimeira();
        transferencias.confirmarRecebimento();

        assertThat(transferencias.possuiTransferenciaComStatus("Recebida")).isTrue();
    }

    @Test
    @Order(8)
    void cenario08_configurarAlertaEVisualizarRelatorio() {
        loginViaUI();
        var alertas = new AlertasPage(page, BASE_URL).abrir().abrirAbaConfiguracoes();
        alertas.criarConfigRuptura("Alta");
        assertThat(alertas.exibeConfigDoTipo("Ruptura")).isTrue();

        // Relatório de posição (oitavo cenário do master doc 7.3).
        var estoque = new EstoquePage(page, BASE_URL).abrir().filtrarPorInsumo(insumoNome);
        assertThat(estoque.linhaTemSaldo(insumoNome, "")).isTrue();
    }

    @Test
    @Order(9)
    void cenario09_lancarNotaFiscalManualCriaInsumoNovo() {
        loginViaUI();
        new DashboardPage(page, BASE_URL).abrir().selecionarFilialNoHeader(filialPrincipalNome);

        // Caminho "cria insumo novo via NF": código + descrição inéditos ⇒
        // ProcessarNotaFiscalUseCase.resolverInsumoEntidade vai cair no ramo
        // criarInsumo (categoria "A classificar", controla_lote=true). Mais
        // limpo do que reaproveitar o insumo do cenário 3 — exercita o "cria
        // insumo via NF" do ADR 0013 sem mexer no saldo de Farinha.
        insumoNovoNomeViaNF = unique("Açúcar Refinado E2E");

        new NotaFiscalPage(page, BASE_URL)
                .abrirLista()
                .abrirLancamento()
                .trocarParaAbaManual()
                // dataEmissao=null preserva o default (hoje, useState do componente).
                .preencherCabecalho(filialPrincipalNome, null, "220.00",
                        "E2E-9001", "1", null)
                .preencherFornecedor(Cnpjs.FORNECEDOR_E2E, "Fornecedor E2E NF Manual")
                .preencherPrimeiroItem("INS-NF-E2E", insumoNovoNomeViaNF,
                        "20", unidadeKgCodigo, "11.00")
                .confirmarLancamento();

        // Saldo do insumo NOVO aparece em /estoque como 20 KG; saldo do
        // insumo original (Farinha) permanece 110 KG.
        var estoque = new EstoquePage(page, BASE_URL).abrir().filtrarPorInsumo(insumoNovoNomeViaNF);
        assertThat(estoque.linhaTemSaldo(insumoNovoNomeViaNF, "20")).isTrue();
    }

    @Test
    @Order(10)
    void cenario10_vendaPosConsumeInsumoViaFichaTecnica() {
        loginViaUI();
        new DashboardPage(page, BASE_URL).abrir().selecionarFilialNoHeader(filialPrincipalNome);

        var vendas = new VendasPosPage(page, BASE_URL).abrir().pesquisar("Pizza");
        vendas.preencherQtdEVender(produtoVendavelNome, "4");

        // 4 × 0.250 KG = 1 KG de Farinha 00 a debitar.
        // fmtSaldo do dialog usa pt-BR mas sem casas pra valores inteiros ⇒ "1 KG".
        assertThat(vendas.previewMostraInsumoComQtd(insumoNome, "1")).isTrue();
        vendas.confirmarVenda();

        // Saldo cai 110 → 109 KG (insumo "Farinha 00 E2E" não foi tocado em
        // cenario09 porque lá criamos um insumo novo).
        var estoque = new EstoquePage(page, BASE_URL).abrir().filtrarPorInsumo(insumoNome);
        assertThat(estoque.linhaTemSaldo(insumoNome, "109")).isTrue();
    }

    private void loginViaUI() {
        new LoginPage(page, BASE_URL).open().entrarCom(TestUsers.ADMIN_EMAIL, TestUsers.ADMIN_SENHA);
    }
}
