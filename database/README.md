# `database/` — Scripts de referencia

Esta carpeta contiene **documentación y scripts de referencia** del esquema y
los datos semilla del proyecto **Hotel Manager**. **Flyway NO ejecuta estos
archivos**: son copias de inspección rápida.

| Archivo      | Descripción                                                         |
|--------------|---------------------------------------------------------------------|
| `schema.sql` | Esquema completo (tablas, PK/FK, CHECK, índices, exclusión GiST).   |
| `seed.sql`   | Datos semilla (usuarios, tipos, habitaciones, huéspedes, reserva).  |
| `README.md`  | Este archivo.                                                       |

## Fuente de verdad de las migraciones

Las migraciones **ejecutables** viven en el backend, para que Spring Boot las
aplique automáticamente al arrancar:

```
backend/src/main/resources/db/migration/
├── V1__init_schema.sql             # esquema base completo
├── V2__seed_data.sql               # datos semilla (EUR-like, pre-MXN)
├── V3__refresh_tokens.sql          # tabla refresh_tokens + índices (auth)
└── V4__adjust_seed_prices_mxn.sql  # ajusta importes semilla a MXN realista
```

| Versión | Tipo    | Descripción                                                                          |
|---------|---------|--------------------------------------------------------------------------------------|
| `V1`    | Schema  | Tablas base + extensión `btree_gist` + EXCLUDE GiST anti-solapamiento.               |
| `V2`    | Datos   | Usuarios, tipos, habitaciones, huéspedes, reserva #1, pago #1, audit (pre-MXN).      |
| `V3`    | Schema  | Tabla `refresh_tokens` (hash + jti + rotación + expiración) y sus cuatro índices.    |
| `V4`    | Datos   | `UPDATE` de `room_types.base_price`, `reservations` #1, `payments` #1 y `audit_events` #2 a MXN realista. |

> `schema.sql` refleja el estado de esquema tras `V1` + `V3` (V2 y V4 son solo
> datos). `seed.sql` refleja el estado de datos tras `V2` + `V4` (importes ya en
> MXN). Las migraciones ya aplicadas (`V1`, `V2`) **no se modifican**; los
> ajustes posteriores van en nuevas versiones (`V3`, `V4`).

Flyway las descubre vía `spring.flyway.enabled=true`
(ver `.env.example` / `compose.yaml`) y las aplica contra el datasource
configurado (`spring.datasource.url`), registrando el estado en la tabla
`flyway_schema_history`. Cada versión se ejecuta **una sola vez** y dentro de una
transacción.

`schema.sql` y `seed.sql` de esta carpeta son **copias fieles** de esas
migraciones, mantenidas a mano para consulta sin levantar el backend.

## Aplicar manualmente (solo para inspección local)

```bash
# 1) Levantar solo PostgreSQL
docker compose up -d postgres

# 2) Crear esquema y cargar semilla
psql -U hotel -d hotelmanager -h localhost -f database/schema.sql
psql -U hotel -d hotelmanager -h localhost -f database/seed.sql
```

> En el flujo normal del proyecto **no hace falta** hacerlo a mano: al arrancar
> el backend (`docker compose up backend` o `./mvnw spring-boot:run`) Flyway
> aplica las migraciones de `backend/.../db/migration/` automáticamente.

## Requisitos del motor

- **PostgreSQL 16** (imagen `postgres:16-alpine` en `compose.yaml`).
- **Extensión `btree_gist`**: la crea `V1__init_schema.sql` con
  `CREATE EXTENSION IF NOT EXISTS btree_gist;` porque la restricción anti-
  solapamiento de `reservation_rooms` usa `EXCLUDE USING gist (room_id WITH =, …)`
  y el operador `=` sobre `BIGINT` necesita el opclass GiST de btree.
  En el setup dev, `POSTGRES_USER` (rol `hotel`) es **superusuario** en la imagen
  oficial de PostgreSQL, por lo que puede crear la extensión. En producción, si el
  rol de la app no es superusuario, crea la extensión previamente con un
  superusuario:
  `psql -U postgres -d hotelmanager -c "CREATE EXTENSION IF NOT EXISTS btree_gist;"`
  (la sentencia es idempotente; `CREATE EXTENSION` es segura en transacción).

## Contrato con el backend (importante)

`V2__seed_data.sql` inserta los usuarios con `password_hash = 'BCRYPT_PENDING'`
(centinela) para no enviar hashes BCrypt inválidos en SQL plano. El **backend**
debe implementar un `DataInitializer`
(`CommandLineRunner` / `ApplicationRunner` / `@PostConstruct`) que, **después**
de que Flyway corra, haga:

```java
// BCryptPasswordEncoder con coste 10
for (User u : userRepository.findAll()) {
    if ("BCRYPT_PENDING".equals(u.getPasswordHash())) {
        u.setPasswordHash(encoder.encode(plaintextPorEmail(u.getEmail())));
        userRepository.save(u);
    }
}
```

Equivalente SQL: `UPDATE users SET password_hash = :bcrypt
WHERE password_hash = 'BCRYPT_PENDING';`

Credenciales en texto plano (solo desarrollo):

| Email                  | Contraseña     | Rol            |
|------------------------|----------------|----------------|
| `admin@hotel.test`     | `admin123`     | `ADMIN`        |
| `recepcion@hotel.test` | `recepcion123` | `RECEPCIONISTA`|

El `DataInitializer` del backend debe usar exactamente esos literales para que
coincidan con `V2__seed_data.sql` y `docs/database.md`.

Más detalle en `docs/database.md`.
