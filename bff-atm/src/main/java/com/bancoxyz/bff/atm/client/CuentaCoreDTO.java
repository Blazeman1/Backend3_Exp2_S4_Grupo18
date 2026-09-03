package com.bancoxyz.bff.atm.client;

/**
 * Espejo LOCAL, y deliberadamente incompleto, del {@code CuentaInternalDTO} de core-service:
 * este canal jamas necesita el historial de movimientos, asi que el campo {@code movimientos}
 * ni siquiera se declara aqui. Jackson ignora silenciosamente los campos del JSON que no
 * calzan con esta clase (comportamiento por defecto de Spring Boot), asi que no hace falta
 * ningun mapeo especial: es la forma mas simple de que este BFF nunca llegue a tener en memoria
 * datos que su canal no deberia manejar.
 */
public record CuentaCoreDTO(long cuentaId, String titular, String tipo, int edadTitular, double saldo) {
}
