package com.bancoxyz.bff.core.service;

import com.bancoxyz.bff.core.model.Cuenta;
import com.bancoxyz.bff.core.model.Movimiento;
import com.bancoxyz.bff.core.repository.CuentaRepository;
import com.bancoxyz.bff.core.util.FechaFlexibleParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Carga, al iniciar la aplicacion, los dos archivos legacy que este servicio consolida:
 * {@code data/intereses.csv} (maestro de cuentas) y {@code data/cuentas_anuales.csv} (historial
 * de movimientos), dejando el resultado disponible en {@link CuentaRepository}.
 *
 * <p>La validacion aplicada replica, deliberadamente, las mismas reglas de negocio ya probadas
 * en el proyecto Exp1 (migracion batch con Spring Batch) de este curso: un registro invalido
 * (tipo de cuenta no soportado, edad fuera de rango, saldo negativo, fecha irreconocible) se
 * omite y se deja constancia en el log, en vez de detener el arranque completo del servicio. La
 * diferencia frente a Exp1 es de mecanismo, no de criterio: alli las reglas vivian en un
 * {@code ItemProcessor} + {@code SkipPolicy} de Spring Batch; aqui, al no tratarse de un proceso
 * batch sino de la carga inicial de un servicio REST, se aplican directamente en un
 * {@link ApplicationRunner}.</p>
 *
 * <p>Al igual que en Exp1, {@code cuenta_id} se repite en {@code intereses.csv} (solo existen
 * ~50 identificadores distintos para 1000 filas): esta clase aplica el mismo criterio de
 * "upsert" (la ultima fila valida de un {@code cuenta_id} determina el estado final de esa
 * cuenta), consistente con el comportamiento ya documentado y verificado con evidencia real en
 * ese proyecto.</p>
 */
@Service
public class CargaDatosService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CargaDatosService.class);
    private static final Set<String> TIPOS_CUENTA_SOPORTADOS = Set.of("ahorro", "prestamo");

    private final CuentaRepository cuentaRepository;

    public CargaDatosService(CuentaRepository cuentaRepository) {
        this.cuentaRepository = cuentaRepository;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        int cuentasOmitidas = cargarCuentas();
        int movimientosOmitidos = cargarMovimientos();

        log.info("############################################################");
        log.info("core-service: datos cargados en memoria.");
        log.info("Cuentas validas cargadas: {} (omitidas por datos invalidos: {})",
                cuentaRepository.totalCuentas(), cuentasOmitidas);
        log.info("Movimientos validos cargados: {} (omitidos por datos invalidos: {})",
                cuentaRepository.totalMovimientos(), movimientosOmitidos);
        log.info("############################################################");
    }

    private int cargarCuentas() throws IOException {
        int omitidas = 0;
        try (BufferedReader lector = new BufferedReader(new InputStreamReader(
                new ClassPathResource("data/intereses.csv").getInputStream(), StandardCharsets.UTF_8))) {
            List<String> lineas = lector.lines().skip(1).toList();
            for (String linea : lineas) {
                String[] campos = linea.split(",", -1);
                if (campos.length < 5) {
                    omitidas++;
                    continue;
                }
                Optional<Cuenta> cuenta = validarCuenta(campos);
                if (cuenta.isPresent()) {
                    cuentaRepository.guardarCuenta(cuenta.get());
                } else {
                    omitidas++;
                }
            }
        }
        return omitidas;
    }

    private Optional<Cuenta> validarCuenta(String[] campos) {
        try {
            long cuentaId = Long.parseLong(campos[0].trim());
            String nombre = campos[1] == null ? "" : campos[1].trim();
            String saldoTexto = campos[2] == null ? "" : campos[2].trim();
            String edadTexto = campos[3] == null ? "" : campos[3].trim();
            String tipo = campos[4] == null ? "" : campos[4].trim().toLowerCase(Locale.ROOT);

            if (nombre.isBlank() || nombre.equalsIgnoreCase("unknown")) {
                return Optional.empty();
            }
            if (!TIPOS_CUENTA_SOPORTADOS.contains(tipo)) {
                return Optional.empty();
            }
            if (edadTexto.isBlank()) {
                return Optional.empty();
            }
            int edad = Integer.parseInt(edadTexto);
            if (edad < 18 || edad > 90) {
                return Optional.empty();
            }
            if (saldoTexto.isBlank()) {
                return Optional.empty();
            }
            double saldo = Double.parseDouble(saldoTexto);
            if (saldo < 0) {
                return Optional.empty();
            }
            return Optional.of(new Cuenta(cuentaId, nombre, tipo, edad, saldo));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private int cargarMovimientos() throws IOException {
        int omitidos = 0;
        try (BufferedReader lector = new BufferedReader(new InputStreamReader(
                new ClassPathResource("data/cuentas_anuales.csv").getInputStream(), StandardCharsets.UTF_8))) {
            List<String> lineas = lector.lines().skip(1).toList();
            for (String linea : lineas) {
                String[] campos = linea.split(",", -1);
                if (campos.length < 5) {
                    omitidos++;
                    continue;
                }
                Optional<Movimiento> movimiento = validarMovimiento(campos);
                if (movimiento.isPresent()) {
                    // Solo se conservan movimientos de cuentas que ya existen en el maestro
                    // cargado desde intereses.csv: un movimiento "huerfano" (cuenta_id sin
                    // titular/saldo conocido) no puede exponerse de forma consistente via API.
                    if (cuentaRepository.buscarPorId(movimiento.get().cuentaId()).isPresent()) {
                        cuentaRepository.agregarMovimiento(movimiento.get());
                    } else {
                        omitidos++;
                    }
                } else {
                    omitidos++;
                }
            }
        }
        return omitidos;
    }

    private Optional<Movimiento> validarMovimiento(String[] campos) {
        try {
            long cuentaId = Long.parseLong(campos[0].trim());
            Optional<LocalDate> fecha = FechaFlexibleParser.parsear(campos[1]);
            if (fecha.isEmpty()) {
                return Optional.empty();
            }
            String tipoMovimiento = campos[2] == null ? "" : campos[2].trim().toLowerCase(Locale.ROOT);
            if (tipoMovimiento.isBlank()) {
                return Optional.empty();
            }
            String montoTexto = campos[3] == null ? "" : campos[3].trim();
            if (montoTexto.isBlank()) {
                return Optional.empty();
            }
            double monto = Double.parseDouble(montoTexto);
            if (monto == 0) {
                return Optional.empty();
            }
            String descripcion = campos[4] == null || campos[4].isBlank()
                    ? "Movimiento sin descripcion registrada"
                    : campos[4].trim();
            return Optional.of(new Movimiento(cuentaId, fecha.get(), tipoMovimiento, monto, descripcion));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
