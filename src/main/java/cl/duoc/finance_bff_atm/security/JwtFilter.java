package cl.duoc.finance_bff_atm.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro de autenticacion JWT que intercepta cada peticion HTTP.
 *
 * Extiende OncePerRequestFilter para garantizar que se ejecuta una sola vez
 * por cada request. Su funcion es extraer el token JWT del header Authorization,
 * validarlo y establecer la autenticacion en el SecurityContext de Spring.
 *
 * Flujo del filtro:
 * 1. Extrae el header "Authorization: Bearer <token>"
 * 2. Decodifica el token y obtiene el username con JwtUtil
 * 3. Carga los datos del usuario desde el UserDetailsService
 * 4. Valida la integridad y expiracion del token
 * 5. Si es valido, establece la autenticacion en el SecurityContext
 * 6. Continua con la cadena de filtros hacia el controlador
 *
 * Nota: Se usa @Lazy en UserDetailsService para evitar una dependencia circular
 * con SecurityConfig, que define tanto el UserDetailsService como inyecta este filtro.
 *
 * @author Duoc UC - Backend 3
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    /** Utilidad para decodificar, validar y extraer datos del token JWT */
    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Servicio de usuarios de Spring Security.
     * Se inyecta con @Lazy para romper la dependencia circular:
     * JwtFilter -> UserDetailsService -> SecurityConfig -> JwtFilter
     */
    @Lazy
    @Autowired
    private UserDetailsService userDetailsService;

    /**
     * Metodo principal del filtro. Se ejecuta en cada request HTTP.
     *
     * Extrae y valida el token JWT del header Authorization.
     * Si el token es valido, autentica al usuario en el contexto de Spring Security
     * para que los endpoints protegidos permitan el acceso.
     *
     * @param request  peticion HTTP entrante
     * @param response respuesta HTTP saliente
     * @param chain    cadena de filtros para continuar el procesamiento
     * @throws ServletException si ocurre un error en el servlet
     * @throws IOException      si ocurre un error de I/O
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        String username = null;
        String jwt = null;

        // Paso 1: Extraer el token del header "Authorization: Bearer <token>"
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
            try {
                username = jwtUtil.extractUsername(jwt);
            } catch (Exception e) {
                // Token invalido, expirado o malformado - se ignora y el request continua sin autenticacion
                System.out.println("Error verificando token: " + e.getMessage());
            }
        }

        // Paso 2: Si se extrajo un username y no hay autenticacion previa en el contexto
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Cargar los datos del usuario (roles, permisos) desde el almacen configurado
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            // Paso 3: Validar que el token sea integro y no haya expirado
            if (jwtUtil.validateToken(jwt, userDetails.getUsername())) {

                // Crear el objeto de autenticacion con los authorities del usuario
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Paso 4: Establecer la autenticacion en el SecurityContext
                // A partir de aqui, Spring Security considera al usuario autenticado
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // Continuar con la cadena de filtros (siguiente filtro o el controlador)
        chain.doFilter(request, response);
    }
}
