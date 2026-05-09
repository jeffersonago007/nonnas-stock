package com.nonnas.e2e.smoke;

import com.nonnas.e2e.AbstractE2ETest;
import com.nonnas.e2e.fixtures.Cnpjs;
import com.nonnas.e2e.fixtures.TestUsers;
import com.nonnas.e2e.pageobjects.AlertasPage;
import com.nonnas.e2e.pageobjects.CargaInicialPage;
import com.nonnas.e2e.pageobjects.DashboardPage;
import com.nonnas.e2e.pageobjects.EstoquePage;
import com.nonnas.e2e.pageobjects.FichaTecnicaPage;
import com.nonnas.e2e.pageobjects.FiliaisPage;
import com.nonnas.e2e.pageobjects.InsumosPage;
import com.nonnas.e2e.pageobjects.LoginPage;
import com.nonnas.e2e.pageobjects.MovimentacoesPage;
import com.nonnas.e2e.pageobjects.ProdutosPage;
import com.nonnas.e2e.pageobjects.TransferenciasPage;
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
    private static String filialPrincipalId;
    private static String filialPrincipalNome;
    private static String filialSecundariaId;
    private static String filialSecundariaNome;
    private static String categoriaId;
    private static String categoriaNome;
    private static String unidadeKgId;
    private static String unidadeKgCodigo;
    private static String insumoId;
    private static String insumoNome;
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
        categoriaId = api.criarCategoria(adminToken, categoriaNome);
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
        filialPrincipalId = api.idFilialPorCnpj(adminToken, Cnpjs.FILIAL_E2E_PRINCIPAL);
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
        String produtoNome = unique("Pizza Margherita E2E");
        var produtos = new ProdutosPage(page, BASE_URL).abrir();
        produtos.criar(unique("PROD"), produtoNome, "Pizzas E2E");
        FichaTecnicaPage ficha = produtos.abrirFichaDe(produtoNome);
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

    private void loginViaUI() {
        new LoginPage(page, BASE_URL).open().entrarCom(TestUsers.ADMIN_EMAIL, TestUsers.ADMIN_SENHA);
    }
}
