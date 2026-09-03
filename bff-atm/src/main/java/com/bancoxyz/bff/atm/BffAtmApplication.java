package com.bancoxyz.bff.atm;

import com.bancoxyz.bff.atm.config.CoreServiceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * BFF Cajeros Automaticos: backend dedicado exclusivamente a operaciones criticas de cajero
 * (consulta de saldo y retiro). Ver README.md para la justificacion completa de por que este
 * canal es el mas restringido de los tres en cuanto a datos expuestos y operaciones permitidas.
 */
@SpringBootApplication
@EnableConfigurationProperties(CoreServiceProperties.class)
public class BffAtmApplication {

    public static void main(String[] args) {
        SpringApplication.run(BffAtmApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        // Timeouts cortos y estrictos: una operacion critica de dinero en efectivo no debe
        // quedar "colgada" indefinidamente esperando al backend generalizado.
        return builder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }
}
