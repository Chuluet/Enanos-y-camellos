package com.example.enanosycamellos.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Seguridad de la API.
 *
 * <p>Esta app no emite tokens ni guarda contraseñas: eso es trabajo de Keycloak.
 * Acá solo se valida el token que llega en la cabecera
 * {@code Authorization: Bearer ...} (firma, emisor y vencimiento) y se decide
 * qué puede hacer quien lo trae. Las URL del emisor y de sus llaves públicas
 * están en application.yml, bajo {@code spring.security.oauth2.resourceserver}.</p>
 *
 * <p>Los permisos son los mismos para los tres recursos (owners, cows, clowns):</p>
 * <ul>
 *   <li>GET      -> rol {@code user} o {@code admin}</li>
 *   <li>el resto -> solo {@code admin}</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] RUTAS_PUBLICAS = {
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/actuator/health",
            "/error"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            RestAccessDeniedHandler restAccessDeniedHandler,
            RestAuthenticationEntryPoint restAuthenticationEntryPoint)
            throws Exception {

        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(RUTAS_PUBLICAS)
                        .permitAll()

                        // Lectura: cualquier usuario autenticado con
                        // un rol válido puede consultar información.
                        .requestMatchers(HttpMethod.GET, "/api/**")
                        .hasAnyRole(
                                "VIEWER",
                                "RACE_ORGANIZER",
                                "ADMINISTRATOR"
                        )

                        // Escritura: solamente administrator.
                        .requestMatchers("/api/**")
                        .hasAnyRole("RACE_ORGANIZER", "ADMINISTRATOR")

                        .anyRequest()
                        .authenticated())

                .exceptionHandling(handling -> handling
                        .accessDeniedHandler(restAccessDeniedHandler)
                        .authenticationEntryPoint(restAuthenticationEntryPoint))   
                                    
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(
                                        jwtAuthenticationConverter)))

                .build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(
                SecurityConfig::rolesDelRealm);

        return converter;
    }

    private static Collection<GrantedAuthority> rolesDelRealm(Jwt jwt) {

        Map<String, Object> realmAccess =
                jwt.getClaim("realm_access");

        if (realmAccess == null ||
                !(realmAccess.get("roles")
                        instanceof Collection<?> roles)) {

            return List.of();
        }

        return roles.stream()
                .map(rol -> (GrantedAuthority)
                        new SimpleGrantedAuthority(
                                "ROLE_" +
                                        rol.toString()
                                                .toUpperCase(Locale.ROOT)))
                .toList();
    }
}