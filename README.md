# Enanos y camellos

Sistema de información para la liga de carreras de enanos y camellos. Backend en Spring Boot expuesto como REST API, autenticación delegada a Keycloak, base de datos PostgreSQL, y una interfaz web en React que consume la API.

---

## Integrantes del equipo

- Santiago Cardozo
- Santiago Toro

---

## Arquitectura

```
HTTP Request
   ↓
Controller      → recibe la petición, valida forma (sin lógica de negocio, sin try/catch)
   ↓
Service         → reglas de negocio, transacciones
   ↓
Repository      → acceso a datos (Spring Data JPA / Hibernate)
   ↓
Entity          → tabla en la base de datos
```

Entre controller y entidad hay dos piezas de apoyo en cada módulo:

- **DTOs** (`*Request`, `*Response`, `*UpdateRequest`, `*PatchRequest`, `*SummaryResponse`): separan el contrato de la API de las entidades JPA. 
- **Mapper** (`CompetitorMapper`, `TeamMapper`, etc.): clase que traduce entre Entity y DTO.

---

## Tecnologías utilizadas

**Backend**
- Java 25
- Spring Boot — Spring Web, Spring Data JPA, Spring Validation, Spring Security
- Lombok
- springdoc-openapi (Swagger UI + `/v3/api-docs`)
- Gradle (wrapper incluido, `./gradlew`)

**Base de datos**
- PostgreSQL 15 (contenedor Docker) como base persistente
- H2 en memoria, solo para tests automatizados (`application-test.yml`)

**Autenticación y autorización**
- Keycloak (contenedor Docker) como servidor OAuth2/OIDC — el backend es un *resource server* puro: no emite tokens ni guarda contraseñas, solo valida el JWT que llega

**Frontend**
- React + TypeScript
- Vite (build tool)
- React Router

**Testing**
- JUnit 5
- Mockito
- Spring `MockMvc` (`@WebMvcTest`)
- `@DataJpaTest` con H2

**Infraestructura**
- Docker y Docker Compose
- Volumen nombrado para persistencia de Postgres

---

## Modelo de base de datos

<img width="5080" height="2984" alt="image" src="https://github.com/user-attachments/assets/6a191319-28d4-4aeb-bc83-33bb6d160356" />

---

## Estrategia de seguridad

El backend es un **resource server** OAuth2/OIDC: Keycloak emite y firma los JWT; Spring Security solo los valida (firma, emisor, vencimiento) contra el `jwk-set-uri` y el `issuer-uri` configurados.

- `SecurityFilterChain` sin sesión (`STATELESS`) y sin CSRF (no aplica: no hay cookies, la identidad viaja en el header `Authorization: Bearer <token>` de cada request).
- Reglas por ruta, evaluadas en orden:
  1. Rutas públicas (Swagger, health checks) → sin autenticación.
  2. `GET /api/**` → cualquier rol autenticado (`VIEWER`, `RACE_ORGANIZER`, `ADMINISTRATOR`).
  3. Cualquier otro verbo en `/api/**` → `RACE_ORGANIZER` o `ADMINISTRATOR`.
- Un `JwtAuthenticationConverter` traduce los roles del claim `realm_access.roles` de Keycloak (ej. `"administrator"`) al formato que Spring Security espera (`ROLE_ADMINISTRATOR`).
- `401` (no autenticado) y `403` (autenticado sin permiso) se manejan con un `AuthenticationEntryPoint`/`AccessDeniedHandler` a medida, para que el cuerpo del error tenga el mismo formato JSON que el resto de errores de la API.
- El cliente de Swagger usa Authorization Code + PKCE (sin client secret), configurado en `OpenApiConfig` — el botón "Authorize" de Swagger UI abre el login real de Keycloak.


## Roles y permisos

| Rol | Permisos previstos por el enunciado |
|---|---|
| **Administrator** | Gestiona usuarios, competidores, equipos, carreras, inscripciones, resultados y el log de auditoría. |
| **Race Organizer** | Gestiona carreras, inscripciones y resultados; solo **ve** competidores y equipos. |
| **Viewer** | Solo lectura: información pública, calendario, resultados y clasificaciones. |

---

## Instalación y ejecución

### Backend (local, sin Docker)

```bash
# 1. Levantar solo la base de datos y Keycloak
docker compose up -d db keycloak

# 2. Correr el backend desde el wrapper de Gradle
./gradlew bootRun
```

Sin `SPRING_PROFILES_ACTIVE` seteado, usa el perfil `dev` por defecto (Postgres/Keycloak en `localhost`, `ddl-auto: update`).

### Frontend (local, sin Docker)

```bash
cd frontend
npm install
npm run dev
```


### Todo con Docker

```bash
docker compose up -d
```

Para bajar todo (conservando los datos de Postgres):
```bash
docker compose down
```

Para bajar todo y además borrar los datos:
```bash
docker compose down -v
```

---

## Variables de entorno

Copiar `.env.template` a `.env` y completar:

| Variable | Descripción | Default |
|---|---|---|
| `DB_NAME` | Nombre de la base de datos | — |
| `DB_USER` | Usuario de Postgres | — |
| `DB_PASSWORD` | Contraseña de Postgres | — |
| `DB_PORT` | Puerto de Postgres publicado al host | `5432` |
| `KEYCLOAK_REALM` | Nombre del realm importado | — |
| `KEYCLOAK_CLIENT_ID` | Client id público (Swagger, PKCE) | — |
| `KEYCLOAK_ADMIN_USER` | Usuario admin de la **consola** de Keycloak | — |
| `KEYCLOAK_ADMIN_PASSWORD` | Contraseña admin de la consola de Keycloak | — |
| `KEYCLOAK_PORT` | Puerto de Keycloak publicado al host | `8180` |
| `APP_PORT` | Puerto del backend publicado al host | `8080` |
| `FRONTEND_PORT` | Puerto del frontend publicado al host | `8082` |

---

## URLs y puertos de cada componente

| Componente | URL |
|---|---|
| API backend | `http://localhost:${APP_PORT}` (default `8080`) |
| Swagger UI | `http://localhost:${APP_PORT}/swagger-ui.html` |
| Contrato OpenAPI (JSON) | `http://localhost:${APP_PORT}/v3/api-docs` |
| Frontend | `http://localhost:${FRONTEND_PORT}` (default `8082`) |
| Consola admin de Keycloak | `http://localhost:${KEYCLOAK_PORT}/admin` (default `8180`) |
| Login/token de Keycloak | `http://localhost:${KEYCLOAK_PORT}/realms/${KEYCLOAK_REALM}` |
| PostgreSQL (cliente SQL externo) | `localhost:${DB_PORT}` (default `5432`) |

---

## Testing

El proyecto sigue una pirámide de testing con un tipo de test especializado por capa (se repite por módulo: Competitor, Team, etc.):

| Test | Qué es real | Qué está mockeado | Qué verifica |
|---|---|---|---|
| `*MapperTest` | Todo (sin Spring) | Nada | Transformación Entity ↔ DTO |
| `*ServiceTest` | El Service real | Los repositorios (Mockito) | Reglas de negocio: 400/404/409, flujos completos |
| `*ControllerTest` (`@WebMvcTest`) | Capa web (rutas, `@Valid`, serialización) | El Service | Contrato HTTP: status codes, JSON, headers |
| `*RepositoryTest` (`@DataJpaTest`) | JPA/Hibernate + H2 real | Nada | Que las queries (incluidas las `@Query` manuales) hagan lo esperado |

Correr toda la suite:
```bash
./gradlew test
```
---

## Usuarios de prueba

| Usuario | Contraseña | Rol |
|---|---|---|
| `admin` | `admin123` | `ADMINISTRATOR` |
| `organizer` | `organizer123` | `RACE_ORGANIZER` |
| `viewer` | `viewer123` | `VIEWER` |

Para obtener un token manualmente: entrar a Swagger UI (`/swagger-ui.html`) → botón **Authorize** → login con cualquiera de los usuarios de arriba. Swagger arma el request `Authorization: Bearer <token>` automáticamente en cada "Try it out" después de eso.

---

## Ejemplos de peticiones a la API

**Listar competidores activos (paginado)**
```bash
curl -H "Authorization: Bearer <token>" \
  "http://localhost:8080/api/competitors?status=ACTIVE&page=0&size=10"
```

**Crear un competidor** (requiere `ADMINISTRATOR` o `RACE_ORGANIZER`)
```bash
curl -X POST "http://localhost:8080/api/competitors" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Byte",
    "nickname": "byte-the-camel",
    "competitorType": "CAMEL",
    "dateOfBirth": "2015-01-01",
    "height": 190.0,
    "weight": 300.0,
    "origin": "Alto de Las Palmas"
  }'
```

**Agregar un competidor a un equipo**
```bash
curl -X POST "http://localhost:8080/api/teams/{teamId}/members/{competitorId}" \
  -H "Authorization: Bearer <token>"
```

**Cambiar el estado de un competidor**
```bash
curl -X PATCH "http://localhost:8080/api/competitors/{id}/status" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"status": "INJURED"}'
```
---

## Limitaciones conocidas

- Frontend: falta buscador por nombre/apodo en el listado de competidores, pantallas de acceso denegado y no-encontrado, y notificaciones de éxito visibles (hoy solo hay notificación de error).
- La protección de rutas del lado del frontend hoy solo oculta los links administrativos; no bloquea la navegación directa a una URL restringida.

## Mejoras futuras

- Refresh tokens.
- Migraciones versionadas (Flyway/Liquibase) en vez de `ddl-auto: update`.
- Rate limiting y métricas/observabilidad.
- Exportación de resultados a CSV/PDF.
- Pipeline de CI (GitHub Actions) con build + tests + imagen Docker automáticos.
