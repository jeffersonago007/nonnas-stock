package com.nonnas.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Base E2E. Inicia Playwright + Chromium uma vez por classe e abre um
 * BrowserContext novo a cada teste (isolamento de cookies/sessão entre cenários).
 *
 * <p>Configuração via system property:
 * <ul>
 *   <li>{@code e2e.base.url} — URL do frontend (default {@code http://localhost:5173}).</li>
 *   <li>{@code e2e.api.url} — URL da API backend (default {@code http://localhost:8080}).</li>
 *   <li>{@code e2e.headless} — {@code true|false} (default true em CI).</li>
 * </ul>
 *
 * <p>Em falha, salva screenshot em {@code target/e2e-screenshots/}.
 */
public abstract class AbstractE2ETest {

    protected static final String BASE_URL = System.getProperty("e2e.base.url", "http://localhost:5173");
    protected static final String API_URL = System.getProperty("e2e.api.url", "http://localhost:8080");
    private static final boolean HEADLESS = Boolean.parseBoolean(System.getProperty("e2e.headless", "true"));

    private static Playwright playwright;
    private static Browser browser;
    protected BrowserContext context;
    protected Page page;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(HEADLESS));
    }

    @AfterAll
    static void closeBrowser() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }

    @BeforeEach
    void newContext() {
        context = browser.newContext();
        page = context.newPage();
    }

    @AfterEach
    void closeContext(TestInfo info) {
        if (page != null && !page.isClosed()) {
            String name = info.getTestMethod().map(m -> m.getName()).orElse("unknown");
            Path screenshot = Paths.get("target/e2e-screenshots", name + ".png");
            try {
                page.screenshot(new Page.ScreenshotOptions().setPath(screenshot).setFullPage(true));
            } catch (Exception ignored) {
                // Best-effort screenshot — falhas aqui não devem mascarar a falha real do teste.
            }
        }
        if (context != null) context.close();
    }
}
