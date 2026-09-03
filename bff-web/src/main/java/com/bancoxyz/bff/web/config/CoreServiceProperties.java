package com.bancoxyz.bff.web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "core-service")
public class CoreServiceProperties {

    /** URL base del backend generalizado (core-service). */
    private String baseUrl = "http://localhost:8080";

    /** Clave interna compartida, enviada en el encabezado X-Internal-Api-Key. */
    private String apiKey;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
