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
├── V1__init_schema.sql   # esquema completo
└── V2__seed_data.sql     # datos semilla
```

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
