package com.bancoxyz.bff.core.controller;

import com.bancoxyz.bff.core.dto.ActualizarSaldoRequest;
import com.bancoxyz.bff.core.dto.CuentaInternalDTO;
import com.bancoxyz.bff.core.exception.CuentaNoEncontradaException;
import com.bancoxyz.bff.core.exception.SaldoInsuficienteException;
import com.bancoxyz.bff.core.model.Cuenta;
import com.bancoxyz.bff.core.repository.CuentaRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API interna (no destinada a ningun frontend) que expone el modelo de dominio COMPLETO,
 * sin ninguna personalizacion. Cada BFF decide, a partir de esta misma respuesta, que
 * subconjunto de datos reenviar a su canal.
 *
 * <p>Protegida por {@link com.bancoxyz.bff.core.config.InternalApiKeyFilter}: toda peticion debe
 * incluir el encabezado {@code X-Internal-Api-Key}.</p>
 */
@RestController
@RequestMapping("/internal/cuentas")
public class CuentaInternalController {

    private static final Logger log = LoggerFactory.getLogger(CuentaInternalController.class);

    private final CuentaRepository cuentaRepository;

    public CuentaInternalController(CuentaRepository cuentaRepository) {
        this.cuentaRepository = cuentaRepository;
    }

    @GetMapping
    public List<CuentaInternalDTO> listar() {
        return cuentaRepository.listarTodas().stream()
                .map(cuenta -> CuentaInternalDTO.desde(cuenta, cuentaRepository.movimientosDe(cuenta.getCuentaId())))
                .toList();
    }

    @GetMapping("/{cuentaId}")
    public CuentaInternalDTO obtener(@PathVariable long cuentaId) {
        Cuenta cuenta = cuentaRepository.buscarPorId(cuentaId)
                .orElseThrow(() -> new CuentaNoEncontradaException(cuentaId));
        return CuentaInternalDTO.desde(cuenta, cuentaRepository.movimientosDe(cuentaId));
    }

    /**
     * Debita saldo de una cuenta. Usado unicamente por bff-atm al confirmar un retiro. Se
     * valida aqui, en el backend generalizado (fuente unica de verdad del saldo), y no en el
     * BFF, precisamente para que sea imposible que un BFF distinto (o una version futura del
     * mismo BFF con un bug) deje una cuenta en saldo negativo saltandose la regla de negocio.
     *
     * <p>Se modela como {@code POST /debito} (una accion) y no como {@code PATCH /saldo} (una
     * actualizacion parcial de recurso) de forma deliberada: {@code RestTemplate}, con la
     * fabrica HTTP por defecto basada en {@code HttpURLConnection} de la JDK, no soporta el
     * verbo PATCH (limitacion documentada de la propia JDK, no de Spring). Usar POST evita esa
     * limitacion sin necesidad de agregar Apache HttpClient solo para poder emitir un PATCH.</p>
     */
    @PostMapping("/{cuentaId}/debito")
    public CuentaInternalDTO debitarSaldo(@PathVariable long cuentaId, @Valid @RequestBody ActualizarSaldoRequest request) {
        Cuenta cuenta = cuentaRepository.buscarPorId(cuentaId)
                .orElseThrow(() -> new CuentaNoEncontradaException(cuentaId));
        double monto = request.monto();
        if (cuenta.getSaldo() < monto) {
            throw new SaldoInsuficienteException(cuentaId, cuenta.getSaldo(), monto);
        }
        cuenta.setSaldo(cuenta.getSaldo() - monto);
        log.info("Cuenta {}: debito de {} aplicado. Nuevo saldo: {}", cuentaId, monto, cuenta.getSaldo());
        return CuentaInternalDTO.desde(cuenta, cuentaRepository.movimientosDe(cuentaId));
    }
}
