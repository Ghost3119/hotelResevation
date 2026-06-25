# Hotel Manager — Plan de reglas hoteleras

Documento de planificación para la evolución del MVP hacia un sistema hotelero
completo con reglas tarifarias, políticas de reservación, housekeeping, y
protección de datos personales. Es la entrada para el **DOMAIN ARCHITECT** que
producirá `docs/hotel-rules.md` con criterios de aceptación definitivos.

## 1. Estado actual (baseline)

### Inventario de entidades JPA (9)
- `User` (id, email, passwordHash, fullName, role, active, createdAt, updatedAt)
- `Guest` (id, firstName, lastName, email, phone, documentNumber, nationality, …)
- `RoomType` (id, name, description, maxCapacity, basePrice, amenities JSONB, active, …)
- `Room` (id, number, floor, roomTypeId, status, observations, …)
- `Reservation` (id, guestId, status, checkIn, checkOut, adults, children, roomTypeId, nightlyPrice, totalAmount, notes, specialRequests, checkInAt, checkOutAt, checkedInBy, checkedOutBy, cancelledAt, cancelledBy, createdBy, …)
- `ReservationRoom` (id, reservationId, roomId, checkIn, checkOut — con GiST EXCLUDE overlap)
- `Payment` (id, reservationId, amount, method, status, reference, paidAt, createdBy, …)
- `AuditEvent` (id, userId, action, entityType, entityId, metadata JSONB, …)
- `RefreshToken` (id, userId, tokenHash, jti, createdAt, expiresAt, revokedAt, replacedByJti, deviceInfo)

### Enums actuales (5)
- `UserRole`: ADMIN, RECEPCIONISTA
- `RoomStatus`: AVAILABLE, RESERVED, OCCUPIED, CLEANING, MAINTENANCE, OUT_OF_SERVICE
- `ReservationStatus`: PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED, NO_SHOW
- `PaymentMethod`: CASH, CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER
- `PaymentStatus`: PENDING, COMPLETED, REFUNDED, CANCELLED

### Endpoints actuales (≈35)
auth (login/refresh/logout/me), users, guests (+reservations), room-types
(+status), rooms (+status/observations), availability, reservations
(+cancel/assign-room/check-in/check-out), payments, dashboard.

### Reglas actuales (RN-1..RN-10 de architecture.md)
- Precios: tarifa plana `nightly_price = roomType.basePrice`; sin temporadas, ni
  impuestos, ni descuentos, ni políticas de cancelación configurables.
- Cancelación: solo si `checkIn > hoy`; sin penalización ni fecha límite.
- Check-in: `CONFIRMED` + `checkIn <= hoy <= checkOut`.
- Check-out: `CHECKED_IN`; saldo pendiente permitido sin permiso especial.
- Housekeeping: estado `CLEANING` tras check-out, sin tareas ni estados
  granulares (DIRTY, INSPECTED, READY).
- Sin bloqueos operativos sin reserva.
- Sin reservas grupales.
- Sin cambio de habitación durante estancia.
- Sin historial de modificaciones.
- Sin protección de datos personales (enmascaramiento, exportación, etc.).

### Migraciones Flyway aplicadas
| Versión | Descripción |
|---------|-------------|
| V1 | Esquema base + btree_gist + EXCLUDE GiST |
| V2 | Datos semilla |
| V3 | Tabla refresh_tokens |
| V4 | Ajuste precios semilla a MXN |

## 2. Brechas identificadas (gap analysis)

### TARIFAS
| Brecha | Estado |
|--------|--------|
| Planes tarifarios múltiples | No existe |
| Tarifa por día de semana | No existe |
| Temporadas (alta/media/baja) | No existe |
| Precios especiales para fechas concretas | No existe |
| Cargos por adulto/niño adicional | No existe |
| Descuentos y promociones | No existe |
| Restricciones de estancia mín/máx | No existe |
| Fechas cerradas para llegada/salida | No existe |
| Impuestos y cargos configurables | No existe |
| Vigencia histórica de reglas | No existe |

### PRECIO HISTÓRICO
| Brecha | Estado |
|--------|--------|
| Desglose inmutable por noche | No existe (solo `nightly_price` plano) |
| Snapshot al confirmar | No existe |
| Cambio de tarifa futura no afecta existentes | Parcial (precio congelado al crear) |

### RESERVACIONES
| Brecha | Estado |
|--------|--------|
| Fecha límite de cancelación | No existe |
| Penalización configurable | No existe |
| Cancelación con motivo y usuario | Parcial (usuario sí, motivo no) |
| No-show manual y automático | Manual sí, automático no |
| Extender/reducir estancia | No existe (PUT edita fechas) |
| Recalcular noches afectadas | No existe (recalcula todo) |
| Historial antes/después del cambio | No existe |
| Reservaciones grupales | No existe |
| Asignación de varias habitaciones | No existe (1:1) |
| Ocupación por habitación | Parcial (1 habitación por reserva) |

### OPERACIÓN
| Brecha | Estado |
|--------|--------|
| Cambio de habitación durante estancia | No existe |
| Historial de ocupación por habitación | No existe |
| Bloqueos por mantenimiento sin reserva | No existe (solo estado ROOM) |
| Bloqueos operativos sin reservación | No existe |
| Housekeeping: DIRTY, CLEANING, INSPECTED, READY | Solo CLEANING |
| Tareas de limpieza con prioridad/responsable/notas | No existe |
| Habitación bloqueada no disponible | Parcial (MAINTENANCE excluido) |

### CHECK-IN / CHECK-OUT
| Brecha | Estado |
|--------|--------|
| Check-in anticipado configurable | No existe |
| Check-out tardío configurable | No existe |
| Excepción de saldo con permiso + motivo + auditoría | No existe (permite cualquier saldo) |

### PRIVACIDAD Y SEGURIDAD
| Brecha | Estado |
|--------|--------|
| Enmascaramiento de documentos | No existe |
| Permiso para revelar datos completos | No existe |
| Auditoría de lectura de datos | No existe |
| Exportación / rectificación / eliminación | No existe |
| Retención configurable | No existe |
| Anonimización preservando finanzas | No existe |
| Roles HOUSEKEEPING, MANAGER, PRIVACY_OFFICER | No existen |
| Zona horaria America/Mexico_City | Hibernate usa UTC |

### CONTRATOS Y TESTING
| Brecha | Estado |
|--------|--------|
| Cliente frontend generado desde OpenAPI | No existe (manual) |
| MSW alineado con backend real | Divergente (PENDING vs CONFIRMED) |
| Pruebas con PostgreSQL real (Testcontainers) | No existe (H2) |
| Pruebas de concurrencia | No existe |
| Pruebas de carga (k6) | No existe |
| Auditoría de accesibilidad (axe) | No existe |

## 3. Nuevo modelo de reglas (propuesta para DOMAIN ARCHITECT)

### 3.1 Entidades de tarifas (a diseñar por DOMAIN + DATABASE)

```
rate_plans
├── seasonal_rates (temporadas con start/end date)
├── daily_rate_overrides (precios especiales por fecha concreta)
├── promotion_rules (descuentos con condiciones)
├── taxes_and_fees (impuestos con vigencia temporal)
└── cancellation_policies (políticas con deadline + penalización)
```

Reglas de precedencia (de mayor a menor prioridad):
1. `daily_rate_overrides` (fecha concreta)
2. `seasonal_rates` (temporada vigente)
3. Tarifa por día de semana del `rate_plan`
4. Precio base del `room_type`

### 3.2 Precio histórico inmutable

`reservation_nightly_rates` — una fila por noche de cada reserva:
- `reservation_id`, `night_date`, `rate_plan_id`, `base_rate`, `adjustments`,
  `discount_amount`, `taxes`, `fees`, `total`
- Se crea como **snapshot al confirmar** (no al cotizar).
- **Nunca se modifica** después de la confirmación.
- Para modificaciones de estancia: se crean nuevas filas solo para las noches
  nuevas; las existentes se conservan inmutables.

### 3.3 Reservas grupales

`reservation_groups` — agrupa múltiples reservas:
- Un grupo tiene un nombre, contacto principal y notas.
- Cada reserva del grupo referencia al grupo.
- Permite múltiples habitaciones por grupo.

### 3.4 Cambio de habitación

`room_stays` — historial de ocupación por habitación:
- `room_id`, `reservation_id`, `check_in`, `check_out`, `status`
- Registra cada cambio de habitación durante una estancia.
- Cuando se cambia de habitación: cierra el `room_stay` anterior y abre uno nuevo.

### 3.5 Bloqueos operativos

`room_blocks` — bloqueos sin reserva:
- `room_id`, `start_date`, `end_date`, `type` (MAINTENANCE, OPERATIONAL),
  `reason`, `created_by`, `created_at`
- Se excluyen de disponibilidad igual que las reservas activas.
- GiST EXCLUDE sobre `(room_id, daterange)` igual que `reservation_rooms`.

### 3.6 Housekeeping

Nuevo enum `HousekeepingStatus`: DIRTY, CLEANING, INSPECTED, READY
`housekeeping_tasks`:
- `room_id`, `status`, `priority`, `assigned_to`, `notes`, `created_at`, `updated_at`
- Transiciones: DIRTY → CLEANING → INSPECTED → READY → (limpia habitación → AVAILABLE)
- La habitación `AVAILABLE` requiere que el último task esté `READY`.

### 3.7 Privacidad

`privacy_requests`:
- `guest_id`, `type` (EXPORT, RECTIFY, DELETE), `status`, `requested_at`,
  `completed_at`, `handled_by`, `notes`
`personal_data_access_logs`:
- `user_id`, `guest_id`, `action` (VIEW, EXPORT, MODIFY), `timestamp`, `justification`

### 3.8 Nuevos roles
- `HOUSEKEEPING`: solo accede a panel de housekeeping.
- `MANAGER`: todo lo de RECEPCIONISTA + reportes + configuración tarifaria.
- `PRIVACY_OFFICER`: gestionar solicitudes de privacidad, anonimización.

## 4. Riesgos

| Riesgo | Mitigación |
|--------|------------|
| Migraciones con datos existentes | Todas las nuevas tablas son aditivas; no se baja ni se altera V1-V4 |
| Snapshot tarifario puede ser costoso | Una fila por noche; ~365 filas/año/reserva (aceptable para boutique) |
| Concurrencia en cálculo tarifario | Transacciones + bloqueo pesimista en asignación |
| GiST en room_blocks | Misma estrategia que reservation_rooms |
| Compatibilidad OpenAPI | Versionar contract; añadir campos opcionales; no romper existentes |
| Pruebas con Testcontainers lentas | Usar contenedor reutilizable; paralelizar |
| Divergencia MSW-backend | Contrato único desde OpenAPI; generar handlers automáticamente |

## 5. Orden de implementación

1. **DOMAIN ARCHITECT**: produce `docs/hotel-rules.md` con reglas definitivas.
2. **DATABASE**: migraciones V5+ basadas en hotel-rules.md.
3. **BACKEND + API CONTRACTS**: acuerdan OpenAPI, implementan backend, generan
   cliente.
4. **SECURITY**: revisa modelo de datos + contratos, añade roles y privacidad.
5. **FRONTEND**: pantallas usando cliente generado.
6. **TEST AUTOMATION**: valida todo.
7. **PERFORMANCE + ACCESSIBILITY**: auditan y corrigen.