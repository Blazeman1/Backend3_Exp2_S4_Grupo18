package com.bancoxyz.bff.web.dto;

import com.bancoxyz.bff.web.client.CuentaCoreDTO;
import com.bancoxyz.bff.web.client.MovimientoCoreDTO;

import java.util.List;

/**
 * Personalizacion del canal WEB (criterio de la pauta "Personaliza la informacion segun las
 * necesidades de cada frontend"): respuesta RICA y COMPLETA -titular, edad, tasa de interes
 * calculada, agregados financieros y el historial COMPLETO de movimientos-, pensada para una
 * interfaz de escritorio con tablas, filtros y graficos. Comparar directamente con
 * {@code CuentaMobileResumenResponse} (bff-mobile) y la ausencia total de historial en bff-atm.
 */
public record CuentaWebResponse(
        long cuentaId,
        String titular,
        String tipoCuenta,
        int edadTitular,
        double saldoActual,
        double tasaInteresMensual,
        double interesProyectadoMensual,
        int totalMovimientos,
        double totalDepositos,
        double totalRetiros,
        double totalCompras,
        List<MovimientoWebResponse> movimientos
) {

    private static final double TASA_AHORRO = 0.0150;
    private static final double TASA_PRESTAMO = 0.0250;

    public static CuentaWebResponse desde(CuentaCoreDTO cuenta) {
        double tasa = "ahorro".equals(cuenta.tipo()) ? TASA_AHORRO : TASA_PRESTAMO;
        double totalDepositos = sumar(cuenta.movimientos(), "deposito");
        double totalRetiros = sumar(cuenta.movimientos(), "retiro");
        double totalCompras = sumar(cuenta.movimientos(), "compra");

        List<MovimientoWebResponse> movimientos = cuenta.movimientos().stream()
                .map(MovimientoWebResponse::desde)
                .toList();

        return new CuentaWebResponse(
                cuenta.cuentaId(),
                cuenta.titular(),
                cuenta.tipo(),
                cuenta.edadTitular(),
                cuenta.saldo(),
                tasa,
                round2(cuenta.saldo() * tasa),
                movimientos.size(),
                totalDepositos,
                totalRetiros,
                totalCompras,
                movimientos
        );
    }

    private static double sumar(List<MovimientoCoreDTO> movimientos, String tipo) {
        return round2(movimientos.stream()
                .filter(m -> tipo.equals(m.tipoMovimiento()))
                .mapToDouble(m -> Math.abs(m.monto()))
                .sum());
    }

    private static double round2(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
