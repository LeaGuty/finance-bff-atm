package cl.duoc.finance_bff_atm.security;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import io.github.cdimascio.dotenv.Dotenv;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

/**
 * Utilidad para la generacion, firma y validacion de tokens JWT.
 *
 * Utiliza el algoritmo HS512 (HMAC-SHA512) para firmar los tokens.
 * La clave secreta se carga desde el archivo .env (variable JWT_SECRET)
 * utilizando la libreria java-dotenv, con un valor por defecto de respaldo.
 *
 * Estructura del token generado:
 * - Subject (sub): username del operador
 * - Claims personalizados: role (rol del usuario)
 * - Issued At (iat): fecha/hora de emision
 * - Expiration (exp): 30 minutos despues de la emision
 * - Firma: HS512 con la clave secreta
 *
 * @author Duoc UC - Backend 3
 */
@Component
public class JwtUtil {

    /** Carga las variables de entorno desde el archivo .env en la raiz del proyecto */
    private final Dotenv dotenv = Dotenv.configure().directory("./").ignoreIfMissing().load();

    /**
     * Clave secreta en Base64 para firmar tokens.
     * Se lee de la variable de entorno JWT_SECRET.
     * Si no existe, se utiliza un valor por defecto (solo para desarrollo).
     */
    private final String SECRET_KEY_STRING = dotenv.get("JWT_SECRET",
        "ZXN0YV9lc191bmFfY2xhdmVfbXV5X3NlZ3VyYV95X2xhcmdhX3BhcmFfY3VtcGxpcl9jb25fbG9zX3JlcXVpc2l0b3NfZGVfSFM1MTJfYmZmXzIwMjZfZHVvY19jaGlsZV9wYXJhX2VsX2V4YW1lbg==");

    /** Clave HMAC derivada de la clave secreta Base64, utilizada para firmar y verificar tokens */
    private final Key SECRET_KEY = Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET_KEY_STRING));

    /** Tiempo de expiracion del token: 30 minutos (en milisegundos) */
    private final long EXPIRATION_TIME = 1000 * 60 * 30;

    /**
     * Genera un token JWT con el username y rol del usuario.
     *
     * @param username nombre de usuario del operador autenticado
     * @param role     rol del usuario (ej: "ROLE_CAJERO_AUT")
     * @return token JWT firmado como String
     */
    public String generateToken(String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        return createToken(claims, username);
    }

    /**
     * Construye y firma el token JWT con los claims, subject y tiempos de expiracion.
     *
     * @param claims  mapa de claims personalizados (ej: role)
     * @param subject username que sera el subject del token
     * @return token JWT firmado con HS512
     */
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SECRET_KEY, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Valida un token JWT verificando que el username coincida y que no haya expirado.
     *
     * @param token    token JWT a validar
     * @param username username esperado para comparar con el subject del token
     * @return true si el token es valido y no ha expirado, false en caso contrario
     */
    public boolean validateToken(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }

    /**
     * Extrae el username (subject) del token JWT.
     *
     * @param token token JWT del cual extraer el username
     * @return username contenido en el subject del token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extrae el rol del usuario desde los claims personalizados del token.
     *
     * @param token token JWT del cual extraer el rol
     * @return rol del usuario (ej: "ROLE_CAJERO_AUT")
     */
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    /**
     * Extrae la fecha de expiracion del token JWT.
     *
     * @param token token JWT del cual extraer la expiracion
     * @return fecha de expiracion del token
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extrae un claim especifico del token usando una funcion resolutora.
     *
     * @param <T>            tipo del claim a extraer
     * @param token          token JWT fuente
     * @param claimsResolver funcion que extrae el claim deseado del objeto Claims
     * @return valor del claim extraido
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Decodifica el token JWT y retorna todos los claims.
     * Verifica la firma del token con la clave secreta.
     *
     * @param token token JWT a decodificar
     * @return objeto Claims con todos los datos del token
     * @throws io.jsonwebtoken.JwtException si el token es invalido o la firma no coincide
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(SECRET_KEY).build().parseClaimsJws(token).getBody();
    }

    /**
     * Verifica si el token JWT ha expirado comparando su fecha de expiracion con la actual.
     *
     * @param token token JWT a verificar
     * @return true si el token ya expiro, false si aun es valido
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
}
