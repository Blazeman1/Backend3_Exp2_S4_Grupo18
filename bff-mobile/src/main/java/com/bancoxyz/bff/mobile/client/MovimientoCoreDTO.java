package com.bancoxyz.bff.mobile.client;

import java.time.LocalDate;

public record MovimientoCoreDTO(LocalDate fecha, String tipoMovimiento, double monto, String descripcion) {
}
