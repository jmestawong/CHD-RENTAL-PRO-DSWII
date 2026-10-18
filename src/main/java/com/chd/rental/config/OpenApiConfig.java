package com.chd.rental.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Documentacion interactiva de los servicios en /swagger-ui.html */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI chdRentalOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("CH&D Rental Pro API")
                        .version("2.0.0")
                        .description("Servicios web REST para la gestion de alquiler de camionetas 4x4 de "
                                + "GRUPO CH&D CONTRATISTAS S.A.C. 1) Ejecute POST /api/auth/login, "
                                + "2) copie el token y presione 'Authorize'.")
                        .contact(new Contact().name("Grupo 10 - Desarrollo de Servicios Web II (CIBERTEC)")))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
