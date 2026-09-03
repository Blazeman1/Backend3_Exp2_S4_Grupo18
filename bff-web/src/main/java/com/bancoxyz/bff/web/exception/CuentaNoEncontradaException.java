package com.bancoxyz.bff.web.exception;

public class CuentaNoEncontradaException extends RuntimeException {
    public CuentaNoEncontradaException(long cuentaId) {
        super("No existe una cuenta con id=" + cuentaId);
    }
}
