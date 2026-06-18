# Hotel Manager

Sistema de gestión hotelera (PMS) para uso interno de recepción y
administración. Aplicación web fullstack con backend en Spring Boot, frontend en
React + TypeScript y base de datos PostgreSQL.

> No es una landing page publicitaria: es una herramienta operativa para el
> personal del hotel.

---

## Resumen

Aplicación de administración hotelera con dos roles:

- **ADMIN**: administra usuarios, habitaciones y tipos de habitación, configura
  precios, consulta reportes y ocupación, y accede a todas las reservas.
- **RECEPCIONISTA**: registra huéspedes, crea y modifica reservas, realiza
  check-in y check-out, registra pagos y consulta disponibilidad.

Funcionalidades implementadas (MVP):

1. Autenticación con JWT y autorización por roles (BCrypt para contraseñas).
2. Huéspedes: registro, edición, consulta, búsqueda y historial de reservas.
3. Tipos de habitación: CRUD con capacidad, precio base y servicios.
4. Habitaciones: CRUD con estados (AVAILABLE, RESERVED, OCCUPIED, CLEANING,
   MAINTENANCE, OUT_OF_SERVICE), cambio de estado y observaciones.
5. Reservas: creación con asignación de habitación, prevención de solapamientos,
   estados (PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED, NO_SHOW),
   cálculo de noches y precio total, cancelación con reglas de negocio.
6. Disponibilidad: búsqueda por fechas, huéspedes y tipo de habitación.
7. Check-in / check-out con control de estados y registro de usuario y fecha.
8. Pagos manuales con métodos y estados, cálculo de total/pagado/saldo.
9. Dashboard con KPIs del día y periodo (llegadas, salidas, ocupación, ingresos).
10. Frontend operativo responsive con navegación lateral y rutas protegidas.

---

## Estructura del proyecto

```
hotel/
├── backend/                      # Spring Boot 3 (Java 21, Maven)
│   ├── src/main/java/com/hotelmanager/
│   │   ├── domain/               # Entidades JPA + enums
│   │   ├── repository/           # Spring Data JPA
│   │   ├── security/             # JWT, Spring Security, BCrypt
│   │   ├── service/              # Lógica de negocio (RN-1..RN-10)
│   │   ├── config/               # OpenAPI, DataInitializer, Jackson, auditoría
│   │   └── web/                  # Controladores, DTOs, mappers, excepciones
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/         # Migraciones Flyway (V1, V2)
│   ├── src/test/                 # Pruebas unitarias y de integración (H2)
│   ├── Dockerfile
│   ├── pom.xml
│   └── mvnw / mvnw.cmd           # Maven Wrapper
├── frontend/                     # React 18 + TypeScript + Vite
│   ├── src/
│   │   ├── api/                  # Cliente HTTP centralizado + módulos API
│   │   ├── auth/                 # AuthContext, RequireAuth, RoleGate
│   │   ├── components/           # Layout, tablas, formularios, diálogos, badges
│   │   ├── hooks/                # Hooks TanStack Query
│   │   ├── pages/                # Dashboard, huéspedes, reservas, habitaciones…
│   │   ├── test/                 # MSW handlers + render de test
│   │   └── utils/                # Formato, constantes, errores
│   ├── Dockerfile
│   └── nginx.conf
├── database/                     # Referencia de esquema y seed (no ejecutado por Flyway)
├── docs/                         # Arquitectura y modelo de datos
│   ├── architecture.md
│   └── database.md
├── compose.yaml                  # PostgreSQL + backend + frontend
├── .env.example
├── .gitignore
└── README.md
```

---

## Stack

| Capa          | Tecnología                                                        |
|---------------|-------------------------------------------------------------------|
| Backend       | Java 21, Spring Boot 3.3, Maven                                   |
| Persistencia  | PostgreSQL 16, Flyway, Spring Data JPA                            |
| Seguridad     | Spring Security + JWT (HS256, jjwt 0.12), BCrypt                  |
| Documentación | OpenAPI 3 (springdoc-openapi)                                     |
| Frontend      | React 18, TypeScript, Vite, React Router 6, TanStack Query 5      |
| HTTP          | axios                                                             |
| Tests FE      | Vitest, Testing Library, MSW                                      |
| Infra         | Docker Compose                                                    |

---

## Requisitos

- Java 21+ (o usar Docker)
- Maven 3.9+ (o usar el wrapper `./mvnw`)
- Node.js 20+ y npm
- Docker + Docker Compose

> En este entorno el backend se compila y prueba con Docker
> (`maven:3.9-eclipse-temurin-21`) porque no hay Maven local.

---

## Credenciales de desarrollo

Solo para desarrollo (semilla Flyway + `DataInitializer`):

| Rol           | Email                | Contraseña      |
|---------------|----------------------|-----------------|
| Administrador | `admin@hotel.test`   | `admin123`      |
| Recepcionista | `recepcion@hotel.test` | `recepcion123` |

> Las contraseñas se insertan con el centinela `BCRYPT_PENDING` en la migración
> `V2` y el `DataInitializer` del backend las cifra con BCrypt al arrancar.

---

## Configuración

Copia `.env.example` a `.env` y ajusta los valores (especialmente
`JWT_SECRET` y `POSTGRES_PASSWORD` para producción):

```bash
cp .env.example .env
```

Variables principales (ver `.env.example` para el listado completo):

- `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_PORT`
- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET`, `JWT_EXPIRATION_SECONDS`, `JWT_ISSUER`
- `CORS_ALLOWED_ORIGINS` (por defecto `http://localhost:5173`)
- `VITE_API_BASE_URL` (frontend)

---

## Comandos de ejecución

### Opción A — Docker Compose (recomendado)

Levanta PostgreSQL, ejecuta migraciones Flyway, arranca backend y frontend:

```bash
docker compose up --build
```

- Frontend: http://localhost:5173
- Backend API: http://localhost:8080/api
- Swagger UI: http://localhost:8080/api/docs
- OpenAPI JSON: http://localhost:8080/api/openapi.json

### Opción B — Desarrollo local por separado

**PostgreSQL** (vía Docker):

```bash
docker compose up postgres
```

**Backend**:

```bash
cd backend
./mvnw spring-boot:run        # o: mvn spring-boot:run
```

**Frontend**:

```bash
cd frontend
npm install
npm run dev
```

---

## Pruebas

### Backend

```bash
cd backend
./mvnw test          # pruebas unitarias y de integración (H2)
./mvnw verify        # test + package + checks
```

> Sin Maven local, en Docker:
> ```
> docker run --rm -v "$(pwd):/app" -w /app maven:3.9-eclipse-temurin-21 mvn -B clean test
> ```

Pruebas implementadas (cubre los casos obligatorios):

- `AuthServiceTest` / `AuthControllerTest`: login correcto, credenciales inválidas, `/me`.
- `SecurityTest`: endpoint de ADMIN accedido por RECEPCIONISTA → 403; ADMIN → 200.
- `ReservationServiceTest`: reserva válida (noches y total correctos), fechas
  inválidas (`checkOut <= checkIn`), solapamiento → `RESERVATION_OVERLAP`,
  cancelación futura vs. mismo día (`CANCEL_NOT_ALLOWED`).
- `AvailabilityServiceTest`: disponibilidad filtra por capacidad y excluye
  solapamientos / mantenimiento / fuera de servicio.
- `CheckInOutServiceTest`: check-in de confirmada vigente, check-in duplicado,
  check-out (habitación → CLEANING).
- `PaymentServiceTest`: registro de pago, cálculo de saldo, reembolso.
- `GuestServiceTest`, `RoomServiceTest` (transiciones de estado).

### Frontend

```bash
cd frontend
npm run lint         # ESLint, 0 errores
npm run test         # Vitest + Testing Library + MSW
npm run build        # tsc -b && vite build
```

Pruebas frontend (flujos principales con MSW mockeando el backend):

- `LoginPage`: login navega al dashboard; error con credenciales inválidas;
  redirección a login sin autenticación.
- `DashboardPage`: renderiza KPIs e ingresos.
- `GuestsPage`: lista, búsqueda y creación de huésped.
- `AvailabilityPage`: búsqueda devuelve habitaciones; rechaza fechas inválidas.
- `ReservationFormPage`: rechaza fechas inválidas en cliente; crea reserva
  válida; muestra `RESERVATION_OVERLAP` del servidor.
- `ReservationDetailPage`: cancela reserva, check-in, registro de pago con
  actualización de saldo, check-out.
- `RequireAuth`: redirección sin auth, acceso de ADMIN, bloqueo 403 de
  RECEPCIONISTA a `/users`.

---

## Documentación

- `docs/architecture.md`: casos de uso, reglas de negocio (RN-1..RN-10), modelo
  preliminar, endpoints, contratos DTO y criterios de aceptación.
- `docs/database.md`: modelo relacional, columnas, índices, diseño de
  prevención de solapamientos y contrato backend.
- `database/schema.sql` / `database/seed.sql`: copias de referencia (no
  ejecutadas por Flyway).
- OpenAPI en tiempo de ejecución: `/api/docs` (Swagger UI) y
  `/api/openapi.json`.

---

## Decisiones técnicas

- **Prevención de solapamientos**: doble defensa. En la base de datos, una
  restricción `EXCLUDE USING gist` sobre `reservation_rooms` con una columna
  generada `overlap_range daterange` impide dos asignaciones solapadas para la
  misma habitación (requiere la extensión `btree_gist`). En la aplicación, la
  validación se hace dentro de una transacción con bloqueo pesimista
  (`SELECT ... FOR UPDATE` sobre la habitación) antes de insertar la
  asignación; las violaciones GiST (SQLSTATE `23P01`) se traducen a
  `409 RESERVATION_OVERLAP`. Al cancelar, se eliminan las filas de
  `reservation_rooms` para liberar la habitación.
- **Hashes BCrypt**: la migración semilla inserta `BCRYPT_PENDING` como
  centinela y el `DataInitializer` del backend los cifra al arrancar (coste 10),
  evitando shipped hashes inválidos y manteniendo idempotencia.
- **Concurrencia**: asignación de habitación y check-in usan `@Transactional` +
  bloqueo pesimista sobre la habitación.
- **Importes**: `BigDecimal` (escala 2) en backend y `NUMERIC(12,2)` en BD;
  nunca `double`. Jackson serializa con 2 decimales.
- **Fechas**: `LocalDate` para fechas de calendario (`check_in`/`check_out`),
  `Instant`/`TIMESTAMPTZ` para auditoría en UTC.
- **DTOs**: las entidades JPA no se exponen; controladores usan DTOs + mappers
  manuales + Bean Validation.
- **Tests backend**: H2 en modo PostgreSQL con Flyway desactivado y
  `ddl-auto=create-drop`; la validación de solapamiento se prueba a nivel de
  servicio (H2 no tiene GiST).
- **Tests frontend**: MSW mockea todos los endpoints usados; sin backend real.
- **`columnDefinition="jsonb"`**: se omite en las anotaciones `@JdbcTypeCode`
  para que Hibernate genere `jsonb` en PostgreSQL (valida contra el esquema)
  pero `json` en H2 (compatibilidad de tests).
- **CORS**: configurado para `http://localhost:5173` en desarrollo.
- **OpenAPI**: publicado en `/api/openapi.json` y `/api/docs`, públicos sin auth.

---

## Limitaciones pendientes

- Sin pasarela de pago real: los pagos son registros manuales (MVP).
- No se almacenan ni procesan datos sensibles de tarjetas.
- Sin refresh token: el JWT expira y requiere re-login (expiración configurable).
- El check-out se permite con saldo pendiente (se registra en auditoría); no
  bloquea por saldo impagado (decisión de MVP).
- No hay i18n formal: los literales de la interfaz están en español.
- Sin despliegue productivo (TLS, secretos gestionados, observabilidad): el
  `compose.yaml` es para desarrollo.

---

## Verificación final

- **Backend**: `mvn -B clean test` → BUILD SUCCESS, 38 tests, 0 fallos.
  `mvn -B clean verify -DskipTests` → jar empaquetado correctamente.
- **Frontend**: `npm run lint` → 0 errores; `npm run test` → 19 tests, 0
  fallos; `npm run build` → `dist/` generado sin errores de tipo.
- **Integración**: `docker compose up` levanta PostgreSQL, ejecuta Flyway
  (V1 + V2), arranca backend y sirve el frontend.
