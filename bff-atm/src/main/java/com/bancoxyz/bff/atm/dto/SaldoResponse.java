package com.bancoxyz.bff.atm.dto;

/**
 * Personalizacion del canal CAJERO (criterio de la pauta "Personaliza la informacion segun las
 * necesidades de cada frontend"): esta es, de las tres, la respuesta MAS reducida del proyecto.
 * Ni siquiera el tipo de cuenta o el nombre del titular se exponen -un cajero automatico
 * publico, en un espacio fisico compartido, no deberia mostrar en pantalla mas informacion
 * personal que la estrictamente necesaria para la operacion solicitada (principio de minimizacion
 * de datos). Contrastar con {@code CuentaWebResponse} (bff-web) y {@code
 * CuentaMobileResumenResponse} (bff-mobile).
 */
public record SaldoResponse(double saldoDisponible) {
}
