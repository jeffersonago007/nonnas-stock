package com.nonnas.nfeimporter;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.time.Clock;

/**
 * App de teste do nfe-importer. Carrega o orquestrador (nfe) + bounded
 * contexts dependentes (catalog, operations, inventory, identity para
 * Filial — necessário porque CriarUsuario/CriarFilial validam FK).
 *
 * <p>Identity também é incluída para reusar AdminBootstrap (admin existente
 * para autoria das movimentações), seguindo padrão do AlertsTestApplication.
 */
@SpringBootApplication(scanBasePackages = {
        "com.nonnas.nfeimporter",
        "com.nonnas.operations",
        "com.nonnas.inventory",
        "com.nonnas.catalog",
        "com.nonnas.identity",
        "com.nonnas.web"
})
@EntityScan(basePackages = {
        "com.nonnas.nfeimporter",
        "com.nonnas.operations",
        "com.nonnas.inventory",
        "com.nonnas.catalog",
        "com.nonnas.identity"
})
@EnableJpaRepositories(basePackages = {
        "com.nonnas.nfeimporter",
        "com.nonnas.operations",
        "com.nonnas.inventory",
        "com.nonnas.catalog",
        "com.nonnas.identity"
})
@ConfigurationPropertiesScan(basePackages = {
        "com.nonnas.nfeimporter",
        "com.nonnas.operations",
        "com.nonnas.inventory",
        "com.nonnas.catalog",
        "com.nonnas.identity"
})
public class NfeImporterTestApplication {

    /**
     * No contexto de teste do nfe-importer (que carrega catalog +
     * inventory-core + operations + identity, cada um com seu Clock),
     * marcamos um {@code @Primary} para resolver ambiguidade. Em produção,
     * {@code alertsClock} já cumpre esse papel quando todos os módulos
     * sobem juntos no app.
     */
    @Bean
    @Primary
    public Clock testClock() {
        return Clock.systemUTC();
    }
}
