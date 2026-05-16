package com.nonnas.saleschannels;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Test app — {@code SalesChannelsConfig} já provê {@code salesChannelsClock}
 * via @ComponentScan, então não declaramos Clock aqui (evita
 * NoUniqueBeanDefinitionException).
 */
@SpringBootApplication(scanBasePackages = {"com.nonnas.saleschannels", "com.nonnas.web"})
@EntityScan(basePackages = {"com.nonnas.saleschannels"})
@EnableJpaRepositories(basePackages = {"com.nonnas.saleschannels"})
@ConfigurationPropertiesScan(basePackages = {"com.nonnas.saleschannels"})
public class SalesChannelsTestApplication {
}
