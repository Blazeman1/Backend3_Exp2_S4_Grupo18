package com.bancoxyz.bff.mobile.client;

import java.util.List;

public record CuentaCoreDTO(
        long cuentaId,
        String titular,
        String tipo,
        int edadTitular,
        double saldo,
        List<MovimientoCoreDTO> movimientos
) {
}
