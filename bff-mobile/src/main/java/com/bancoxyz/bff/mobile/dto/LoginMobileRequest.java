package com.bancoxyz.bff.mobile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Credenciales del canal movil: numero de cuenta + PIN de 4 digitos (ver {@code PinGenerator}),
 * en vez del nombre completo del titular que usa el canal web -mas rapido de escribir en un
 * teclado tactil, y el patron de credencial (PIN corto) que un usuario espera de una app movil.
 */
public record LoginMobileRequest(@NotNull Long cuentaId, @NotBlank String pin) {
}
