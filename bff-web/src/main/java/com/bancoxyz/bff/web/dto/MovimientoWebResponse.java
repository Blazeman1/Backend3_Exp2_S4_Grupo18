package com.bancoxyz.bff.web.dto;

import com.bancoxyz.bff.web.client.MovimientoCoreDTO;

import java.time.LocalDate;

/** Movimiento completo, sin recortar: el canal web puede permitirse mostrar todos los campos. */
public record MovimientoWebResponse(LocalDate fecha, String tipoMovimiento, double monto, String descripcion) {

    public static MovimientoWebResponse desde(MovimientoCoreDTO movimiento) {
        return new MovimientoWebResponse(movimiento.fecha(), movimiento.tipoMovimiento(), movimiento.monto(), movimiento.descripcion());
    }
}
