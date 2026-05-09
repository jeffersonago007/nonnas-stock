package com.nonnas.app.observability;

import io.sentry.Hint;
import io.sentry.SentryEvent;
import io.sentry.SentryOptions;
import io.sentry.protocol.Message;
import io.sentry.protocol.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Master doc 15.1 / T17 — BeforeSendCallback do Sentry.
 *
 * Filtra dois tipos de evento:
 *   1. Erros conhecidos e ignoraveis (cliente cancelando request, etc) — drop.
 *   2. Mensagens com PII (CPF/CNPJ/JWT/senha) — passa pelo SensitiveDataMasker.
 *
 * Sem DSN configurado, o SDK e auto-desabilitado pelo Sentry-Spring; este
 * config registra o callback de qualquer forma — em prod ele entra em ação
 * automaticamente.
 */
@Configuration
public class SentryConfig {

    private static final Set<String> EXCEPTION_NOMES_IGNORADOS = Set.of(
            // Cliente fechou conexão antes da resposta — ruído puro.
            "ClientAbortException",
            "AsyncRequestTimeoutException",
            // Token expirado é fluxo normal de auth, não erro a investigar.
            "JwtException"
    );

    @Autowired
    public void configure(SentryOptions options) {
        options.setBeforeSend((event, hint) -> filtrar(event, hint));
        options.setBeforeBreadcrumb((breadcrumb, hint) -> {
            if (breadcrumb.getMessage() != null) {
                breadcrumb.setMessage(SensitiveDataMasker.mask(breadcrumb.getMessage()));
            }
            return breadcrumb;
        });
    }

    private static SentryEvent filtrar(SentryEvent event, Hint hint) {
        // Drop por tipo de exceção
        if (event.getThrowable() != null) {
            String simpleName = event.getThrowable().getClass().getSimpleName();
            if (EXCEPTION_NOMES_IGNORADOS.contains(simpleName)) {
                return null;
            }
        }

        // Mascara message
        Message msg = event.getMessage();
        if (msg != null && msg.getFormatted() != null) {
            msg.setFormatted(SensitiveDataMasker.mask(msg.getFormatted()));
        }

        // Mascara querystring + body do request
        Request req = event.getRequest();
        if (req != null) {
            if (req.getQueryString() != null) {
                req.setQueryString(SensitiveDataMasker.mask(req.getQueryString()));
            }
            if (req.getData() instanceof String body) {
                req.setData(SensitiveDataMasker.mask(body));
            }
        }

        // Mascara extras (livre Map<String,Object>)
        Map<String, Object> extras = event.getExtras();
        if (extras != null && !extras.isEmpty()) {
            Map<String, Object> sanitized = new HashMap<>();
            for (Map.Entry<String, Object> e : extras.entrySet()) {
                Object v = e.getValue();
                sanitized.put(e.getKey(),
                        v instanceof String s ? SensitiveDataMasker.mask(s) : v);
            }
            // Sentry SDK não expõe setExtras direto — copiar via setExtra é redundante,
            // mas deixa registrado o ponto onde rolar a substituição se necessário.
            sanitized.forEach(event::setExtra);
        }

        return event;
    }
}
