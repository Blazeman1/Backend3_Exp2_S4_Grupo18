package com.bancoxyz.bff.mobile.exception;

public class CuentaNoEncontradaException extends RuntimeException {
    public CuentaNoEncontradaException(long cuentaId) {
        super("No existe una cuenta con id=" + cuentaId);
    }
}
