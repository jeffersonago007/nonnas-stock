package com.nonnas.identity.infrastructure.security;

import com.nonnas.identity.domain.FilialId;
import com.nonnas.identity.domain.Perfil;
import com.nonnas.identity.domain.Usuario;
import com.nonnas.identity.domain.UsuarioId;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final JwtProperties props;
    private final SecretKey key;
    private final Clock clock;

    public JwtTokenProvider(JwtProperties props, Clock clock) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
        this.clock = clock;
    }

    public IssuedToken issueAccess(Usuario usuario) {
        UUID jti = UUID.randomUUID();
        Instant now = clock.instant();
        Instant exp = now.plus(props.accessTtl());
        String token = Jwts.builder()
                .id(jti.toString())
                .subject(usuario.id().value().toString())
                .claim("email", usuario.email().value())
                .claim("nome", usuario.nome())
                .claim("perfil", usuario.perfil().name())
                .claim("filialId", usuario.filialId().map(f -> f.value().toString()).orElse(null))
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .issuer(props.issuer())
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        return new IssuedToken(token, jti, exp);
    }

    public IssuedToken issueRefresh(Usuario usuario, UUID familyId, UUID parentJti) {
        UUID jti = UUID.randomUUID();
        Instant now = clock.instant();
        Instant exp = now.plus(props.refreshTtl());
        String token = Jwts.builder()
                .id(jti.toString())
                .subject(usuario.id().value().toString())
                .claim("familyId", familyId.toString())
                .claim("parentJti", parentJti != null ? parentJti.toString() : null)
                .claim("type", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .issuer(props.issuer())
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        return new IssuedToken(token, jti, exp);
    }

    public ParsedAccess parseAccess(String token) {
        Claims claims = parse(token);
        if (!"access".equals(claims.get("type"))) {
            throw new io.jsonwebtoken.JwtException("Token type incorrect — esperado access");
        }
        return new ParsedAccess(
                UsuarioId.of(UUID.fromString(claims.getSubject())),
                UUID.fromString(claims.getId()),
                (String) claims.get("email"),
                Perfil.valueOf((String) claims.get("perfil")),
                claims.get("filialId") != null
                        ? FilialId.of(UUID.fromString((String) claims.get("filialId")))
                        : null,
                claims.getExpiration().toInstant()
        );
    }

    public ParsedRefresh parseRefresh(String token) {
        Claims claims = parse(token);
        if (!"refresh".equals(claims.get("type"))) {
            throw new io.jsonwebtoken.JwtException("Token type incorrect — esperado refresh");
        }
        UUID parentJti = null;
        Object parent = claims.get("parentJti");
        if (parent != null) parentJti = UUID.fromString((String) parent);
        return new ParsedRefresh(
                UsuarioId.of(UUID.fromString(claims.getSubject())),
                UUID.fromString(claims.getId()),
                UUID.fromString((String) claims.get("familyId")),
                parentJti,
                claims.getExpiration().toInstant()
        );
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(props.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public java.time.Duration accessTtl() {
        return props.accessTtl();
    }

    public record IssuedToken(String value, UUID jti, Instant expiresAt) {}

    public record ParsedAccess(
            UsuarioId usuarioId, UUID jti, String email, Perfil perfil, FilialId filialId, Instant expiresAt) {}

    public record ParsedRefresh(
            UsuarioId usuarioId, UUID jti, UUID familyId, UUID parentJti, Instant expiresAt) {}
}
