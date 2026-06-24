-- =============================================================================
-- V11__seed_rate_engine.sql
-- Hotel Manager -- Semilla del motor tarifario para desarrollo.
-- Dialecto: PostgreSQL 16. Ejecutado por Flyway tras V10.
--
-- Carga:
--   * 4 cancellation_policies: Flexible, Semi-flexible, Strict, Non-refundable.
--   * 4 rate_plans (uno por room_type, cada uno is_default = TRUE):
--       - Standard       -> room_type 2 (Doble, base 1800)  -> 1800/noche
--       - Non-refundable -> room_type 1 (Sencilla, base 1200) -> 960/noche
--       - Corp           -> room_type 3 (Suite, base 4500)  -> 4050/noche
--       - Weekend        -> room_type 4 (Familiar, base 2800) -> 2800 + 1.15 Fri/Sat
--     El plan Standard se asigna al room_type 2 (Doble) para coincidir con la
--     reserva semilla #1 (room_type_id = 2, nightly_price = 1800.00).
--   * 2 seasonal_rates sobre el plan Standard (id=1): Alta (Jun-Ago, x1.25) y
--     Baja (Sep-Nov, x0.85). Rangos '[)' (end_date exclusivo).
--   * 2 taxes_and_fees: IVA 16% e ISH 3% (TAX_PERCENT, applies_to ROOM_RATE).
--     NOTA: valores de demostracion; no constituyen asesoría fiscal/legal.
--   * 4 daily_rate_overrides: Nochevieja (Dec 31) a 2x base_price por tipo.
--   * 1 promotion_rule: EARLYBIRD10 (PERCENTAGE 10, min_nights 3, 30 dias).
--   * Backfill de reservation_nightly_rates para la reserva semilla #1 usando
--     el plan Standard (id=1) a 1800.00/noche con IVA 16% + ISH 3% (19% total)
--     aplicado por noche. 2 noches -> 2 filas.
--   * reservations #1: cancellation_policy_id = 2 (Semi-flexible) y
--     total_amount = suma de nightly totals (2 x 2142.00 = 4284.00).
--   * Resincroniza secuencias IDENTITY tras inserts con IDs explicitos.
--
-- Calculo nightly (reserva #1, plan Standard):
--   base_rate           = 1800.00
--   extra_person_charge =    0.00
--   discount_amount     =    0.00
--   taxes_amount        =  342.00   (1800.00 * (0.16 + 0.03) = 1800.00 * 0.19)
--   fees_amount         =    0.00
--   total               = 2142.00   (CHECK: 1800 + 0 - 0 + 342 + 0 = 2142)
--   total_amount (reserva) = 2 * 2142.00 = 4284.00
-- =============================================================================

-- Cancellation policies -------------------------------------------------------
INSERT INTO cancellation_policies
    (id, name, deadline_hours, penalty_type, penalty_value,
     no_show_penalty_type, no_show_penalty_value, active)
VALUES
    (1, 'Flexible',       72, 'NONE',         0.00,
        'NONE',         0.00, TRUE),
    (2, 'Semi-flexible',  48, 'FIRST_NIGHT',  0.00,
        'FIRST_NIGHT',  0.00, TRUE),
    (3, 'Strict',         24, 'PERCENTAGE',  50.00,
        'PERCENTAGE',  50.00, TRUE),
    (4, 'Non-refundable',  0, 'PERCENTAGE', 100.00,
        'PERCENTAGE', 100.00, TRUE);

-- Rate plans ------------------------------------------------------------------
-- weekday_rates: 7 valores lun-dom (NUMERIC(12,2) >= 0).
-- Plan Standard (id=1) -> room_type 2 (Doble, base 1800) -> 1800 todos los dias.
-- Plan Non-refundable (id=2) -> room_type 1 (Sencilla, base 1200) -> 960 (x0.8).
-- Plan Corp (id=3) -> room_type 3 (Suite, base 4500) -> 4050 (x0.9).
-- Plan Weekend (id=4) -> room_type 4 (Familiar, base 2800) -> 2800 base,
--   3220 (x1.15) en Fri(4) y Sat(5).
INSERT INTO rate_plans
    (id, code, name, room_type_id, weekday_rates,
     adult_extra_rate, child_extra_rate, cancellation_policy_id,
     min_nights, max_nights, is_default, active, valid_from)
VALUES
    (1, 'STD',  'Standard',
        2, '[1800.00, 1800.00, 1800.00, 1800.00, 1800.00, 1800.00, 1800.00]'::jsonb,
        0.00, 0.00, 2, 1, NULL, TRUE, TRUE, CURRENT_DATE),
    (2, 'NRF',  'Non-refundable',
        1, '[960.00, 960.00, 960.00, 960.00, 960.00, 960.00, 960.00]'::jsonb,
        0.00, 0.00, 4, 1, NULL, TRUE, TRUE, CURRENT_DATE),
    (3, 'CORP', 'Corp',
        3, '[4050.00, 4050.00, 4050.00, 4050.00, 4050.00, 4050.00, 4050.00]'::jsonb,
        0.00, 0.00, 2, 1, NULL, TRUE, TRUE, CURRENT_DATE),
    (4, 'WKND', 'Weekend',
        4, '[2800.00, 2800.00, 2800.00, 2800.00, 3220.00, 3220.00, 2800.00]'::jsonb,
        0.00, 0.00, 3, 1, NULL, TRUE, TRUE, CURRENT_DATE);

-- Seasonal rates sobre el plan Standard (id=1) --------------------------------
-- Alta: 2026-06-01 .. 2026-09-01 (Jun-Ago, end exclusivo). Multiplier 1.25.
-- Baja: 2026-09-01 .. 2026-12-01 (Sep-Nov, end exclusivo). Multiplier 0.85.
-- No solapan (Alta termina en Sep-01 exclusivo, Baja empieza en Sep-01 inclusivo).
INSERT INTO seasonal_rates
    (id, rate_plan_id, name, start_date, end_date, season_type, price_mode, weekdays)
VALUES
    (1, 1, 'Alta', DATE '2026-06-01', DATE '2026-09-01', 'ALTA', 'MULTIPLIER',
        '[1.25, 1.25, 1.25, 1.25, 1.25, 1.25, 1.25]'::jsonb),
    (2, 1, 'Baja', DATE '2026-09-01', DATE '2026-12-01', 'BAJA', 'MULTIPLIER',
        '[0.85, 0.85, 0.85, 0.85, 0.85, 0.85, 0.85]'::jsonb);

-- Taxes and fees --------------------------------------------------------------
-- Valores de DEMOSTRACION (no asesoría fiscal/legal). IVA 16% + ISH 3% sobre
-- ROOM_RATE, vigentes desde CURRENT_DATE sin fin.
INSERT INTO taxes_and_fees
    (id, name, type, rate_value, applies_to, valid_from, valid_to, active)
VALUES
    (1, 'IVA', 'TAX_PERCENT', 16.00, 'ROOM_RATE', CURRENT_DATE, NULL, TRUE),
    (2, 'ISH', 'TAX_PERCENT',  3.00, 'ROOM_RATE', CURRENT_DATE, NULL, TRUE);

-- Daily rate overrides: Nochevieja (Dec 31) a 2x base_price por tipo ---------
-- rate_plan_id NULL = aplica a cualquier plan. 4 filas (una por room_type).
INSERT INTO daily_rate_overrides
    (id, room_type_id, rate_plan_id, date, price, reason)
VALUES
    (1, 1, NULL, DATE '2026-12-31', 2400.00, 'Nochevieja 2x base'),
    (2, 2, NULL, DATE '2026-12-31', 3600.00, 'Nochevieja 2x base'),
    (3, 3, NULL, DATE '2026-12-31', 9000.00, 'Nochevieja 2x base'),
    (4, 4, NULL, DATE '2026-12-31', 5600.00, 'Nochevieja 2x base');

-- Promotion rule: EARLYBIRD10 -------------------------------------------------
-- 10% off para estancias de 3+ noches, valido 30 dias desde hoy, cualquier plan.
INSERT INTO promotion_rules
    (id, code, description, discount_type, discount_value,
     min_nights, min_guests, valid_from, valid_to, rate_plan_id,
     stackable, priority, active)
VALUES
    (1, 'EARLYBIRD10', '10% off for stays of 3+ nights',
        'PERCENTAGE', 10.00, 3, 1, CURRENT_DATE, (CURRENT_DATE + 30),
        NULL, FALSE, 0, TRUE);

-- Backfill reservation_nightly_rates para la reserva semilla #1 ----------------
-- Reserva #1: check_in = CURRENT_DATE + 1, check_out = CURRENT_DATE + 3,
-- 2 noches (CURRENT_DATE + 1 y CURRENT_DATE + 2). Plan Standard (id=1),
-- base_rate = 1800.00. IVA 16% + ISH 3% = 19% = 342.00 taxes/noche.
-- total/noche = 1800 + 0 - 0 + 342 + 0 = 2142.00 (valida CHECK aritmetico).
INSERT INTO reservation_nightly_rates
    (id, reservation_id, rate_plan_id, night_date,
     base_rate, extra_person_charge, discount_amount,
     taxes_amount, fees_amount, total, included)
VALUES
    (1, 1, 1, (CURRENT_DATE + 1),
        1800.00, 0.00, 0.00, 342.00, 0.00, 2142.00, TRUE),
    (2, 1, 1, (CURRENT_DATE + 2),
        1800.00, 0.00, 0.00, 342.00, 0.00, 2142.00, TRUE);

-- Actualizar reserva #1: cancellation_policy_id = 2 (Semi-flexible) y
-- total_amount = 2 * 2142.00 = 4284.00 (suma de nightly totals).
UPDATE reservations
   SET cancellation_policy_id = 2,
       total_amount = 4284.00,
       updated_at = now()
 WHERE id = 1;

-- Resincroniza secuencias IDENTITY tras inserts con IDs explicitos ------------
SELECT setval(pg_get_serial_sequence('cancellation_policies',    'id'),
              (SELECT MAX(id) FROM cancellation_policies));
SELECT setval(pg_get_serial_sequence('rate_plans',                'id'),
              (SELECT MAX(id) FROM rate_plans));
SELECT setval(pg_get_serial_sequence('seasonal_rates',            'id'),
              (SELECT MAX(id) FROM seasonal_rates));
SELECT setval(pg_get_serial_sequence('daily_rate_overrides',      'id'),
              (SELECT MAX(id) FROM daily_rate_overrides));
SELECT setval(pg_get_serial_sequence('promotion_rules',           'id'),
              (SELECT MAX(id) FROM promotion_rules));
SELECT setval(pg_get_serial_sequence('taxes_and_fees',            'id'),
              (SELECT MAX(id) FROM taxes_and_fees));
SELECT setval(pg_get_serial_sequence('reservation_nightly_rates', 'id'),
              (SELECT MAX(id) FROM reservation_nightly_rates));
