package com.bancoxyz.bff.core.model;

import java.time.LocalDate;

/**
 * Un movimiento historico de una cuenta, consolidado a partir del legacy
 * {@code cuentas_anuales.csv}. Inmutable (record): a diferencia de {@link Cuenta}, el historial
 * de movimientos nunca se modifica despues de cargado -un retiro nuevo hecho en el cajero se
 * refleja actualizando {@link Cuenta#getSaldo()}, no agregando un Movimiento nuevo a este
 * historial legacy, que representa exclusivamente el registro importado del sistema anterior.
 */
public record Movimiento(long cuentaId, LocalDate fecha, String tipoMovimiento, double monto, String descripcion) {
}
