package cl.duoc.finance_bff_atm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

/**
 * Clase principal de la aplicacion Finance BFF ATM.
 *
 * Este microservicio actua como BFF (Backend For Frontend) para cajeros automaticos (ATM).
 * Se encarga de orquestar las llamadas al backend principal de finanzas, aplicar logica
 * de seguridad (JWT), enmascarar datos sensibles y formatear las respuestas para
 * la interfaz del cajero automatico.
 *
 * Tecnologias: Spring Boot 4.0.2, Spring Security, JWT (JJWT), HTTPS/TLS.
 * Puerto por defecto: 8083 (HTTPS).
 *
 * @author Duoc UC - Backend 3
 */
@SpringBootApplication
public class FinanceBffAtmApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceBffAtmApplication.class, args);
    }

    /**
     * Bean de RestTemplate para realizar llamadas HTTP al backend principal.
     * Se utiliza en la capa de servicio para consumir los endpoints REST
     * del microservicio de finanzas (cuentas y transacciones).
     *
     * @return instancia de RestTemplate configurada por defecto
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
