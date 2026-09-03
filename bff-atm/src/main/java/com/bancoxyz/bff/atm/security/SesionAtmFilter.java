package com.bancoxyz.bff.atm.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Exige el encabezado {@code X-Atm-Session} (token opaco emitido por {@link SesionAtmService})
 * en toda operacion de cajero, salvo el propio endpoint de inicio de sesion.
 */
@Component
public class SesionAtmFilter extends OncePerRequestFilter {

    public static final String ATRIBUTO_CUENTA_ID = "cuentaIdAutenticado";
    public static final String ATRIBUTO_TOKEN_SESION = "tokenSesion";
    private static final String ENCABEZADO = "X-Atm-Session";

    private final SesionAtmService sesionAtmService;

    public SesionAtmFilter(SesionAtmService sesionAtmService) {
        this.sesionAtmService = sesionAtmService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().equals("/api/atm/sesion");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = request.getHeader(ENCABEZADO);
        Optional<Long> cuentaId = token == null ? Optional.empty() : sesionAtmService.validar(token);
        if (cuentaId.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Sesion de cajero invalida o expirada (dura solo 2 " +
                    "minutos). Vuelva a validar su tarjeta y PIN.\"}");
            return;
        }
        request.setAttribute(ATRIBUTO_CUENTA_ID, cuentaId.get());
        request.setAttribute(ATRIBUTO_TOKEN_SESION, token);
        filterChain.doFilter(request, response);
    }
}
