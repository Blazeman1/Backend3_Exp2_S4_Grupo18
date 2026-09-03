package com.bancoxyz.bff.web.dto;

public record LoginResponse(String token, long expiraEnSegundos, long cuentaId, String titular) {
}
