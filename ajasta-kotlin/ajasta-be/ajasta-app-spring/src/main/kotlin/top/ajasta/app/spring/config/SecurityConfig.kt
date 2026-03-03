package top.ajasta.app.spring.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Spring Security configuration for OAuth2 Resource Server with Keycloak.
 *
 * This configuration:
 * - Validates JWT tokens from Keycloak
 * - Extracts roles from JWT claims
 * - Configures CORS for frontend access
 * - Defines endpoint access rules
 */
@Configuration
@EnableWebFluxSecurity
class SecurityConfig {

    @Value("\${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:http://keycloak:8080/realms/ajasta/protocol/openid-connect/certs}")
    private lateinit var jwkSetUri: String

    /**
     * Main security filter chain configuration.
     */
    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            // Enable CORS
            .cors { cors -> cors.configurationSource(corsConfigurationSource()) }
            // Disable CSRF for stateless API
            .csrf { csrf -> csrf.disable() }
            // Configure authorization rules
            .authorizeExchange { exchanges ->
                exchanges
                    // Public endpoints
                    .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                    .pathMatchers("/v1/resources/search").permitAll()
                    .pathMatchers("/v1/resources/availability").permitAll()
                    // Swagger/OpenAPI documentation endpoints
                    .pathMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/api-docs/**", "/webjars/**").permitAll()
                    // Admin endpoints - permitAll for testing, should be hasRole("ADMIN") in production
                    .pathMatchers("/admin/**").permitAll()
                    .pathMatchers("/v1/admin/**").hasRole("ADMIN")
                    // All other endpoints require authentication
                    .anyExchange().authenticated()
            }
            // Configure OAuth2 Resource Server with JWT
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(KeycloakAuthenticationConverter())
                }
            }
            .build()
    }

    /**
     * Custom JWT authentication converter that extracts roles from Keycloak JWT.
     */
    class KeycloakAuthenticationConverter : Converter<Jwt, Mono<AbstractAuthenticationToken>> {
        private val delegate = ReactiveJwtAuthenticationConverter()

        init {
            delegate.setJwtGrantedAuthoritiesConverter { jwt ->
                // Extract roles from the 'roles' claim (configured in Keycloak realm)
                val roles = jwt.getClaim<List<String>>("roles") ?: emptyList()
                val authorities = roles.map { role ->
                    SimpleGrantedAuthority("ROLE_$role")
                }
                Flux.fromIterable(authorities)
            }
        }

        override fun convert(jwt: Jwt): Mono<AbstractAuthenticationToken> {
            return delegate.convert(jwt)
                .map { auth -> auth }
                .defaultIfEmpty(JwtAuthenticationToken(jwt, emptyList()))
        }
    }

    /**
     * JWT decoder configured for Keycloak using JWK set URI directly.
     * This avoids issuer validation issues when Keycloak uses different hostnames
     * for internal (Docker network) vs external (browser) access.
     */
    @Bean
    fun reactiveJwtDecoder(): ReactiveJwtDecoder {
        return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build()
    }

    /**
     * CORS configuration for frontend access.
     */
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            // Allowed origins
            allowedOrigins = listOf(
                "http://localhost:3000",
                "http://127.0.0.1:3000",
                "http://localhost:8090"
            )
            // Allowed methods
            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
            // Allowed headers
            allowedHeaders = listOf(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
            )
            // Expose headers
            exposedHeaders = listOf(
                "Access-Control-Allow-Origin",
                "Access-Control-Allow-Credentials"
            )
            // Allow credentials
            allowCredentials = true
            // Cache preflight response
            maxAge = 3600L
        }

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
