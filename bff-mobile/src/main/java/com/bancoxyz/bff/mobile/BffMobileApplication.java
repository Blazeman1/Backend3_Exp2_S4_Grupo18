package com.bancoxyz.bff.mobile;

import com.bancoxyz.bff.mobile.config.CoreServiceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * BFF Movil: backend dedicado a la app movil del Banco XYZ. Ver README.md para la
 * justificacion de por que este canal recibe respuestas deliberadamente reducidas.
 */
@SpringBootApplication
@EnableConfigurationProperties(CoreServiceProperties.class)
public class BffMobileApplication {

    public static void main(String[] args) {
        SpringApplication.run(BffMobileApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        // Timeouts mas agresivos que en bff-web: una app movil en una red celular inestable
        // no deberia dejar al usuario esperando una respuesta que igual va a descartar.
        return builder
                .connectTimeout(Duration.ofSeconds(3))
                .readTimeout(Duration.ofSeconds(5))
                .build();
    }
}
