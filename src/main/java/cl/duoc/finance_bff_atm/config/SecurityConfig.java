package cl.duoc.finance_bff_atm.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import cl.duoc.finance_bff_atm.security.JwtFilter;

/**
 * Configuracion de seguridad de Spring Security para el BFF ATM.
 *
 * Define las politicas de acceso a los endpoints, la gestion de sesiones
 * (stateless para JWT), los usuarios autorizados y la integracion del
 * filtro JWT en la cadena de seguridad.
 *
 * Politica de acceso:
 * - /auth/login    -> Publico (permite obtener el token JWT)
 * - /bff/atm/v1/** -> Requiere rol CAJERO_AUT (operadores de cajero autorizados)
 * - Cualquier otro  -> Requiere autenticacion
 *
 * @author Duoc UC - Backend 3
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Filtro JWT personalizado que intercepta cada request para validar el token */
    @Autowired
    private JwtFilter jwtFilter;

    /**
     * Configura la cadena de filtros de seguridad HTTP.
     *
     * - CSRF deshabilitado: no se requiere porque la autenticacion es via tokens JWT,
     *   no mediante cookies de sesion.
     * - Sesiones STATELESS: cada request se autentica de forma independiente con su token.
     * - Se agrega el JwtFilter antes del filtro estandar de usuario/password de Spring.
     *
     * @param http configurador de seguridad HTTP de Spring
     * @return cadena de filtros de seguridad construida
     * @throws Exception si ocurre un error en la configuracion
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/login").permitAll()
                .requestMatchers("/bff/atm/v1/**").hasRole("CAJERO_AUT")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Almacen de usuarios en memoria para autenticacion.
     *
     * En este entorno de desarrollo se define un usuario de prueba con rol CAJERO_AUT.
     * En produccion, esto deberia reemplazarse por un proveedor de usuarios
     * respaldado por base de datos o un servicio de directorio (LDAP, OAuth2, etc.).
     *
     * Usuario de prueba:
     * - username: cajero_atm_01
     * - password: admin_atm
     * - rol: CAJERO_AUT
     *
     * @return gestor de usuarios en memoria con el usuario de prueba configurado
     */
    @Bean
    public InMemoryUserDetailsManager userDetailsService() {
        UserDetails user = User.withDefaultPasswordEncoder()
            .username("cajero_atm_01")
            .password("admin_atm")
            .roles("CAJERO_AUT")
            .build();
        return new InMemoryUserDetailsManager(user);
    }

    /**
     * Expone el AuthenticationManager como bean para poder inyectarlo
     * en el AuthController y autenticar las credenciales del login.
     *
     * @param config configuracion de autenticacion de Spring
     * @return instancia del AuthenticationManager
     * @throws Exception si ocurre un error al obtener el manager
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
