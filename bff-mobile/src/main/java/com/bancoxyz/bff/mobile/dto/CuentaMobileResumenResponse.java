package com.bancoxyz.bff.mobile.dto;

import com.bancoxyz.bff.mobile.client.CuentaCoreDTO;
import com.bancoxyz.bff.mobile.client.MovimientoCoreDTO;

import java.util.Comparator;
import java.util.List;

/**
 * Personalizacion del canal MOVIL (criterio de la pauta "Personaliza la informacion segun las
 * necesidades de cada frontend"): a diferencia de {@code CuentaWebResponse} (bff-web), esta
 * respuesta:
 * <ul>
 *   <li>NO incluye el nombre del titular ni su edad (la app ya los tiene en el dispositivo desde
 *       el login/perfil; no hace falta repetirlos en cada consulta de saldo).</li>
 *   <li>NO incluye el historial completo, solo los <b>ultimos 3</b> movimientos (lo que cabe en
 *       una pantalla movil sin scroll).</li>
 *   <li>NO incluye agregados financieros calculados (totalDepositos, totalRetiros, etc.) -esos
 *       calculos, si se necesitaran, se pueden pedir bajo demanda en una pantalla especifica,
 *       no en la carga inicial que el usuario ve al abrir la app-.</li>
 * </ul>
 * El resultado es un payload considerablemente mas liviano que el de bff-web para la misma
 * cuenta, que es exactamente el objetivo declarado en las instrucciones especificas para este
 * canal ("respuestas ligeras, datos esenciales para reducir consumo de ancho de banda").
 */
public record CuentaMobileResumenResponse(
        double saldo,
        String tipo,
        List<MovimientoMobileResponse> ultimosMovimientos
) {

    private static final int CANTIDAD_ULTIMOS_MOVIMIENTOS = 3;

    public static CuentaMobileResumenResponse desde(CuentaCoreDTO cuenta) {
        List<MovimientoMobileResponse> ultimos = cuenta.movimientos().stream()
                .sorted(Comparator.comparing(MovimientoCoreDTO::fecha).reversed())
                .limit(CANTIDAD_ULTIMOS_MOVIMIENTOS)
                .map(m -> new MovimientoMobileResponse(m.fecha(), m.tipoMovimiento(), m.monto()))
                .toList();
        return new CuentaMobileResumenResponse(cuenta.saldo(), cuenta.tipo(), ultimos);
    }
}
