-- =============================================================================
-- database/seed.sql  -- COPIA DE REFERENCIA (NO la ejecuta Flyway).
-- Estado final de los datos semilla: V2__seed_data.sql seguido de
-- V4__adjust_seed_prices_mxn.sql (ajuste a MXN realista).
-- Para cargar tras schema.sql:
--     psql -U hotel -d hotelmanager -f database/seed.sql
-- En el proyecto real, Flyway aplica V2 y V4 al arrancar Spring Boot; este
-- archivo existe solo para documentacion / inspeccion rapida.
-- Dialecto: PostgreSQL 16.
--
-- Nota MXN (V4): los importes mostrados aqui ya reflejan el ajuste a MXN
-- realista aplicado por V4__adjust_seed_prices_mxn.sql. En V2 los precios
-- originales eran tipo EUR (80/120/250/180); V4 los actualiza a
-- 1200/1800/4500/2800 MXN-noche y reajusta la reserva #1 y el pago #1.
-- Los importes historicos se tratan como MXN (no hay conversion de moneda).
--
-- ================================================= CONTRATO PARA EL BACKEND ===
-- Las password_hash se insertan con el valor centinela 'BCRYPT_PENDING'.
-- El backend DEBE implementar un DataInitializer (CommandLineRunner /
-- ApplicationRunner / @PostConstruct) que, DESPUES de Flyway, reemplace esas
-- filas con hashes BCrypt reales (coste 10):
--   UPDATE users SET password_hash = :bcrypt WHERE password_hash = 'BCRYPT_PENDING';
-- usando BCryptPasswordEncoder.encode(plaintext). Solo asi el login funciona.
--
-- Credenciales en texto plano (SOLO desarrollo):
--   admin@hotel.test     / admin123       (ADMIN)
--   recepcion@hotel.test / recepcion123   (RECEPCIONISTA)
-- =============================================================================

-- Users -----------------------------------------------------------------------
INSERT INTO users (id, email, password_hash, full_name, role, active) VALUES
    (1, 'admin@hotel.test',     'BCRYPT_PENDING', 'Administrador del Sistema', 'ADMIN',         TRUE),
    (2, 'recepcion@hotel.test', 'BCRYPT_PENDING', 'Recepcionista Demo',        'RECEPCIONISTA', TRUE);

-- Room types ------------------------------------------------------------------
-- Precios en MXN (V4 ajusta los originales tipo EUR 80/120/250/180 a valores
-- realistas para Mexico: 1200/1800/4500/2800).
INSERT INTO room_types (id, name, description, max_capacity, base_price, amenities, active) VALUES
    (1, 'Sencilla',
        'Habitacion individual con cama de 1,40 m.',
        1, 1200.00,
        '["wifi","tv"]'::jsonb,
        TRUE),
    (2, 'Doble',
        'Habitacion doble con dos camas o cama de matrimonio.',
        2, 1800.00,
        '["wifi","tv","minibar"]'::jsonb,
        TRUE),
    (3, 'Suite',
        'Suite amplia con salon y jacuzzi.',
        4, 4500.00,
        '["wifi","tv","minibar","jacuzzi","caja_fuerte"]'::jsonb,
        TRUE),
    (4, 'Familiar',
        'Habitacion familiar con capacidad para 4 personas.',
        4, 2800.00,
        '["wifi","tv","minibar","caja_fuerte"]'::jsonb,
        TRUE);

-- Rooms -----------------------------------------------------------------------
-- Todos AVAILABLE excepto 103 (CLEANING) y 302 (MAINTENANCE).
-- La habitacion 201 (id=5) se pasa a RESERVED mas abajo al asignarla a la
-- reserva CONFIRMED (RN-7: AVAILABLE -> RESERVED al asignar a reserva confirmada).
INSERT INTO rooms (id, number, floor, room_type_id, status, observations) VALUES
    (1,  '101', 1, 1, 'AVAILABLE',   NULL),
    (2,  '102', 1, 2, 'AVAILABLE',   NULL),
    (3,  '103', 1, 2, 'CLEANING',    'Pendiente de limpieza tras check-out.'),
    (4,  '104', 1, 3, 'AVAILABLE',   NULL),
    (5,  '201', 2, 2, 'AVAILABLE',   NULL),
    (6,  '202', 2, 2, 'AVAILABLE',   NULL),
    (7,  '203', 2, 4, 'AVAILABLE',   NULL),
    (8,  '204', 2, 3, 'AVAILABLE',   NULL),
    (9,  '301', 3, 3, 'AVAILABLE',   NULL),
    (10, '302', 3, 4, 'MAINTENANCE', 'Revision de fontaneria programada.');

-- Guests ----------------------------------------------------------------------
INSERT INTO guests (id, first_name, last_name, email, phone, document_number, nationality) VALUES
    (1, 'Juan', 'Perez',  'juan.perez@example.com',  '+34600123456',  'X1234567Z',  'Espana'),
    (2, 'Ana',  'Garcia', 'ana.garcia@example.com',  '+525512345678', 'MEX123456',  'Mexico'),
    (3, 'John', 'Smith',  'john.smith@example.com',  '+12025551234',  'USA123456',  'Estados Unidos');

-- Reservation -----------------------------------------------------------------
-- Reserva CONFIRMED de 2 noches: check_in = manana, check_out = dentro de 3 dias.
-- Tipo Doble (id=2), precio 1800.00 MXN/noche -> total = 2 * 1800.00 = 3600.00.
-- adults=2, children=0. Creada por el admin (user id=1).
INSERT INTO reservations (
    id, guest_id, status, check_in, check_out, adults, children,
    room_type_id, nightly_price, total_amount, notes, special_requests, created_by
) VALUES (
    1, 1, 'CONFIRMED',
    (CURRENT_DATE + 1),
    (CURRENT_DATE + 3),
    2, 0,
    2, 1800.00, 3600.00,
    'Reserva semilla de demostracion.',
    'Cama matrimonial si es posible.',
    1
);

-- Asignacion de la habitacion 201 (id=5) a la reserva 1 para el mismo rango ----
INSERT INTO reservation_rooms (id, reservation_id, room_id, check_in, check_out)
VALUES (1, 1, 5, (CURRENT_DATE + 1), (CURRENT_DATE + 3));

-- RN-7: al asignar habitacion a reserva confirmada, AVAILABLE -> RESERVED -----
UPDATE rooms
   SET status = 'RESERVED', updated_at = now()
 WHERE id = 5;

-- Payment ---------------------------------------------------------------------
-- Pago parcial: 1500.00 MXN COMPLETED en efectivo (CASH).
-- Saldo restante = 3600.00 - 1500.00 = 2100.00, para que el flujo de check-out
-- con saldo pendiente sea testeable (RN-6).
INSERT INTO payments (id, reservation_id, amount, method, status, reference, paid_at, created_by)
VALUES (1, 1, 1500.00, 'CASH', 'COMPLETED', 'SEED-0001', now(), 1);

-- Audit events ----------------------------------------------------------------
INSERT INTO audit_events (id, user_id, action, entity_type, entity_id, metadata) VALUES
    (1, 1, 'SEED_INIT', 'SYSTEM', NULL,
     '{"note":"Inicializacion de datos semilla"}'::jsonb),
    (2, 1, 'RESERVATION_CREATED', 'RESERVATION', 1,
     '{"guestId":1,"roomTypeId":2,"roomId":5,"nights":2,"nightlyPrice":"1800.00","totalAmount":"3600.00"}'::jsonb);

-- Resincroniza las secuencias IDENTITY tras los inserts con IDs explicitos -----
SELECT setval(pg_get_serial_sequence('users',             'id'), (SELECT MAX(id) FROM users));
SELECT setval(pg_get_serial_sequence('room_types',        'id'), (SELECT MAX(id) FROM room_types));
SELECT setval(pg_get_serial_sequence('rooms',             'id'), (SELECT MAX(id) FROM rooms));
SELECT setval(pg_get_serial_sequence('guests',            'id'), (SELECT MAX(id) FROM guests));
SELECT setval(pg_get_serial_sequence('reservations',      'id'), (SELECT MAX(id) FROM reservations));
SELECT setval(pg_get_serial_sequence('reservation_rooms', 'id'), (SELECT MAX(id) FROM reservation_rooms));
SELECT setval(pg_get_serial_sequence('payments',          'id'), (SELECT MAX(id) FROM payments));
SELECT setval(pg_get_serial_sequence('audit_events',      'id'), (SELECT MAX(id) FROM audit_events));
