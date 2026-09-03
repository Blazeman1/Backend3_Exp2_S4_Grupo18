package com.bancoxyz.bff.web.exception;

/** Lanzada cuando un token JWT valido intenta consultar una cuenta que no es la suya. */
public class AccesoNoAutorizadoException extends RuntimeException {
    public AccesoNoAutorizadoException(String mensaje) {
        super(mensaje);
    }
}
