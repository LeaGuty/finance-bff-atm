package cl.duoc.finance_bff_atm.service;

import cl.duoc.finance_bff_atm.model.MovimientoAtmDTO;
import cl.duoc.finance_bff_atm.model.ResumenCajeroDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementacion del servicio BFF ATM.
 *
 * Orquesta las llamadas al microservicio backend de finanzas
 * (http://localhost:8080/api/v1)
 * y transforma las respuestas aplicando logica de seguridad y formato para el
 * cajero ATM.
 *
 * Responsabilidades:
 * - Propagar el token JWT del request original al backend (pass-through de
 * autorizacion)
 * - Consultar datos de cuenta y transacciones desde el backend
 * - Enmascarar el nombre del cliente por seguridad (ej: "Juan Perez" -> "J***
 * P****")
 * - Limitar los movimientos a los 3 mas recientes para impresion en voucher
 * - Manejar errores de comunicacion con el backend de forma graceful
 *
 * @author Duoc UC - Backend 3
 */
@Service
public class FinanceAtmServiceImpl implements FinanceAtmService {

    /** RestTemplate para realizar llamadas HTTP al backend principal */
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private cl.duoc.finance_bff_atm.security.JwtUtil jwtUtil;

    /** URL base del microservicio backend de finanzas */
    private final String BACKEND_URL = "http://localhost:8080/api/v1";

    /**
     * Extrae el header Authorization del request HTTP actual y lo prepara
     * para reenviarlo al backend. Esto permite propagar el token JWT
     * del operador del cajero al microservicio de finanzas (pass-through).
     *
     * @return HttpHeaders con el header Authorization del request original
     */
    private HttpHeaders getHeadersConToken() {
        HttpHeaders headers = new HttpHeaders();

        try {
            // 1. Obtener el usuario autenticado con GitHub desde el contexto de seguridad
            org.springframework.security.oauth2.core.user.OAuth2User oauthUser = (org.springframework.security.oauth2.core.user.OAuth2User) org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication().getPrincipal();

            // 2. Extraer el nombre de usuario de GitHub para log o trazabilidad (opcional)
            String githubUser = oauthUser.getAttribute("login");

            // 3. Generar el JWT para el backend.
            // Mapeamos a "cajero_atm_01" porque el microservicio finance-batch utiliza
            // un InMemoryUserDetailsManager que solo conoce este usuario para el rol ATM.
            String usernameInternoBff = "cajero_atm_01";
            String tokenInterno = jwtUtil.generateToken(usernameInternoBff, "ROLE_CAJERO_AUT");

            // 4. Inyectarlo en el Header con el prefijo Bearer
            headers.set("Authorization", "Bearer " + tokenInterno);

        } catch (Exception e) {
            System.err.println("Error generando el token relay para el Core: " + e.getMessage());
        }

        return headers;
    }

    /**
     * {@inheritDoc}
     *
     * Realiza dos llamadas al backend:
     * 1. Obtiene los datos de la cuenta (nombre, saldo) desde /cuentas/{id}
     * 2. Obtiene las transacciones desde /cuentas/{id}/transacciones
     *
     * Luego aplica transformaciones de seguridad (enmascaramiento de nombre)
     * y formato (ultimos 3 movimientos ordenados por fecha descendente).
     *
     * En caso de error (backend no disponible, cuenta no existe, etc.),
     * retorna un DTO con mensaje de error y valores por defecto.
     */
    @Override
    public ResumenCajeroDTO consultarSaldoSeguro(Long id) {
        ResumenCajeroDTO resumen = new ResumenCajeroDTO();

        try {
            HttpEntity<String> entity = new HttpEntity<>(getHeadersConToken());

            // Llamada 1: Obtener datos de la cuenta (nombre del titular y saldo)
            String urlCuenta = BACKEND_URL + "/cuentas/" + id;
            ResponseEntity<Map> responseCuenta = restTemplate.exchange(
                    urlCuenta,
                    HttpMethod.GET,
                    entity,
                    Map.class);

            Map<String, Object> cuentaData = responseCuenta.getBody();

            if (cuentaData != null) {
                String nombreReal = (String) cuentaData.get("nombre");

                // Conversion segura del saldo: el backend puede retornar Integer o Double
                Double saldo = 0.0;
                if (cuentaData.get("saldo") instanceof Number) {
                    saldo = ((Number) cuentaData.get("saldo")).doubleValue();
                }

                // Enmascarar el nombre del cliente por seguridad ATM
                resumen.setNombreClienteEnmascarado(enmascararNombre(nombreReal));
                resumen.setSaldoActual(saldo);
            }

            // Llamada 2: Obtener las transacciones de la cuenta
            String urlMovs = BACKEND_URL + "/cuentas/" + id + "/transacciones";
            ResponseEntity<List<MovimientoAtmDTO>> responseMovs = restTemplate.exchange(
                    urlMovs,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<MovimientoAtmDTO>>() {
                    });

            List<MovimientoAtmDTO> movimientos = responseMovs.getBody();
            if (movimientos != null) {
                // Ordenar por fecha descendente y tomar solo los 3 mas recientes
                // para impresion de mini-estado de cuenta en voucher
                List<MovimientoAtmDTO> miniEstado = movimientos.stream()
                        .sorted(Comparator.comparing(MovimientoAtmDTO::getFecha).reversed())
                        .limit(3)
                        .collect(Collectors.toList());
                resumen.setUltimos3Movimientos(miniEstado);
            }

            resumen.setMensajeSistema("Consulta de Saldo Exitosa. Retire su comprobante.");

        } catch (Exception e) {
            // Error de comunicacion con el backend o datos invalidos.
            // Se retorna un DTO con valores por defecto y el mensaje de error.
            resumen.setMensajeSistema("Error de operación o seguridad: " + e.getMessage());
            resumen.setNombreClienteEnmascarado("Unknown");
            resumen.setSaldoActual(0.0);
            resumen.setUltimos3Movimientos(Collections.emptyList());
        }

        return resumen;
    }

    /**
     * Enmascara el nombre del cliente por seguridad.
     * Reemplaza todos los caracteres de cada palabra excepto el primero con
     * asteriscos.
     *
     * Ejemplo: "Juan Perez" -> "J*** P****"
     *
     * @param nombreReal nombre completo del cliente
     * @return nombre enmascarado o "****" si el nombre es nulo o vacio
     */
    private String enmascararNombre(String nombreReal) {
        if (nombreReal == null || nombreReal.isEmpty())
            return "****";
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
