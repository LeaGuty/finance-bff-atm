# Finance BFF ATM

Microservicio Backend For Frontend (BFF) para cajeros automaticos (ATM) del sistema financiero.

## Descripcion

Este proyecto implementa una capa BFF que actua como intermediario entre la interfaz del cajero automatico y el microservicio backend de finanzas. Se encarga de:

- **Autenticacion JWT**: Login de operadores de cajero con generacion de tokens firmados (HS512).
- **Autorizacion por roles**: Solo operadores con rol `CAJERO_AUT` pueden acceder a los endpoints del ATM.
- **Enmascaramiento de datos**: Oculta el nombre del cliente por seguridad (ej: "Juan Perez" -> "J*** P****").
- **Orquestacion de servicios**: Consume el backend de finanzas y consolida la respuesta para el cajero.
- **HTTPS/TLS**: Comunicacion cifrada con certificado auto-firmado (PKCS12).

## Tecnologias

| Tecnologia | Version |
|---|---|
| Java | 21 |
| Spring Boot | 4.0.2 |
| Spring Security | (gestionado por Spring Boot) |
| JJWT (JSON Web Token) | 0.11.5 |
| Lombok | (gestionado por Spring Boot) |
| java-dotenv | 5.2.2 |
| Maven | Wrapper incluido |

## Estructura del Proyecto

```
src/main/java/cl/duoc/finance_bff_atm/
├── FinanceBffAtmApplication.java    # Clase principal y bean RestTemplate
├── config/
│   └── SecurityConfig.java          # Configuracion de Spring Security y usuarios
├── controller/
│   ├── AuthController.java          # Endpoint POST /auth/login
│   └── FinanceAtmController.java    # Endpoint GET /bff/atm/v1/saldo/{id}
├── model/
│   ├── MovimientoAtmDTO.java        # DTO de transaccion para voucher
│   └── ResumenCajeroDTO.java        # DTO de resumen de cuenta para ATM
├── security/
│   ├── JwtFilter.java               # Filtro de autenticacion JWT por request
│   └── JwtUtil.java                 # Generacion y validacion de tokens JWT
└── service/
    ├── FinanceAtmService.java        # Interfaz del servicio
    └── FinanceAtmServiceImpl.java    # Implementacion con logica de negocio
```

## Configuracion

### Requisitos Previos

- **Java 21** o superior
- **Maven 3.9+** (o usar el wrapper incluido `mvnw`)
- **Backend de finanzas** corriendo en `http://localhost:8080/api/v1`

### Variables de Entorno

Crear un archivo `.env` en la raiz del proyecto:

```env
JWT_SECRET=<clave_base64_para_HS512>
```

> La clave debe ser un string Base64 de al menos 64 bytes para cumplir con los requisitos de HS512.

### Propiedades de la Aplicacion

El servidor se ejecuta en el puerto **8083** con HTTPS habilitado:

```properties
server.port=8083
server.ssl.enabled=true
server.ssl.key-store=classpath:finance-keystore.p12
server.ssl.key-store-type=PKCS12
```

## Ejecucion

```bash
# Windows
mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

La aplicacion estara disponible en: `https://localhost:8083`

## Endpoints

### Autenticacion (Publico)

#### `POST /auth/login`

Autentica un operador de cajero y retorna un token JWT.

**Request:**
```json
{
  "username": "cajero_atm_01",
  "password": "admin_atm"
}
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9..."
}
```

**Response (401 Unauthorized):**
```
Error: Credenciales invalidas
```

### Operaciones ATM (Requiere JWT + Rol CAJERO_AUT)

#### `GET /bff/atm/v1/saldo/{id}`

Consulta el saldo y ultimos movimientos de una cuenta.

**Headers requeridos:**
```
Authorization: Bearer <token_jwt>
```

**Response (200 OK):**
```json
{
  "mensajeSistema": "Consulta de Saldo Exitosa. Retire su comprobante.",
  "nombreClienteEnmascarado": "J*** P****",
  "saldoActual": 150000.0,
  "ultimos3Movimientos": [
    {
      "fecha": "2026-02-09",
      "tipo": "Abono",
      "monto": 500000.0
    },
    {
      "fecha": "2026-02-08",
      "tipo": "Giro",
      "monto": 50000.0
    }
  ]
}
```

## Seguridad

- **JWT con HS512**: Tokens firmados con HMAC-SHA512, expiran en 30 minutos.
- **HTTPS/TLS**: Certificado PKCS12 auto-firmado para cifrado en transito.
- **Enmascaramiento**: Los nombres de clientes se ocultan en la respuesta del ATM.
- **Roles**: Acceso restringido por rol `CAJERO_AUT` mediante Spring Security.
- **Stateless**: Sin sesiones de servidor; cada request se autentica con su token.

## Usuario de Prueba

| Campo | Valor |
|---|---|
| Username | `cajero_atm_01` |
| Password | `admin_atm` |
| Rol | `CAJERO_AUT` |

> Este usuario esta definido en memoria (InMemoryUserDetailsManager) solo para desarrollo.

## Arquitectura

```
┌──────────┐     HTTPS      ┌──────────────┐      HTTP       ┌─────────────┐
│  Cajero  │ ──────────────> │  BFF ATM     │ ──────────────> │  Backend    │
│  ATM     │ <────────────── │  (este app)  │ <────────────── │  Finanzas   │
│          │   JWT + JSON    │  :8083       │   JWT + JSON    │  :8080      │
└──────────┘                 └──────────────┘                 └─────────────┘
```

## Autor

Duoc UC - Backend 3
