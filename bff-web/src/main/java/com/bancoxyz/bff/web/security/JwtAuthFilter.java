package com.bancoxyz.bff.web.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Filtro de autenticacion del canal WEB: exige {@code Authorization: Bearer <jwt>} en todas las
 * rutas de negocio ({@code /api/web/cuentas/**}), pero deja pasar libremente el login
 * ({@code /api/web/auth/login}), que es donde precisamente se emite el token.
 *
 * <p>Al validar el token, deja el {@code cuentaId} del titular autenticado disponible como
 * atributo de la request ({@link #ATRIBUTO_CUENTA_ID}), que el controlador usa para verificar
 * que un token valido solo pueda consultar SU PROPIA cuenta (autorizacion, no solo
 * autenticacion) - ver {@code CuentaWebController}.</p>
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String ATRIBUTO_CUENTA_ID = "cuentaIdAutenticado";

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/web/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String encabezado = request.getHeader("Authorization");
        if (encabezado == null || !encabezado.startsWith("Bearer ")) {
            rechazar(response, "Falta el encabezado Authorization: Bearer <token>");
            return;
        }
        String token = encabezado.substring("Bearer ".length());
        Optional<Claims> claims = jwtService.validar(token);
        if (claims.isEmpty()) {
            rechazar(response, "Token invalido o expirado. Vuelva a iniciar sesion.");
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
