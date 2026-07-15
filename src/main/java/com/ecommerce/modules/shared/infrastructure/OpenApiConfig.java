package com.ecommerce.modules.shared.infrastructure;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "API Empresarial de E-Commerce",
                description = "Documentación de la API para la Plataforma SaaS Multi-Tenant de E-Commerce",
                version = "1.0",
                contact = @Contact(
                        nombre = "Soporte Técnico",
                        email = "soporte@ecommerce.com"
                )
        ),
        servers = {
                @Server(
                        description = "Entorno Local",
                        url = "http://localhost:8080"
                )
        },
        security = {
                @SecurityRequirement(nombre = "bearerAuth")
        }
)
@SecurityScheme(
        nombre = "bearerAuth",
        description = "Autenticación por Token JWT. Por favor ingrese su token JWT abajo. No incluya el prefijo 'Bearer ', se añade automáticamente.",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
}
