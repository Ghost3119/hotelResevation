# Hotel Manager — Estrategia de pruebas

Documento de referencia para el agente **TEST AUTOMATION** y el agente
**PERFORMANCE AND ACCESSIBILITY**. Define la pirámide de pruebas, las
obligatorias, y los umbrales.

## 1. Pirámide de pruebas

```
                    ┌─────────────┐
                    │   E2E (PW)  │   ← Playwright navegador real
                    │     + a11y  │   ← axe-core
                   ┌┴─────────────┴┐
                   │  Contrato (MSW)│   ← OpenAPI ↔ MSW ↔ backend
                  ┌┴───────────────┴┐
                  │ Integración (PG) │   ← Testcontainers PostgreSQL real
                 ┌┴──────────────────┴┐
                 │  Unitarios (JUnit)  │   ← Mockito, servicios aislados
                 └────────────────────┘
```

## 2. Backend

### Unitarios (JUnit 5 + Mockito)
- Aislados de BD. Mocking de repositorios.
- Cobertura: cálculo tarifario, reglas de cancelación, no-show, validaciones.

### Integración (Spring + Testcontainers PostgreSQL 16)
- **NO usar H2 para validar comportamiento de PostgreSQL.** H2 se mantiene
  solo si una prueba unitaria lo necesita para algo trivial.
- `@SpringBootTest` + `@Testcontainers` + `PostgreSQLContainer`.
- Flyway ejecuta desde cero (V1-Vn).
- Verificar: GiST EXCLUDE, CHECK constraints, índices, migraciones compatibles.

### Pruebas obligatorias de integración

| # | Prueba                                     | Descripción                              |
|---|--------------------------------------------|------------------------------------------|
| 1 | Snapshot tarifario por noche               | Una reserva guarda desglose inmutable    |
| 2 | Temporadas superpuestas                    | No se solapan; error si solapan          |
| 3 | Impuestos con vigencia                     | Solo aplica el impuesto vigente en la fecha |
| 4 | Promociones incompatibles                  | No se apilan; error o prioridad          |
| 5 | Cancelación antes del límite               | Sin penalización                         |
| 6 | Cancelación después del límite             | Con penalización calculada               |
| 7 | No-show manual                             | Marca reserva + libera habitación         |
| 8 | No-show automático                         | Job/scheduled marca reservas vencidas    |
| 9 | Extensión de estancia                      | Recalcula solo noches nuevas             |
| 10 | Reducción de estancia                     | Recalcula; libera noches eliminadas       |
| 11 | Cambio de habitación durante estancia     | Cierra room_stay anterior, abre nuevo     |
| 12 | Reservación grupal                        | Múltiples habitaciones bajo un grupo      |
| 13 | Bloqueo de mantenimiento                  | Habitación bloqueada no disponible        |
| 14 | Flujo completo housekeeping                | DIRTY→CLEANING→INSPECTED→READY→AVAILABLE |
| 15 | Excepción de saldo con permiso             | Requiere rol + motivo + auditoría        |
| 16 | Excepción de saldo sin permiso             | 403 / 422                                |
| 17 | Exportación de datos del huésped           | Genera export + audita                   |
| 18 | Anonimización                              | Datos sustituidos; reservas intactas     |
| 19 | Enmascaramiento por rol                    | RECEP ve `X•••Z`, PRIVACY_OFFICER ve todo|
| 20 | Auditoría de acceso a datos                | LOG de cada lectura de datos completos  |
| 21 | Contratos OpenAPI                          | Respuesta coincide con schema            |
| 22 | Refresh token rotación                    | Token viejo revocado al usarse           |
| 23 | Refresh token reutilización                | Revoca cadena completa                   |
| 24 | Reserva con tarifa inexistente             | 404 o 422                                 |
| 25 | Habitación bloqueada no disponible         | Excluida de /availability                |

### Pruebas de concurrencia

Usar `CountDownLatch` + `ExecutorService` (múltiples hilos) o `@RepeatedTest`:

| # | Escenario                                 | Resultado esperado                       |
|---|-------------------------------------------|------------------------------------------|
| C1| 2 reservas simultáneas misma habitación    | 1 éxito, 1 → 409 RESERVATION_OVERLAP     |
| C2| 2 CHECKED_IN simultáneos misma reserva    | 1 éxito, 1 → 409 DUPLICATE_CHECKIN        |
| C3| 2 cambios de habitación simultáneos       | 1 éxito, 1 → 409                         |
| C4| 2 refresh token rotates simultáneos       | 1 éxito, 1 → 401 (reuse detection)       |
| C5| 2 asignaciones de habitación simultáneas  | 1 éxito, 1 → 409                         |
| C6| 2 cancelaciones simultáneas               | 1 éxito, 1 → 409 o idempotente           |
| C7| Actualización concurrente de tarifas      | Sin corrupción; bloqueo optimista/pessimista |

## 3. Frontend

### Unitarios (Vitest + Testing Library + MSW)
- Cobertura existente: 46 tests (auth, rooms, availability, reservations, etc.)
- Nuevos:
  - Calendario de ocupación renderiza correctamente.
  - Planes tarifarios, temporadas, impuestos.
  - Cotizador con desglose por noche.
  - Modificación de estancia.
  - Cambio de habitación.
  - Panel de housekeeping.
  - Solicitudes de privacidad.
  - Refresh interceptor (401 → refresh → retry).

### Contractuales (OpenAPI ↔ MSW)
- MSW handlers generados o validados desde OpenAPI.
- Detectar divergencia entre MSW y backend.
- El build falla si el cliente generado está desactualizado.

### E2E (Playwright navegador real)

| # | Camino                                    | Tipo     |
|---|-------------------------------------------|----------|
| E1| Login → dashboard                         | Éxito    |
| E2| Login → cotización → reserva → pago → check-in → check-out | Éxito |
| E3| Formulario inválido (fechas)              | Error 422|
| E4| Reserva de habitación ya ocupada          | 409      |
| E5| Acceso sin permisos a /users              | 403      |
| E6| Recurso inexistente                       | 404      |
| E7| Sesión expirada → refresh → continúa      | Éxito    |
| E8| Cancelación con penalización              | Éxito + desglose |
| E9| No-show                                   | Éxito    |
| E10| Cambio de habitación durante estancia    | Éxito    |
| E11| Housekeeping: marcar tarea como lista     | Éxito    |
| E12| Exportación de datos del huésped         | Éxito + auditoría |

## 4. Accesibilidad (axe-core + Playwright)

Auditar con `@axe-core/playwright`:
- Navegación por teclado (Tab/Shift+Tab/Enter).
- Orden de foco lógico.
- Modales: trampa de foco + retorno al abrir/cerrar.
- Etiquetas asociadas a inputs (`<label for>` o aria-label).
- Mensajes de error anunciables (role=alert, aria-live).
- Contraste de color (WCAG AA 4.5:1 texto, 3:1 grandes).
- Tablas: `<th scope>` y `caption`.
- Calendario: navegable con teclado.
- Lectura con screen reader (NVDA/VoiceOver básico).
- Zoom 200% y viewport móvil (375px).

Corregir infracciones **críticas y serias** como mínimo.

## 5. Rendimiento (k6)

### Configuración
- Imagen con volumen realista: 100 habitaciones, 500 reservas (30 días).
- Backend con PostgreSQL real (Docker Compose).

### Escenarios

| Escenario                      | VUs | Duración | Métrica clave    | Umbral        |
|--------------------------------|-----|----------|------------------|--------------|
| Disponibilidad concurrente     | 20  | 30s      | p95 latencia     | < 500ms      |
| Creación de reservas           | 10  | 30s      | p95 latencia     | < 800ms      |
| Login + dashboard              | 5   | 15s      | p95 latencia     | < 400ms      |
| Mezcla (80% read / 20% write)  | 15  | 60s      | Tasa de error    | < 1%         |

Umbrales **documentados, no inventados**. Se basan en una herramienta de
recepción hotelera de uso intensivo. Ajustar tras primera medición.

### Plan de ejecución de PostgreSQL
- Verificar `EXPLAIN ANALYZE` de las consultas de disponibilidad y creación
  de reservas.
- Índices: `idx_reservations_status`, GiST en `reservation_rooms`, índices
  nuevos de tarifas.

## 6. CI (objetivo)

```yaml
backend:
  - mvn clean verify            # unit + integration (Testcontainers)
frontend:
  - npm ci
  - npm run api:generate         # falla si OpenAPI desactualizado
  - npm run lint
  - npm run test                 # vitest + MSW contractuales
  - npm run build
  - npm run test:e2e             # requiere backend + postgres levantados
  - npm run test:a11y             # axe + Playwright
integration:
  - docker compose up --build
  - k6 run performance/availability.js
```

## 7. Resultados iniciales (baseline)

| Suite          | Tests | Failures | Estado   |
|----------------|-------|----------|----------|
| Backend (H2)   | 52    | 0        | Verde    |
| Frontend vitest| 46    | 0        | Verde    |
| Frontend lint  | —     | 0 err   | Verde    |
| Frontend build | —     | —        | Verde    |
| Frontend E2E   | 8     | 0        | Verde*   |

*E2E requiere backend levantado; se ejecutan contra stack real.

## 8. Riesgos de testing

| Riesgo                              | Mitigación                           |
|-------------------------------------|--------------------------------------|
| Testcontainers lento en CI          | Reusable container + paralelización  |
| Pruebas de concurrencia no deterministas | CountDownLatch + asserts con timeout |
| MSW divergente del backend           | Generación/validación desde OpenAPI   |
| axe no detecta todo                 | Combinar con revisión manual periódica |
| k6 requiere stack real               | Documentar prerequisitos claramente  |