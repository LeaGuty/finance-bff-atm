package cl.duoc.finance_bff_atm.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import cl.duoc.finance_bff_atm.security.JwtUtil;

/**
 * Controlador REST para la autenticacion de operadores de cajero ATM.
 *
 * Expone el endpoint publico POST /auth/login que recibe credenciales
 * (usuario y password), las valida contra Spring Security y, si son correctas,
 * genera y retorna un token JWT firmado con el rol del usuario.
 *
 * Flujo de autenticacion:
 * 1. El operador envia sus credenciales en el body (JSON).
 * 2. Spring Security valida contra el almacen de usuarios (InMemory).
 * 3. Si es valido, se genera un token JWT con username y rol.
 * 4. El token se retorna al cliente para usarlo en requests posteriores.
 *
 * @author Duoc UC - Backend 3
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    /** Manager de autenticacion de Spring Security para validar credenciales */
    @Autowired
    private AuthenticationManager authenticationManager;

    /** Utilidad para generar y firmar tokens JWT */
    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Endpoint de login para operadores del cajero ATM.
     *
     * Recibe un JSON con username y password, valida las credenciales
     * y retorna un token JWT si la autenticacion es exitosa.
     *
     * @param request objeto con las credenciales del operador (username, password)
     * @return ResponseEntity con el token JWT (200 OK) o mensaje de error (401 UNAUTHORIZED)
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            // Validar usuario y contraseña con Spring Security.
            // Busca en la configuracion InMemory definida en SecurityConfig.
            Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            // Obtener el primer rol/authority del usuario autenticado
            String role = auth.getAuthorities().iterator().next().getAuthority();

            // Generar el token JWT firmado con HS512, incluyendo username y rol
            String token = jwtUtil.generateToken(request.getUsername(), role);

            // Retornar el token al operador del cajero
            return ResponseEntity.ok(new LoginResponse(token));

        } catch (AuthenticationException e) {
            // Credenciales invalidas: usuario no existe o password incorrecto
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error: Credenciales inválidas");
        }
    }

    // ========================================================================================
    // DTOs internos para request/response del login
    // ========================================================================================

    /**
     * DTO para recibir las credenciales de login.
     * Formato JSON esperado: { "username": "cajero_atm_01", "password": "admin_atm" }
     */
    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    /**
     * DTO para la respuesta del login con el token JWT generado.
     * Formato JSON de respuesta: { "token": "eyJhbGciOiJIUzUxMiJ9..." }
     */
    public static class LoginResponse {
        private String token;

        public LoginResponse(String token) { this.token = token; }

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
    }
}
