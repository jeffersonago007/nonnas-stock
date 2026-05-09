package com.nonnas.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * CORS para o frontend. Origens vêm de {@code nonnas.cors.allowed-origins}
 * (lista) — vazia por default em prod; configurada em dev para localhost.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final List<String> allowedOrigins;

    public CorsConfig(@Value("${nonnas.cors.allowed-origins:}") List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            return;
        }
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
