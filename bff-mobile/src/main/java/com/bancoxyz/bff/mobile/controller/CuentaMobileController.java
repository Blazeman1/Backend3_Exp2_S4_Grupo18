package com.bancoxyz.bff.mobile.controller;

import com.bancoxyz.bff.mobile.client.CoreServiceClient;
import com.bancoxyz.bff.mobile.client.CuentaCoreDTO;
import com.bancoxyz.bff.mobile.dto.CuentaMobileResumenResponse;
import com.bancoxyz.bff.mobile.exception.AccesoNoAutorizadoException;
import com.bancoxyz.bff.mobile.security.JwtAuthFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/mobile/cuentas")
public class CuentaMobileController {

    private final CoreServiceClient coreServiceClient;

    public CuentaMobileController(CoreServiceClient coreServiceClient) {
        this.coreServiceClient = coreServiceClient;
    }

    /**
     * Unico endpoint de negocio expuesto por este canal: un "resumen" liviano, no el detalle
     * completo que si ofrece bff-web. Nombre de ruta ({@code /resumen}) deliberadamente
     * explicito para que quede claro, con solo leer la URL, que esta es una vista reducida.
     */
    @GetMapping("/{cuentaId}/resumen")
    public CuentaMobileResumenResponse obtenerResumen(@PathVariable long cuentaId, HttpServletRequest request) {
        long cuentaAutenticada = (long) Optional.ofNullable(request.getAttribute(JwtAuthFilter.ATRIBUTO_CUENTA_ID))
                .orElseThrow(() -> new AccesoNoAutorizadoException("No hay una sesion valida."));
        if (cuentaAutenticada != cuentaId) {
            throw new AccesoNoAutorizadoException("Su sesion no tiene acceso a la cuenta " + cuentaId + ".");
        }
        CuentaCoreDTO cuenta = coreServiceClient.obtenerCuenta(cuentaId);
        return CuentaMobileResumenResponse.desde(cuenta);
    }
}
