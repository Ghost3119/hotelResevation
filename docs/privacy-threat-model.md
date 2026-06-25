# Hotel Manager — Modelo de amenazas y privacidad

Documento de referencia para el agente **SECURITY AND PRIVACY**. No afirma
cumplimiento legal automático; marca los puntos que requieren revisión legal
en México (LFPDPPP, NOM-024-SSA3-2012, etc.).

## 1. Actores y activos

### Actores
| Rol              | Acceso previsto                                              |
|------------------|--------------------------------------------------------------|
| ADMIN            | Todo, excepto revelar documentos sin permiso explícito       |
| MANAGER          | RECEP + reportes + configuración tarifaria                   |
| RECEPCIONISTA    | Huéspedes, reservas, check-in/out, pagos, disponibilidad     |
| HOUSEKEEPING     | Solo panel de housekeeping (sin datos financieros ni documentos) |
| PRIVACY_OFFICER  | Solicitudes de privacidad, anonimización, auditoría de acceso |
| Huésped (sujeto) | Titular de sus datos personales; puede ejercer ARCO          |

### Activos de datos personales
- `guests.document_number` — identificador oficial (INE, pasaporte).
- `guests.email`, `guests.phone` — contacto personal.
- `guests.nationality` — dato personal.
- `reservations.*` — historial de estancia (vinculado al huésped).
- `payments.*` — datos financieros (sin números de tarjeta almacenados).
- `audit_events.metadata` — puede contener referencias a datos personales.

### Activos de seguridad
- `users.password_hash` — BCrypt.
- `refresh_tokens.token_hash` — SHA-256.
- JWT signing key (`app.jwt.secret`).
- Cookies de refresh token.

## 2. Modelo de amenazas (STRIDE)

| Amenaza            | Escenario                                                          | Contrrol                                              |
|--------------------|--------------------------------------------------------------------|------------------------------------------------------|
| Spoofing           | Robo de access token JWT                                           | Expiración 15 min + HttpOnly cookie refresh            |
| Spoofing           | Reutilización de refresh token revocado                             | Detección de reutilización → revoca cadena completa    |
| Tampering          | Modificación de tarifa histórica                                    | `reservation_nightly_rates` inmutable post-confirmación |
| Tampering          | Modificación de reserva durante concurrencia                       | Bloqueo pesimista + GiST EXCLUDE                      |
| Repudiation        | Cambio de habitación sin auditoría                                 | `room_stays` + `audit_events` con user_id             |
| Information        | Personal con roles no autorizados accede a documentos             | Enmascaramiento por rol + permiso `VIEW_FULL_PII`     |
| disclosure        |                                                                     |                                                        |
| Information        | Datos en logs                                                       | Sin tokens, passwords, documentos en logs             |
| disclosure        | Fuga de BD con documentos en claro                                 | Cifrado de campo opcional (ver §3)                     |
| Denial of service  | Agotamiento de conexión por peticiones masivas                     | Rate limiting (futuro), timeouts                       |
| Elevation of       | RECEPCIONISTA accede a /users                                      | @PreAuthorize por rol                                 |
| privilege         | HOUSEKEEPING accede a datos financieros                           | Roles granulares + mínimo privilegio                    |

## 3. Cifrado de campos sensibles

### Documento de identidad (`document_number`)
- **Riesgo**: si la BD se filtra, los documentos quedan expuestos.
- **Opción A (más simple)**: no cifrar en BD, pero enmascarar en todas las
  respuestas API y UI. Solo revelar con permiso `VIEW_FULL_PII` + auditoría.
  Recomendado para MVP evolucionado.
- **Opción B (más segura)**: cifrado autenticado (AES-GCM) con clave externa.
  - Problema: búsqueda por documento se vuelve costosa (no se puede `WHERE
    document_number = ?`).
  - Solución: índice ciego (HMAC-SHA256 con clave separada) en columna
    `document_hash` para búsqueda; el documento se cifra en
    `document_number_encrypted`. Buscar: `WHERE document_hash = hmac(:value)`.
  - **No usar cifrado determinista inseguro** (AES-ECB) — usar AES-GCM.
- **Recomendación**: Opción B con HMAC-SHA-256 para índice ciego + AES-GCM
  para almacenamiento. Documentar rotación de claves.

### Abstracción de secretos
- `app.jwt.secret`, claves de cifrado de documentos, y secretos de refresh
  token no se versionan en Git.
- Usar variables de entorno (`JWT_SECRET`, `PII_ENCRYPTION_KEY`,
  `PII_HMAC_KEY`) con `.env.example` documentando los nombres.
- En producción: usar un gestor de secretos (Vault, AWS Secrets Manager, etc.).
  Documentar la abstracción sin implementar la integración de un proveedor
  específico.

### Cifrado de backups
- Documentar que los backups de PostgreSQL deben cifrarse (pg_dump + gpg o
  cifrado a nivel almacenamiento). Rotación de claves de backup documentada.

## 4. Roles y permisos (target)

| Recurso            | ADMIN | MANAGER | RECEP | HOUSEKEEPING | PRIVACY_OFFICER |
|--------------------|:-----:|:-------:|:-----:|:------------:|:---------------:|
| users              | CRUD  | —       | —     | —            | —               |
| rate-plans         | CRUD  | CRUD    | R     | —            | —               |
| seasonal-rates     | CRUD  | CRUD    | R     | —            | —               |
| taxes-fees         | CRUD  | CRUD    | R     | —            | —               |
| cancellation-policies | CRUD | CRUD   | R     | —            | —               |
| rooms              | CRUD  | R       | R + status | R + status | —               |
| room-blocks        | CRUD  | CRUD    | —     | —            | —               |
| housekeeping-tasks | R     | R + assign | — | R + update status | —          |
| guests             | R*    | R*      | CRUD* | —            | R*              |
| reservations       | CRUD  | CRUD    | CRUD  | —            | R               |
| availability       | R     | R       | R     | —            | —               |
| payments           | CRUD  | CRUD    | CRUD  | —            | —               |
| dashboard          | R     | R       | R     | R (cleaning) | —               |
| privacy-requests   | R     | —       | —     | —            | CRUD            |
| data-access-logs   | R     | —       | —     | —            | R               |
| audit-events       | R     | R       | —     | —            | R               |

`R*` = lectura con enmascaramiento (documento `X•••••Z`); revelación completa
requiere permiso `VIEW_FULL_PII` (otorgado puntualmente por ADMIN/PRIVACY_OFFICER).

## 5. Controles de privacidad (ARCO)

### Acceso
- El huésped solicita ver sus datos → `privacy_requests` con type=EXPORT.
- PRIVACY_OFFICER valida identidad del solicitante (fuera del sistema).
- El sistema exporta los datos del huésped + reservas + pagos en formato
  legible. Auditoría: `personal_data_access_logs` registra la exportación.

### Rectificación
- `privacy_requests` con type=RECTIFY.
- Permite corregir datos del huésped. Auditoría registra cambios.

### Cancelación / Eliminación
- `privacy_requests` con type=DELETE.
- **Anonimización** (no borrado físico): se sustituyen `first_name`,
  `last_name`, `email`, `phone`, `document_number`, `nationality` por
  valores anónimos (ej. `ANONIMIZADO`, `null`).
- **Preserva integridad financiera y auditoría**: las reservas y pagos
  asociados se conservan (referencian al `guest_id` anonimizado).
- No se puede revertir la anonimización.

### Oposición
- Mismo flujo que DELETE pero con tipo específico. Se marca el huésped como
  `do_not_contact = true` (nuevo campo) y se anonimiza.

### Retención configurable
- `app.privacy.retention-days` (default 1095 = 3 años).
- Job programado anonimiza huéspedes cuya última reserva > retención.
- Documentar que la retención real debe alinearse con obligaciones fiscales
  mexicanas (CFDI, etc.) — **requiere revisión legal**.

## 6. Puntos que requieren revisión legal en México

⚠️ **Este sistema no afirma cumplimiento legal automático.** Los siguientes
puntos requieren revisión por un abogado especializado en protección de datos
en México:

1. **LFPDPPP** (Ley Federal de Protección de Datos Personales en Posesión de
   los Particulares): determinación de datos sensibles, consentimiento,
   aviso de privacidad, derechos ARCO.
2. **NOM-024-SSA3-2012**: si se almacenan datos de salud relevantes para
   housekeeping (alergias, necesidades especiales).
3. **Código Civil Federal / normativas hoteleras**: retención de registros de
   huéspedes (la obligación de registro ante la Secretaría de Turismo puede
   requerir conservar datos identificativos por un período específico).
4. **SAT / CFDI**: obligaciones fiscales de facturación electrónica que pueden
   requerir conservar datos de facturación.
5. **Mecanismos de consentimiento**: si se capturan datos de huéspedes desde el
   frontend, el aviso de privacidad y la obtención de consentimiento deben
   formalizarse (fuera del alcance técnico, pero documentado).
6. **Transferencia internacional de datos**: si el sistema se aloja en un
   proveedor cloud en el extranjero, puede requerir notificaciones.

Todas estas consideraciones se documentan en `docs/privacy.md` (a producir
por el agente SECURITY AND PRIVACY).

## 7. Minimización de datos en logs

| Dato               | ¿Se loguea? | Nota                                     |
|--------------------|-------------|------------------------------------------|
| JWT secret         | Nunca       | Solo via env var                         |
| Password plaintext | Nunca       | BCrypt solo                              |
| Refresh token raw  | Nunca       | Solo JTI/userId en logs                  |
| Document number    | Nunca       | Solo el huésped ID afectado              |
| Email              | Solo en login | `user@example.com` en audit_events      |
| Payment card data  | Nunca       | No se almacena                           |
| Audit metadata     | Sí          | Acción, entidad, userId, JTI (no tokens) |

## 8. Riesgos residuales

| Riesgo                          | Probabilidad | Impacto | Mitigación                       |
|--------------------------------|-------------|---------|----------------------------------|
| Clave de cifro PII filtrada    | Media       | Alto    | Rotación documentada, Vault      |
| DB dump expuesto               | Baja        | Crítico | Cifrado campo + backups cifrados  |
| Acceso de HOUSEKEEPING a datos  | Baja        | Medio   | Roles granulares + enmascaramiento |
| Retención mal calibrada        | Media       | Medio   | Revisión legal periódica          |