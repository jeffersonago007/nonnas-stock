package com.nonnas.e2e.smoke;

import com.microsoft.playwright.Page;
import com.nonnas.e2e.AbstractE2ETest;
import com.nonnas.e2e.fixtures.Cnpjs;
import com.nonnas.e2e.fixtures.TestUsers;
import com.nonnas.e2e.pageobjects.DashboardPage;
import com.nonnas.e2e.pageobjects.EstoquePage;
import com.nonnas.e2e.pageobjects.LoginPage;
import com.nonnas.e2e.pageobjects.MovimentacoesPage;
import com.nonnas.e2e.pageobjects.NotaFiscalPage;
import com.nonnas.e2e.pageobjects.TransferenciasPage;
import com.nonnas.e2e.pageobjects.VendasPosPage;
import com.nonnas.e2e.support.ApiClient;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cenário operacional ponta-a-ponta solicitado pelo Jefferson:
 * <ol>
 *   <li>NF-e com quantidade alta (100 UN) cria insumo e entra no estoque da Principal.</li>
 *   <li>Venda de 40% (40 UN) via tela Vendas (auto-promove insumo a REVENDA).</li>
 *   <li>Transferência de 40% (40 UN) Principal → Secundária com fluxo completo:
 *       solicitar → aprovar → despachar → receber.</li>
 *   <li>Saída manual de 10% (10 UN) com observação "erro operacional".</li>
 *   <li>Validação do alerta de estoque crítico (saldo final 10 UN ≤ threshold 15).</li>
 * </ol>
 *
 * <p>Cada cenário grava screenshot dedicado em
 * {@code target/e2e-screenshots/fluxo-completo/&lt;ordem&gt;-&lt;descricao&gt;.png}
 * para inspeção visual. Logs SLF4J narram cada fase em runtime.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FluxoCompletoEstoqueE2ETest extends AbstractE2ETest {

    private static final Path SCREEN_DIR = Paths.get("target/e2e-screenshots/fluxo-completo");

    private static void log(String msg) {
        System.out.println("[E2E-FLUXO] " + msg);
    }
    private static void log(String fmt, Object... args) {
        System.out.println("[E2E-FLUXO] " + String.format(fmt.replace("{}", "%s"), args));
    }

    // CNPJs válidos (DVs calculados) dedicados a este teste pra não colidir
    // com SmokeE2ETest nem com fixtures do backend.
    private static final String CNPJ_EMPRESA = "33000167000101";
    private static final String CNPJ_FILIAL_PRINCIPAL = "60746948000112";
    private static final String CNPJ_FILIAL_SECUNDARIA = "02558157000162";

    private static final String FILIAL_PRINCIPAL_NOME = "Loja Principal Fluxo E2E";
    private static final String FILIAL_SECUNDARIA_NOME = "Loja Secundária Fluxo E2E";
    private static final String FORNECEDOR_RAZAO = "Fornecedor Fluxo E2E LTDA";
    private static final String INSUMO_CODIGO_NF = "AGUA-FLUXO-E2E";
    private static final String INSUMO_NOME_NF = "AGUA MINERAL FLUXO E2E";

    private static final int THRESHOLD_CRITICO = 15;

    private static ApiClient api;
    private static String adminToken;
    private static String empresaId;
    private static String filialPrincipalId;
    private static String filialSecundariaId;
    private static String insumoId;
    private static String unidadeUnId;

    @Test
    @Order(1)
    void cenario01_lancaNfeEntradaAlta() {
        log("============================================================");
        log("Cenário 1: Lançar NF-e manual com 100 UN do insumo");
        log("============================================================");

        bootstrapApiSetup();
        loginViaUI();
        new DashboardPage(page, BASE_URL).abrir().selecionarFilialNoHeader(FILIAL_PRINCIPAL_NOME);

        log("📝 Abrindo formulário de lançamento manual");
        new NotaFiscalPage(page, BASE_URL)
                .abrirLista()
                .abrirLancamento()
                .trocarParaAbaManual()
                .preencherCabecalho(FILIAL_PRINCIPAL_NOME, null, "1000.00",
                        "FLUXO-E2E-NF-1", "1", null)
                .preencherFornecedor(Cnpjs.FORNECEDOR_E2E, FORNECEDOR_RAZAO)
                .preencherPrimeiroItem(INSUMO_CODIGO_NF, INSUMO_NOME_NF,
                        "100", "UN", "10.00");

        snapshot("01-nf-preenchida");
        log("✅ Confirmando lançamento da NF-e");
        new NotaFiscalPage(page, BASE_URL).confirmarLancamento();

        // Pós-cenário: resolve insumoId real (criado pela NF) e configura o
        // alerta de estoque crítico. Próxima venda/saída exercita o listener.
        insumoId = api.idInsumoPorCodigo(adminToken, INSUMO_CODIGO_NF);
        log("🆕 Insumo criado pela NF-e: id={}", insumoId);
        api.criarAlertaConfigEstoqueMinimoAbsoluto(adminToken, insumoId, filialPrincipalId,
                THRESHOLD_CRITICO, /* prioridade */ 3);
        log("🚨 AlertaConfig ESTOQUE_MINIMO_ABSOLUTO threshold={} configurado", THRESHOLD_CRITICO);

        // Validação visual no /estoque.
        var estoque = new EstoquePage(page, BASE_URL).abrir().filtrarPorInsumo(INSUMO_NOME_NF);
        assertThat(estoque.linhaTemSaldo(INSUMO_NOME_NF, "100")).isTrue();
        snapshot("01-estoque-apos-nf");
    }

    @Test
    @Order(2)
    void cenario02_vendaQuarentaPorCento() {
        log("============================================================");
        log("Cenário 2: Venda de 40 UN (40% de 100)");
        log("============================================================");
        loginViaUI();
        new DashboardPage(page, BASE_URL).abrir().selecionarFilialNoHeader(FILIAL_PRINCIPAL_NOME);

        var vendas = new VendasPosPage(page, BASE_URL).abrir().pesquisar(INSUMO_NOME_NF);
        log("🛒 Clicando Vender no card do insumo (auto-promoção REVENDA na 1ª venda)");
        vendas.preencherQtdEVender(INSUMO_NOME_NF, "40");

        // Como o insumo entra como órfão, o dialog é o "Cadastrar e vender" — não
        // o de ficha técnica. O VendasPosPage.confirmarVenda confirma qualquer
        // dos dois (botão "Cadastrar e vender" também leva ao toast "Venda registrada").
        snapshot("02-dialog-confirmacao-venda");
        confirmarVendaInsumoOrfao();

        log("✅ Saldo esperado após venda: 60 UN (100 - 40)");
        var estoque = new EstoquePage(page, BASE_URL).abrir().filtrarPorInsumo(INSUMO_NOME_NF);
        assertThat(estoque.linhaTemSaldo(INSUMO_NOME_NF, "60")).isTrue();
        snapshot("02-estoque-apos-venda");
    }

    @Test
    @Order(3)
    void cenario03_transferenciaQuarentaPorCentoFluxoCompleto() {
        log("============================================================");
        log("Cenário 3: Transferência 40 UN Principal → Secundária (fluxo completo)");
        log("============================================================");
        loginViaUI();
        new DashboardPage(page, BASE_URL).abrir().selecionarFilialNoHeader(FILIAL_PRINCIPAL_NOME);

        var transf = new TransferenciasPage(page, BASE_URL).abrir().abrirNovaTransferencia();
        log("📦 Preenchendo rota Principal → Secundária + 40 UN");
        transf.preencherRota(FILIAL_PRINCIPAL_NOME, FILIAL_SECUNDARIA_NOME)
              .preencherPrimeiroItem(INSUMO_NOME_NF, "UN", "40");
        snapshot("03-transferencia-solicitada-form");
        transf.confirmarSolicitacao();

        log("✓ Aprovando transferência");
        transf.abrir().aprovarPrimeira();
        snapshot("03-transferencia-aprovada");

        log("🚚 Despachando (gera saída de estoque na Principal)");
        transf.abrir().despacharPrimeira();
        snapshot("03-transferencia-despachada");

        log("📥 Recebendo na Secundária");
        transf.abrir().abrirRecebimentoDaPrimeira();
        transf.confirmarRecebimento();
        snapshot("03-transferencia-recebida");

        assertThat(transf.possuiTransferenciaComStatus("Recebida")).isTrue();

        log("✅ Saldo Principal esperado após transferência: 20 UN (60 - 40)");
        var estoque = new EstoquePage(page, BASE_URL).abrir().filtrarPorInsumo(INSUMO_NOME_NF);
        assertThat(estoque.linhaTemSaldo(INSUMO_NOME_NF, "20")).isTrue();
        snapshot("03-estoque-principal-apos-transferencia");
    }

    @Test
    @Order(4)
    void cenario04_saidaManualErroOperacional() {
        log("============================================================");
        log("Cenário 4: Saída manual 10 UN ('erro operacional')");
        log("============================================================");
        loginViaUI();
        new DashboardPage(page, BASE_URL).abrir().selecionarFilialNoHeader(FILIAL_PRINCIPAL_NOME);

        new MovimentacoesPage(page, BASE_URL)
                .abrir()
                .abrirAbaSaida()
                .preencherSaida(INSUMO_NOME_NF, "UN", "10", "erro operacional");
        snapshot("04-saida-preenchida");

        new MovimentacoesPage(page, BASE_URL).confirmarSaida();
        log("✅ Saldo esperado após saída: 10 UN (20 - 10) — abaixo do threshold de alerta {}",
                THRESHOLD_CRITICO);

        var estoque = new EstoquePage(page, BASE_URL).abrir().filtrarPorInsumo(INSUMO_NOME_NF);
        assertThat(estoque.linhaTemSaldo(INSUMO_NOME_NF, "10")).isTrue();
        snapshot("04-estoque-apos-saida");
    }

    @Test
    @Order(5)
    void cenario05_alertaEstoqueCriticoFoiDisparado() {
        log("============================================================");
        log("Cenário 5: Verificar alerta de estoque crítico (saldo 10 ≤ threshold {})",
                THRESHOLD_CRITICO);
        log("============================================================");

        int ativos = api.contarAlertasDisparadosAtivos(adminToken, insumoId, filialPrincipalId);
        log("📊 Alertas ATIVOS encontrados via API: {}", ativos);
        assertThat(ativos)
                .as("Esperado pelo menos 1 AlertaDisparado ativo após saídas que cruzaram o threshold")
                .isGreaterThanOrEqualTo(1);

        // Evidência visual da página de Alertas com o disparo aparecendo.
        loginViaUI();
        new DashboardPage(page, BASE_URL).abrir().selecionarFilialNoHeader(FILIAL_PRINCIPAL_NOME);
        page.navigate(BASE_URL + "/alertas");
        page.waitForLoadState();
        snapshot("05-tela-alertas-com-disparo");
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private void bootstrapApiSetup() {
        if (api == null) {
            api = new ApiClient(API_URL);
            adminToken = api.loginComoAdmin(TestUsers.ADMIN_EMAIL, TestUsers.ADMIN_SENHA);
            log("🔐 Login API OK");
        }
        if (empresaId == null) {
            empresaId = api.criarEmpresa(adminToken, "Nonnas Fluxo E2E", CNPJ_EMPRESA);
            log("🏢 Empresa: {}", empresaId);
        }
        if (filialPrincipalId == null) {
            try {
                filialPrincipalId = api.idFilialPorCnpj(adminToken, CNPJ_FILIAL_PRINCIPAL);
            } catch (IllegalStateException e) {
                filialPrincipalId = api.criarFilial(adminToken, empresaId,
                        FILIAL_PRINCIPAL_NOME, CNPJ_FILIAL_PRINCIPAL);
            }
            log("🏪 Filial Principal: {}", filialPrincipalId);
        }
        if (filialSecundariaId == null) {
            try {
                filialSecundariaId = api.idFilialPorCnpj(adminToken, CNPJ_FILIAL_SECUNDARIA);
            } catch (IllegalStateException e) {
                filialSecundariaId = api.criarFilial(adminToken, empresaId,
                        FILIAL_SECUNDARIA_NOME, CNPJ_FILIAL_SECUNDARIA);
            }
            log("🏪 Filial Secundária: {}", filialSecundariaId);
        }
        if (unidadeUnId == null) {
            unidadeUnId = api.idUnidadePorCodigo(adminToken, "UN");
        }
        // Garante fornecedor cadastrado (a NF reusa pelo CNPJ).
        api.criarFornecedor(adminToken, FORNECEDOR_RAZAO, Cnpjs.FORNECEDOR_E2E);
    }

    private void loginViaUI() {
        new LoginPage(page, BASE_URL).open()
                .entrarCom(TestUsers.ADMIN_EMAIL, TestUsers.ADMIN_SENHA);
    }

    /**
     * Confirma o dialog que aparece na venda de insumo órfão ("Cadastrar e
     * vender"). É o caminho de auto-promoção REVENDA do
     * {@code VenderInsumoOrfaoUseCase}.
     */
    private void confirmarVendaInsumoOrfao() {
        page.locator("button:has-text('Cadastrar e vender')").click();
        page.waitForSelector("text=Venda registrada");
    }

    /**
     * Salva screenshot full-page em {@link #SCREEN_DIR}/{order}-{descricao}.png.
     * Best-effort — não derruba o test se o I/O falhar.
     */
    private void snapshot(String descricao) {
        try {
            Path file = SCREEN_DIR.resolve(descricao + ".png");
            page.screenshot(new Page.ScreenshotOptions().setPath(file).setFullPage(true));
            log("📷 Screenshot: {}", file);
        } catch (Exception e) {
            log("Falha ao salvar screenshot {}: {}", descricao, e.getMessage());
        }
    }
}
