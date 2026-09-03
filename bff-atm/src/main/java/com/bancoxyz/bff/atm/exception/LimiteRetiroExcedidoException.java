package com.bancoxyz.bff.atm.exception;

public class LimiteRetiroExcedidoException extends RuntimeException {
    public LimiteRetiroExcedidoException(double limite) {
        super("El monto solicitado excede el limite maximo de retiro por operacion ($" + limite + ").");
    }
}
