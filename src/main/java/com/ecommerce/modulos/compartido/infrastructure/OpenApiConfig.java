package com.ecommerce.modulos.compartido.infrastructure;

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
                title = "NexaSaaS E-Commerce API",
                description = "Documentación de la API para NexaSaaS: Plataforma SaaS Multi-Inquilino de E-Commerce",
                version = "1.0",
                contact = @Contact(
                        name = "Soporte Técnico",
                        email = "soporte@nexasaas.com"
                )
        ),
        servers = {
                @Server(
                        description = "Entorno Local",
                        url = "http://localhost:8081"
                )
        },
        security = {
                @SecurityRequirement(name = "bearerAuth")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        description = "Autenticación por Token JWT. Por favor ingrese su token JWT abajo. No incluya el prefijo 'Bearer ', se añade automáticamente.",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
}
