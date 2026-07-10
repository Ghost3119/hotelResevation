# Hotel Manager

Sistema web de administracion hotelera (PMS) para recepcion, administracion,
operacion y privacidad. El proyecto esta construido como una aplicacion
fullstack con Spring Boot 3, React 18 + TypeScript, PostgreSQL 16 y Docker
Compose.

Tambien incluye controles y evidencias para una defensa Blue Team de la materia
de ciberseguridad: hardening HTTP, CORS para ngrok, auditoria de accesos,
registro de intentos de ataque, scripts SAST/DAST y pruebas automatizadas.

> La aplicacion no es una landing page. Es una herramienta operativa para el
> personal del hotel.

---

## Resumen

Funcionalidades principales:

1. Autenticacion con JWT, refresh token en cookie HttpOnly, rotacion de refresh
   tokens, deteccion de reutilizacion y contrasenas BCrypt.
2. Roles: `ADMIN`, `MANAGER`, `RECEPCIONISTA`, `HOUSEKEEPING` y
   `PRIVACY_OFFICER`.
3. Huespedes: alta, edicion, busqueda, historial, enmascaramiento de datos
   personales y auditoria de acceso a datos completos.
4. Tipos de habitacion y habitaciones: CRUD, estados operativos y control de
   disponibilidad.
5. Reservas: creacion, asignacion de habitacion, prevencion de solapamientos,
   cancelacion, grupos de reserva, movimientos de habitacion y control de
   estados.
6. Reglas hoteleras: politicas de cancelacion, bloqueos, no-show, early
   check-in, late check-out, excepciones con motivo y auditoria.
7. Motor de tarifas: planes tarifarios, temporadas, overrides diarios,
   promociones, impuestos/cargos y cotizacion con desglose por noche.
8. Disponibilidad: busqueda por fechas, huespedes y tipo de habitacion.
9. Check-in/check-out: control de estados, housekeeping y registro de usuario,
   fecha y acciones de auditoria.
10. Pagos manuales en pesos mexicanos (MXN), saldos, ajustes y reembolsos.
11. Privacidad: solicitudes `EXPORT`, `RECTIFY` y `DELETE`, exportacion de datos,
   anonimizacion y bitacora de acceso.
12. Dashboard operativo con KPIs, ocupacion, llegadas, salidas e ingresos.
13. Frontend responsive con Tailwind CSS, rutas protegidas y cliente generado
   desde OpenAPI.
14. Seguridad Blue Team: cabeceras HTTP, CSP, CORS con ngrok, rate limiting en
   autenticacion, logs de ataques y scripts para Snyk/npm audit/OWASP ZAP.

---

## Estructura

```text
hotel/
|-- backend/                       # Spring Boot 3, Java 21, Maven
|   |-- src/main/java/com/hotelmanager/
|   |   |-- config/                # OpenAPI, Jackson, auditoria, inicializacion
|   |   |-- domain/                # Entidades JPA y enums
|   |   |-- repository/            # Spring Data JPA
|   |   |-- security/              # JWT, refresh, filtros, CORS, monitoreo
|   |   |-- service/               # Reglas de negocio
|   |   `-- web/                   # Controladores, DTOs, mappers, errores
|   |-- src/main/resources/
|   |   |-- application.yml
|   |   `-- db/migration/          # Flyway V1-V14
|   `-- src/test/                  # Unitarias, integracion y Testcontainers
|-- frontend/                      # React 18, TypeScript, Vite, Tailwind
|   |-- src/api/generated/         # Cliente/tipos generados desde OpenAPI
|   |-- src/auth/                  # AuthContext, refresh flow, rutas protegidas
|   |-- src/components/            # Componentes reutilizables
|   |-- src/hooks/                 # Hooks TanStack Query
|   |-- src/pages/                 # Pantallas de operacion
|   |-- src/test/                  # MSW alineado con OpenAPI
|   `-- e2e/                       # Playwright y axe
|-- database/                      # Esquema y seed de referencia
|-- docs/                          # Arquitectura, reglas, privacidad, pruebas
|-- performance/                   # Pruebas k6
|-- security/                      # Scripts SAST/DAST/audit y reportes
|-- compose.yaml
|-- .env.example
`-- README.md
```

---

## Stack

| Capa | Tecnologia |
| --- | --- |
| Backend | Java 21, Spring Boot 3.5, Maven |
| Persistencia | PostgreSQL 16, Flyway, Spring Data JPA, Testcontainers |
| Seguridad | Spring Security, JWT HS256, refresh token rotativo, BCrypt, rate limit |
| Documentacion API | OpenAPI 3 / Swagger UI |
| Frontend | React 18, TypeScript, Vite, React Router, TanStack Query |
| UI | Tailwind CSS v3 |
| HTTP | axios con `withCredentials` para cookie refresh |
| Tests FE | Vitest, Testing Library, MSW, Playwright, axe |
| Rendimiento | k6 |
| Seguridad | npm audit, Snyk CLI, OWASP ZAP baseline |
| Infra | Docker Compose, nginx como frontend/proxy `/api` |

---

## Acceso de desarrollo

El repositorio no contiene cuentas ni contrasenas funcionales. Flyway tampoco
crea usuarios de acceso. Para un entorno local nuevo, define valores unicos en
el archivo `.env` ignorado por Git y habilita el bootstrap una sola vez:

```env
BOOTSTRAP_USERS_ENABLED=true
BOOTSTRAP_ADMIN_EMAIL=
BOOTSTRAP_ADMIN_PASSWORD=
BOOTSTRAP_RECEPTIONIST_EMAIL=
BOOTSTRAP_RECEPTIONIST_PASSWORD=
```

Las contrasenas deben tener entre 14 y 72 caracteres ASCII e incluir mayuscula,
minuscula, numero y simbolo. El backend solo persiste hashes BCrypt. Desactiva
`BOOTSTRAP_USERS_ENABLED` y vacia los valores `BOOTSTRAP_*_PASSWORD` despues de
crear/verificar las cuentas.

---

## Configuracion

Copia `.env.example` a `.env` y completa los secretos obligatorios antes de
arrancar. No hay valores de respaldo para la base de datos ni para JWT:

```bash
cp .env.example .env
```

Variables importantes:

- `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`
- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`,
  `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET`, `JWT_ACCESS_TOKEN_EXPIRATION_SECONDS`, `JWT_ISSUER`
- `REFRESH_TOKEN_EXPIRATION_SECONDS`, `REFRESH_TOKEN_SECURE`
- `APP_SECURITY_EXPOSE_DOCS`, `APP_SECURITY_EXPOSE_H2_CONSOLE`
- `AUTH_RATE_LIMIT_ENABLED`, `AUTH_RATE_LIMIT_MAX_ATTEMPTS`,
  `AUTH_RATE_LIMIT_WINDOW_SECONDS`, `AUTH_RATE_LIMIT_MAX_TRACKED_KEYS`
- `TRUSTED_PROXY_CIDRS`
- `BOOTSTRAP_USERS_ENABLED` y `BOOTSTRAP_*`
- `CORS_ALLOWED_ORIGINS`, `CORS_ALLOWED_ORIGIN_PATTERNS`
- `PRIVACY_RETENTION_DAYS`
- `CHECKIN_EARLY_CHECKIN_ALLOWED`, `CHECKIN_EARLY_CHECKIN_HOURS`
- `CHECKOUT_LATE_CHECKOUT_ALLOWED`, `CHECKOUT_LATE_CHECKOUT_HOURS`
- `APP_TIMEZONE`
- `VITE_API_BASE_URL`

Para ngrok, el frontend debe llamar a la API por ruta relativa. No habilites un
comodin para todos los subdominios de ngrok:

```env
VITE_API_BASE_URL=/api
CORS_ALLOWED_ORIGIN_PATTERNS=
```

Si se expone por HTTPS con ngrok y no solo en desarrollo local:

```env
REFRESH_TOKEN_SECURE=true
APP_SECURITY_EXPOSE_DOCS=false
```

Si realmente hay un cliente separado que necesita CORS, agrega solamente su
origen exacto a `CORS_ALLOWED_ORIGINS`.

> Una base existente puede conservar las antiguas credenciales aunque se quite
> el texto del repositorio. La migracion V14 revoca sesiones y desactiva las
> cuentas semilla historicas. Como V2 se saneo, una instalacion que ya la aplico
> debe recrear su base de desarrollo o ejecutar `flyway repair` de forma
> controlada antes de migrar. Cambiar `POSTGRES_PASSWORD` tampoco cambia el rol
> dentro de un volumen ya inicializado; rota ese rol con `ALTER ROLE` o recrea el
> volumen solo si es aceptable perder sus datos.

---

## Ejecucion

### Docker Compose recomendado

```bash
docker compose up --build
```

- Frontend: http://localhost:5173
- API por proxy nginx: http://localhost:5173/api

PostgreSQL y el backend no publican puertos al host en el Compose endurecido.
Swagger/OpenAPI esta desactivado por defecto.

### Exponer para la materia con ngrok

Levanta el stack y expone solo el frontend/proxy:

```bash
docker compose up --build
ngrok http 5173
```

Usa la URL HTTPS de ngrok, por ejemplo:

```text
https://TU-SUBDOMINIO.ngrok-free.dev
```

No expongas el puerto `8080` directamente ni el servidor dev de Vite. El flujo
esperado es:

```text
navegador/ngrok -> nginx frontend :5173 -> /api -> backend :8080
```

Para ver logs de seguridad durante la defensa:

```bash
docker logs -f hotel-backend
```

Eventos relevantes en logs:

- `HTTP_REQUEST`
- `INJECTION_ATTEMPT`
- `UNAUTHORIZED_ACCESS`
- `CRITICAL_UNAUTHORIZED_ACCESS`
- respuestas `429` por rate limit de login/refresh

---

## Pruebas

### Backend

```bash
cd backend
./mvnw test
./mvnw verify
```

Ultima verificacion local: 76 tests, 0 fallos.

Cubre autenticacion, refresh token, permisos, reservas, disponibilidad,
check-in/check-out, pagos, reglas hoteleras, privacidad, concurrencia y
restricciones reales de PostgreSQL con Testcontainers.

### Frontend

```bash
cd frontend
npm run lint
npm run test
npm run build
npm run test:e2e
npm run test:a11y
npm run test:load
```

Ultima verificacion local:

- `npm run lint`: 0 errores.
- `npm run test`: 64 tests, 0 fallos.
- `npm run build`: OK.
- `npm run test:e2e`: 13 tests, 0 fallos contra stack real.
- `npm run test:a11y`: 2 tests, 0 fallos.

`npm run api:generate` genera cliente y tipos frontend desde OpenAPI. Los mocks
MSW y contract tests se usan para evitar divergencias entre frontend y backend.

---

## Seguridad y evidencias Blue Team

Scripts disponibles:

```powershell
.\security\npm-audit.ps1
.\security\snyk-scan.ps1
.\security\zap-baseline.ps1 -Target https://TU-SUBDOMINIO.ngrok-free.dev
```

Notas:

- `npm audit` y `npm audit --omit=dev` no reportan vulnerabilidades tras la
  actualizacion a Vite 8/Vitest 4 y del arbol OpenAPI.
- Snyk requiere CLI autenticado (`snyk auth`).
- OWASP ZAP debe ejecutarse contra la URL actual de ngrok.
- El reporte base esta en `docs/blue-team-report.md`.

Controles implementados:

- CSP y cabeceras: `X-Frame-Options`, `X-Content-Type-Options`,
  `Referrer-Policy`, `Permissions-Policy`, COOP/CORP y HSTS.
- CORS con origenes exactos; no se habilitan comodines multiusuario de ngrok.
- API frontend por `/api` para evitar exponer `localhost:8080` en el navegador.
- Cookie refresh `HttpOnly`, `SameSite=Lax`, `Path=/api/auth` y `Secure`
  configurable.
- Rate limit en `POST /api/auth/login` y `POST /api/auth/refresh`.
- Logs de intentos XSS, SQLi, path traversal, JNDI y command injection.
- Errores internos genericos para no filtrar stack traces al cliente.
- Acceso a datos personales completos auditado.

---

## Documentacion

- `docs/architecture.md`: arquitectura, casos de uso y endpoints.
- `docs/database.md`: modelo relacional, migraciones, indices y restricciones.
- `docs/hotel-rules.md`: reglas hoteleras avanzadas.
- `docs/privacy.md`: privacidad, retencion, acceso y anonimización.
- `docs/privacy-threat-model.md`: modelo de amenazas de datos personales.
- `docs/testing-strategy.md`: estrategia de pruebas, contrato, E2E, carga y a11y.
- `docs/blue-team-report.md`: plantilla/evidencia para la defensa Blue Team.
- `security/README.md`: comandos de seguridad.

---

## Decisiones tecnicas

- **Solapamientos**: doble defensa. PostgreSQL usa `EXCLUDE USING gist` y el
  backend valida en transaccion con bloqueo pesimista.
- **Dinero**: `BigDecimal` en Java y `NUMERIC(12,2)` en PostgreSQL. Moneda:
  MXN.
- **Tokens**: access token corto solo en memoria + refresh token rotativo en
  cookie HttpOnly. Solo se almacena hash SHA-256 del refresh token.
- **Privacidad**: respuestas normales usan datos enmascarados; exportar o ver
  datos completos requiere permiso y genera auditoria.
- **OpenAPI**: el frontend genera cliente/tipos desde el contrato.
- **MSW**: los mocks se alinean con OpenAPI y se validan con contract tests.
- **ngrok**: se publica el frontend/proxy, no el backend directo.
- **Produccion**: el compose actual es de demostracion/desarrollo, no reemplaza
  un despliegue con TLS, secretos gestionados y observabilidad formal.

---

## Limitaciones conocidas

- No hay pasarela de pago real; los pagos son registros manuales.
- No se almacenan ni procesan datos de tarjetas.
- El compose esta orientado a desarrollo y defensa academica.
- Los secretos de `.env.example` son placeholders y deben cambiarse antes de
  cualquier exposicion real.
- Snyk y ZAP deben ejecutarse en el entorno de quien presenta para generar
  evidencia actual.
- El access token se mantiene solo en memoria; una recarga restaura la sesion
  mediante la cookie HttpOnly de refresh.

---

## Estado verificado

- Docker Compose levanta PostgreSQL, Flyway V1-V14, backend y frontend.
- Login por `http://localhost:5173/api/auth/login` funciona.
- Login desde origen ngrok funciona con `CORS_ALLOWED_ORIGIN_PATTERNS`.
- El build del frontend no contiene `localhost:8080`.
- Cabeceras de seguridad presentes en nginx.
- Logs detectan intentos XSS y accesos criticos no autorizados.
- Backend: 76 tests.
- Frontend: 64 unit/contract tests, 13 E2E, 2 a11y.
