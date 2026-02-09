package cl.duoc.finance_bff_atm.service;

import cl.duoc.finance_bff_atm.model.MovimientoAtmDTO;
import cl.duoc.finance_bff_atm.model.ResumenCajeroDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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

    @Override
    public ResumenCajeroDTO consultarSaldoSeguro(Long id) {
        ResumenCajeroDTO resumen = new ResumenCajeroDTO();

        try {
            // 1. Obtener datos de la cuenta (Usamos Map o un DTO temporal interno para no crear más clases)
            // Aquí pedimos el objeto completo al Core, pero solo usaremos lo que necesitamos.
            String urlCuenta = BACKEND_URL + "/cuentas/" + id;
            Map<String, Object> cuentaData = restTemplate.getForObject(urlCuenta, Map.class);

            if (cuentaData != null) {
                String nombreReal = (String) cuentaData.get("nombre");
                Double saldo = Double.valueOf(cuentaData.get("saldo").toString());

                // APLICAR SEGURIDAD
                resumen.setNombreClienteEnmascarado(enmascararNombre(nombreReal));
                resumen.setSaldoActual(saldo);
            }

            // 2. Obtener movimientos para el "Mini-Estado" (Voucher)
            String urlMovs = BACKEND_URL + "/cuentas/" + id + "/transacciones";
            ResponseEntity<List<MovimientoAtmDTO>> response = restTemplate.exchange(
                urlMovs,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<MovimientoAtmDTO>>() {}
            );

            List<MovimientoAtmDTO> movimientos = response.getBody();
            if (movimientos != null) {
                // Solo los últimos 3 para operación rápida de cajero
                List<MovimientoAtmDTO> miniEstado = movimientos.stream()
                    .sorted(Comparator.comparing(MovimientoAtmDTO::getFecha).reversed()) // Más reciente primero
                    .limit(3)
                    .collect(Collectors.toList());
                resumen.setUltimos3Movimientos(miniEstado);
            }

            resumen.setMensajeSistema("Consulta de Saldo Exitosa. Retire su comprobante.");

        } catch (Exception e) {
            resumen.setMensajeSistema("Error en operación: " + e.getMessage());
            resumen.setNombreClienteEnmascarado("Unknown");
            resumen.setSaldoActual(0.0);
            resumen.setUltimos3Movimientos(Collections.emptyList());
        }

        return resumen;
    }

    // --- MÉTODOS PRIVADOS DE SEGURIDAD ---

    private String enmascararNombre(String nombreReal) {
        if (nombreReal == null || nombreReal.isEmpty()) return "****";
        
        String[] partes = nombreReal.split(" ");
        StringBuilder enmascarado = new StringBuilder();

        for (String parte : partes) {
            if (parte.length() > 1) {
                // Deja la primera letra visible y reemplaza el resto con '*'
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