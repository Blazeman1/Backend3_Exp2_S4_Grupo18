package com.bancoxyz.bff.atm.dto;

import jakarta.validation.constraints.NotBlank;

/** Credenciales del cajero: tarjeta + PIN (dos factores), tal como exige un cajero real. */
public record SesionRequest(@NotBlank String numeroTarjeta, @NotBlank String pin) {
}
