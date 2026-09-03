package com.bancoxyz.bff.web.controller;

import com.bancoxyz.bff.web.client.CoreServiceClient;
import com.bancoxyz.bff.web.client.CuentaCoreDTO;
import com.bancoxyz.bff.web.dto.CuentaWebResponse;
import com.bancoxyz.bff.web.dto.MovimientoWebResponse;
import com.bancoxyz.bff.web.exception.AccesoNoAutorizadoException;
import com.bancoxyz.bff.web.security.JwtAuthFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Endpoints de negocio del canal WEB. Ambos requieren un JWT valido (ver {@link JwtAuthFilter})
 * y, ademas, autorizan que el token solo pueda consultar SU PROPIA cuenta -autenticacion
 * (quien eres) y autorizacion (que puedes ver) son controles distintos y aqui se aplican los dos.
 */
@RestController
@RequestMapping("/api/web/cuentas")
public class CuentaWebController {

    private final CoreServiceClient coreServiceClient;

    public CuentaWebController(CoreServiceClient coreServiceClient) {
        this.coreServiceClient = coreServiceClient;
    }

    @GetMapping("/{cuentaId}")
    public CuentaWebResponse obtenerCuenta(@PathVariable long cuentaId, HttpServletRequest request) {
        verificarPropietario(cuentaId, request);
        CuentaCoreDTO cuenta = coreServiceClient.obtenerCuenta(cuentaId);
        return CuentaWebResponse.desde(cuenta);
    }

    /**
     * Soporte de filtros de consulta (por tipo de movimiento y rango de fechas): el tipo de
     * interfaz compleja (tablas filtrables) que las instrucciones especificas piden para el
     * canal web, y que bff-mobile/bff-atm deliberadamente no ofrecen.
     */
    @GetMapping("/{cuentaId}/movimientos")
    public List<MovimientoWebResponse> listarMovimientos(
            @PathVariable long cuentaId,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) LocalDate desde,
            @RequestParam(required = false) LocalDate hasta,
            HttpServletRequest request) {
        verificarPropietario(cuentaId, request);
        CuentaCoreDTO cuenta = coreServiceClient.obtenerCuenta(cuentaId);
        return cuenta.movimientos().stream()
                .filter(m -> tipo == null || tipo.equalsIgnoreCase(m.tipoMovimiento()))
                .filter(m -> desde == null || !m.fecha().isBefore(desde))
                .filter(m -> hasta == null || !m.fecha().isAfter(hasta))
                .map(MovimientoWebResponse::desde)
                .toList();
    }

    private void verificarPropietario(long cuentaId, HttpServletRequest request) {
        long cuentaAutenticada = (long) Optional.ofNullable(request.getAttribute(JwtAuthFilter.ATRIBUTO_CUENTA_ID))
                .orElseThrow(() -> new AccesoNoAutorizadoException("No hay una sesion valida."));
        if (cuentaAutenticada != cuentaId) {
            throw new AccesoNoAutorizadoException("Su sesion no tiene acceso a la cuenta " + cuentaId + ".");
        }
    }
}
