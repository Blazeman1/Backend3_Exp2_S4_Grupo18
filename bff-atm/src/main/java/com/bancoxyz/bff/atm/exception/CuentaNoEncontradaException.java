package com.bancoxyz.bff.atm.exception;

public class CuentaNoEncontradaException extends RuntimeException {
    public CuentaNoEncontradaException(long cuentaId) {
        super("No existe una cuenta con id=" + cuentaId);
    }
}
