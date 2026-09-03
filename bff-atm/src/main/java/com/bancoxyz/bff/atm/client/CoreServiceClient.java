package com.bancoxyz.bff.atm.client;

import com.bancoxyz.bff.atm.config.CoreServiceProperties;
import com.bancoxyz.bff.atm.exception.CuentaNoEncontradaException;
import com.bancoxyz.bff.atm.exception.SaldoInsuficienteException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class CoreServiceClient {

    private final RestTemplate restTemplate;
    private final CoreServiceProperties propiedades;

    public CoreServiceClient(RestTemplate restTemplate, CoreServiceProperties propiedades) {
        this.restTemplate = restTemplate;
        this.propiedades = propiedades;
    }

    public CuentaCoreDTO obtenerCuenta(long cuentaId) {
        try {
            var respuesta = restTemplate.exchange(
                    propiedades.getBaseUrl() + "/internal/cuentas/{cuentaId}",
                    HttpMethod.GET,
                    new HttpEntity<>(cabecerasInternas()),
                    CuentaCoreDTO.class,
                    cuentaId);
            return respuesta.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            throw new CuentaNoEncontradaException(cuentaId);
        }
    }

    /**
     * Confirma un retiro debitando el saldo en el backend generalizado (fuente unica de verdad
     * del dinero disponible). La validacion real de "saldo suficiente" ocurre en core-service,
     * no aqui: este BFF solo aplica sus propias reglas de canal (limite maximo por operacion,
     * ver {@code LimiteRetiroExcedidoException}) antes de reenviar la solicitud.
     */
    public CuentaCoreDTO debitarSaldo(long cuentaId, double monto) {
        try {
            var respuesta = restTemplate.exchange(
                    propiedades.getBaseUrl() + "/internal/cuentas/{cuentaId}/debito",
                    HttpMethod.POST,
                    new HttpEntity<>(Map.of("monto", monto), cabecerasInternas()),
                    CuentaCoreDTO.class,
                    cuentaId);
            return respuesta.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            throw new CuentaNoEncontradaException(cuentaId);
        } catch (HttpClientErrorException.Conflict e) {
            throw new SaldoInsuficienteException("Saldo insuficiente para realizar el retiro solicitado.");
        }
    }

    private HttpHeaders cabecerasInternas() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Api-Key", propiedades.getApiKey());
        return headers;
    }
}
