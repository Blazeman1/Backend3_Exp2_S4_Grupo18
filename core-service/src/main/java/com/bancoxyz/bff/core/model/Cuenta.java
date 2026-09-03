package com.bancoxyz.bff.core.model;

/**
 * Modelo de dominio de una cuenta bancaria del Banco XYZ, consolidado a partir del legacy
 * {@code intereses.csv} (ver {@link com.bancoxyz.bff.core.service.CargaDatosService}).
 *
 * <p>Es un objeto mutable simple (no un record) a proposito: {@link #saldo} se actualiza en
 * memoria cuando el BFF de cajeros automaticos confirma un retiro (ver
 * {@code CuentaInternalController#actualizarSaldo}), y un record inmutable obligaria a
 * reconstruir y reemplazar la entrada completa del repositorio en cada operacion.</p>
 */
public class Cuenta {

    private final long cuentaId;
    private final String titular;
    private final String tipo; // "ahorro" | "prestamo"
    private final int edadTitular;
    private double saldo;

    public Cuenta(long cuentaId, String titular, String tipo, int edadTitular, double saldo) {
        this.cuentaId = cuentaId;
        this.titular = titular;
        this.tipo = tipo;
        this.edadTitular = edadTitular;
        this.saldo = saldo;
    }

    public long getCuentaId() {
        return cuentaId;
    }

    public String getTitular() {
        return titular;
    }

    public String getTipo() {
        return tipo;
    }

    public int getEdadTitular() {
        return edadTitular;
    }

    public double getSaldo() {
        return saldo;
    }

    public void setSaldo(double saldo) {
        this.saldo = saldo;
    }
}
