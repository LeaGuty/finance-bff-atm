package cl.duoc.finance_bff_atm.service;

import cl.duoc.finance_bff_atm.model.MovimientoAtmDTO;
import cl.duoc.finance_bff_atm.model.ResumenCajeroDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementacion del servicio BFF ATM con Tolerancia a Fallos (Resilience4j).
 *
 * @author Duoc UC - Backend 3
 */
@Service
public class FinanceAtmServiceImpl implements FinanceAtmService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private cl.duoc.finance_bff_atm.security.JwtUtil jwtUtil;

    @Value("${backend.url:http://localhost:8080/api/v1}")
    private String BACKEND_URL; 

    private HttpHeaders getHeadersConToken() {
        HttpHeaders headers = new HttpHeaders();
        try {
            String usernameInternoBff = "cajero_atm_01";
            String tokenInterno = jwtUtil.generateToken(usernameInternoBff, "ROLE_CAJERO_AUT");
            headers.set("Authorization", "Bearer " + tokenInterno);
        } catch (Exception e) {
            System.err.println("Error generando el token relay para el Core: " + e.getMessage());
        }
        return headers;
    }

    /**
     * Consulta el saldo al backend principal.
     * Si el backend principal falla, Resilience4j atrapará la excepción
     * y ejecutará automáticamente el método 'fallbackConsultarSaldo'.
     */
    @Override
    @CircuitBreaker(name = "financeCore", fallbackMethod = "fallbackConsultarSaldo")
    public ResumenCajeroDTO consultarSaldoSeguro(Long id) {
        
        // ¡Ojo! Ya no hay try-catch aquí. 
        // Dejamos que falle intencionalmente si el Core está caído para que el cortacircuitos actúe.
        HttpEntity<String> entity = new HttpEntity<>(getHeadersConToken());
        ResumenCajeroDTO resumen = new ResumenCajeroDTO();

        // 1. Obtener datos de la cuenta
        String urlCuenta = BACKEND_URL + "/cuentas/" + id;
        ResponseEntity<Map> responseCuenta = restTemplate.exchange(urlCuenta, HttpMethod.GET, entity, Map.class);
        Map<String, Object> cuentaData = responseCuenta.getBody();

        if (cuentaData != null) {
            String nombreReal = (String) cuentaData.get("nombre");
            Double saldo = 0.0;
            if (cuentaData.get("saldo") instanceof Number) {
                saldo = ((Number) cuentaData.get("saldo")).doubleValue();
            }
            resumen.setNombreClienteEnmascarado(enmascararNombre(nombreReal));
            resumen.setSaldoActual(saldo);
        }

        // 2. Obtener las transacciones
        String urlMovs = BACKEND_URL + "/cuentas/" + id + "/transacciones";
        ResponseEntity<List<MovimientoAtmDTO>> responseMovs = restTemplate.exchange(
                urlMovs, HttpMethod.GET, entity, new ParameterizedTypeReference<List<MovimientoAtmDTO>>() {});

        List<MovimientoAtmDTO> movimientos = responseMovs.getBody();
        if (movimientos != null) {
            List<MovimientoAtmDTO> miniEstado = movimientos.stream()
                    .sorted(Comparator.comparing(MovimientoAtmDTO::getFecha).reversed())
                    .limit(3)
                    .collect(Collectors.toList());
            resumen.setUltimos3Movimientos(miniEstado);
        }

        resumen.setMensajeSistema("Consulta de Saldo Exitosa. Retire su comprobante.");
        return resumen;
    }

    /**
     * MÉTODO DE RESPALDO (FALLBACK)
     * Se ejecuta automáticamente cuando el 'finance-batch' está caído o tarda demasiado.
     */
    public ResumenCajeroDTO fallbackConsultarSaldo(Long id, Throwable t) {
        System.err.println("¡Circuit Breaker activado! Falló la comunicación con el Core: " + t.getMessage());
        
        ResumenCajeroDTO resumenFallback = new ResumenCajeroDTO();
        resumenFallback.setMensajeSistema("Servicios temporalmente no disponibles. Por favor, intente más tarde.");
        resumenFallback.setNombreClienteEnmascarado("Información no disponible");
        resumenFallback.setSaldoActual(0.0);
        resumenFallback.setUltimos3Movimientos(Collections.emptyList());
        
        return resumenFallback;
    }

    private String enmascararNombre(String nombreReal) {
        if (nombreReal == null || nombreReal.isEmpty()) return "****";
        String[] partes = nombreReal.split(" ");
        StringBuilder enmascarado = new StringBuilder();
        for (String parte : partes) {
            if (parte.length() > 1) {
                enmascarado.append(parte.charAt(0)).append("*".repeat(parte.length() - 1));
            } else {
                enmascarado.append(parte);
            }
            enmascarado.append(" ");
        }
        return enmascarado.toString().trim();
    }
}