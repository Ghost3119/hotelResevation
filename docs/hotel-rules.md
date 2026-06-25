# Hotel Manager — Reglas hoteleras definitivas

Documento maestro de reglas de negocio para la evolución del MVP. Complementa
las reglas existentes RN-1..RN-10 de `architecture.md` con las reglas de
evolución ER-1..ER-30. Es el **contrato vinculante** para Database, Backend,
Frontend, API Contracts, Security y Testing.

**Moneda**: MXN. **Zona horaria**: `America/Mexico_City`. **Importes**:
`NUMERIC(12,2)` / `BigDecimal`, escala 2, nunca `double`.

---

## 1. Tarifas

### ER-1 Planes tarifarios (`rate_plans`)
Un *plan tarifario* define los precios por noche para un tipo de habitación,
con tarifa diferenciada por día de la semana y una política de cancelación
asociada.

Campos: `id`, `code` (único), `name`, `room_type_id` (FK), `weekday_rates`
(7 valores lun-dom, `NUMERIC(12,2)`), `adult_extra_rate`, `child_extra_rate`,
`cancellation_policy_id` (FK, nullable), `min_nights` (default 1), `max_nights`
(nullable), `active`, `valid_from` (DATE), `valid_to` (DATE, nullable),
`created_at`, `updated_at`.

Reglas:
- ER-1.1: Un `room_type` puede tener múltiples planes activos simultáneos,
  pero solo uno es el *plan por defecto* (`is_default = true`).
- ER-1.2: `valid_to` nullable = sin fin. `valid_from <= valid_to` si ambos
  presentes.
- ER-1.3: `min_nights >= 1`; `max_nights > min_nights` si no es null.
- ER-1.4: Las tarifas de día de semana (`weekday_rates`)son >= 0.
- ER-1.5: Los cargos extra (`adult_extra_rate`, `child_extra_rate`) >= 0.

### ER-2 Temporadas (`seasonal_rates`)
Una *temporada* define un multiplicador o precio absoluto para un rango de
fechas dentro de un plan tarifario.

Campos: `id`, `rate_plan_id` (FK), `name`, `start_date` (DATE), `end_date`
(DATE), `season_type` (ALTA, MEDIA, BAJA), `price_mode` (MULTIPLIER o ABSOLUTE),
`weekday_multiplier` (7 valores, `NUMERIC(5,3)`) o `weekday_price` (7 valores,
`NUMERIC(12,2)`), `created_at`, `updated_at`.

Reglas:
- ER-2.1: `start_date < end_date`.
- ER-2.2: **No solapamiento**: dos temporadas del mismo `rate_plan_id` no
  pueden tener rangos solapados. Restricción a nivel de aplicación + índice
  GiST EXCLUDE sobre `(rate_plan_id, daterange(start_date, end_date, '[)'))`.
- ER-2.2-AC: Intentar crear una temporada solapada → 409 `SEASON_OVERLAP`.

### ER-3 Precios especiales por fecha (`daily_rate_overrides`)
Un *override* fija el precio de una fecha concreta para un tipo de habitación
y un plan, con máxima prioridad.

Campos: `id`, `room_type_id` (FK), `rate_plan_id` (FK, nullable), `date` (DATE),
`price` (`NUMERIC(12,2)`), `reason` (TEXT, nullable), `created_at`, `updated_at`.

Reglas:
- ER-3.1: Un override por `(room_type_id, rate_plan_id, date)` — UNIQUE.
- ER-3.2: `rate_plan_id` null = aplica a cualquier plan.
- ER-3.3: `price >= 0`.

### ER-4 Promociones (`promotion_rules`)
Una *promoción* aplica un descuento condicional.

Campos: `id`, `code` (único), `description`, `discount_type` (PERCENTAGE o
FIXED), `discount_value` (`NUMERIC(12,2)`), `min_nights` (INT, default 1),
`min_guests` (INT, default 1), `valid_from` (DATE), `valid_to` (DATE, nullable),
`rate_plan_id` (FK, nullable — null = cualquier plan), `stackable` (BOOLEAN
default false), `priority` (INT default 0), `active`, `created_at`,
`updated_at`.

Reglas:
- ER-4.1: `discount_value >= 0`; si PERCENTAGE, `0 <= value <= 100`.
- ER-4.2: Si `stackable = false`, solo se aplica UNA promo por reserva (la de
  mayor `priority`, o la primera por `code` si empate).
- ER-4.3: Si `stackable = true`, múltiples promos pueden apilarse, pero el
  descuento total no puede exceder el 100% del subtotal.
- ER-4.4: Una promo aplica si: fecha de checkIn dentro de `[valid_from,
  valid_to]`, `nights >= min_nights`, `guests >= min_guests`, y `rate_plan_id`
  coincide (o es null).
- ER-4.4-AC: Promociones incompatibles (no stackable, misma prioridad) → se
  aplica la de código alfabéticamente menor y se registra la omisión en el
  desglose.

### ER-5 Impuestos y cargos (`taxes_and_fees`)
Un *impuesto o cargo* es configurable con vigencia temporal.

Campos: `id`, `name`, `type` (TAX_PERCENT, FEE_FIXED), `value` (`NUMERIC(12,2)`
— porcentaje si TAX, importe fijo si FEE), `applies_to` (ROOM_RATE, TOTAL,
PER_NIGHT), `valid_from` (DATE), `valid_to` (DATE, nullable), `active`,
`created_at`, `updated_at`.

Reglas:
- ER-5.1: `valid_from <= valid_to` si ambos presentes.
- ER-5.2: Múltiples impuestos activos en la misma fecha se aplican
  aditivamente (uno tras otro sobre la base correspondiente).
- ER-5.3: `TAX_PERCENT` con `applies_to=ROOM_RATE`: se calcula sobre la suma de
  tarifas por noche. `applies_to=TOTAL`: sobre el total tras otros impuestos.
  `applies_to=PER_NIGHT`: por cada noche individualmente.
- ER-5.4: `FEE_FIXED` suma un importe fijo (por reserva si `applies_to=TOTAL`,
  por noche si `PER_NIGHT`).
- ER-5.5: No codificar tasas fiscales mexicanas (IVA, ISH) como constantes.
  Deben configurarse con vigencia y quedar sujetas a revisión contable/legal.
- ER-5.6: Solo los impuestos `active=true` y vigentes en la fecha de la noche
  se aplican. Si un impuesto cambia vigencia, las reservas confirmadas no se
  recalculan (snapshot inmutable).

### ER-6 Restricciones de estancia y fechas cerradas
- ER-6.1: `min_nights` y `max_nights` del plan tarifario se validan al crear
  y modificar la reserva. Violación → 422 `STAY_RESTRICTION`.
- ER-6.2: Fechas cerradas para llegada (CTA) y salida (CTD) se modelan como
  `daily_rate_overrides` con un campo `closed_to_arrival` / `closed_to_departure`
  booleano opcional, o como una tabla `closed_dates` separada con
  `(room_type_id nullable, date, closed_arrival, closed_departure)`. Decisión:
  tabla `closed_dates` para simplicidad.
  Campos: `id`, `room_type_id` (nullable), `date` (DATE), `closed_arrival`
  (BOOLEAN), `closed_departure` (BOOLEAN).
- ER-6.2-AC: Intentar reservar con checkIn en fecha CTA → 422 `CLOSED_TO_ARRIVAL`.

### ER-7 Precedencia de tarifas
Orden de prioridad (mayor a menor) para una noche dada:
1. `daily_rate_overrides` (fecha + room_type + plan, o fecha + room_type sin
   plan) para esa fecha.
2. `seasonal_rates` del plan tarifario, vigentes en esa fecha.
3. `weekday_rates` del plan tarifario (tarifa por día de semana).
4. `room_types.base_price` (fallback).

### ER-8 Cotización (antes de confirmar)
- `POST /api/availability/quote` — entra: `checkIn`, `checkOut`, `roomTypeId`,
  `adults`, `children`, `ratePlanId` (opcional, default = plan por defecto),
  `promotionCode` (opcional).
- Sale: desglose por noche (`[{date, baseRate, extraPersonCharge, discount,
  taxes, fees, total}]`) + `subtotal`, `totalDiscount`, `totalTaxes`,
  `totalFees`, `grandTotal`.
- La cotización es **informativa**: no bloquea inventario ni garantiza precio.
  El precio se congela solo al confirmar (snapshot ER-9).

---

## 2. Precio histórico inmutable

### ER-9 Snapshot por noche (`reservation_nightly_rates`)
- Al pasar una reserva de PENDING a CONFIRMED, el sistema crea una fila
  `reservation_nightly_rates` por cada noche del rango.
- Campos: `id`, `reservation_id` (FK), `rate_plan_id` (FK), `night_date` (DATE),
  `base_rate`, `extra_person_charge`, `discount_amount`, `taxes_amount`,
  `fees_amount`, `total` (todos `NUMERIC(12,2)`), `included` (BOOLEAN default
  true), `created_at`.
- **Inmutables**: una vez creadas, `base_rate`, `extra_person_charge`,
  `discount_amount`, `taxes_amount`, `fees_amount`, `total` no se modifican.
- `included` puede pasar a `false` al reducir estancia (para auditoría), pero
  nunca a true de nuevo.
- `reservations.total_amount` = `SUM(reservation_nightly_rates.total) WHERE
  included = true`. Recalcular solo al añadir/quitar noches.
- ER-9-AC: Cambiar una tarifa futura (`rate_plans`, `seasonal_rates`,
  `daily_rate_overrides`) NO altera reservas confirmadas existentes.

---

## 3. Reservaciones (evolucionadas)

### Estados (sin cambios en el diagrama, nuevas reglas)
```
PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
PENDING → CANCELLED
CONFIRMED → CANCELLED (con penalización, ER-11)
CONFIRMED → NO_SHOW (manual ER-12 o automática ER-13)
CHECKED_IN → CHECKED_OUT
```
- No se puede modificar una reserva CHECKED_IN, CHECKED_OUT, CANCELLED ni
  NO_SHOW (salvo cambio de habitación ER-15 y checkout ER-18).

### ER-10 Cancelación con políticas (`cancellation_policies`)
- `cancellation_policies`: `id`, `name`, `deadline_hours` (INT — horas antes
  de checkIn), `penalty_type` (NONE, PERCENTAGE, FIXED, FIRST_NIGHT),
  `penalty_value` (`NUMERIC(12,2)` — porcentaje o importe), `no_show_penalty_type`
  (mismos valores), `no_show_penalty_value`, `active`, `created_at`,
  `updated_at`.
- Una reserva referencia `cancellation_policy_id` al confirmar (copiada desde
  el plan tarifario).
- ER-10.1: Cancelar si `checkIn > hoy` y dentro del deadline → sin penalización
  (o política NONE).
- ER-10.2: Cancelar si `checkIn > hoy` pero fuera del deadline → penalización
  calculada según `penalty_type`:
  - NONE: 0.
  - PERCENTAGE: `%` del `total_amount`.
  - FIXED: importe fijo.
  - FIRST_NIGHT: tarifa de la primera noche (del snapshot).
- ER-10.3: La penalización se registra como un `reservation_adjustment` y, si
  genera un cargo, como un `Payment` con `method=ADJUSTMENT`, `status=COMPLETED`,
  `reference='CANCELLATION_PENALTY'`.
- ER-10.4: Cancelar requiere `reason` (motivo, TEXT no null) y `user_id`
  (automático del contexto).
- ER-10.5: Cancelar libera la asignación de habitación (DELETE reservation_rooms
  + room → AVAILABLE/RESERVED según otros assignments).
- ER-10-AC1: Cancelar antes del deadline → 200, sin penalización.
- ER-10-AC2: Cancelar después del deadline → 200, con penalización en
  `reservation_adjustments` y `payments`.

### ER-11 No-show manual
- Operador marca NO_SHOW una reserva CONFIRMED con `checkIn < hoy`.
- Genera penalización según `no_show_penalty_type/penalty_value` de la política
  (o `penalty_type/penalty_value` si no hay específica de no-show).
- Libera la habitación.
- Auditoría + `reason`.
- ER-11-AC: Marcar NO_SHOW una reserva CONFIRMED vencida → 200, estado NO_SHOW,
  penalización registrada.

### ER-12 No-show automático
- Un job (`@Scheduled` o endpoint manual `/api/admin/mark-no-shows`) identifica
  reservas CONFIRMED con `checkIn < hoy` y marca NO_SHOW.
- Usar la misma lógica de penalización que el manual.
- Registrar `audit_event` con `action=AUTO_NO_SHOW`.
- ER-12-AC: Ejecutar el job → reservas vencidas pasan a NO_SHOW; reservas con
  checkIn hoy o futuro no se afectan.

### ER-13 Modificación de estancia
- Tipos: EXTEND (nuevo checkOut > actual), REDUCE (nuevo checkOut < actual),
  CHANGE_CHECKIN (mover checkIn).
- Solo permitido para reservas PENDING o CONFIRMED (`checkIn > hoy`).
- ER-13.1: Validar disponibilidad para el nuevo rango con bloqueo pesimista.
- ER-13.2: EXTEND: crear nuevos `reservation_nightly_rates` para las noches
  nuevas usando el motor tarifario vigente a la fecha de modificación. Recalcular
  `total_amount`.
- ER-13.3: REDUCE: marcar `included=false` en los snapshots de las noches
  eliminadas. Recalcular `total_amount`. Liberar `reservation_rooms` para las
  noches eliminadas.
- ER-13.4: CHANGE_CHECKIN: re-snapshot las noches afectadas (with new rates if
  dates moved) within a transaction. Avoid overlap.
- ER-13.5: Registrar `reservation_adjustment` con tipo (`EXTEND`, `REDUCE`,
  `CHANGE_DATES`), `old_value`, `new_value`, `reason`, `user_id`, `created_at`.
- ER-13-AC1: Extender una estancia 2 noches más → nuevos snapshots para las 2
  noches, total recalculado.
- ER-13-AC2: Reducir 1 noche → snapshot marcado `included=false`, total
  recalculado, habitación liberada para esa noche.

### ER-14 Reservaciones grupales (`reservation_groups`)
- `reservation_groups`: `id`, `name`, `contact_guest_id` (FK), `notes`,
  `created_by` (FK), `created_at`.
- Cada reserva del grupo referencia `group_id` (nullable FK).
- ER-14.1: Una grupos tiene N reservas. Cada reserva puede tener su propio
  huésped, tipo de habitación, fechas.
- ER-14.2: Operación de cancelación grupal: cancela todas las reservas del
  grupo que no estén CANCELLED/NO_SHOW/CHECKED_OUT.
- ER-14.3: La política de cancelación aplica por reserva individual (no hereda
  del grupo), salvo configuración de la política con `applies_to =
  GROUP_LEADER_ONLY`.
- ER-14.4: Crear un grupo requiere al menos nombre y un huésped de contacto.

### ER-15 Asignación de múltiples habitaciones
- Una reserva puede tener múltiples `reservation_rooms` (varias habitaciones).
- `reservation_rooms` ya soporta esto; el constraint GiST es por `room_id +
  daterange`.
- ER-15.1: Al asignar una habitación, validar disponibilidad con bloqueo
  pesimista por habitación.
- ER-15.2: La capacidad total (`adults + children`) se valida contra la suma de
  capacidades de las habitaciones asignadas, o contra el `room_type.max_capacity`
  individual (decisión: validar por `room_type.max_capacity` individual, ya que
  todas las habitaciones de una reserva son del mismo tipo).

---

## 4. Operación

### ER-16 Cambio de habitación durante estancia (`room_stays`)
- `room_stays`: `id`, `room_id` (FK), `reservation_id` (FK), `check_in` (DATE),
  `check_out` (DATE), `actual_check_in` (TIMESTAMPTZ), `actual_check_out`
  (TIMESTAMPTZ, nullable), `created_at`.
- ER-16.1: Solo para reservas CHECKED_IN.
- ER-16.2: Cerrar el `room_stays` actual (`actual_check_out = now`), crear uno
  nuevo para la nueva habitación.
- ER-16.3: Actualizar `reservation_rooms`: DELETE el assignment antiguo para el
  rango restante, INSERT nuevo. Bloqueo pesimista. Validar solapamiento.
- ER-16.4: La habitación antigua pasa a CLEANING; la nueva a OCCUPIED.
- ER-16.5: Registrar `reservation_adjustment` con tipo `CHANGE_ROOM`.
- ER-16-AC: Cambiar de hab. 101 a 102 durante estancia → 101 CLEANING, 102
  OCCUPIED, `room_stays` registra ambos, `reservation_adjustment` creado.

### ER-17 Bloqueos operativos (`room_blocks`)
- `room_blocks`: `id`, `room_id` (FK), `start_date` (DATE), `end_date` (DATE),
  `block_type` (MAINTENANCE, OPERATIONAL), `reason` (TEXT), `created_by` (FK),
  `created_at`, `released_at` (TIMESTAMPTZ, nullable).
- ER-17.1: GiST EXCLUDE sobre `(room_id, daterange(start_date, end_date, '[)'))`
  — igual que `reservation_rooms`, pero independiente.
- ER-17.2: Una habitación bloqueada se excluye de `/availability`.
- ER-17.3: `released_at` (nullable) libera el bloqueo; la habitación vuelve a
  estar disponible si su `status` es AVAILABLE.
- ER-17.4: Conflictos entre `room_blocks` y `reservation_rooms` no se
  previenen a nivel de BD (son tablas separadas); el backend valida que no se
  asigne una reserva a una habitación con un bloqueo solapante.
- ER-17-AC: Crear bloqueo en hab. 101 para una fecha futura → 101 no aparece
  en availability para esas fechas.

### ER-18 Housekeeping (`housekeeping_tasks`)
- Enum `HousekeepingStatus`: DIRTY, CLEANING, INSPECTED, READY.
- `housekeeping_tasks`: `id`, `room_id` (FK), `status` (HousekeepingStatus default
  DIRTY), `priority` (LOW, NORMAL, HIGH, URGENT), `assigned_to` (FK users,
  nullable), `notes` (TEXT), `created_at`, `updated_at`, `completed_at`
  (TIMESTAMPTZ, nullable), `created_by` (FK).
- ER-18.1: Tras check-out (`OCCUPIED → CLEANING`), se crea un task con
  `status=DIRTY`.
- ER-18.2: Flujo: DIRTY → (asignar) → CLEANING → (limpieza completa) →
  INSPECTED → (inspección OK) → READY → (habitación disponible = AVAILABLE,
  `completed_at` set).
- ER-18.3: Una habitación no puede pasar a AVAILABLE si su último task no está
  READY.
- ER-18.4: Si la inspección falla, el task vuelve a DIRTY (con nota del
  motivo).
- ER-18.5: El rol HOUSEKEEPING solo puede ver/actualizar tareas de limpieza;
  no accede a datos de huéspedes ni financieros.
- ER-18-AC: Check-out → task DIRTY; limpiador marca CLEANING → INSPECTED →
  inspector marca READY → hab. AVAILABLE.

---

## 5. Check-in / Check-out (evolucionados)

### ER-19 Check-in anticipado
- Configurable: `app.checkin.early-checkin-allowed` (BOOLEAN default false),
  `app.checkin.early-checkin-hours` (INT default 0).
- Si `allowed = true`, se permite check-in hasta `early-checkin-hours` antes
  del `checkIn` de la reserva.
- Si `allowed = false` o la anticipación excede el límite → 422
  `EARLY_CHECKIN_NOT_ALLOWED`.
- ER-19-AC: Con `early-checkin-hours=2`, una reserva con checkIn=15:00 se
  permite hacer check-in desde las 13:00.

### ER-20 Check-out tardío
- Configurable: `app.checkout.late-checkout-allowed` (BOOLEAN default false),
  `app.checkout.late-checkout-hours` (INT default 0).
- Si `allowed = true`, se permite checkout hasta `late-checkout-hours` después
  del `checkOut` de la reserva.
- Si excede → 422 `LATE_CHECKOUT_NOT_ALLOWED`.

### ER-21 Excepción de saldo pendiente
- Por defecto, si `balance > 0` al hacer checkout → 422 `OUTSTANDING_BALANCE`.
- Excepción: si el usuario tiene rol MANAGER o ADMIN Y el request incluye
  `reason` (TEXT no vacío):
  - Se permite el checkout.
  - Se registra `audit_event` con `action=CHECKOUT_WITH_BALANCE`, `metadata`
    con `balance`, `reason`, `user_id`.
- RECEPCIONISTA no puede hacer checkout con saldo (sin la bandera de permiso).
- ER-21-AC1: RECEP intenta checkout con saldo → 422.
- ER-21-AC2: MANAGER con reason → 200, auditoría registrada.

---

## 6. Zona horaria

### ER-22 Zona horaria America/Mexico_City
- Todas las fechas de negocio (checkIn, checkOut, seasons, overrides,
  closed_dates) son `DATE` (sin zona horaria).
- "Hoy" para lógica de negocio = `LocalDate.now(ZoneId.of("America/Mexico_City"))`.
- Timestamps de auditoría (`check_in_at`, `created_at`, etc.) son `TIMESTAMPTZ`
  almacenados en UTC; el frontend los muestra en Mexico City.
- Hibernate: `hibernate.jdbc.time_zone` se mantiene en UTC para almacenamiento.
  El servicio usa `ZoneId.of("America/Mexico_City")` para `LocalDate.now()`.
- ER-22-AC: A las 23:00 UTC del día X (que es 17:00 CST del día X si CST=UTC-6),
  "hoy" = día X (Mexico City).

---

## 7. Concurrencia

### ER-23 Bloqueo y aislamiento
- ER-23.1: Asignación de habitación y check-in usan `@Transactional` +
  `SELECT ... FOR UPDATE` sobre la habitación (bloqueo pesimista).
- ER-23.2: Modificación de estancia usa bloqueo pesimista sobre la reserva y
  las habitaciones afectadas.
- ER-23.3: Rotación de refresh token es atómica: `UPDATE ... SET revoked_at
  = now WHERE token_hash = ? AND revoked_at IS NULL` + `INSERT` nuevo. Si el
  `UPDATE` afecta 0 filas → el token ya fue usado → reutilización detectada.
- ER-23.4: Actualización de tarifas usa bloqueo optimista (`@Version`) en
  `rate_plans` y `seasonal_rates` para prevenir updates perdidos.
- ER-23.5: Las violaciones de GiST (SQLSTATE 23P01) se traducen a 409
  `RESERVATION_OVERLAP` o `BLOCK_OVERLAP`.
- ER-23-AC1: 2 reservas simultáneas para la misma habitación → 1 éxito, 1 →
  409 `RESERVATION_OVERLAP`.
- ER-23-AC2: 2 rotaciones simultáneas del mismo refresh token → 1 éxito, 1 →
  401 (reuse detection).

---

## 8. Privacidad (cross-ref a `privacy-threat-model.md`)

### ER-24 Enmascaramiento de documentos
- `GuestDto.documentNumber` se enmascara por defecto: `X1234567Z` → `X•••••7Z`
  (primer y último carácter, resto `•`).
- Revelar el documento completo requiere permiso `VIEW_FULL_PII` otorgado
  puntualmente por ADMIN/PRIVACY_OFFICER.
- ER-24-AC: RECEP obtiene `/api/guests/1` → `documentNumber: "X•••••7Z"`;
  PRIVACY_OFFICER con permiso → `documentNumber: "X1234567Z"`.

### ER-25 Auditoría de acceso a datos
- Cada lectura de datos completos (no enmascarados) genera un
  `personal_data_access_log`: `user_id`, `guest_id`, `action` (VIEW, EXPORT,
  MODIFY), `justification` (TEXT), `created_at`.
- ER-25-AC: PRIVACY_OFFICER exporta datos de huésped → se crea
  `personal_data_access_log` con `action=EXPORT`.

### ER-26 Solicitudes de privacidad (`privacy_requests`)
- Tipos: EXPORT, RECTIFY, DELETE.
- `privacy_requests`: `id`, `guest_id` (FK), `type`, `status` (PENDING,
  IN_PROGRESS, COMPLETED, REJECTED), `requested_at`, `completed_at`,
  `handled_by` (FK), `notes`.
- ER-26.1: EXPORT → genera los datos del huésped + reservas + pagos; audita.
- ER-26.2: RECTIFY → actualiza datos; audita.
- ER-26.3: DELETE → anonimiza: sustituye `first_name` = 'ANONIMIZADO',
  `last_name` = '', `email` = null, `phone` = null, `document_number` = null,
  `nationality` = null. Conserva `guest_id` y reservas/pagos (integridad
  financiera).
- ER-26.4: Anonimización es irreversible.
- ER-26.5: Solo PRIVACY_OFFICER y ADMIN pueden gestionar solicitudes.

---

## 9. Nuevos roles

### ER-27 Roles y matriz de permisos
| Recurso              | ADMIN | MANAGER | RECEP | HOUSEKEEPING | PRIVACY_OFFICER |
|----------------------|:-----:|:-------:|:-----:|:------------:|:---------------:|
| users                | CRUD  | —       | —     | —            | R               |
| rate-plans           | CRUD  | CRUD    | R     | —            | —               |
| seasonal-rates       | CRUD  | CRUD    | R     | —            | —               |
| taxes-fees           | CRUD  | CRUD    | R     | —            | —               |
| cancellation-policies| CRUD  | CRUD    | R     | —            | —               |
| rooms                | CRUD  | R       | R+status | R+status   | —               |
| room-blocks          | CRUD  | CRUD    | —     | —            | —               |
| housekeeping-tasks   | R     | R+assign| —     | R+update     | —               |
| guests               | R*    | R*      | CRUD* | —            | R               |
| reservations         | CRUD  | CRUD    | CRUD  | —            | R               |
| availability+quote   | R     | R       | R     | —            | —               |
| payments             | CRUD  | CRUD    | CRUD  | —            | —               |
| dashboard            | R     | R       | R     | R (cleaning) | —               |
| privacy-requests     | R     | —       | —     | —            | CRUD            |
| personal-data-logs   | R     | —       | —     | —            | R               |
| audit-events         | R     | R       | —     | —            | R               |

`R*` = lectura con enmascaramiento; revelación completa requiere permiso puntual.

---

## 10. Endpoints nuevos (resumen para API Contracts)

### Tarifas
| Método | Path                           | Roles                 |
|--------|--------------------------------|-----------------------|
| GET    | `/api/rate-plans`              | ADMIN, MANAGER, RECEP |
| POST   | `/api/rate-plans`              | ADMIN, MANAGER        |
| GET    | `/api/rate-plans/{id}`         | ADMIN, MANAGER, RECEP |
| PUT    | `/api/rate-plans/{id}`         | ADMIN, MANAGER        |
| PATCH  | `/api/rate-plans/{id}/status`  | ADMIN, MANAGER        |
| GET    | `/api/seasonal-rates`          | * ( filtered by plan) |
| POST   | `/api/seasonal-rates`          | ADMIN, MANAGER        |
| PUT    | `/api/seasonal-rates/{id}`     | ADMIN, MANAGER        |
| DELETE | `/api/seasonal-rates/{id}`     | ADMIN, MANAGER        |
| GET    | `/api/daily-rate-overrides`     | *                     |
| POST   | `/api/daily-rate-overrides`     | ADMIN, MANAGER        |
| PUT    | `/api/daily-rate-overrides/{id}`| ADMIN, MANAGER        |
| DELETE | `/api/daily-rate-overrides/{id}`| ADMIN, MANAGER        |
| GET    | `/api/taxes-and-fees`          | *                     |
| POST   | `/api/taxes-and-fees`          | ADMIN, MANAGER        |
| PUT    | `/api/taxes-and-fees/{id}`     | ADMIN, MANAGER        |
| GET    | `/api/promotion-rules`         | *                     |
| POST   | `/api/promotion-rules`         | ADMIN, MANAGER        |
| PUT    | `/api/promotion-rules/{id}`    | ADMIN, MANAGER        |
| GET    | `/api/cancellation-policies`   | *                     |
| POST   | `/api/cancellation-policies`   | ADMIN, MANAGER        |
| PUT    | `/api/cancellation-policies/{id}` | ADMIN, MANAGER     |

### Cotización
| Método | Path                           | Roles            |
|--------|--------------------------------|------------------|
| POST   | `/api/availability/quote`      | ADMIN, MANAGER, RECEP |

### Reservas (nuevas operaciones)
| Método | Path                                    | Roles            |
|--------|-----------------------------------------|------------------|
| PUT    | `/api/reservations/{id}/modify-stay`    | ADMIN, MANAGER, RECEP |
| POST   | `/api/reservations/{id}/no-show`       | ADMIN, MANAGER, RECEP |
| POST   | `/api/reservations/{id}/change-room`    | ADMIN, MANAGER, RECEP |
| GET    | `/api/reservations/{id}/nightly-rates`  | ADMIN, MANAGER, RECEP |
| GET    | `/api/reservations/{id}/adjustments`    | ADMIN, MANAGER, RECEP |

### Grupos
| Método | Path                           | Roles            |
|--------|--------------------------------|------------------|
| GET    | `/api/reservation-groups`      | ADMIN, MANAGER, RECEP |
| POST   | `/api/reservation-groups`      | ADMIN, MANAGER, RECEP |
| GET    | `/api/reservation-groups/{id}` | ADMIN, MANAGER, RECEP |
| POST   | `/api/reservation-groups/{id}/cancel` | ADMIN, MANAGER |

### Bloqueos
| Método | Path                          | Roles            |
|--------|-------------------------------|------------------|
| GET    | `/api/room-blocks`            | ADMIN, MANAGER, RECEP |
| POST   | `/api/room-blocks`            | ADMIN, MANAGER   |
| PUT    | `/api/room-blocks/{id}`       | ADMIN, MANAGER   |
| POST   | `/api/room-blocks/{id}/release` | ADMIN, MANAGER |

### Housekeeping
| Método | Path                               | Roles                    |
|--------|-------------------------------------|--------------------------|
| GET    | `/api/housekeeping-tasks`           | ADMIN, MANAGER, HOUSEKEEPING |
| POST   | `/api/housekeeping-tasks`           | ADMIN, MANAGER           |
| PATCH  | `/api/housekeeping-tasks/{id}/status` | ADMIN, MANAGER, HOUSEKEEPING |

### Privacidad
| Método | Path                              | Roles            |
|--------|-----------------------------------|------------------|
| GET    | `/api/privacy-requests`           | ADMIN, PRIVACY_OFFICER |
| POST   | `/api/privacy-requests`           | PRIVACY_OFFICER  |
| GET    | `/api/privacy-requests/{id}`      | ADMIN, PRIVACY_OFFICER |
| PUT    | `/api/privacy-requests/{id}`      | PRIVACY_OFFICER  |
| POST   | `/api/privacy-requests/{id}/export` | PRIVACY_OFFICER |
| POST   | `/api/privacy-requests/{id}/anonymize` | PRIVACY_OFFICER |
| GET    | `/api/guests/{id}/full`           | PRIVACY_OFFICER (with VIEW_FULL_PII) |
| GET    | `/api/personal-data-access-logs` | ADMIN, PRIVACY_OFFICER |

### Admin
| Método | Path                          | Roles            |
|--------|-------------------------------|------------------|
| POST   | `/api/admin/mark-no-shows`     | ADMIN, MANAGER   |