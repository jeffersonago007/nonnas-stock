package com.nonnas.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;
import java.util.UUID;

/**
 * AuditorAware do MVP retorna {@code Optional.empty()} — entidades anotadas com
 * {@code @CreatedBy} ficam sem autor automático. Será conectado ao usuário
 * autenticado em T13 (frontend de cadastros) usando o JWT de identity.
 */
@Configuration
public class AuditingConfig {

    @Bean
    public AuditorAware<UUID> auditorAware() {
        return Optional::empty;
    }
}
