package com.bancoxyz.bff.atm.security;

/**
 * Misma formula deterministica que {@code bff-mobile.security.PinGenerator}, duplicada
 * intencionalmente en este modulo (ver README.md, seccion "Por que no se comparte codigo entre
 * los BFF"): el PIN de una tarjeta es, en la vida real, un dato validado contra la MISMA
 * identidad del titular sin importar si el canal es un cajero o la app movil, asi que ambos
 * canales derivan el mismo valor a partir del {@code cuentaId} - solo con fines de
 * demostracion academica, ver la nota de seguridad en {@code PinGenerator} de bff-mobile.
 */
public final class PinGenerator {

    private PinGenerator() {
    }

    public static String generar(long cuentaId) {
        long valor = (cuentaId * 73L) % 10000;
        return String.format("%04d", valor);
    }
}
