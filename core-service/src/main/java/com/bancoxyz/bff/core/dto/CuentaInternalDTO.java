package com.bancoxyz.bff.core.dto;

import com.bancoxyz.bff.core.model.Cuenta;
import com.bancoxyz.bff.core.model.Movimiento;

import java.util.List;

/**
 * DTO interno con el detalle COMPLETO de una cuenta, incluyendo el historial completo de
 * movimientos. Este es el "dato crudo" del backend generalizado; cada BFF decide que
 * subconjunto de estos campos exponer a su frontend (ver README.md, seccion de personalizacion).
 */
public record CuentaInternalDTO(
        long cuentaId,
        String titular,
        String tipo,
        int edadTitular,
        double saldo,
        List<MovimientoDTO> movimientos
) {

    public static CuentaInternalDTO desde(Cuenta cuenta, List<Movimiento> movimientos) {
        return new CuentaInternalDTO(
                cuenta.getCuentaId(),
                cuenta.getTitular(),
                cuenta.getTipo(),
                cuenta.getEdadTitular(),
                cuenta.getSaldo(),
                movimientos.stream().map(MovimientoDTO::desde).toList()
        );
    }
}
