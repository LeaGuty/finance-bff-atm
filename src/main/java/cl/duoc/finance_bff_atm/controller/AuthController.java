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

@RestController
@RequestMapping("/auth")
public class AuthController {


    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil; 

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            // 1. Validar usuario y contraseña con Spring Security
            // Esto buscará en la configuración "InMemory" que definimos en SecurityConfig
            Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            // 2. Si la autenticación es exitosa, obtenemos el rol del usuario
            // (Asumimos que tiene al menos un rol)
            String role = auth.getAuthorities().iterator().next().getAuthority();

            // 3. Generamos el Token JWT firmado
            String token = jwtUtil.generateToken(request.getUsername(), role);

            // 4. Devolvemos el token al cliente
            return ResponseEntity.ok(new LoginResponse(token));

        } catch (AuthenticationException e) {
            // Si el usuario o contraseña están mal
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error: Credenciales inválidas");
        }
    }

    // --- CLASES DTO AUXILIARES (Para recibir y enviar datos) ---
    
    // Para recibir el JSON: { "username": "...", "password": "..." }
    public static class LoginRequest {
        private String username;
        private String password;

        // Getters y Setters necesarios
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    // Para responder el JSON: { "token": "eyJhbG..." }
    public static class LoginResponse {
        private String token;

        public LoginResponse(String token) { this.token = token; }
        
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
    }
}