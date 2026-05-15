package com.nonnas.e2e.smoke;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.nonnas.e2e.AbstractE2ETest;
import com.nonnas.e2e.fixtures.TestUsers;
import com.nonnas.e2e.pageobjects.LoginPage;
import com.nonnas.e2e.support.ApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static com.microsoft.playwright.options.AriaRole.BUTTON;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cobertura de regressão de todas as telas de Administração:
 * Filiais, Fornecedores, Categorias, Unidades, Empresas, Usuários.
 *
 * <p>Cada teste exercita o CRUD completo (criar → editar → desativar → reativar)
 * de uma entidade. Use como smoke pra confirmar que novas versões do sistema
 * não quebraram nenhum fluxo administrativo.
 *
 * <p>Screenshots vão pra {@code target/e2e-screenshots/admin-crud/}.
 *
 * <p>Os nomes/códigos usados são sufixados com nanoTime pra evitar colisão entre
 * runs sem precisar limpar banco.
 */
@Disabled("""
    Dívida T-RBAC-02: cada cenário cria filial/categoria/unidade/empresa/usuário
    com nome único (nanoTime) mas reusa o mesmo CNPJ fixo entre runs. Em ambiente
    local sem reset de banco, as edições/desativações dependem de ações sobre
    linha específica que o `tr:has-text(...)` resolve ambiguamente quando há
    muitos itens com prefixo "CRUD-*" acumulados.
    Reabilitar quando houver reset de banco entre runs ou cleanup automático no
    @AfterEach. RBAC e validações de domínio cobertos por ITs e por
    RbacMenuE2ETest.""")
class AdminCrudE2ETest extends AbstractE2ETest {

    private static final Path SCREEN_DIR = Paths.get("target/e2e-screenshots/admin-crud");

    // Empresa única reusada em filiais/usuários (idempotente via API).
    private static final String CNPJ_EMPRESA_BASE = "33000167000101";

    private static ApiClient api;
    private static String adminToken;
    private static String empresaIdBase;
    private static String empresaNomeBase = "Nonnas Admin E2E";

    private static void log(String msg) {
        System.out.println("[E2E-ADMIN] " + msg);
    }
    private static void log(String fmt, Object... args) {
        System.out.println("[E2E-ADMIN] " + String.format(fmt.replace("{}", "%s"), args));
    }

    private static String unico(String prefix) {
        return prefix + "-" + Long.toHexString(System.nanoTime() & 0xffffff);
    }

    /**
     * Gera um CNPJ válido (com dígitos verificadores corretos) usando nanoTime
     * como base para evitar colisão entre runs sem precisar limpar banco.
     */
    private static String cnpjValido() {
        long base = System.nanoTime() & 0x3FFFFFFFFFL;
        String b12 = String.format("%012d", base % 1000000000000L);
        int dv1 = computeDvCnpj(b12, new int[]{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        int dv2 = computeDvCnpj(b12 + dv1, new int[]{6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        return b12 + dv1 + dv2;
    }

    private static int computeDvCnpj(String s, int[] weights) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += Character.digit(s.charAt(i), 10) * weights[i];
        }
        int rem = sum % 11;
        return rem < 2 ? 0 : 11 - rem;
    }

    @BeforeEach
    void prepararEmpresaBase() {
        if (api == null) {
            api = new ApiClient(API_URL);
            adminToken = api.loginComoAdmin(TestUsers.ADMIN_EMAIL, TestUsers.ADMIN_SENHA);
        }
        if (empresaIdBase == null) {
            empresaIdBase = api.criarEmpresa(adminToken, empresaNomeBase, CNPJ_EMPRESA_BASE);
            log("🏢 Empresa base: {} ({})", empresaIdBase, empresaNomeBase);
        }
        new LoginPage(page, BASE_URL).open()
                .entrarCom(TestUsers.ADMIN_EMAIL, TestUsers.ADMIN_SENHA);
    }

    // ---------------------------------------------------------------------
    // Cenários
    // ---------------------------------------------------------------------

    @Test
    void crud_filiais() {
        log("============================================================");
        log("CRUD Filiais");
        log("============================================================");
        page.navigate(BASE_URL + "/filiais");
        page.waitForSelector("h1:has-text('Filiais')");

        String nome = unico("Filial CRUD");
        String nomeEditado = nome + " (editada)";
        String cnpj = cnpjValido();

        // CRIAR
        clicarBotao("Nova filial");
        Locator dialogFilial = page.locator("[role=dialog]");
        dialogFilial.waitFor();
        dialogFilial.getByLabel("Empresa").click();
        // Qualquer empresa serve — o teste foca em CRUD da filial.
        page.locator("[role=option]").first().click();
        dialogFilial.locator("input#nome").fill(nome);
        dialogFilial.locator("input#cnpj").fill(cnpj);
        dialogFilial.locator("textarea#endereco").fill("Rua E2E Admin, 1");
        snap("filiais-01-criar-form");
        clicarBotao("Criar");
        page.waitForSelector("text=Filial criada");
        log("✅ Filial criada: {}", nome);
        snap("filiais-02-criada");
        assertThat(linhaExiste(nome)).isTrue();

        // EDITAR
        clicarAcaoNaLinha(nome, "Editar");
        page.waitForSelector("text=/Editar filial/i");
        page.locator("input#nome").fill(nomeEditado);
        snap("filiais-03-editar-form");
        clicarBotao("Salvar");
        page.waitForSelector("text=Filial atualizada");
        log("✅ Filial editada: {}", nomeEditado);
        snap("filiais-04-editada");
        assertThat(linhaExiste(nomeEditado)).isTrue();

        // DESATIVAR
        clicarAcaoNaLinha(nomeEditado, "Desativar");
        page.waitForSelector("text=Filial desativada");
        log("✅ Filial desativada");
        snap("filiais-05-desativada");

        // REATIVAR
        clicarAcaoNaLinha(nomeEditado, "Ativar");
        page.waitForSelector("text=/Filial (reativada|ativada)/");
        log("✅ Filial reativada");
        snap("filiais-06-reativada");
    }

    @Test
    void crud_fornecedores() {
        log("============================================================");
        log("CRUD Fornecedores");
        log("============================================================");
        page.navigate(BASE_URL + "/fornecedores");
        page.waitForSelector("h1:has-text('Fornecedores')");

        String razao = unico("Fornecedor CRUD");
        String razaoEditada = razao + " ATUALIZADO";
        String cnpj = cnpjValido();

        // CRIAR
        clicarBotao("Novo fornecedor");
        page.waitForSelector("text=/Novo fornecedor/i");
        page.locator("input#razaoSocial").fill(razao);
        page.locator("input#cnpj").fill(cnpj);
        snap("fornecedores-01-criar-form");
        clicarBotao("Criar");
        page.waitForSelector("text=Fornecedor criado");
        log("✅ Fornecedor criado: {}", razao);
        snap("fornecedores-02-criado");

        // EDITAR
        clicarAcaoNaLinha(razao, "Editar");
        page.waitForSelector("text=/Editar fornecedor/i");
        page.locator("input#razaoSocial").fill(razaoEditada);
        snap("fornecedores-03-editar-form");
        clicarBotao("Salvar");
        page.waitForSelector("text=Fornecedor atualizado");
        log("✅ Fornecedor editado: {}", razaoEditada);
        snap("fornecedores-04-editado");

        // DESATIVAR
        clicarAcaoNaLinha(razaoEditada, "Desativar");
        page.waitForSelector("text=Fornecedor desativado");
        log("✅ Fornecedor desativado");
        snap("fornecedores-05-desativado");

        // REATIVAR
        clicarAcaoNaLinha(razaoEditada, "Ativar");
        page.waitForSelector("text=/Fornecedor (reativado|ativado)/");
        log("✅ Fornecedor reativado");
        snap("fornecedores-06-reativado");
    }

    @Test
    void crud_categorias() {
        log("============================================================");
        log("CRUD Categorias");
        log("============================================================");
        page.navigate(BASE_URL + "/admin/categorias");
        page.waitForSelector("h1:has-text('Categorias de insumo')");

        String nome = unico("Categoria CRUD");
        String nomeEditado = nome + " (editada)";

        // CRIAR
        clicarBotao("Nova categoria");
        page.waitForSelector("text=/Nova categoria/i");
        page.locator("input#nome").fill(nome);
        snap("categorias-01-criar-form");
        clicarBotao("Criar");
        page.waitForSelector("text=Categoria criada");
        log("✅ Categoria criada: {}", nome);
        snap("categorias-02-criada");

        // EDITAR
        clicarAcaoNaLinha(nome, "Editar");
        page.waitForSelector("text=/Editar categoria/i");
        page.locator("input#nome").fill(nomeEditado);
        snap("categorias-03-editar-form");
        clicarBotao("Salvar");
        page.waitForSelector("text=Categoria atualizada");
        log("✅ Categoria editada: {}", nomeEditado);
        snap("categorias-04-editada");

        // DESATIVAR
        clicarAcaoNaLinha(nomeEditado, "Desativar");
        page.waitForSelector("text=Categoria desativada");
        log("✅ Categoria desativada");
        snap("categorias-05-desativada");

        // REATIVAR
        clicarAcaoNaLinha(nomeEditado, "Ativar");
        page.waitForSelector("text=/Categoria (reativada|ativada)/");
        log("✅ Categoria reativada");
        snap("categorias-06-reativada");
    }

    @Test
    void crud_unidades() {
        log("============================================================");
        log("CRUD Unidades de medida");
        log("============================================================");
        page.navigate(BASE_URL + "/admin/unidades");
        page.waitForSelector("h1:has-text('Unidades de medida')");

        // Código de unidade — máx 20 chars no schema. Usamos sufixo curto.
        String codigo = "X" + Long.toHexString(System.nanoTime() & 0xffff).toUpperCase();
        String nome = "Unidade " + codigo;
        String nomeEditado = nome + " ed.";

        // CRIAR
        clicarBotao("Nova unidade");
        Locator dialogUni = page.locator("[role=dialog]");
        dialogUni.waitFor();
        dialogUni.locator("input#codigo").fill(codigo);
        dialogUni.locator("input#nome").fill(nome);
        // Tipo: PESO/VOLUME/UNIDADE — selecionamos UNIDADE.
        dialogUni.getByLabel("Tipo").click();
        page.locator("[role=option]:has-text('UNIDADE')").click();
        snap("unidades-01-criar-form");
        clicarBotao("Criar");
        page.waitForSelector("text=Unidade criada");
        log("✅ Unidade criada: {} ({})", codigo, nome);
        snap("unidades-02-criada");

        // EDITAR (código é imutável; só o nome muda)
        clicarAcaoNaLinha(codigo, "Editar");
        page.waitForSelector("text=/Editar unidade/i");
        page.locator("input#nome").fill(nomeEditado);
        snap("unidades-03-editar-form");
        clicarBotao("Salvar");
        page.waitForSelector("text=Unidade atualizada");
        log("✅ Unidade editada: {}", nomeEditado);
        snap("unidades-04-editada");

        // DESATIVAR
        clicarAcaoNaLinha(codigo, "Desativar");
        page.waitForSelector("text=Unidade desativada");
        log("✅ Unidade desativada");
        snap("unidades-05-desativada");

        // REATIVAR
        clicarAcaoNaLinha(codigo, "Ativar");
        page.waitForSelector("text=/Unidade (reativada|ativada)/");
        log("✅ Unidade reativada");
        snap("unidades-06-reativada");
    }

    @Test
    void crud_empresas() {
        log("============================================================");
        log("CRUD Empresas");
        log("============================================================");
        page.navigate(BASE_URL + "/admin/empresas");
        page.waitForSelector("h1:has-text('Empresas')");

        String razao = unico("Empresa CRUD");
        String razaoEditada = razao + " S.A.";
        String cnpj = cnpjValido();

        // CRIAR
        clicarBotao("Nova empresa");
        page.waitForSelector("text=/Nova empresa/i");
        page.locator("input#razaoSocial").fill(razao);
        page.locator("input#cnpj").fill(cnpj);
        snap("empresas-01-criar-form");
        clicarBotao("Criar");
        page.waitForSelector("text=Empresa criada");
        log("✅ Empresa criada: {}", razao);
        snap("empresas-02-criada");

        // EDITAR (CNPJ imutável; razão social muda)
        clicarAcaoNaLinha(razao, "Editar");
        page.waitForSelector("text=/Editar empresa/i");
        page.locator("input#razaoSocial").fill(razaoEditada);
        snap("empresas-03-editar-form");
        clicarBotao("Salvar");
        page.waitForSelector("text=Empresa atualizada");
        log("✅ Empresa editada: {}", razaoEditada);
        snap("empresas-04-editada");

        // DESATIVAR
        clicarAcaoNaLinha(razaoEditada, "Desativar");
        page.waitForSelector("text=Empresa desativada");
        log("✅ Empresa desativada");
        snap("empresas-05-desativada");

        // REATIVAR
        clicarAcaoNaLinha(razaoEditada, "Ativar");
        page.waitForSelector("text=/Empresa (reativada|ativada)/");
        log("✅ Empresa reativada");
        snap("empresas-06-reativada");
    }

    @Test
    void crud_usuarios() {
        log("============================================================");
        log("CRUD Usuários");
        log("============================================================");
        page.navigate(BASE_URL + "/admin/usuarios");
        page.waitForSelector("h1:has-text('Usuários')");

        String nome = unico("Usuário CRUD");
        String nomeEditado = nome + " ed.";
        String email = "crud" + Long.toHexString(System.nanoTime() & 0xffff) + "@e2e.com";

        // CRIAR
        clicarBotao("Novo usuário");
        Locator dialogUsu = page.locator("[role=dialog]");
        dialogUsu.waitFor();
        dialogUsu.locator("input#nome").fill(nome);
        dialogUsu.locator("input#email").fill(email);
        // Senha obedece política T02: 10+ chars, 1 letra, 1 número, 1 especial.
        dialogUsu.locator("input#senha").fill("Senha-E2E-123!");
        dialogUsu.locator("#perfil").click();
        page.locator("[role=option]:has-text('OPERADOR')").click();
        // Operador exige filial vinculada — pega a primeira filial real (pula "Sem filial").
        dialogUsu.locator("#filialId").click();
        // Primeira opção é "Sem filial (acesso global)" — pegar a 2ª (filial real).
        page.locator("[role=option]").nth(1).click();
        snap("usuarios-01-criar-form");
        dialogUsu.getByRole(BUTTON, new com.microsoft.playwright.Locator.GetByRoleOptions().setName("Criar")).click();
        // Em caso de falha do backend, snapshot do estado pós-submit pra debug.
        try {
            page.waitForSelector("text=Usuário criado",
                    new Page.WaitForSelectorOptions().setTimeout(10_000));
        } catch (RuntimeException e) {
            snap("usuarios-99-erro-criar");
            throw e;
        }
        log("✅ Usuário criado: {} ({})", nome, email);
        snap("usuarios-02-criado");

        // EDITAR (email é imutável; só nome muda)
        clicarAcaoNaLinha(email, "Editar");
        page.waitForSelector("text=/Editar usuário/i");
        page.locator("input#nome").fill(nomeEditado);
        snap("usuarios-03-editar-form");
        clicarBotao("Salvar");
        page.waitForSelector("text=Usuário atualizado");
        log("✅ Usuário editado: {}", nomeEditado);
        snap("usuarios-04-editado");

        // DESATIVAR
        clicarAcaoNaLinha(email, "Desativar");
        page.waitForSelector("text=Usuário desativado");
        log("✅ Usuário desativado");
        snap("usuarios-05-desativado");

        // REATIVAR
        clicarAcaoNaLinha(email, "Ativar");
        page.waitForSelector("text=/Usuário (reativado|ativado)/");
        log("✅ Usuário reativado");
        snap("usuarios-06-reativado");
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    /** Clica o botão por nome acessível (suporta múltiplos na tela escolhendo o primeiro visível). */
    private void clicarBotao(String nome) {
        page.getByRole(BUTTON, new Page.GetByRoleOptions().setName(nome)).first().click();
    }

    /** Encontra a linha que contém o texto e clica num dos botões de ação (Editar/Desativar/Ativar). */
    private void clicarAcaoNaLinha(String textoLinha, String acao) {
        Locator linha = page.locator("tr:has-text('" + textoLinha + "')").first();
        linha.getByRole(BUTTON, new Locator.GetByRoleOptions().setName(acao)).click();
    }

    /** Verifica se uma linha contendo o texto existe na tabela atual. */
    private boolean linhaExiste(String textoLinha) {
        // Toast "X criado" aparece via Sonner ANTES da react-query refetchar a
        // tabela. Em ambientes lentos a refetch pode demorar — timeout generoso
        // (10s) evita flakiness, e a verificação ainda é determinística porque
        // a única forma da linha aparecer é se o item realmente foi persistido.
        try {
            page.waitForSelector("tr:has-text('" + textoLinha + "')",
                    new Page.WaitForSelectorOptions().setTimeout(10_000));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void snap(String descricao) {
        try {
            Path file = SCREEN_DIR.resolve(descricao + ".png");
            page.screenshot(new Page.ScreenshotOptions().setPath(file).setFullPage(true));
            log("📷 Screenshot: {}", file);
        } catch (Exception e) {
            log("Falha ao salvar screenshot {}: {}", descricao, e.getMessage());
        }
    }
}
