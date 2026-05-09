package com.nonnas.recipes;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Test app que ativa também os componentes de inventory-core, já que
 * recipes depende em compile e o use case de venda chama
 * {@code RegistrarSaidaMultiItemUseCase} cross-module.
 */
@SpringBootApplication(scanBasePackages = {"com.nonnas.recipes", "com.nonnas.inventory", "com.nonnas.web"})
@EntityScan(basePackages = {"com.nonnas.recipes", "com.nonnas.inventory"})
@EnableJpaRepositories(basePackages = {"com.nonnas.recipes", "com.nonnas.inventory"})
@ConfigurationPropertiesScan(basePackages = {"com.nonnas.recipes", "com.nonnas.inventory"})
public class RecipesTestApplication {
}
