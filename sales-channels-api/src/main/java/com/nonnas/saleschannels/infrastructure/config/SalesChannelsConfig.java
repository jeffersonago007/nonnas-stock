package com.nonnas.saleschannels.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(SalesChannelsProperties.class)
public class SalesChannelsConfig {

    /**
     * RestClient compartilhado para todos os adapters HTTP. baseUrl é
     * passado por adapter (via credencial), então este RestClient é
     * configurado só com timeouts.
     *
     * <p>Usamos {@link ClientHttpRequestFactoryBuilder#simple()} (baseado
     * em {@link java.net.HttpURLConnection}) em vez do auto-detect (JDK
     * HttpClient) porque o último gera "Uma conexão estabelecida foi
     * anulada pelo software" em testes Windows + localhost + POST com
     * body — race condition do HTTP/2 Multiplexing no loopback. O factory
     * simples é HTTP/1.1, sem multiplex, e funciona uniformemente em
     * Linux/macOS/Windows.
     */
    @Bean
    RestClient salesChannelsRestClient(SalesChannelsProperties props) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(props.http().connectTimeout())
                .withReadTimeout(props.http().readTimeout());
        return RestClient.builder()
                .requestFactory(ClientHttpRequestFactoryBuilder.simple().build(settings))
                .build();
    }

    /**
     * Clock UTC para os schedulers do módulo. NÃO marcamos {@code @Primary}
     * porque {@code alertsClock} já é o primário no app integrado — se
     * dois beans fossem @Primary, Spring lançaria
     * NoUniqueBeanDefinitionException no boot. Mesmo padrão de nfe-importer.
     */
    @Bean
    Clock salesChannelsClock() {
        return Clock.systemUTC();
    }
}
