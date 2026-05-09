package com.nonnas.app.observability;

import java.util.regex.Pattern;

/**
 * Mascara dados sensiveis em strings antes que vao para log/Sentry/etc.
 * Defesa em profundidade — ninguem deveria logar isso, mas dev humano
 * inevitavelmente escorrega.
 *
 * Padroes cobertos: CPF, CNPJ, JWT (eyJ...) e campos de senha em JSON.
 *
 * Uso: log.info("payload: {}", SensitiveDataMasker.mask(req.toString()));
 */
public final class SensitiveDataMasker {

    private static final Pattern CPF = Pattern.compile(
            "\\b\\d{3}[.]?\\d{3}[.]?\\d{3}[-]?\\d{2}\\b");
    private static final Pattern CNPJ = Pattern.compile(
            "\\b\\d{2}[.]?\\d{3}[.]?\\d{3}[/]?\\d{4}[-]?\\d{2}\\b");
    private static final Pattern JWT = Pattern.compile(
            "eyJ[A-Za-z0-9_-]+\\.eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+");
    private static final Pattern SENHA_JSON = Pattern.compile(
            "(\"(?:senha|password|secret|token)\"\\s*:\\s*)\"[^\"]*\"",
            Pattern.CASE_INSENSITIVE);

    private SensitiveDataMasker() {}

    public static String mask(String input) {
        if (input == null || input.isEmpty()) return input;
        String s = input;
        s = CPF.matcher(s).replaceAll("***.***.***-**");
        s = CNPJ.matcher(s).replaceAll("**.***.***/****-**");
        s = JWT.matcher(s).replaceAll("<jwt_redacted>");
        s = SENHA_JSON.matcher(s).replaceAll("$1\"***\"");
        return s;
    }
}
