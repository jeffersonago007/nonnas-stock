package com.nonnas.e2e.support;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper minimalista para setup de E2E que usa REST diretamente em vez de
 * passar pela UI. Mantém os Page Objects focados em fluxo de usuário real,
 * sem virar uma fábrica de fixture.
 *
 * <p>Parsing de JSON é feito com regex propositalmente — adicionar Jackson
 * ao classpath do e2e introduz risco de conflito de versão com Spring Boot,
 * e os campos que precisamos extrair (id, accessToken) são triviais.
 */
public class ApiClient {

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final String apiUrl;

    public ApiClient(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String loginComoAdmin(String email, String senha) {
        String body = "{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}";
        HttpResponse<String> resp = post("/api/v1/auth/login", null, body);
        return extractJsonString(resp.body(), "accessToken");
    }

    /**
     * Idempotente: se já existe empresa com o CNPJ, devolve o id existente.
     * Útil pra re-execuções locais sem precisar dropar o banco entre runs.
     */
    public String criarEmpresa(String token, String razaoSocial, String cnpj) {
        String existing = idEmpresaPorCnpj(token, cnpj);
        if (existing != null) return existing;
        String body = "{\"razaoSocial\":\"" + razaoSocial + "\",\"cnpj\":\"" + cnpj + "\"}";
        HttpResponse<String> resp = post("/api/v1/empresas", token, body);
        return extractJsonString(resp.body(), "id");
    }

    public String idEmpresaPorCnpj(String token, String cnpj) {
        HttpResponse<String> resp = get("/api/v1/empresas", token);
        Pattern p = Pattern.compile(
                "\\{\"id\":\"([^\"]+)\"[^}]*\"cnpj\":\"" + Pattern.quote(cnpj) + "\"",
                Pattern.DOTALL);
        Matcher m = p.matcher(resp.body());
        return m.find() ? m.group(1) : null;
    }

    public String criarFilial(String token, String empresaId, String nome, String cnpj) {
        String body = "{\"empresaId\":\"" + empresaId + "\",\"nome\":\"" + nome
                + "\",\"cnpj\":\"" + cnpj + "\",\"endereco\":\"E2E\"}";
        HttpResponse<String> resp = post("/api/v1/filiais", token, body);
        return extractJsonString(resp.body(), "id");
    }

    public String criarCategoria(String token, String nome) {
        String body = "{\"nome\":\"" + nome + "\"}";
        HttpResponse<String> resp = post("/api/v1/categorias-insumo", token, body);
        return extractJsonString(resp.body(), "id");
    }

    public String idUnidadePorCodigo(String token, String codigo) {
        HttpResponse<String> resp = get("/api/v1/unidades-medida", token);
        // Procura o objeto que tem "codigo":"<codigo>" e devolve o id próximo.
        Pattern p = Pattern.compile(
                "\\{\"id\":\"([^\"]+)\"[^}]*\"codigo\":\"" + Pattern.quote(codigo) + "\"",
                Pattern.DOTALL);
        Matcher m = p.matcher(resp.body());
        if (!m.find()) {
            throw new IllegalStateException("Unidade " + codigo + " não encontrada no backend");
        }
        return m.group(1);
    }

    public String idInsumoPorNome(String token, String nome) {
        HttpResponse<String> resp = get("/api/v1/insumos", token);
        // Catalog faz UPPERCASE em nomes (commit 3a12308). Comparação aqui é
        // case-insensitive pra absorver tanto o input do test quanto o nome
        // canonizado pelo backend.
        Pattern p = Pattern.compile(
                "\\{\"id\":\"([^\"]+)\"[^}]*\"nome\":\"" + Pattern.quote(nome) + "\"",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(resp.body());
        if (!m.find()) {
            throw new IllegalStateException("Insumo " + nome + " não encontrado no backend");
        }
        return m.group(1);
    }

    public String idFilialPorCnpj(String token, String cnpj) {
        HttpResponse<String> resp = get("/api/v1/filiais", token);
        Pattern p = Pattern.compile(
                "\\{\"id\":\"([^\"]+)\"[^}]*\"cnpj\":\"" + Pattern.quote(cnpj) + "\"",
                Pattern.DOTALL);
        Matcher m = p.matcher(resp.body());
        if (!m.find()) {
            throw new IllegalStateException("Filial CNPJ " + cnpj + " não encontrada");
        }
        return m.group(1);
    }

    private HttpResponse<String> post(String path, String token, String body) {
        HttpRequest.Builder req = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (token != null) req.header("Authorization", "Bearer " + token);
        return send(req.build());
    }

    private HttpResponse<String> get(String path, String token) {
        HttpRequest.Builder req = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + path))
                .GET();
        if (token != null) req.header("Authorization", "Bearer " + token);
        return send(req.build());
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
            int s = resp.statusCode();
            if (s < 200 || s >= 300) {
                throw new IllegalStateException("HTTP " + s + " em " + request.uri()
                        + " — body: " + resp.body());
            }
            return resp;
        } catch (Exception e) {
            throw new RuntimeException("Falha ao chamar " + request.uri(), e);
        }
    }

    private static String extractJsonString(String json, String field) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher m = p.matcher(json);
        if (!m.find()) {
            throw new IllegalStateException("Campo " + field + " não achado no JSON: " + json);
        }
        return m.group(1);
    }
}
