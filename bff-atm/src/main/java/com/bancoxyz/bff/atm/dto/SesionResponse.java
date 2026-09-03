package com.bancoxyz.bff.atm.dto;

public record SesionResponse(String sessionToken, long expiraEnSegundos, long cuentaId) {
}
