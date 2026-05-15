package com.nonnas.web.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Filtro ativo apenas no perfil {@code test}. Quando o contexto chega sem
 * autenticação (cenário típico de IT de bounded context que não carrega o
 * JwtAuthenticationFilter de identity), injeta um {@link AuthSubject} para que
 * o {@link SecurityScope} dentro dos controllers consiga operar.
 *
 * <p>Headers opcionais para customizar o principal:
 * <ul>
 *   <li>{@code X-Test-Perfil} — ADMIN (default), GERENTE, OPERADOR, CONSULTA</li>
 *   <li>{@code X-Test-FilialId} — UUID da filial; obrigatório quando perfil != ADMIN</li>
 * </ul>
 *
 * <p>Se {@code JwtAuthenticationFilter} já populou o contexto (ITs de identity),
 * este filtro fica inerte — só preenche quando vazio.
 */
@Component
@Profile("test")
public class TestPrincipalFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp,
                                    FilterChain chain) throws ServletException, IOException {
        String perfilHeader = req.getHeader("X-Test-Perfil");
        boolean temHeaderTeste = perfilHeader != null && !perfilHeader.isBlank();
        boolean contextoVazio = SecurityContextHolder.getContext().getAuthentication() == null;

        // Só interferimos quando:
        //   (a) o IT do bounded context envia X-Test-Perfil explicitamente — então
        //       limpamos qualquer principal residual (SecurityContextHolder é
        //       ThreadLocal e os ITs sem SecurityFilterChain do identity não
        //       limpam entre requests), OU
        //   (b) o contexto está vazio e nenhum header foi enviado — populamos
        //       com ADMIN para não bloquear ITs legados que não conhecem a feature.
        // ITs do identity (com JwtAuthenticationFilter ativo) caem no caso "header
        // ausente, contexto preenchido" e este filtro não toca em nada.
        if (!temHeaderTeste && !contextoVazio) {
            chain.doFilter(req, resp);
            return;
        }

        String perfil = temHeaderTeste ? perfilHeader : "ADMIN";
        UUID filialId = null;
        String filialHeader = req.getHeader("X-Test-FilialId");
        if (filialHeader != null && !filialHeader.isBlank()) {
            try {
                filialId = UUID.fromString(filialHeader);
            } catch (IllegalArgumentException ignored) {
                // header malformado é ignorado — IT pode estar exercitando outro fluxo
            }
        }
        AuthSubject subject = new TestAuthSubject(UUID.randomUUID(), perfil, filialId);
        var auth = new UsernamePasswordAuthenticationToken(subject, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + perfil)));
        SecurityContextHolder.getContext().setAuthentication(auth);
        try {
            chain.doFilter(req, resp);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private record TestAuthSubject(UUID userId, String perfilName, UUID filialIdOrNull)
            implements AuthSubject {}
}
