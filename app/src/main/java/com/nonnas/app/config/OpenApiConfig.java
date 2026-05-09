package com.nonnas.app.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI nonnasStockOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Nonnas Stock API")
                        .version("1.0.0-SNAPSHOT")
                        .description("Sistema de controle de estoque centralizado para a rede Nonnas Paola.")
                        .contact(new Contact().name("Equipe Nonnas Paola").email("ti@nonnaspaola.com.br"))
                        .license(new License().name("Proprietária")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token JWT emitido por POST /api/v1/auth/login")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .tags(List.of(
                        new Tag().name("Identidade").description("Empresas, filiais, usuários e autenticação."),
                        new Tag().name("Catálogo").description("Insumos, categorias, unidades de medida, fornecedores."),
                        new Tag().name("Estoque").description("Lotes, saldos e movimentações (FEFO)."),
                        new Tag().name("Receitas").description("Produtos vendáveis e fichas técnicas versionadas."),
                        new Tag().name("Operações").description("Transferências, ajustes e carga inicial."),
                        new Tag().name("Alertas").description("Configuração e disparos de alertas de estoque/vencimento."),
                        new Tag().name("Relatórios").description("Posição, curva ABC, ruptura, vencimento e divergências.")));
    }
}
