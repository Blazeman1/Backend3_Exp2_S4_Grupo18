package com.bancoxyz.bff.mobile.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Autenticacion y autorizacion especificas del canal MOVIL: sesiones de solo <b>5 minutos</b>
 * (mucho mas cortas que el canal web) y un claim minimo (solo {@code cuentaId}, sin el nombre
 * del titular) - una app movil consulta el servidor con mas frecuencia y en redes menos
 * confiables, por lo que conviene forzar renovaciones de token frecuentes en vez de sostener
 * una sesion larga expuesta a un dispositivo potencialmente perdido o robado.
 */
@Service
public class JwtService {

    private static final Duration VALIDEZ_SESION_MOVIL = Duration.ofMinutes(5);

    private final Key clave;

    public JwtService(@Value("${seguridad.jwt.secreto}") String secreto) {
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
    }

    public String generarToken(long cuentaId) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .setSubject(String.valueOf(cuentaId))
                .claim("canal", "MOVIL")
                .setIssuedAt(Date.from(ahora))
                .setExpiration(Date.from(ahora.plus(VALIDEZ_SESION_MOVIL)))
                .signWith(clave, SignatureAlgorithm.HS256)
                .compact();
    }

    public long validezEnSegundos() {
        return VALIDEZ_SESION_MOVIL.toSeconds();
    }

    public Optional<Claims> validar(String token) {
        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(clave).build().parseClaimsJws(token).getBody();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
