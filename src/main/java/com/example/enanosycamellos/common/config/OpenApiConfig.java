package com.example.enanosycamellos.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_KEYCLOAK = "keycloak";

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${keycloak.public-url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String keycloakRealm;

    @Bean
    public OpenAPI enanosycamellosAPI() {
        String openidConnect = keycloakUrl + "/realms/" + keycloakRealm + "/protocol/openid-connect";

        return new OpenAPI()
                .info(new Info()
                        .title("Enanos y Camellos API")
                        .version("0.0.1-SNAPSHOT"))
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort)))
                .components(new Components()
                        .addSecuritySchemes(ESQUEMA_KEYCLOAK, new SecurityScheme()
                                .type(SecurityScheme.Type.OAUTH2)
                                .flows(new OAuthFlows().authorizationCode(new OAuthFlow()
                                        .authorizationUrl(openidConnect + "/auth")
                                        .tokenUrl(openidConnect + "/token")
                                        .scopes(new Scopes())))))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_KEYCLOAK));
    }
}