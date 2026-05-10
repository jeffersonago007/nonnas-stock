package com.nonnas.nfeimporter.infrastructure.config;

import com.nonnas.nfeimporter.parser.XmlNfeParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração de produção do nfe-importer. Não declara bean de Clock
 * para evitar colidir com {@code alertsClock} (já {@code @Primary}) quando
 * todos os módulos sobem juntos no app. Em testes isolados, o
 * {@code NfeImporterTestApplication} define seu próprio Clock {@code @Primary}.
 */
@Configuration
public class NfeImporterConfig {

    @Bean
    public XmlNfeParser xmlNfeParser() {
        return new XmlNfeParser();
    }
}
