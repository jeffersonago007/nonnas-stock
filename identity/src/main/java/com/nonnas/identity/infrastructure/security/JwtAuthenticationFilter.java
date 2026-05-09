package com.nonnas.identity.infrastructure.security;

import com.nonnas.identity.application.ports.TokensRevogadosPort;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;
    private final TokensRevogadosPort blacklist;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, TokensRevogadosPort blacklist) {
        this.tokenProvider = tokenProvider;
        this.blacklist = blacklist;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            try {
                JwtTokenProvider.ParsedAccess parsed = tokenProvider.parseAccess(token);
                if (blacklist.estaRevogado(parsed.jti().toString())) {
                    // Token foi revogado em logout/troca-de-senha — não autenticamos.
                    log.debug("JWT revogado (jti={}) — request rejeitada", parsed.jti());
                    SecurityContextHolder.clearContext();
                } else {
                    AuthenticatedPrincipal principal = new AuthenticatedPrincipal(
                            parsed.usuarioId(), parsed.email(), parsed.perfil(), parsed.filialId());
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            List.of(new SimpleGrantedAuthority(parsed.perfil().authority()))
                    );
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (JwtException ex) {
                log.debug("JWT inválido: {}", ex.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
