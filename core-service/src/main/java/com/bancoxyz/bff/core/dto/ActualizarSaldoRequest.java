package com.bancoxyz.bff.core.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Solicitud de debito de saldo. Usada exclusivamente por bff-atm al confirmar un retiro en un
 * cajero automatico (ninguna otra operacion de este proyecto modifica el saldo de una cuenta).
 */
public record ActualizarSaldoRequest(@NotNull Double monto) {
}
