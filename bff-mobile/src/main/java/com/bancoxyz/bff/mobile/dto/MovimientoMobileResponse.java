package com.bancoxyz.bff.mobile.dto;

import java.time.LocalDate;

/** Version reducida de un movimiento: sin descripcion (texto libre, el campo mas pesado en bytes). */
public record MovimientoMobileResponse(LocalDate fecha, String tipo, double monto) {
}
