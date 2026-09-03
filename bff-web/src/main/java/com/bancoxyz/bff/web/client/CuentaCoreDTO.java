package com.bancoxyz.bff.web.client;

import java.util.List;

/**
 * Espejo local del {@code CuentaInternalDTO} expuesto por core-service. Se define una copia
 * propia (en vez de compartir un JAR de DTOs entre modulos) de forma deliberada: la estrategia
 * de "backends independientes" busca justamente que cada BFF pueda evolucionar su contrato sin
 * coordinar despliegues con los demas; el acoplamiento se limita al contrato HTTP/JSON de
 * core-service, nunca a codigo Java compartido.
 */
public record CuentaCoreDTO(
        long cuentaId,
        String titular,
        String tipo,
        int edadTitular,
        double saldo,
        List<MovimientoCoreDTO> movimientos
) {
}
