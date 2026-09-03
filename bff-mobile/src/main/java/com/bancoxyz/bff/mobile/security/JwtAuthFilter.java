package com.bancoxyz.bff.mobile.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String ATRIBUTO_CUENTA_ID = "cuentaIdAutenticado";

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/mobile/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String encabezado = request.getHeader("Authorization");
        if (encabezado == null || !encabezado.startsWith("Bearer ")) {
            rechazar(response, "Falta el encabezado Authorization: Bearer <token>");
            return;
        }
        Optional<Claims> claims = jwtService.validar(encabezado.substring("Bearer ".length()));
        if (claims.isEmpty()) {
            rechazar(response, "Token invalido o expirado (recuerda: la sesion movil dura solo 5 minutos).");
            return;
        }
        request.setAttribute(ATRIBUTO_CUENTA_ID, Long.parseLong(claims.get().getSubject()));
        filterChain.doFilter(request, response);
    }

    private void rechazar(HttpServletResponse response, String mensaje) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + mensaje + "\"}");
    }
}
