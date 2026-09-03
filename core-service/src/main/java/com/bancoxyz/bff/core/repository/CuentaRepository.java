package com.bancoxyz.bff.core.repository;

import com.bancoxyz.bff.core.model.Cuenta;
import com.bancoxyz.bff.core.model.Movimiento;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repositorio en memoria (no una base de datos) de cuentas y movimientos.
 *
 * <p>Decision deliberada: el foco de esta actividad es el patron arquitectonico BFF -como se
 * personaliza y expone informacion a distintos clientes-, no la capa de persistencia. Un
 * {@code Map} en memoria, cargado una vez al iniciar la aplicacion (ver
 * {@link com.bancoxyz.bff.core.service.CargaDatosService}), cumple ese objetivo sin la
 * complejidad adicional de un motor de base de datos, y podria reemplazarse por un
 * {@code JpaRepository} real sin que ningun BFF se entere del cambio: los tres BFF solo conocen
 * el contrato HTTP expuesto por {@code CuentaInternalController}, nunca esta clase.</p>
 *
 * <p>Se usa {@link ConcurrentHashMap} porque los tres BFF pueden invocar a core-service de forma
 * concurrente (por ejemplo, el cajero automatico actualizando un saldo mientras la web consulta
 * otra cuenta), y {@code LinkedHashMap} normal no es thread-safe.</p>
 */
@Repository
public class CuentaRepository {

    private final Map<Long, Cuenta> cuentas = new ConcurrentHashMap<>();
    private final Map<Long, List<Movimiento>> movimientosPorCuenta = new ConcurrentHashMap<>();

    public void guardarCuenta(Cuenta cuenta) {
        cuentas.put(cuenta.getCuentaId(), cuenta);
    }

    public void agregarMovimiento(Movimiento movimiento) {
        movimientosPorCuenta
                .computeIfAbsent(movimiento.cuentaId(), id -> Collections.synchronizedList(new ArrayList<>()))
                .add(movimiento);
    }

    public Optional<Cuenta> buscarPorId(long cuentaId) {
        return Optional.ofNullable(cuentas.get(cuentaId));
    }

    public List<Cuenta> listarTodas() {
        return new ArrayList<>(new LinkedHashMap<>(cuentas).values());
    }

    public List<Movimiento> movimientosDe(long cuentaId) {
        return List.copyOf(movimientosPorCuenta.getOrDefault(cuentaId, List.of()));
    }

    public int totalCuentas() {
        return cuentas.size();
    }

    public int totalMovimientos() {
        return movimientosPorCuenta.values().stream().mapToInt(List::size).sum();
    }
}
