# Hotel Manager — Privacy & Data Protection

This is the **definitive privacy documentation** for the Hotel Manager system.
It is produced by the SECURITY AND PRIVACY workstream and references the
threat model in [`privacy-threat-model.md`](./privacy-threat-model.md) and the
privacy rules ER-24..ER-27 in [`hotel-rules.md`](./hotel-rules.md).

> ⚠️ **Legal disclaimer.** This document describes technical controls and
> operational procedures. It does **not** assert automatic legal compliance.
> Section 11 lists the points that require review by a lawyer specialized in
> Mexican data-protection law before production rollout.

---

## Table of contents

1. [Data classification](#1-data-classification)
2. [Document masking policy (enmascaramiento)](#2-document-masking-policy-enmascaramiento)
3. [Data access audit logging](#3-data-access-audit-logging)
4. [Privacy requests flow (ARCO)](#4-privacy-requests-flow-arco)
5. [Anonymization procedure](#5-anonymization-procedure)
6. [Retention policy](#6-retention-policy)
7. [Encryption at rest](#7-encryption-at-rest)
8. [TLS for production](#8-tls-for-production)
9. [Secret management abstraction](#9-secret-management-abstraction)
10. [Role matrix](#10-role-matrix)
11. [Points requiring legal review in Mexico](#11-points-requiring-legal-review-in-mexico)
12. [Log minimization rules](#12-log-minimization-rules)
13. [Backup encryption](#13-backup-encryption)

---

## 1. Data classification

The following personal data is stored by the system, classified by
sensitivity. Sensitivity levels follow the LFPDPPP notion of "sensitive
personal data" (datos personales sensibles) as a reference — final
classification requires legal review (§11).

| Entity              | Field               | Data type       | Sensitivity | Notes                                            |
|---------------------|---------------------|-----------------|:-----------:|--------------------------------------------------|
| `guests`            | `document_number`   | String          | **High**     | Official ID (INE, passport). Masked in all API responses by default (§2). |
| `guests`            | `first_name`        | String          | Medium       | Identifiable. Anonymized on DELETE (§5).         |
| `guests`            | `last_name`         | String          | Medium       | Identifiable. Anonymized on DELETE (§5).         |
| `guests`            | `email`             | String          | Medium       | Personal contact. Logged only in login context.  |
| `guests`            | `phone`             | String          | Medium       | Personal contact. Anonymized on DELETE (§5).     |
| `guests`            | `nationality`       | String          | Low          | Personal data. Anonymized on DELETE (§5).        |
| `guests`            | `do_not_contact`    | Boolean         | Low          | Opposition flag (ER-26 / ARCO opposition).       |
| `reservations`      | `guest_id`, dates, status | FK / DATE / Enum | Medium | Stay history linked to a guest. Preserved on anonymization for financial integrity. |
| `payments`          | amounts, method, reference | NUMERIC / Enum / String | Medium | **No card numbers are ever stored.** Preserved on anonymization. |
| `reservation_nightly_rates` | per-night breakdown | NUMERIC | Low | Immutable financial snapshot. Preserved. |
| `audit_events`      | `metadata`          | JSON            | Variable     | May contain references to personal data; must follow log minimization (§12). |
| `personal_data_access_logs` | `user_id`, `guest_id`, `action`, `justification` | FK / Enum / String | Medium | Audit trail of PII access (§3). |
| `users`             | `password_hash`     | String          | **High**     | BCrypt only. Never logged. Never returned by API. |
| `refresh_tokens`    | `token_hash`        | String          | **High**     | SHA-256 of the raw token. Raw token never persisted. |

**What is NOT stored:**
- Payment card numbers, CVVs, or track data — never persisted.
- Biometric data — not collected.
- Health data — not collected in the current schema (see NOM-024 in §11 if
  housekeeping notes ever reference allergies or special needs).

---

## 2. Document masking policy (enmascaramiento)

**Rule ER-24.** The `document_number` field is masked in **every** standard API
response. Only a dedicated full-access endpoint reveals the complete value,
and only to authorized roles with audit logging.

### Masking format

The mask preserves the first and last character and replaces the middle with
the bullet character `•` (U+2022):

```
X1234567Z  →  X•••••7Z
```

- For a document of length `N`, the masked form is `first + '•'×(N−2) + last`.
- If `N ≤ 2`, the mask is `first + '•'×(N−1)` (degenerate case, never reveals
  the full value).

### Where masking is applied

| Layer               | DTO              | `documentNumber` value | Endpoint(s)                 |
|---------------------|------------------|------------------------|-----------------------------|
| `GuestMapper.toDto`     | `GuestDto`       | **Masked**         | `GET /api/guests`, `GET /api/guests/{id}`, `POST`, `PUT` |
| `GuestMapper.toFullDto` | `GuestFullDto`   | **Unmasked**       | `GET /api/guests/{id}/full` |

The masking is performed in `GuestMapper.maskDocument(...)` and is applied
unconditionally by `toDto`. There is no role-based bypass on the standard
`GuestDto` path — the unmasked value is only reachable through the `/full`
endpoint.

### Who can reveal the full document

| Role              | Can view full PII? | Mechanism                                              |
|-------------------|:------------------:|--------------------------------------------------------|
| PRIVACY_OFFICER   | Yes                | `GET /api/guests/{id}/full` — requires a 1-500 character `justification` query param. Access is audit-logged (§3). |
| ADMIN             | Indirect           | Can grant `VIEW_FULL_PII` permission; does not call `/full` directly per the controller's `@PreAuthorize("hasRole('PRIVACY_OFFICER')")`. |
| RECEPCIONISTA     | No                 | Sees masked `GuestDto` only.                           |
| MANAGER           | No                 | Sees masked `GuestDto` only.                           |
| HOUSEKEEPING      | No                 | No access to guest endpoints at all.                   |

> **`VIEW_FULL_PII` permission.** The threat model (§4) describes a
> `VIEW_FULL_PII` permission granted punctually by ADMIN/PRIVACY_OFFICER for
> case-by-case revelation. The current MVP-evolution implementation enforces
> this at the role level (`PRIVACY_OFFICER` only on `/full`). A finer-grained
> permission model is a documented future enhancement. The error code
> `VIEW_FULL_PII_REQUIRED` is reserved for when the finer-grained check is
> implemented.

**Acceptance criterion (ER-24-AC):** RECEP calls `GET /api/guests/1` →
`documentNumber: "X•••••7Z"`; PRIVACY_OFFICER calls
`GET /api/guests/1/full` → `documentNumber: "X1234567Z"` and an
`personal_data_access_log` row is created with `action=VIEW`.

---

## 3. Data access audit logging

**Rule ER-25.** Every access to **unmasked** personal data generates a row in
`personal_data_access_log`. This is the auditable trail that proves who saw
what, when, and why.

### Schema

| Field            | Type     | Description                                                        |
|------------------|----------|--------------------------------------------------------------------|
| `id`             | BIGINT   | Primary key.                                                       |
| `user_id`        | BIGINT   | The authenticated user who performed the access.                   |
| `guest_id`       | BIGINT   | The guest whose data was accessed.                                 |
| `action`         | Enum     | `VIEW`, `EXPORT`, `MODIFY`, `ANONYMIZE` (`DataAccessAction`).      |
| `justification`  | TEXT     | Required 1-500 character reason supplied by the operator for VIEW. |
| `created_at`     | TIMESTAMPTZ | UTC timestamp of the access.                                    |

### What triggers a log entry

| Trigger                                                                  | `action`     | Where it happens                          |
|--------------------------------------------------------------------------|--------------|-------------------------------------------|
| `GET /api/guests/{id}/full`                                              | `VIEW`       | `PrivacyService.getGuestFull(...)`        |
| `POST /api/privacy-requests/{id}/export` (EXPORT request executed)       | `EXPORT`     | `PrivacyService.export(...)`              |
| `POST /api/privacy-requests/{id}/anonymize` (DELETE request executed)    | `ANONYMIZE`  | `PrivacyService.anonymize(...)`           |
| RECTIFY privacy request execution (future)                               | `MODIFY`     | `PrivacyService` (to be wired)            |

Masked reads (`GET /api/guests/{id}` returning `GuestDto`) do **not** generate
an access log entry, because no unmasked PII is disclosed.

### Who can read the audit trail

`GET /api/personal-data-access-logs` is restricted to `ADMIN` and
`PRIVACY_OFFICER` via `@PreAuthorize("hasAnyRole('ADMIN','PRIVACY_OFFICER')")`
on `PersonalDataAccessLogController`. The endpoint supports filtering by
`guestId` and pagination.

**Acceptance criterion (ER-25-AC):** PRIVACY_OFFICER exports a guest's data →
a `personal_data_access_log` row is created with `action=EXPORT`.

---

## 4. Privacy requests flow (ARCO)

**Rule ER-26.** Privacy requests model the Mexican ARCO rights (Acceso,
Rectificación, Cancelación, Oposición). They are managed through the
`privacy_requests` table and the `/api/privacy-requests` endpoints.

### Schema

| Field          | Type    | Description                                                        |
|----------------|---------|--------------------------------------------------------------------|
| `id`           | BIGINT  | Primary key.                                                       |
| `guest_id`     | FK      | The guest the request pertains to.                                 |
| `type`         | Enum    | `EXPORT`, `RECTIFY`, `DELETE` (`PrivacyRequestType`).              |
| `status`       | Enum    | `PENDING`, `IN_PROGRESS`, `COMPLETED`, `REJECTED` (`PrivacyRequestStatus`). |
| `requested_at` | TIMESTAMPTZ | When the request was created.                                 |
| `completed_at` | TIMESTAMPTZ | When it was finalized (nullable).                             |
| `handled_by`   | FK      | The user who processed the request (nullable).                     |
| `notes`        | TEXT    | Operator notes / justification.                                    |

### Flow per type

#### EXPORT (Acceso)
1. PRIVACY_OFFICER creates a `privacy_requests` row with `type=EXPORT`.
2. **Identity verification** of the requester happens **outside** the system
   (the officer confirms the guest's identity through a separate channel).
3. `POST /api/privacy-requests/{id}/export` executes the export.
4. The system returns a `GuestFullExportDto` containing:
   - The full (unmasked) guest record (`GuestFullDto`).
   - All reservations for the guest.
   - All payments across those reservations.
5. A `personal_data_access_log` row is created with `action=EXPORT`.
6. The request transitions to `COMPLETED`; an `audit_event` is recorded.

#### RECTIFY (Rectificación)
1. PRIVACY_OFFICER creates a `privacy_requests` row with `type=RECTIFY`.
2. The guest record is corrected via the standard `PUT /api/guests/{id}`
   endpoint (RECEPCIONISTA/ADMIN/PRIVACY_OFFICER).
3. The request transitions to `COMPLETED`; a `personal_data_access_log` with
   `action=MODIFY` should be recorded (to be wired).
4. An `audit_event` is recorded.

#### DELETE / Cancelación (Anonymization)
1. PRIVACY_OFFICER creates a `privacy_requests` row with `type=DELETE`.
2. `POST /api/privacy-requests/{id}/anonymize` executes the anonymization
   (see §5 for the field-by-field procedure).
3. A `personal_data_access_log` row is created with `action=ANONYMIZE`.
4. The request transitions to `COMPLETED`; an `audit_event` is recorded.
5. **Irreversible** — there is no un-anonymize operation.

#### Oposición
Modeled as a DELETE request with the additional side effect of setting
`guests.do_not_contact = true` before anonymizing. The guest's identifying
fields are then anonymized as in DELETE.

### Endpoint access

| Method | Path                                        | Roles                          |
|--------|---------------------------------------------|--------------------------------|
| GET    | `/api/privacy-requests`                     | ADMIN, PRIVACY_OFFICER         |
| POST   | `/api/privacy-requests`                     | PRIVACY_OFFICER                |
| GET    | `/api/privacy-requests/{id}`                | ADMIN, PRIVACY_OFFICER         |
| PUT    | `/api/privacy-requests/{id}`                | PRIVACY_OFFICER                |
| POST   | `/api/privacy-requests/{id}/export`         | PRIVACY_OFFICER                |
| POST   | `/api/privacy-requests/{id}/anonymize`      | PRIVACY_OFFICER                |

Only `PRIVACY_OFFICER` and `ADMIN` can manage privacy requests (ER-26.5).
`ADMIN` has read access to oversight the process; only `PRIVACY_OFFICER` can
create/update/execute requests, enforced by method-level `@PreAuthorize`.

---

## 5. Anonymization procedure

**Rule ER-26.3 / ER-26.4.** Anonymization is the irreversible replacement of
identifying fields with null or sentinel values. It is **not** a physical
delete — the `guest_id` and all dependent financial records are preserved to
maintain fiscal and audit integrity.

### Fields anonymized

| Field               | Anonymized value | Rationale                                  |
|---------------------|------------------|--------------------------------------------|
| `first_name`        | `"ANONIMIZADO"`  | Sentinel makes the anonymized state visible. |
| `last_name`         | `""` (empty)     | Emptied; combined with the sentinel first name. |
| `email`             | `null`           | No contact path remains.                   |
| `phone`             | `null`           | No contact path remains.                   |
| `document_number`   | `null`           | The primary identifier is removed.         |
| `nationality`       | `null`           | Personal attribute removed.                |

### Fields preserved (financial & audit integrity)

| Field / Entity                | Preserved? | Why                                                  |
|-------------------------------|:----------:|------------------------------------------------------|
| `guests.id`                   | Yes        | Referenced by reservations, payments, logs.         |
| `guests.created_at`           | Yes        | Audit timeline integrity.                           |
| `guests.do_not_contact`       | Yes        | Opposition preference retained.                     |
| `reservations.*`              | Yes        | Fiscal records (CFDI), stay history.                |
| `reservation_nightly_rates.*` | Yes        | Immutable price snapshots.                          |
| `payments.*`                  | Yes        | Financial audit trail; no card data exists anyway.  |
| `audit_events.*`              | Yes        | Tamper-evident audit log.                           |
| `personal_data_access_logs.*` | Yes        | The anonymization action itself must remain auditable. |

**Irreversibility.** Once `anonymize(...)` runs, the original PII values are
gone (overwritten in place). There is no backup of the original values within
the application layer. Database-level backups are the only residual copy and
must be managed under the retention/backup policy (§6, §13).

**Implementation:** `PrivacyService.anonymize(Long id)`.

---

## 6. Retention policy

**Rule (privacy-threat-model §5).** Data retention is configurable and
defaults to 3 years, but the effective retention period **must** be aligned
with Mexican fiscal and hospitality-record obligations — this requires legal
review (§11).

### Configuration

| Property / Env var             | Default | Description                                                      |
|--------------------------------|---------|------------------------------------------------------------------|
| `app.privacy.retention-days`   | `1095`  | Number of days after a guest's last reservation before the scheduled job anonymizes the guest. |
| `PRIVACY_RETENTION_DAYS`       | `1095`  | The env var bound to the above property (see `.env.example`).    |

### Scheduled anonymization (path forward)

A scheduled job (e.g. `@Scheduled`) is documented as the intended mechanism:
it selects guests whose most recent reservation's `checkOut` (or
`created_at` fallback) is older than `retention-days` and anonymizes them per
§5. Each anonymization creates a `personal_data_access_log` with
`action=ANONYMIZE` and an `audit_event`.

> **Status:** The retention property and env var are documented in
> `.env.example`. The scheduled job itself is a future implementation item —
> it must not be enabled until the retention period has been confirmed by
> legal review against CFDI and hospitality-record obligations.

### Why legal review is required before enabling

- **SAT / CFDI:** electronic invoicing records may need to be retained for
  specific periods; anonymizing a guest referenced by a CFDI could conflict
  with fiscal documentation requirements.
- **Hospitality registries:** Mexican regulations may require keeping guest
  identification records for a defined period.
- The 3-year default is a conservative placeholder, **not** a legal
  determination.

---

## 7. Encryption at rest

### Current state (MVP-evolution)

The MVP-evolution uses **Option A** from the threat model (§3): PII is stored
in plaintext in the database but **masked in all API responses** (§2), with
full-value access restricted to `PRIVACY_OFFICER` and audit-logged (§3). This
protects against casual exposure via the API but does **not** protect against
a full database dump.

### Path forward — field-level encryption (Option B, recommended)

For production hardening, the threat model recommends **Option B**: encrypted
storage of `document_number` with a blind index for searchable lookups.

| Column                       | Mechanism                | Purpose                                      |
|------------------------------|--------------------------|----------------------------------------------|
| `document_number_encrypted`  | **AES-GCM** (authenticated encryption) | Stores the ciphertext. Non-deterministic IV per row. |
| `document_hash`              | **HMAC-SHA-256** with a separate key | Blind index for `WHERE document_hash = hmac(:value)` lookups without decrypting. |

**Why AES-GCM, not AES-ECB:** AES-ECB is deterministic and leaks patterns;
AES-GCM provides authenticated encryption (confidentiality + integrity). Each
row uses a fresh random IV (12 bytes) prepended to the ciphertext.

**Why a separate HMAC key:** the blind index key must be distinct from the
encryption key so that compromise of one does not collapse both
confidentiality and searchability. The HMAC is deterministic, which is
acceptable for an index (it does not reveal the plaintext).

**Keys required (env vars, documented in `.env.example` — not implemented):**

| Env var             | Purpose                          | Rotation |
|---------------------|----------------------------------|----------|
| `PII_ENCRYPTION_KEY`| AES-GCM key for `document_number_encrypted`. | Documented rotation procedure required; re-encryption migration needed on rotation. |
| `PII_HMAC_KEY`      | HMAC-SHA-256 key for `document_hash` blind index. | Rotation requires re-indexing all rows. |

> **Status:** Field-level encryption is **documented as a path forward** and is
> **not implemented** in the MVP-evolution. The masking + audit-logging
> controls (§2, §3) are the implemented protections. Encryption must be
> implemented before storing production PII if a database-dump threat is in
> scope.

**Never use** deterministic ECB or unsalted hashes (MD5/SHA-1) for document
storage or indexing.

---

## 8. TLS for production

| Environment | TLS status      | Requirement                                                              |
|-------------|-----------------|--------------------------------------------------------------------------|
| Development | Not enforced    | Local traffic over HTTP is acceptable for dev.                            |
| Production  | **Required**    | All HTTP traffic must be terminated over TLS (HTTPS). No plaintext HTTP endpoints exposed externally. |

**Production requirements:**
- TLS 1.2+ minimum; TLS 1.3 preferred. Disable TLS 1.0/1.1 and legacy ciphers.
- Certificates managed via a recognized CA or internal PKI; automated renewal
  (e.g. ACME/Let's Encrypt or the platform's cert manager).
- HSTS header on production responses to enforce HTTPS in browsers.
- Refresh-token cookies must be marked `Secure` in production
  (`REFRESH_TOKEN_SECURE=true` in `.env.example` / `app.refresh-token.secure`).
- Internal service-to-service traffic (e.g. backend ↔ PostgreSQL) should also
  use TLS or run within a trusted network segment.

> **Status:** TLS is a deployment/infrastructure concern, not implemented in
  the application code for dev. `REFRESH_TOKEN_SECURE` defaults to `false` for
  local development and **must** be set to `true` in production.

---

## 9. Secret management abstraction

No secrets are versioned in Git. All secrets are injected via environment
variables, with `.env.example` documenting the names and placeholder values.

### Secrets inventory

| Env var                          | Used by               | Dev default (placeholder)        | Production source            |
|----------------------------------|-----------------------|----------------------------------|------------------------------|
| `JWT_SECRET`                     | `app.jwt.secret`      | `change-this-...`                | Secrets manager              |
| `SPRING_DATASOURCE_PASSWORD`     | DB connection         | Required env secret              | Secrets manager              |
| `POSTGRES_PASSWORD`              | DB container          | Required env secret              | Secrets manager              |
| `REFRESH_TOKEN_SECURE`           | Cookie flag           | `false`                          | `true` (env / secrets mgr)   |
| `PII_ENCRYPTION_KEY` *(future)*  | AES-GCM (§7)          | — (not set in MVP)               | Secrets manager              |
| `PII_HMAC_KEY` *(future)*        | HMAC blind index (§7) | — (not set in MVP)               | Secrets manager              |

### Abstraction layers

1. **Local development:** `.env` file (gitignored) sourced by the developer or
   the compose stack. `.env.example` is the contract of variable names.
2. **Application binding:** `application.yml` references env vars with
   `${VAR:default}` so the app runs with sensible dev defaults but picks up
   injected values in other environments.
3. **Production:** a secrets manager (**HashiCorp Vault**, AWS Secrets
   Manager, GCP Secret Manager, or equivalent) injects secrets at runtime. The
   application does **not** bind to a specific provider — the abstraction is
   "read from env var", and the platform populates the env var from the
   secrets manager. This keeps the code provider-agnostic.

**Rules:**
- `.env` (with real values) is gitignored and must never be committed.
- `.env.example` contains only placeholder values and is committed.
- Secrets are never logged (§12).
- Secret rotation procedures must be documented per secret (especially
  `PII_ENCRYPTION_KEY` and `PII_HMAC_KEY` when encryption is enabled).

---

## 10. Role matrix

Reproduced from `privacy-threat-model.md` §4 and `hotel-rules.md` ER-27. This
is the **target** permission matrix enforced via `@PreAuthorize` on
controllers, over the catch-all `requestMatchers("/api/**").authenticated()`
in `SecurityConfig`.

| Resource              | ADMIN | MANAGER | RECEP | HOUSEKEEPING | PRIVACY_OFFICER |
|-----------------------|:-----:|:-------:|:-----:|:------------:|:---------------:|
| users                 | CRUD  | —       | —     | —            | R               |
| rate-plans            | CRUD  | CRUD    | R     | —            | —               |
| seasonal-rates        | CRUD  | CRUD    | R     | —            | —               |
| taxes-fees            | CRUD  | CRUD    | R     | —            | —               |
| cancellation-policies | CRUD  | CRUD    | R     | —            | —               |
| rooms                 | CRUD  | R       | R+status | R+status   | —               |
| room-blocks           | CRUD  | CRUD    | —     | —            | —               |
| housekeeping-tasks    | R     | R+assign| —     | R+update     | —               |
| guests                | R*    | R*      | CRUD* | —            | R*              |
| reservations          | CRUD  | CRUD    | CRUD  | —            | R               |
| availability+quote    | R     | R       | R     | —            | —               |
| payments              | CRUD  | CRUD    | CRUD  | —            | —               |
| dashboard             | R     | R       | R     | R (cleaning) | —               |
| privacy-requests      | R     | —       | —     | —            | CRUD            |
| personal-data-logs    | R     | —       | —     | —            | R               |
| audit-events          | R     | R       | —     | —            | R               |

`R*` = read with masking (document `X•••••Z`); full revelation requires
`PRIVACY_OFFICER` calling `/api/guests/{id}/full` (§2). `CRUD*` includes
masked reads for the document field.

### Enforcement mechanism

- `SecurityConfig` (line 59): `requestMatchers("/api/**").authenticated()` —
  all API endpoints require a valid JWT.
- `@EnableMethodSecurity(prePostEnabled = true)` — enables `@PreAuthorize`.
- Per-controller `@PreAuthorize` annotations enforce role checks (e.g.
  `GuestController` → `hasAnyRole('ADMIN','RECEPCIONISTA','PRIVACY_OFFICER')`,
  `HousekeepingController` → `hasAnyRole('ADMIN','MANAGER','HOUSEKEEPING')`,
  `PrivacyController` → `hasAnyRole('ADMIN','PRIVACY_OFFICER')`,
  `PersonalDataAccessLogController` → `hasAnyRole('ADMIN','PRIVACY_OFFICER')`).
- The `/api/guests/{id}/full` endpoint further restricts to
  `hasRole('PRIVACY_OFFICER')` at the method level.

---

## 11. Points requiring legal review in Mexico

⚠️ **The system does not assert legal compliance.** The following points must be
reviewed by a lawyer specialized in data protection in Mexico before
production processing of real guest data.

1. **LFPDPPP** (Ley Federal de Protección de Datos Personales en Posesión de
   los Particulares):
   - Determination of which fields constitute *datos personales sensibles*
     (sensitive personal data) — `document_number` is very likely sensitive.
   - **Consent** acquisition mechanism (consentimiento) and its evidentiary
     record.
   - **Aviso de privacidad** (privacy notice) content and delivery to guests.
   - ARCO rights procedure alignment with the system's `privacy_requests`
     flow (§4) — including response timelines and the identity-verification
     step.
   - Whether the `justification` field on access logs satisfies the
     accountability principle.

2. **NOM-024-SSA3-2012** (health data): if housekeeping notes or guest
   preferences ever reference allergies, disabilities, or special health
   needs, this NOM may apply. The current schema does not store structured
   health data, but free-text `notes` could capture it inadvertently. A
   review of what `notes` fields may contain is needed.

3. **Código Civil Federal / hospitality regulations**: the obligation to
   register guests with the Secretaría de Turismo (or equivalent) may require
   retaining identifying data (name, document) for a specific period. This
   directly affects the retention policy (§6) and the anonymization procedure
   (§5) — anonymizing too early could violate record-keeping obligations.

4. **SAT / CFDI** (electronic invoicing): invoices referencing a guest may
   need to retain billing-relevant data (RFC, name, fiscal address) for fiscal
   retention periods. Anonymizing a guest referenced by issued CFDIs could
   conflict with fiscal documentation requirements. The retention job (§6)
   must account for this.

5. **Consent mechanisms**: if guest data is captured via a frontend form, the
   aviso de privacidad presentation and consent capture must be formalized.
   This is outside the technical scope but must be documented and implemented
   on the UI side.

6. **International data transfer**: if the system is hosted on a cloud
   provider located outside Mexico, cross-border transfer notifications or
   safeguards may be required under LFPDPPP.

7. **Breach notification**: the procedure and timeline for notifying the
   INAI and affected data subjects in case of a security breach must be
   defined (the threat model lists DB dump exposure as a residual risk).

---

## 12. Log minimization rules

**Rule (privacy-threat-model §7).** Logs and audit metadata must never contain
secrets or raw PII. The following table is the binding policy for what may and
may not appear in application logs, `audit_events.metadata`, and
`personal_data_access_logs`.

| Data                  | Logged? | Rule / Note                                                |
|-----------------------|:-------:|------------------------------------------------------------|
| JWT signing secret    | Never   | Only referenced via env var; never printed.                |
| Password plaintext    | Never   | Only BCrypt hashes stored; never logged.                   |
| Refresh token (raw)   | Never   | Only the JTI / userId may appear in logs.                  |
| `document_number`     | Never   | Only the affected `guest_id` may be logged.                |
| Email                 | Only in login context | `user@example.com` may appear in `audit_events` for auth events; not in general business logs. |
| Payment card data     | Never   | Not stored at all; nothing to log.                         |
| `PII_ENCRYPTION_KEY`  | Never   | Secret; never logged.                                      |
| `PII_HMAC_KEY`        | Never   | Secret; never logged.                                      |
| Action / entity / userId / JTI | Yes | Standard audit fields — no secrets.                    |
| `guest_id`            | Yes     | The non-identifying reference used in audit trails.        |

**Implementation guidance:**
- `audit_events.metadata` is a JSON map of string→string; only IDs, enums,
  and non-sensitive context values are permitted.
- Exception handlers must not log request bodies that may contain PII (e.g.
  `GuestCreateRequest` with a `document_number`). Log the error type and
  entity ID, not the payload.
- Structured logging should redact known sensitive field names
  (`documentNumber`, `password`, `token`, `email` outside auth context).

---

## 13. Backup encryption

Database backups contain the full plaintext PII (under Option A, §7) or
ciphertext (under Option B). Either way, backups are a high-value asset and
must be protected.

**Requirements:**

| Requirement                          | Detail                                                                 |
|--------------------------------------|------------------------------------------------------------------------|
| Backup encryption                    | PostgreSQL dumps (`pg_dump`) must be encrypted at rest — e.g. `pg_dump \| gpg --symmetric --cipher-algo AES256` — or rely on storage-level encryption (encrypted S3/EBS volumes). |
| Key separation                       | Backup encryption keys must be distinct from application secrets (`JWT_SECRET`, `PII_*`) and stored in the secrets manager (§9). |
| Key rotation                         | Backup key rotation must be documented; old backups re-encrypted or destroyed per the rotation schedule. |
| Access control                       | Backup files must be access-restricted (least privilege); restore operations must themselves be audit-logged. |
| Retention alignment                  | Backup retention must be coordinated with the data retention policy (§6) and any legal hold obligations (§11). Anonymized guests in the live DB may still exist in older backups — document this residual risk. |
| Transport                            | Backup transfers (to offsite/cloud storage) must use TLS (§8).         |

> **Status:** Backup encryption is a documented operational requirement. It is
> not enforced by the application code; it must be implemented in the
> deployment/ops pipeline (e.g. compose/CI scripts or the platform's backup
> facility).

---

*Document owner: SECURITY AND PRIVACY workstream. Cross-references:
[`privacy-threat-model.md`](./privacy-threat-model.md) (threat model, STRIDE),
[`hotel-rules.md`](./hotel-rules.md) (ER-24..ER-27 privacy rules).*
