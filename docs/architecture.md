# Hotel Manager — Arquitectura

Documento de arquitectura y contrato entre capas. Es la fuente de verdad para
los agentes DATABASE, BACKEND y FRONTEND. Cualquier cambio de contrato debe
reflejarse aquí primero.

## 1. Visión general

Sistema de gestión hotelera (PMS) para uso interno en recepción y
administración. No es un sistema de venta pública al cliente final.

### Stack

| Capa        | Tecnología                                                        |
|-------------|-------------------------------------------------------------------|
| Backend     | Java 21 (compila con `--release 21`), Spring Boot 3, Maven        |
| Persistencia| PostgreSQL 16, Flyway, Spring Data JPA                            |
| Seguridad   | Spring Security + JWT (HS256), BCrypt                             |
| Documentación| OpenAPI 3 (springdoc-openapi)                                    |
| Frontend    | React 18 + TypeScript + Vite, React Router, TanStack Query        |
| Infra       | Docker Compose                                                    |

### Estructura de carpetas

```
hotel/
├── backend/        # Spring Boot
├── frontend/       # React + Vite
├── database/       # Migraciones Flyway (referencia/documentación)
├── docs/           # Arquitectura y modelo de datos
├── compose.yaml
├── .env.example
└── README.md
```

> Las migraciones Flyway ejecutables viven en
> `backend/src/main/resources/db/migration`. La carpeta `database/` contiene
> documentación y scripts de referencia (no ejecutados por Flyway).

## 2. Roles y permisos

| Rol           | Código      | Permisos                                                                  |
|---------------|-------------|---------------------------------------------------------------------------|
| Administrador | `ADMIN`     | Usuarios, tipos de habitación, habitaciones, precios, reportes, reservas |
| Recepcionista | `RECEPCIONISTA` | Huéspedes, reservas, check-in/out, pagos, disponibilidad            |

Matriz de permisos (endpoint → roles permitidos):

| Recurso            | ADMIN | RECEPCIONISTA |
|--------------------|:-----:|:-------------:|
| auth/login         |   Sí  |      Sí       |
| users              |   Sí  |      No       |
| room-types         |   Sí  |   Solo lectura|
| rooms              |   Sí  | Solo lectura + cambio estado|
| guests             |   Sí  |      Sí       |
| reservations       |   Sí  |      Sí       |
| availability       |   Sí  |      Sí       |
| checkin / checkout |   Sí  |      Sí       |
| payments           |   Sí  |      Sí       |
| dashboard          |   Sí  |      Sí       |
| reports            |   Sí  |      No       |

## 3. Casos de uso

### AUTENTICACIÓN
- UC-AUTH-1: Iniciar sesión con correo y contraseña.
- UC-AUTH-2: Obtener perfil del usuario autenticado.
- UC-AUTH-3: Renovar token (opcional, refresh).

### USUARIOS
- UC-USR-1: Crear usuario (ADMIN).
- UC-USR-2: Listar usuarios paginados (ADMIN).
- UC-USR-3: Actualizar usuario (ADMIN).
- UC-USR-4: Activar/desactivar usuario (ADMIN).

### HUÉSPEDES
- UC-GST-1: Registrar huésped.
- UC-GST-2: Editar huésped.
- UC-GST-3: Consultar huésped por id.
- UC-GST-4: Buscar huéspedes por nombre, correo o documento (paginado).
- UC-GST-5: Consultar historial de reservas de un huésped.

### TIPOS DE HABITACIÓN
- UC-RT-1: Crear tipo (ADMIN).
- UC-RT-2: Listar tipos (ambos roles, solo activos para recepción).
- UC-RT-3: Actualizar tipo (ADMIN).
- UC-RT-4: Activar/desactivar tipo (ADMIN).

### HABITACIONES
- UC-RM-1: Crear habitación (ADMIN).
- UC-RM-2: Listar habitaciones con filtros por piso, tipo y estado (paginado).
- UC-RM-3: Actualizar habitación (ADMIN).
- UC-RM-4: Cambiar estado de habitación (ambos roles, según reglas).
- UC-RM-5: Registrar observaciones.

### RESERVAS
- UC-RES-1: Crear reserva.
- UC-RES-2: Listar reservas con filtros (estado, fecha, huésped) paginado.
- UC-RES-3: Consultar reserva por id (con habitaciones y pagos).
- UC-RES-4: Modificar reserva (fechas, adultos/niños, notas) si no está CHECKED_IN/CHECKED_OUT/CANCELLED.
- UC-RES-5: Cancelar reserva.
- UC-RES-6: Asignar / reasignar habitación a una reserva.

### DISPONIBILIDAD
- UC-AVL-1: Buscar habitaciones disponibles por rango de fechas, huéspedes y tipo.

### CHECK-IN / CHECK-OUT
- UC-CI-1: Realizar check-in (reserva CONFIRMED, vigente).
- UC-CI-2: Evitar check-in duplicado.
- UC-CO-1: Realizar check-out (saldo cero o confirmado).
- UC-CO-2: Pasar habitación a CLEANING.

### PAGOS
- UC-PAY-1: Registrar pago manual contra una reserva.
- UC-PAY-2: Listar pagos de una reserva.
- UC-PAY-3: Reembolsar / cancelar pago.
- UC-PAY-4: Consultar total, pagado y saldo de una reserva.

### DASHBOARD
- UC-DASH-1: Consultar métricas del día y periodo (llegadas, salidas, ocupación, ingresos).

## 4. Reglas de negocio

### RN-1 Fechas
- `checkOut` debe ser estrictamente posterior a `checkIn`.
- La noche del `checkOut` **no** cuenta como noche ocupada.
- No se permiten reservas en el pasado (`checkIn >= hoy`).
- Noches = `checkOut.toEpochDay() - checkIn.toEpochDay()`.

### RN-2 Disponibilidad y solapamiento
- Una habitación está asignada a una reserva mediante `reservation_rooms` con
  `check_in` / `check_out` (rango de la reserva).
- Dos asignaciones sobre la misma habitación son **incompatibles** si sus rangos
  se solapan: `a.check_in < b.check_out AND b.check_in < a.check_out`.
- La consulta de disponibilidad excluye habitaciones con asignaciones activas
  (estado de reserva distinto de CANCELLED/NO_SHOW) que se solapen con el rango
  solicitado, y cuya capacidad soporte el total de huéspedes.
- El total de huéspedes = `adults + children` y debe ser `<= room_type.max_capacity`.

### RN-3 Estados de reserva
```
PENDING -> CONFIRMED -> CHECKED_IN -> CHECKED_OUT
PENDING -> CANCELLED
CONFIRMED -> CANCELLED  (con regla de cancelación)
CONFIRMED -> NO_SHOW
CHECKED_IN -> CHECKED_OUT
```
- Solo se puede hacer check-in de reservas `CONFIRMED` con `checkIn <= hoy < checkOut`.
- Solo se puede hacer check-out de reservas `CHECKED_IN`.
- No se puede modificar una reserva `CHECKED_IN`, `CHECKED_OUT`, `CANCELLED` ni `NO_SHOW`.

### RN-4 Cancelación
- Se puede cancelar `PENDING` o `CONFIRMED` si `checkIn > hoy` (no el mismo día
  ni en el pasado). Si la reserva tiene habitación asignada, se libera.
- Al cancelar se libera la asignación de habitación.

### RN-5 Check-in
- Requiere reserva `CONFIRMED` y `checkIn <= hoy <= checkOut` (hoy dentro del rango).
- Si la reserva no tiene habitación asignada, el sistema asigna automáticamente
  una disponible del tipo solicitado (opción por defecto) o se exige indicar una.
- La habitación pasa a `OCCUPIED`.
- Se registra `check_in_at` y `checked_in_by` (usuario).

### RN-6 Check-out
- Requiere reserva `CHECKED_IN`.
- El saldo debe ser `<= 0` (total pagado) o el operador confirma checkout con
  saldo pendiente dejando nota. (Para el MVP: se permite checkout pero se
  registra el saldo pendiente.)
- La habitación pasa a `CLEANING`.
- Se registra `check_out_at` y `checked_out_by`.

### RN-7 Estados de habitación
```
AVAILABLE, RESERVED, OCCUPIED, CLEANING, MAINTENANCE, OUT_OF_SERVICE
```
- `AVAILABLE` -> `RESERVED` (al asignar a reserva confirmada).
- `RESERVED`/`AVAILABLE` -> `OCCUPIED` (check-in).
- `OCCUPIED` -> `CLEANING` (check-out).
- `CLEANING` -> `AVAILABLE` (limpieza finalizada).
- Cualquiera -> `MAINTENANCE` / `OUT_OF_SERVICE` (ADMIN).
- `MAINTENANCE`/`OUT_OF_SERVICE` -> `AVAILABLE` (ADMIN).
- Transiciones controladas en servicio; no se permite pasar a `OCCUPIED` desde
  `MAINTENANCE` directamente.

### RN-8 Pagos
- Importes en `BigDecimal` con escala 2 y moneda implícita (EUR).
- `total = noches * precio_noche` (precio tomado del tipo de habitación vigente
  al crear la reserva; se almacena `nightly_price` en la reserva para
  inmutabilidad histórica).
- `paid = sum(payments.amount where status = COMPLETED)`.
- `balance = total - paid`.
- Un pago `COMPLETED` puede ser `REFUNDED` (genera negativo en paid) o
  `CANCELLED` (no suma). `PENDING` no suma.
- No se almacenan datos sensibles de tarjetas.

### RN-9 Concurrencia
- La asignación de habitación a reserva y el check-in usan bloqueo pesimista
  (`SELECT ... FOR UPDATE`) o `@Version` (lock optimista) sobre la habitación /
  reserva para evitar asignaciones dobles. Se valida solapamiento dentro de la
  transacción.

### RN-10 Auditoría
- Toda operación sensible (login, check-in, check-out, pago, cambio de estado
  de habitación, cancelación) genera un `audit_event` con `user_id`, `action`,
  `entity_type`, `entity_id`, `metadata` (jsonb) y `created_at`.

## 5. Modelo de datos preliminar

Entidades mínimas (detalle de columnas en `docs/database.md`):

- `users` (id, email unique, password_hash, full_name, role, active, created_at, updated_at)
- `roles` (enum: ADMIN, RECEPCIONISTA) — representado como check constraint o tabla catálogo.
- `guests` (id, first_name, last_name, email, phone, document_number, nationality, created_at, updated_at)
- `room_types` (id, name unique, description, max_capacity, base_price, amenities jsonb/text, active)
- `rooms` (id, number unique, floor, room_type_id, status, observations, created_at, updated_at)
- `reservations` (id, guest_id, status, check_in, check_out, adults, children, nightly_price, total_amount, notes, special_requests, check_in_at, check_out_at, checked_in_by, checked_out_by, cancelled_at, cancelled_by, created_at, updated_at, created_by)
- `reservation_rooms` (id, reservation_id, room_id, check_in, check_out) — asignación de habitación a reserva para un rango.
- `payments` (id, reservation_id, amount, method, status, paid_at, reference, created_at, created_by)
- `audit_events` (id, user_id, action, entity_type, entity_id, metadata jsonb, created_at)

Restricción clave anti-solapamiento: índice + validación en servicio. Opcional
exclusion constraint con `tstzrange` si se modela con rangos. Para portabilidad
se valida en aplicación dentro de transacción con bloqueo.

## 6. Endpoints

Base path: `/api`. Todas las fechas en `YYYY-MM-DD` (ISO-8601 local date).
Todos los listados paginados usan `page` (0-based) y `size` (default 20, max 100)
y devuelven `{ content: [...], page, size, totalElements, totalPages }`.

### Autenticación
| Método | Path                    | Auth | Roles | Descripción                |
|--------|-------------------------|------|-------|----------------------------|
| POST   | `/api/auth/login`       | No   | -     | Login, devuelve JWT        |
| GET    | `/api/auth/me`          | Sí   | *     | Perfil del usuario         |

### Usuarios
| Método | Path                    | Roles | Descripción            |
|--------|-------------------------|-------|------------------------|
| GET    | `/api/users`            | ADMIN | Listar paginado        |
| POST   | `/api/users`            | ADMIN | Crear                  |
| GET    | `/api/users/{id}`       | ADMIN | Consultar              |
| PUT    | `/api/users/{id}`       | ADMIN | Actualizar             |
| PATCH  | `/api/users/{id}/status`| ADMIN | Activar/desactivar     |

### Huéspedes
| Método | Path                              | Roles            |
|--------|-----------------------------------|------------------|
| GET    | `/api/guests`                     | ADMIN, RECEP     |
| POST   | `/api/guests`                     | ADMIN, RECEP     |
| GET    | `/api/guests/{id}`                | ADMIN, RECEP     |
| PUT    | `/api/guests/{id}`                | ADMIN, RECEP     |
| GET    | `/api/guests/{id}/reservations`   | ADMIN, RECEP     |

### Tipos de habitación
| Método | Path                          | Roles                 |
|--------|-------------------------------|-----------------------|
| GET    | `/api/room-types`             | ADMIN, RECEP (lectura)|
| POST   | `/api/room-types`             | ADMIN                 |
| GET    | `/api/room-types/{id}`        | ADMIN, RECEP          |
| PUT    | `/api/room-types/{id}`        | ADMIN                 |
| PATCH  | `/api/room-types/{id}/status` | ADMIN                 |

### Habitaciones
| Método | Path                             | Roles                      |
|--------|----------------------------------|----------------------------|
| GET    | `/api/rooms`                     | ADMIN, RECEP               |
| POST   | `/api/rooms`                     | ADMIN                      |
| GET    | `/api/rooms/{id}`                | ADMIN, RECEP               |
| PUT    | `/api/rooms/{id}`                | ADMIN                      |
| PATCH  | `/api/rooms/{id}/status`         | ADMIN, RECEP (según reglas)|
| PATCH  | `/api/rooms/{id}/observations`   | ADMIN, RECEP               |

### Disponibilidad
| Método | Path                                                | Roles            |
|--------|-----------------------------------------------------|------------------|
| GET    | `/api/availability?checkIn=&checkOut=&guests=&roomTypeId=` | ADMIN, RECEP |

### Reservas
| Método | Path                                  | Roles            |
|--------|---------------------------------------|------------------|
| GET    | `/api/reservations`                   | ADMIN, RECEP     |
| POST   | `/api/reservations`                   | ADMIN, RECEP     |
| GET    | `/api/reservations/{id}`              | ADMIN, RECEP     |
| PUT    | `/api/reservations/{id}`              | ADMIN, RECEP     |
| POST   | `/api/reservations/{id}/cancel`       | ADMIN, RECEP     |
| POST   | `/api/reservations/{id}/assign-room`  | ADMIN, RECEP     |

### Check-in / Check-out
| Método | Path                                       | Roles            |
|--------|--------------------------------------------|------------------|
| POST   | `/api/reservations/{id}/check-in`          | ADMIN, RECEP     |
| POST   | `/api/reservations/{id}/check-out`         | ADMIN, RECEP     |

### Pagos
| Método | Path                                       | Roles            |
|--------|--------------------------------------------|------------------|
| GET    | `/api/reservations/{id}/payments`          | ADMIN, RECEP     |
| POST   | `/api/reservations/{id}/payments`          | ADMIN, RECEP     |
| PATCH  | `/api/payments/{id}/status`                | ADMIN, RECEP     |

### Dashboard
| Método | Path                                              | Roles            |
|--------|---------------------------------------------------|------------------|
| GET    | `/api/dashboard?from=&to=`                        | ADMIN, RECEP     |

## 7. Contratos (DTO)

### Auth
```jsonc
// POST /api/auth/login  request
{ "email": "string", "password": "string" }
// response 200
{ "token": "string", "type": "Bearer", "expiresIn": 3600,
  "user": { "id": 1, "email": "string", "fullName": "string", "role": "ADMIN" } }

// GET /api/auth/me response
{ "id": 1, "email": "string", "fullName": "string", "role": "ADMIN", "active": true }
```

### Paginación (envoltura común)
```jsonc
{ "content": [ ... ], "page": 0, "size": 20, "totalElements": 0, "totalPages": 0 }
```

### Usuario
```jsonc
// request create/update
{ "email": "string", "password": "string?", "fullName": "string", "role": "ADMIN|RECEPCIONISTA", "active": true }
// response
{ "id": 1, "email": "string", "fullName": "string", "role": "ADMIN", "active": true }
// PATCH /status
{ "active": true }
```

### Huésped
```jsonc
// request
{ "firstName": "string", "lastName": "string", "email": "string?",
  "phone": "string?", "documentNumber": "string", "nationality": "string" }
// response
{ "id": 1, "firstName": "string", "lastName": "string", "email": "string",
  "phone": "string", "documentNumber": "string", "nationality": "string",
  "createdAt": "2026-06-17T10:00:00" }
```

### Tipo de habitación
```jsonc
// request
{ "name": "string", "description": "string?", "maxCapacity": 2,
  "basePrice": 120.00, "amenities": ["string"], "active": true }
// response
{ "id": 1, "name": "string", "description": "string", "maxCapacity": 2,
  "basePrice": 120.00, "amenities": ["string"], "active": true }
```

### Habitación
```jsonc
// request
{ "number": "101", "floor": 1, "roomTypeId": 1, "status": "AVAILABLE",
  "observations": "string?" }
// response
{ "id": 1, "number": "101", "floor": 1, "roomTypeId": 1, "roomTypeName": "Doble",
  "status": "AVAILABLE", "observations": "string" }
```

### Reserva
```jsonc
// request create
{ "guestId": 1, "checkIn": "2026-07-01", "checkOut": "2026-07-03",
  "adults": 2, "children": 0, "roomTypeId": 1, "roomId": 1?,
  "notes": "string?", "specialRequests": "string?" }
// response (detalle)
{ "id": 1, "status": "CONFIRMED", "guestId": 1, "guestName": "string",
  "checkIn": "2026-07-01", "checkOut": "2026-07-03", "nights": 2,
  "adults": 2, "children": 0, "roomTypeId": 1, "roomTypeName": "Doble",
  "rooms": [ { "roomId": 1, "roomNumber": "101", "checkIn": "2026-07-01", "checkOut": "2026-07-03" } ],
  "nightlyPrice": 120.00, "totalAmount": 240.00,
  "paidAmount": 0.00, "balance": 240.00,
  "notes": "string", "specialRequests": "string",
  "checkInAt": null, "checkOutAt": null,
  "createdAt": "2026-06-17T10:00:00" }

// POST /cancel  -> status CANCELLED
// POST /assign-room { "roomId": 1 }
// POST /check-in  { "roomId": 1? }  response: reserva actualizada
// POST /check-out  response: reserva actualizada
```

### Disponibilidad
```jsonc
// GET /api/availability?checkIn=2026-07-01&checkOut=2026-07-03&guests=2&roomTypeId=1
// response
[ { "roomId": 1, "number": "101", "floor": 1, "roomTypeId": 1,
    "roomTypeName": "Doble", "maxCapacity": 2, "basePrice": 120.00 } ]
```

### Pago
```jsonc
// POST /api/reservations/{id}/payments
{ "amount": 100.00, "method": "CASH|CREDIT_CARD|DEBIT_CARD|BANK_TRANSFER",
  "reference": "string?" }
// response
{ "id": 1, "reservationId": 1, "amount": 100.00, "method": "CASH",
  "status": "COMPLETED", "reference": "string", "paidAt": "2026-06-17T10:00:00" }
// PATCH /api/payments/{id}/status
{ "status": "COMPLETED|REFUNDED|CANCELLED" }
```

### Dashboard
```jsonc
// GET /api/dashboard?from=2026-06-17&to=2026-06-17
{ "arrivalsToday": 2, "departuresToday": 1, "occupiedRooms": 5,
  "availableRooms": 10, "cleaningRooms": 2, "occupancyRate": 33.3,
  "incomePeriod": 1250.00, "recentReservations": [ { ...reserva resumida } ] }
```

### Errores
```jsonc
// 4xx / 5xx
{ "timestamp": "2026-06-17T10:00:00", "status": 400,
  "error": "Bad Request", "code": "RESERVATION_OVERLAP",
  "message": "string", "path": "/api/reservations",
  "fieldErrors": [ { "field": "checkOut", "message": "string" } ] }
```

Códigos de error de negocio: `RESERVATION_OVERLAP`, `INVALID_DATES`,
`ROOM_NOT_AVAILABLE`, `INVALID_STATE_TRANSITION`, `CHECKIN_NOT_ALLOWED`,
`CHECKOUT_NOT_ALLOWED`, `DUPLICATE_CHECKIN`, `CAPACITY_EXCEEDED`,
`CANCEL_NOT_ALLOWED`, `RESOURCE_NOT_FOUND`, `FORBIDDEN`, `CONFLICT`.

## 8. Criterios de aceptación

1. **Login** funciona para ADMIN y RECEPCIONISTA y devuelve JWT válido.
2. Rutas frontend protegidas: sin token -> `/login`; sin rol -> página 403.
3. Crear reserva válida devuelve 201 con `nights` y `totalAmount` correctos.
4. Fechas inválidas (`checkOut <= checkIn`) -> 400 `INVALID_DATES`.
5. Reserva superpuesta en la misma habitación -> 409 `RESERVATION_OVERLAP`.
6. Búsqueda de disponibilidad devuelve solo habitaciones libres y con capacidad.
7. Cancelar reserva `CONFIRMED` futura -> 200 y libera habitación.
8. Check-in de reserva `CONFIRMED` vigente -> 200, estado `CHECKED_IN`, habitación `OCCUPIED`.
9. Check-in duplicado -> 409 `DUPLICATE_CHECKIN`.
10. Registrar pago `COMPLETED` actualiza `paidAmount` y `balance`.
11. Saldo = `totalAmount - paidAmount` calculado correctamente.
12. Check-out -> 200, estado `CHECKED_OUT`, habitación `CLEANING`.
13. Endpoint de ADMIN accedido por RECEPCIONISTA -> 403 `FORBIDDEN`.
14. Paginación funciona en `/api/guests`, `/api/rooms`, `/api/reservations`.
15. OpenAPI publicado en `/api/docs` y `/api/openapi.json` accesible sin auth.
16. `./mvnw test` y `npm run test` en verde. `npm run build` y `./mvnw verify` exitosos.

## 9. Convenciones técnicas

- Fechas de calendario: `LocalDate` (Java) / `string YYYY-MM-DD` (JSON).
- Fechas y horas de auditoría: `Instant`/`LocalDateTime` almacenado en UTC; JSON en ISO-8601 con offset Z.
- Importes: `BigDecimal` (Java), escala 2; JSON como número con 2 decimales.
- IDs: `Long` autogenerados (bigserial en BD).
- Contraseñas: BCrypt (coste 10).
- JWT: HS256, claim `sub`=userId, `email`, `role`, `exp`; expiración 1h (configurable).
- Cabecera auth: `Authorization: Bearer <token>`.
- CORS: permitir `http://localhost:5173` en dev.
- Variables de entorno en `.env` (no commitear `.env`); ejemplo en `.env.example`.
- Migraciones Flyway: `V1__init.sql`, `V2__seed.sql`, etc. en
  `backend/src/main/resources/db/migration`.
- No exponer entidades JPA en controladores; siempre DTO + mapper.
- Manejo global de errores con `@RestControllerAdvice`.
- Documentación OpenAPI con anotaciones en controladores.
