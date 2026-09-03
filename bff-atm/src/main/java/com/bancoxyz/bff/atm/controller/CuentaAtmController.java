package com.bancoxyz.bff.atm.controller;

import com.bancoxyz.bff.atm.client.CoreServiceClient;
import com.bancoxyz.bff.atm.client.CuentaCoreDTO;
import com.bancoxyz.bff.atm.dto.RetiroRequest;
import com.bancoxyz.bff.atm.dto.RetiroResponse;
import com.bancoxyz.bff.atm.dto.SaldoResponse;
import com.bancoxyz.bff.atm.exception.LimiteRetiroExcedidoException;
import com.bancoxyz.bff.atm.exception.SesionInvalidaException;
import com.bancoxyz.bff.atm.security.SesionAtmFilter;
import com.bancoxyz.bff.atm.security.SesionAtmService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * Las dos UNICAS operaciones que este canal ofrece, tal como piden explicitamente las
 * instrucciones especificas: "Implementar BFF Cajeros Automaticos: interfaz segura y eficiente
 * para operaciones criticas como retiros y consultas de saldo." No existe (deliberadamente)
 * ningun endpoint de historial de movimientos en este modulo.
 */
@RestController
@RequestMapping("/api/atm/cuentas")
public class CuentaAtmController {

    private static final Logger log = LoggerFactory.getLogger(CuentaAtmController.class);

    /** Limite de seguridad propio del canal cajero: ningun retiro individual puede superar este monto. */
    private static final double LIMITE_MAXIMO_RETIRO = 500_000.0;

    private final CoreServiceClient coreServiceClient;
    private final SesionAtmService sesionAtmService;

    public CuentaAtmController(CoreServiceClient coreServiceClient, SesionAtmService sesionAtmService) {
        this.coreServiceClient = coreServiceClient;
        this.sesionAtmService = sesionAtmService;
    }

    @GetMapping("/{cuentaId}/saldo")
    public SaldoResponse consultarSaldo(@PathVariable long cuentaId, HttpServletRequest request) {
        verificarPropietario(cuentaId, request);
        CuentaCoreDTO cuenta = coreServiceClient.obtenerCuenta(cuentaId);
        return new SaldoResponse(cuenta.saldo());
    }

    @PostMapping("/{cuentaId}/retiro")
    public RetiroResponse retirar(@PathVariable long cuentaId, @Valid @RequestBody RetiroRequest request,
                                   HttpServletRequest httpRequest) {
        verificarPropietario(cuentaId, httpRequest);
        if (request.monto() > LIMITE_MAXIMO_RETIRO) {
            throw new LimiteRetiroExcedidoException(LIMITE_MAXIMO_RETIRO);
        }

        CuentaCoreDTO cuentaActualizada = coreServiceClient.debitarSaldo(cuentaId, request.monto());
        log.info("Retiro confirmado: cuenta={} monto={} nuevoSaldo={}", cuentaId, request.monto(), cuentaActualizada.saldo());

        // Tras un retiro exitoso, la sesion se invalida de inmediato: cualquier operacion
        // adicional (otro retiro, otra consulta) exige validar tarjeta y PIN nuevamente. Ver el
        // razonamiento completo en el javadoc de SesionAtmService.
        String token = (String) httpRequest.getAttribute(SesionAtmFilter.ATRIBUTO_TOKEN_SESION);
        sesionAtmService.invalidar(token);

        return new RetiroResponse(true, request.monto(), cuentaActualizada.saldo());
    }

    private void verificarPropietario(long cuentaId, HttpServletRequest request) {
        long cuentaAutenticada = (long) Optional.ofNullable(request.getAttribute(SesionAtmFilter.ATRIBUTO_CUENTA_ID))
                .orElseThrow(() -> new SesionInvalidaException("No hay una sesion de cajero valida."));
        if (cuentaAutenticada != cuentaId) {
            throw new SesionInvalidaException("La tarjeta validada no corresponde a la cuenta " + cuentaId + ".");
        }
    }
}
