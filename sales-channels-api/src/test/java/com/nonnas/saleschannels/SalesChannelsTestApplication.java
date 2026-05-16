package com.nonnas.saleschannels;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.time.Clock;

/**
 * Test app — escaneia também recipes/inventory/catalog porque o
 * {@code ProcessarPedidoCanalUseCase} (T-CANAL-04) injeta
 * {@code RegistrarVendaSimuladaUseCase} (recipes), que por sua vez
 * depende de inventory-core + catalog. Sem isso, o startup do contexto
 * Spring falha com NoSuchBeanDefinitionException.
 *
 * <p>{@code SalesChannelsConfig} já provê {@code salesChannelsClock} via
 * @ComponentScan, então não declaramos Clock aqui (evita
 * NoUniqueBeanDefinitionException).
 */
@SpringBootApplication(scanBasePackages = {
        "com.nonnas.saleschannels",
        "com.nonnas.recipes",
        "com.nonnas.inventory",
        "com.nonnas.catalog",
        "com.nonnas.web"})
@EntityScan(basePackages = {
        "com.nonnas.saleschannels",
        "com.nonnas.recipes",
        "com.nonnas.inventory",
        "com.nonnas.catalog"})
@EnableJpaRepositories(basePackages = {
        "com.nonnas.saleschannels",
        "com.nonnas.recipes",
        "com.nonnas.inventory",
        "com.nonnas.catalog"})
@ConfigurationPropertiesScan(basePackages = {
        "com.nonnas.saleschannels",
        "com.nonnas.recipes",
        "com.nonnas.inventory",
        "com.nonnas.catalog"})
public class SalesChannelsTestApplication {

    /**
     * Em produção (app/), {@code alertsClock} é @Primary porque alerts é
     * scanneado. Aqui em test alerts não entra no contexto — 3 Clocks
     * concorrem (sales/inventory/catalog), nenhum primário. Declaramos um
     * @Primary aqui para resolver a ambiguidade no startup do test context.
     */
    @Bean
    @Primary
    Clock testPrimaryClock() {
        return Clock.systemUTC();
    }
}
