package com.bancoxyz.bff.core.dto;

import com.bancoxyz.bff.core.model.Movimiento;

import java.time.LocalDate;

/**
 * DTO interno de un movimiento. core-service siempre expone el detalle COMPLETO: es
 * responsabilidad de cada BFF (no de este servicio) decidir cuanto de esta informacion
 * reenviar a su frontend.
 */
public record MovimientoDTO(LocalDate fecha, String tipoMovimiento, double monto, String descripcion) {

    public static MovimientoDTO desde(Movimiento movimiento) {
        return new MovimientoDTO(movimiento.fecha(), movimiento.tipoMovimiento(), movimiento.monto(), movimiento.descripcion());
    }
}
