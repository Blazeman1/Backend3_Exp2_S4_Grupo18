package com.bancoxyz.bff.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Credenciales del canal web. Simplificacion deliberada con fines academicos: el dataset legacy
 * del Banco XYZ no incluye contrasenas, asi que se usa el nombre del titular (dato que el
 * cliente real conoceria) como segundo factor de identificacion junto al numero de cuenta. En
 * un sistema real, aqui iria un usuario/contrasena validado contra un directorio de identidad
 * (o un flujo OAuth2/OIDC completo).
 */
public record LoginWebRequest(@NotNull Long cuentaId, @NotBlank String nombre) {
}
