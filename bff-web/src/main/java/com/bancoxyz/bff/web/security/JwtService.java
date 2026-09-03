package com.bancoxyz.bff.web.security;

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
 * Emision y validacion de JWT propios del canal WEB.
 *
 * <p>Autenticacion y autorizacion especificas de este canal (punto explicito de las
 * instrucciones especificas): sesion de <b>30 minutos</b> (razonable para un usuario trabajando
 * en un panel de escritorio, que puede dejar la pestana abierta mientras revisa varias
 * pantallas), con claims que incluyen el nombre del titular (util para mostrarlo en la interfaz
 * sin una segunda consulta). Compara esto con bff-mobile (5 minutos, sin nombre en el claim) y
 * bff-atm (sesion de operacion de 2 minutos, sin JWT en absoluto).</p>
 */
@Service
public class JwtService {

    private static final Duration VALIDEZ_SESION_WEB = Duration.ofMinutes(30);

    private final Key clave;

    public JwtService(@Value("${seguridad.jwt.secreto}") String secreto) {
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
    }

    public String generarToken(long cuentaId, String titular) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .setSubject(String.valueOf(cuentaId))
                .claim("titular", titular)
                .claim("canal", "WEB")
                .setIssuedAt(Date.from(ahora))
                .setExpiration(Date.from(ahora.plus(VALIDEZ_SESION_WEB)))
                .signWith(clave, SignatureAlgorithm.HS256)
                .compact();
    }

    public long validezEnSegundos() {
        return VALIDEZ_SESION_WEB.toSeconds();
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
