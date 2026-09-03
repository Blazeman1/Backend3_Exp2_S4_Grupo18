package com.bancoxyz.bff.core.exception;

public class SaldoInsuficienteException extends RuntimeException {
    public SaldoInsuficienteException(long cuentaId, double saldoActual, double montoSolicitado) {
        super("Cuenta " + cuentaId + ": saldo insuficiente (saldo=" + saldoActual + ", solicitado=" + montoSolicitado + ")");
    }
}
