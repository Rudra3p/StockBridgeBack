package com.stockbridge.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class OpenApiConfig {

    private static final String BEARER_AUTH = "BearerAuth";
    private static final String API_KEY_AUTH = "ApiKeyAuth";

    @Bean
    public OpenAPI stockBridgeOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("StockBridge B2B Marketplace API (DEV)")
                        .description("REST API for StockBridge with API Key and JWT Authentication")
                        .version("1.0.0")
                        .contact(new Contact().name("StockBridge Engineering Team").email("dev@stockbridge.local"))
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")))
                .addSecurityItem(new SecurityRequirement().addList(API_KEY_AUTH).addList(BEARER_AUTH))
                .components(new Components()
                        .addSecuritySchemes(API_KEY_AUTH,
                                new SecurityScheme()
                                        .name("X-API-KEY")
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .description("Enter your backend API Key"))
                        .addSecuritySchemes(BEARER_AUTH,
                                new SecurityScheme()
                                        .name(BEARER_AUTH)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT token (format: Bearer <token>)")));
    }
}
