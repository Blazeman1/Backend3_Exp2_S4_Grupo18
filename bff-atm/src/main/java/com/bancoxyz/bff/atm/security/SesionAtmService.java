package com.bancoxyz.bff.atm.security;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Autenticacion y autorizacion especificas del canal de CAJEROS AUTOMATICOS: la mas estricta de
 * los tres canales, acorde a que aqui se ejecutan operaciones criticas con dinero en efectivo.
 *
 * <p>A diferencia de bff-web y bff-mobile (JWT autocontenido, firmado, que el propio cliente
 * conserva y reenvia), aqui se usa un <b>token opaco de sesion, guardado en memoria del
 * servidor</b>, valido solo <b>2 minutos</b> -el tiempo tipico que una persona esta parada frente
 * al cajero-, y que se <b>invalida automaticamente</b> apenas se confirma un retiro exitoso
 * (ver {@link #invalidar}), obligando a validar el PIN de nuevo para cualquier operacion
 * adicional. Un JWT autocontenido no permite esta revocacion inmediata del lado del servidor sin
 * mantener ademas una lista de revocacion; un token opaco, al vivir enteramente en el servidor,
 * se invalida con solo borrarlo del mapa.</p>
 */
@Service
public class SesionAtmService {

    private static final long VALIDEZ_SESION_SEGUNDOS = 120;

    private record Sesion(long cuentaId, Instant expira) {
        boolean vigente() {
            return Instant.now().isBefore(expira);
        }
    }

    private final Map<String, Sesion> sesiones = new ConcurrentHashMap<>();

    public String crear(long cuentaId) {
        String token = UUID.randomUUID().toString();
        sesiones.put(token, new Sesion(cuentaId, Instant.now().plusSeconds(VALIDEZ_SESION_SEGUNDOS)));
        return token;
    }

    public long validezEnSegundos() {
        return VALIDEZ_SESION_SEGUNDOS;
    }

    /** @return el {@code cuentaId} asociado a la sesion, si el token existe y no ha expirado. */
    public Optional<Long> validar(String token) {
        Sesion sesion = sesiones.get(token);
        if (sesion == null || !sesion.vigente()) {
            sesiones.remove(token);
            return Optional.empty();
        }
        return Optional.of(sesion.cuentaId());
    }

    public void invalidar(String token) {
        sesiones.remove(token);
    }
}
