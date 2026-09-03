package com.bancoxyz.bff.web.controller;

import com.bancoxyz.bff.web.client.CoreServiceClient;
import com.bancoxyz.bff.web.client.CuentaCoreDTO;
import com.bancoxyz.bff.web.dto.LoginResponse;
import com.bancoxyz.bff.web.dto.LoginWebRequest;
import com.bancoxyz.bff.web.exception.AutenticacionException;
import com.bancoxyz.bff.web.exception.CuentaNoEncontradaException;
import com.bancoxyz.bff.web.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/web/auth")
public class AuthController {

    private final CoreServiceClient coreServiceClient;
    private final JwtService jwtService;

    public AuthController(CoreServiceClient coreServiceClient, JwtService jwtService) {
        this.coreServiceClient = coreServiceClient;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginWebRequest request) {
        // Nota deliberada: cualquier fallo de autenticacion (cuenta inexistente o nombre que no
        // calza) se reporta con el MISMO mensaje generico, para no revelar mediante el mensaje
        // de error si un numero de cuenta existe o no (evita enumeracion de cuentas validas).
        CuentaCoreDTO cuenta;
        try {
            cuenta = coreServiceClient.obtenerCuenta(request.cuentaId());
        } catch (CuentaNoEncontradaException e) {
            throw new AutenticacionException("Credenciales invalidas.");
        }
        if (!cuenta.titular().trim().equalsIgnoreCase(request.nombre().trim())) {
            throw new AutenticacionException("Credenciales invalidas.");
        }
        String token = jwtService.generarToken(cuenta.cuentaId(), cuenta.titular());
        return new LoginResponse(token, jwtService.validezEnSegundos(), cuenta.cuentaId(), cuenta.titular());
    }
}
