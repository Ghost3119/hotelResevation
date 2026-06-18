-- =============================================================================
-- V4__adjust_seed_prices_mxn.sql
-- Hotel Manager -- Ajuste de los importes semilla (V2) a cantidades realistas
-- en MXN. Dialecto: PostgreSQL 16. Ejecutado por Flyway tras V3.
--
-- Contexto (BUG-3): los importes semilla de V2 estaban en cantidades tipo EUR
-- (80, 120, 250, 180) que resultan irreales para un hotel en Mexico tratando
-- todas las cantidades como MXN. No se convierten tipos de cambio: los importes
-- existentes se tratan como MXN; solo se ajustan las cantidades de DEMO a
-- valores realistas para MXN.
--
-- No se insertan filas con IDs explicitos: no hace falta resincronizar
-- secuencias (setval) en esta migracion.
--
-- Tabla de cambios:
--   room_types.base_price:
--     Sencilla  (id=1): 80.00   -> 1200.00 MXN/noche
--     Doble     (id=2): 120.00  -> 1800.00 MXN/noche
--     Suite     (id=3): 250.00  -> 4500.00 MXN/noche
--     Familiar  (id=4): 180.00  -> 2800.00 MXN/noche
--   reservations #1 (tipo Doble, 2 noches):
--     nightly_price 120.00 -> 1800.00 ; total_amount 240.00 -> 3600.00
--   payments #1:
--     amount 100.00 -> 1500.00 ; saldo = 3600.00 - 1500.00 = 2100.00
--   audit_events #2 (RESERVATION_CREATED de la reserva 1):
--     metadata.totalAmount "240.00" -> "3600.00" ; se anade nightlyPrice "1800.00"
-- =============================================================================

-- Room types: base_price EUR-like -> MXN realista ------------------------------
UPDATE room_types
   SET base_price = 1200.00, updated_at = now()
 WHERE id = 1; -- Sencilla

UPDATE room_types
   SET base_price = 1800.00, updated_at = now()
 WHERE id = 2; -- Doble

UPDATE room_types
   SET base_price = 4500.00, updated_at = now()
 WHERE id = 3; -- Suite

UPDATE room_types
   SET base_price = 2800.00, updated_at = now()
 WHERE id = 4; -- Familiar

-- Reservation #1: nightly_price y total_amount en MXN -------------------------
-- Doble (id=2) ahora 1800.00/noche; 2 noches -> total = 2 * 1800.00 = 3600.00.
UPDATE reservations
   SET nightly_price = 1800.00,
       total_amount  = 3600.00,
       updated_at    = now()
 WHERE id = 1;

-- Payment #1: 1500.00 COMPLETED; saldo = 3600.00 - 1500.00 = 2100.00 ----------
-- Sigue dejando saldo pendiente para probar el check-out con balance (RN-6).
UPDATE payments
   SET amount = 1500.00
 WHERE id = 1;

-- Audit event de la reserva 1: reflejar nuevos importes MXN -------------------
-- Se mantiene el formato de cadena para los importes (consistente con V2).
UPDATE audit_events
   SET metadata = '{"guestId":1,"roomTypeId":2,"roomId":5,"nights":2,"nightlyPrice":"1800.00","totalAmount":"3600.00"}'::jsonb
 WHERE id = 2;
