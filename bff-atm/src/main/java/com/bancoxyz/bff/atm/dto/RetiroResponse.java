package com.bancoxyz.bff.atm.dto;

public record RetiroResponse(boolean retiroExitoso, double montoRetirado, double saldoDisponible) {
}
