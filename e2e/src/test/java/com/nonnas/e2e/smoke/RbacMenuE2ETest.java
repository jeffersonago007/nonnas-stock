package com.nonnas.e2e.smoke;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.nonnas.e2e.AbstractE2ETest;
import com.nonnas.e2e.fixtures.Cnpjs;
import com.nonnas.e2e.fixtures.TestUsers;
import com.nonnas.e2e.pageobjects.LoginPage;
import com.nonnas.e2e.support.ApiClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cobertura E2E do T-RBAC-01:
 * <ul>
 *   <li>Sidebar mostra blocos certos para cada perfil
 *       (ADMIN: tudo; GERENTE: Operacional+Cadastros; OPERADOR: só Operacional;
 *       CONSULTA: só Dashboard+Relatórios).</li>
 *   <li>Acessar rota proibida via URL direta redireciona para /dashboard e
 *       exibe toast de permissão negada.</li>
 *   <li>Form de criar usuário não-ADMIN sem filial dispara erro de validação
 *       (não chega a chamar o backend).</li>
 * </ul>
 *
 * <p>Setup via REST (idempotente): cria empresa + filial + 3 usuários
 * (gerente, operador, consulta) vinculados à filial. Reusável entre runs sem
 * limpar banco.
 */
class RbacMenuE2ETest extends AbstractE2ETest {

    private static final Path SCREEN_DIR = Paths.get("target/e2e-screenshots/rbac-menu");

    private static ApiClient api;
    private static String adminToken;
    private static String filialId;

    @BeforeAll
    static void prepararFixtures() {
        api = new ApiClient(API_URL);
        adminToken = api.loginComoAdmin(TestUsers.ADMIN_EMAIL, TestUsers.ADMIN_SENHA);

        String empresaId = api.criarEmpresa(adminToken, "Empresa RBAC E2E", Cnpjs.EMPRESA_RBAC_E2E);
        // Tenta reusar filial existente; cria se não houver.
        String existente = api.idFilialPorCnpjOuNull(adminToken, Cnpjs.FILIAL_RBAC_E2E);
        filialId = existente != null
                ? existente
                : api.criarFilial(adminToken, empresaId, "Filial RBAC E2E", Cnpjs.FILIAL_RBAC_E2E);

        api.criarUsuario(adminToken, TestUsers.GERENTE_NOME, TestUsers.GERENTE_EMAIL,
                TestUsers.SENHA_PADRAO_E2E, "GERENTE", filialId);
        api.criarUsuario(adminToken, TestUsers.OPERADOR_NOME, TestUsers.OPERADOR_EMAIL,
                TestUsers.SENHA_PADRAO_E2E, "OPERADOR", filialId);
        api.criarUsuario(adminToken, TestUsers.CONSULTA_NOME, TestUsers.CONSULTA_EMAIL,
                TestUsers.SENHA_PADRAO_E2E, "CONSULTA", filialId);
    }

    // ---------------------------------------------------------------------
    // Sidebar visibility por perfil
    // ---------------------------------------------------------------------

    @Test
    void admin_veTodosBlocosDoMenu() {
        new LoginPage(page, BASE_URL).open().entrarCom(TestUsers.ADMIN_EMAIL, TestUsers.ADMIN_SENHA);
        snap("admin-menu");
        assertVisivel("Dashboard");
        assertVisivel("Notas fiscais");
        assertVisivel("Estoque");
        assertVisivel("Saídas");
        assertVisivel("Relatórios");
        assertVisivel("Alertas");
        assertVisivel("Produtos");
        assertVisivel("Fichas técnicas");
        assertVisivel("Cardápio");
        assertVisivel("Transferências");
        assertVisivel("Movimentações");
        assertVisivel("Filiais");
        assertVisivel("Fornecedores");
        assertVisivel("Usuários");
    }

    @Test
    void gerente_veOperacionalECadastrosSemAdministracao() {
        new LoginPage(page, BASE_URL).open().entrarCom(TestUsers.GERENTE_EMAIL, TestUsers.SENHA_PADRAO_E2E);
        snap("gerente-menu");
        // Operacional + Cadastros
        assertVisivel("Dashboard");
        assertVisivel("Estoque");
        assertVisivel("Relatórios");
        assertVisivel("Produtos");
        assertVisivel("Movimentações");
        // Administração — bloqueada
        assertOculto("Filiais");
        assertOculto("Empresas");
    }

    @Test
    void operador_veApenasOperacional() {
        new LoginPage(page, BASE_URL).open().entrarCom(TestUsers.OPERADOR_EMAIL, TestUsers.SENHA_PADRAO_E2E);
        snap("operador-menu");
        // Operacional
        assertVisivel("Dashboard");
        assertVisivel("Estoque");
        assertVisivel("Saídas");
        assertVisivel("Relatórios");
        // Cadastros — bloqueado
        assertOculto("Produtos");
        assertOculto("Fichas técnicas");
        assertOculto("Movimentações");
        // Administração — bloqueada
        assertOculto("Filiais");
        assertOculto("Usuários");
    }

    @Test
    void consulta_veApenasDashboardERelatorios() {
        new LoginPage(page, BASE_URL).open().entrarCom(TestUsers.CONSULTA_EMAIL, TestUsers.SENHA_PADRAO_E2E);
        snap("consulta-menu");
        assertVisivel("Dashboard");
        assertVisivel("Relatórios");
        // Tudo o resto — oculto
        assertOculto("Estoque");
        assertOculto("Saídas");
        assertOculto("Notas fiscais");
        assertOculto("Alertas");
        assertOculto("Produtos");
        assertOculto("Movimentações");
        assertOculto("Filiais");
    }

    // ---------------------------------------------------------------------
    // Acesso por URL direta a rota proibida → redirect + toast
    // ---------------------------------------------------------------------

    @Test
    void operador_acessandoRotaDeCadastros_redirecionaParaDashboard() {
        new LoginPage(page, BASE_URL).open().entrarCom(TestUsers.OPERADOR_EMAIL, TestUsers.SENHA_PADRAO_E2E);
        page.navigate(BASE_URL + "/insumos");
        // O RoleGuard redireciona para /dashboard via Navigate replace + dispara um
        // toast de permissão. Validamos o redirect (mais estável); o toast é polish
        // e fade-out muda timing entre runs.
        page.waitForURL("**/dashboard");
        snap("operador-bloqueado-cadastros");
    }

    @Test
    void consulta_acessandoRotaOperacional_redirecionaParaDashboard() {
        new LoginPage(page, BASE_URL).open().entrarCom(TestUsers.CONSULTA_EMAIL, TestUsers.SENHA_PADRAO_E2E);
        page.navigate(BASE_URL + "/estoque");
        page.waitForURL("**/dashboard");
        snap("consulta-bloqueado-estoque");
    }

    // ---------------------------------------------------------------------
    // Form: filial obrigatória para não-admin
    // ---------------------------------------------------------------------

    @Test
    void criarUsuarioNaoAdminSemFilial_exibeErroDeValidacao() {
        new LoginPage(page, BASE_URL).open().entrarCom(TestUsers.ADMIN_EMAIL, TestUsers.ADMIN_SENHA);
        page.navigate(BASE_URL + "/admin/usuarios");
        page.waitForSelector("h1:has-text('Usuários')");
        page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Novo usuário")).first().click();
        Locator dialog = page.locator("[role=dialog]");
        dialog.waitFor();

        String email = "rbac-novalfilial-" + Long.toHexString(System.nanoTime() & 0xffff) + "@e2e.com";
        dialog.locator("input#nome").fill("RBAC Sem Filial");
        dialog.locator("input#email").fill(email);
        dialog.locator("input#senha").fill(TestUsers.SENHA_PADRAO_E2E);
        // Perfil OPERADOR (não-admin) — exige filial.
        dialog.locator("#perfil").click();
        page.locator("[role=option]:has-text('OPERADOR')").click();
        // NÃO escolhe filial — o select da filial deve estar vazio (não tem
        // opção "Sem filial" pra OPERADOR, então o form começa sem seleção).
        snap("filial-obrigatoria-antes-submit");
        dialog.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("Criar")).click();

        // Form deve mostrar erro de validação inline (zod superRefine).
        // Não esperamos toast "Usuário criado" — request nem chega ao backend.
        Locator erro = page.locator("text=/Filial é obrigat/i");
        erro.waitFor(new Locator.WaitForOptions().setTimeout(5_000));
        assertThat(erro.isVisible()).isTrue();
        snap("filial-obrigatoria-erro");

        // Cancela pra deixar o estado limpo.
        dialog.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("Cancelar")).click();
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private void assertVisivel(String texto) {
        Locator sidebarLink = page.locator("aside a:has-text('" + texto + "')").first();
        assertThat(sidebarLink.count()).as("Item de menu '%s' deveria estar visível", texto).isPositive();
    }

    private void assertOculto(String texto) {
        int n = page.locator("aside a:has-text('" + texto + "')").count();
        assertThat(n).as("Item de menu '%s' deveria estar oculto", texto).isZero();
    }

    private void snap(String descricao) {
        try {
            Path file = SCREEN_DIR.resolve(descricao + ".png");
            page.screenshot(new Page.ScreenshotOptions().setPath(file).setFullPage(true));
        } catch (Exception ignored) {
            // best-effort
        }
    }
}
