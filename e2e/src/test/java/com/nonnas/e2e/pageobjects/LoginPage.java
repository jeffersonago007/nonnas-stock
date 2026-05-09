package com.nonnas.e2e.pageobjects;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public class LoginPage {

    private final Page page;
    private final String baseUrl;

    public LoginPage(Page page, String baseUrl) {
        this.page = page;
        this.baseUrl = baseUrl;
    }

    public LoginPage open() {
        page.navigate(baseUrl + "/login");
        page.waitForSelector("input#email");
        return this;
    }

    public LoginPage preencherCredenciais(String email, String senha) {
        page.locator("input#email").fill(email);
        page.locator("input#senha").fill(senha);
        return this;
    }

    public void submeter() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Entrar")).click();
    }

    public LoginPage entrarCom(String email, String senha) {
        preencherCredenciais(email, senha);
        submeter();
        page.waitForURL("**/dashboard");
        return this;
    }

    public boolean exibeErroDeCredenciais() {
        return page.locator("[role=alert]").count() > 0;
    }
}
