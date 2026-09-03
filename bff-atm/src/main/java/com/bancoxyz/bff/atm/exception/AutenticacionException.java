package com.bancoxyz.bff.atm.exception;

public class AutenticacionException extends RuntimeException {
    public AutenticacionException(String mensaje) {
        super(mensaje);
    }
}
