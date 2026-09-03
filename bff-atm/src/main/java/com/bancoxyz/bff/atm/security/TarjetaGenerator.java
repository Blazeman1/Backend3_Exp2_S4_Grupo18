package com.bancoxyz.bff.atm.security;

/**
 * Genera y decodifica un numero de tarjeta sintetico de 16 digitos, con formato
 * {@code 4915 + cuentaId (12 digitos con ceros a la izquierda)}, solo con fines de demostracion
 * academica (el dataset legacy no incluye numeros de tarjeta).
 */
public final class TarjetaGenerator {

    private static final String PREFIJO = "4915";

    private TarjetaGenerator() {
    }

    public static String generar(long cuentaId) {
        return PREFIJO + String.format("%012d", cuentaId);
    }

    /** @return el {@code cuentaId} codificado en el numero de tarjeta, o vacio si el formato no es valido. */
    public static java.util.Optional<Long> extraerCuentaId(String numeroTarjeta) {
        if (numeroTarjeta == null || !numeroTarjeta.startsWith(PREFIJO) || numeroTarjeta.length() != 16) {
            return java.util.Optional.empty();
        }
        try {
            return java.util.Optional.of(Long.parseLong(numeroTarjeta.substring(PREFIJO.length())));
        } catch (NumberFormatException e) {
            return java.util.Optional.empty();
        }
    }
}
