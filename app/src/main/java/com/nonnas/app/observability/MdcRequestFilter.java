package com.nonnas.app.observability;

import com.nonnas.identity.infrastructure.security.AuthenticatedPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Adiciona {@code traceId}, {@code usuarioId} e {@code filialId} ao MDC do
 * Logback em toda request. Logs estruturados (master doc 15.5) carregam
 * essas chaves automaticamente — basta ter {@code <pattern>} ou JSON
 * encoder configurado pra incluí-las.
 *
 * <p>{@code traceId} é correlation id de 16 hex chars (cabe num span Trace).
 * Cliente pode propagar via header {@code X-Request-ID} se quiser amarrar
 * com seu próprio rastreio.
 *
 * <p>Roda DEPOIS do JwtAuthenticationFilter (Ordered.LOWEST) pra que o
 * usuário já esteja autenticado quando lermos o MDC.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class MdcRequestFilter extends OncePerRequestFilter {

    public static final String MDC_TRACE_ID = "traceId";
    public static final String MDC_USUARIO_ID = "usuarioId";
    public static final String MDC_FILIAL_ID = "filialId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String traceId = headerOrFresh(request, "X-Request-ID");
        try {
            MDC.put(MDC_TRACE_ID, traceId);
            response.setHeader("X-Request-ID", traceId);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof AuthenticatedPrincipal p) {
                MDC.put(MDC_USUARIO_ID, p.usuarioId().value().toString());
                if (p.filialId() != null) {
                    MDC.put(MDC_FILIAL_ID, p.filialId().value().toString());
                }
            }
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_TRACE_ID);
            MDC.remove(MDC_USUARIO_ID);
            MDC.remove(MDC_FILIAL_ID);
        }
    }

    private static String headerOrFresh(HttpServletRequest request, String header) {
        String fromHeader = request.getHeader(header);
        if (fromHeader != null && !fromHeader.isBlank()) {
            return fromHeader;
        }
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
