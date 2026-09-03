package com.bancoxyz.bff.core.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro que exige el encabezado {@code X-Internal-Api-Key} en toda peticion, con el valor
 * configurado en {@code core.internal-api-key} (ver application.yml).
 *
 * <p>Este es el mecanismo que materializa, a nivel de codigo, el principio central del patron
 * BFF: <b>ningun frontend deberia poder llamar jamas directamente a este backend generalizado</b>.
 * Los tres BFF conocen esta clave (inyectada en su propia configuracion); un navegador, una app
 * movil o un cajero automatico -que solo conocen la URL publica de su respectivo BFF- no la
 * conocen y no pueden invocar este servicio sin pasar antes por el BFF correspondiente. En una
 * topologia de red real esto se reforzaria ademas a nivel de infraestructura (subred privada,
 * sin IP publica), pero a nivel de aplicacion esta clave compartida ya demuestra el principio.</p>
 */
@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(InternalApiKeyFilter.class);
    private static final String HEADER = "X-Internal-Api-Key";

    @Value("${core.internal-api-key}")
    private String claveEsperada;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String claveRecibida = request.getHeader(HEADER);
        if (claveEsperada.equals(claveRecibida)) {
            filterChain.doFilter(request, response);
            return;
        }
        log.warn("Acceso rechazado a {} {}: encabezado {} ausente o invalido (posible intento de un " +
                "frontend de saltarse su BFF y llamar directo al backend generalizado)",
                request.getMethod(), request.getRequestURI(), HEADER);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Acceso no autorizado: este backend solo puede ser " +
                "consumido por los BFF del Banco XYZ.\"}");
    }
}
