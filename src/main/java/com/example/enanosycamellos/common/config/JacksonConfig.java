package com.example.enanosycamellos.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot 4 auto-configures a Jackson 3 {@code JsonMapper} for HTTP
 * serialization, but NOT a classic Jackson 2 {@code ObjectMapper} bean.
 * AuditLogService needs one to serialize old/new values as JSON text before
 * storing them, so we declare it explicitly here. This does not affect or
 * replace how Spring MVC serializes HTTP responses (that keeps using the
 * auto-configured JsonMapper on its own).
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }
}