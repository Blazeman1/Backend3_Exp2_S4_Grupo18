package com.bancoxyz.bff.mobile.security;

/**
 * Genera un PIN de 4 digitos deterministico a partir del {@code cuentaId}, solo con fines de
 * demostracion academica (el dataset legacy no incluye ninguna credencial real).
 *
 * <p><b>Importante:</b> en un sistema real el PIN se almacenaria HASHEADO (nunca en texto plano
 * ni derivado matematicamente) en un servicio de identidad separado, con politicas de bloqueo
 * tras intentos fallidos. Esta funcion determinista existe unicamente para que el equipo
 * evaluador pueda calcular y probar credenciales validas sin necesitar una base de datos de
 * usuarios real -ver la tabla de credenciales de demostracion en el README del proyecto-.</p>
 */
public final class PinGenerator {

    private PinGenerator() {
    }

    public static String generar(long cuentaId) {
        long valor = (cuentaId * 73L) % 10000;
        return String.format("%04d", valor);
    }
}
