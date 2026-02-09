package cl.duoc.finance_bff_atm.service;

import cl.duoc.finance_bff_atm.model.MovimientoAtmDTO;
import cl.duoc.finance_bff_atm.model.ResumenCajeroDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity; // <--- NUEVO
import org.springframework.http.HttpHeaders; // <--- NUEVO
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder; // <--- NUEVO
import org.springframework.web.context.request.ServletRequestAttributes; // <--- NUEVO

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FinanceAtmServiceImpl implements FinanceAtmService {

    @Autowired
    private RestTemplate restTemplate;

    private final String BACKEND_URL = "http://localhost:8080/api/v1";

    // --- MÉTODO AUXILIAR ---
    private HttpHeaders getHeadersConToken() {
        HttpHeaders headers = new HttpHeaders();
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            String authHeader = attributes.getRequest().getHeader("Authorization");
            if (authHeader != null) {
                headers.set("Authorization", authHeader);
            }
        }
        return headers;
    }

    @Override
    public ResumenCajeroDTO consultarSaldoSeguro(Long id) {
        ResumenCajeroDTO resumen = new ResumenCajeroDTO();

        try {
            HttpEntity<String> entity = new HttpEntity<>(getHeadersConToken());

            // 1. Obtener datos de la cuenta (Map)
            String urlCuenta = BACKEND_URL + "/cuentas/" + id;
            
            // Ojo: Map.class aquí
            ResponseEntity<Map> responseCuenta = restTemplate.exchange(
                urlCuenta, 
                HttpMethod.GET, 
                entity, 
                Map.class
            );
            
            Map<String, Object> cuentaData = responseCuenta.getBody();

            if (cuentaData != null) {
                String nombreReal = (String) cuentaData.get("nombre");
                // Convertir saldo de forma segura
                Double saldo = 0.0;
                if (cuentaData.get("saldo") instanceof Number) {
                    saldo = ((Number) cuentaData.get("saldo")).doubleValue();
                }
                
                resumen.setNombreClienteEnmascarado(enmascararNombre(nombreReal));
                resumen.setSaldoActual(saldo);
            }

            // 2. Obtener movimientos
            String urlMovs = BACKEND_URL + "/cuentas/" + id + "/transacciones";
            ResponseEntity<List<MovimientoAtmDTO>> responseMovs = restTemplate.exchange(
                urlMovs,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<MovimientoAtmDTO>>() {}
            );

            List<MovimientoAtmDTO> movimientos = responseMovs.getBody();
            if (movimientos != null) {
                List<MovimientoAtmDTO> miniEstado = movimientos.stream()
                    .sorted(Comparator.comparing(MovimientoAtmDTO::getFecha).reversed())
                    .limit(3)
                    .collect(Collectors.toList());
                resumen.setUltimos3Movimientos(miniEstado);
            }

            resumen.setMensajeSistema("Consulta de Saldo Exitosa. Retire su comprobante.");

        } catch (Exception e) {
            resumen.setMensajeSistema("Error de operación o seguridad: " + e.getMessage());
            resumen.setNombreClienteEnmascarado("Unknown");
            resumen.setSaldoActual(0.0);
            resumen.setUltimos3Movimientos(Collections.emptyList());
        }

        return resumen;
    }

    private String enmascararNombre(String nombreReal) {
        if (nombreReal == null || nombreReal.isEmpty()) return "****";
        String[] partes = nombreReal.split(" ");
        StringBuilder enmascarado = new StringBuilder();
        for (String parte : partes) {
            if (parte.length() > 1) {
                enmascarado.append(parte.charAt(0));
                enmascarado.append("*".repeat(parte.length() - 1));
            } else {
                enmascarado.append(parte);
            }
            enmascarado.append(" ");
        }
        return enmascarado.toString().trim();
    }
}