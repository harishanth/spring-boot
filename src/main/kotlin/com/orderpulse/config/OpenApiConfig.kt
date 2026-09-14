package com.orderpulse.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * OpenAPI 3 (Swagger) Configuration with Bearer JWT Security Scheme.
 */
@Configuration
class OpenApiConfig {

    companion object {
        const val SECURITY_SCHEME_NAME = "Bearer Authentication"
    }

    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("OrderPulse API - High-Concurrency Order & Payment Engine")
                    .version("1.0.0")
                    .description(
                        "Interview Showcase Project: Spring Boot 3 + Kotlin backend demonstrating Clean Architecture, " +
                        "Optimistic Locking, JWT Authentication, RFC 7807 ProblemDetails, and Caffeine Caching."
                    )
                    .contact(Contact().name("OrderPulse Team").email("support@orderpulse.com"))
                    .license(License().name("Apache 2.0").url("https://spring.io"))
            )
            .addSecurityItem(SecurityRequirement().addList(SECURITY_SCHEME_NAME))
            .components(
                Components().addSecuritySchemes(
                    SECURITY_SCHEME_NAME,
                    SecurityScheme()
                        .name(SECURITY_SCHEME_NAME)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Enter your JWT token obtained from /api/v1/auth/login or /api/v1/auth/register")
                )
            )
    }
}
