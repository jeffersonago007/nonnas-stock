package com.nonnas.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point que agrega todos os bounded contexts (identity, catalog,
 * inventory-core, recipes, operations, alerts, reporting). Único módulo
 * autorizado a importar de múltiplos {@code com.nonnas.<modulo>}.
 *
 * <p>{@code @EntityScan} e {@code @EnableJpaRepositories} são explícitos
 * porque o auto-configure padrão só inspeciona o pacote da main class,
 * deixando os módulos invisíveis ao Spring Data.
 */
@SpringBootApplication(scanBasePackages = "com.nonnas")
@EntityScan(basePackages = "com.nonnas")
@EnableJpaRepositories(basePackages = "com.nonnas")
@ConfigurationPropertiesScan(basePackages = "com.nonnas")
@EnableScheduling
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class NonnasStockApplication {

    public static void main(String[] args) {
        SpringApplication.run(NonnasStockApplication.class, args);
    }
}
