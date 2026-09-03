package com.bancoxyz.bff.atm.exception;

public class SesionInvalidaException extends RuntimeException {
    public SesionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
