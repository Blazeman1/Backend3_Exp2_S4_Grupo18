package com.bancoxyz.bff.web.client;

import java.time.LocalDate;

public record MovimientoCoreDTO(LocalDate fecha, String tipoMovimiento, double monto, String descripcion) {
}
