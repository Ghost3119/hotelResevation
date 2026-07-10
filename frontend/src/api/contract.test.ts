import { describe, it, expect, beforeAll } from 'vitest'
import { expectTypeOf } from 'vitest'
import { PATHS, paths } from './generated/schema'
import type {
  AuthResponse,
  QuoteResultDto,
  ReservationDto,
} from './generated/schema'
import { generatedApi } from './generated/client'
import { setMeUser } from '../test/handlers'
import { adminUser } from '../test/handlers'

describe('OpenAPI contract: schema', () => {
  it('exports a non-empty path map with the key endpoints', () => {
    expect(PATHS.length).toBeGreaterThan(20)
    for (const required of [
      '/api/auth/login',
      '/api/reservations',
      '/api/reservations/{id}/check-in',
      '/api/availability/quote',
      '/api/rate-plans',
      '/api/seasonal-rates',
      '/api/daily-rate-overrides',
      '/api/promotion-rules',
      '/api/taxes-and-fees',
      '/api/cancellation-policies',
      '/api/reservation-groups',
      '/api/room-blocks',
      '/api/housekeeping-tasks',
      '/api/privacy-requests',
      '/api/personal-data-access-logs',
      '/api/admin/mark-no-shows',
      '/api/dashboard',
    ]) {
      expect(PATHS).toContain(required)
    }
  })

  it('declares the key operations on the paths interface (type-level)', () => {
    expectTypeOf<paths['/api/auth/login']['post']>().toMatchTypeOf<
      { requestBody: unknown; responses: Record<string, unknown> }
    >()
    expectTypeOf<paths['/api/reservations']['post']>().toMatchTypeOf<{
      requestBody: { content: { 'application/json': unknown } }
      responses: Record<string, unknown>
    }>()
    expectTypeOf<paths['/api/availability/quote']['post']>().not.toBeUnknown()
    expectTypeOf<paths['/api/reservations/{id}/modify-stay']>().toMatchTypeOf<{ put: unknown }>()
    expectTypeOf<paths['/api/admin/mark-no-shows']>().toMatchTypeOf<{ post: unknown }>()
  })

  it('DTO types compile against expected shapes', () => {
    expectTypeOf<ReservationDto>().toHaveProperty('status')
    expectTypeOf<ReservationDto>().toHaveProperty('nightlyPrice')
    expectTypeOf<AuthResponse>().toHaveProperty('token')
    expectTypeOf<AuthResponse>().toHaveProperty('user')
    expectTypeOf<QuoteResultDto>().toHaveProperty('nightly')
    expectTypeOf<QuoteResultDto>().toHaveProperty('grandTotal')
  })
})

describe('OpenAPI contract: MSW handlers return schema-shaped payloads', () => {
  beforeAll(() => setMeUser(adminUser))

  it('auth/login returns an AuthResponse', async () => {
    const res = await generatedApi.auth.login({ email: 'admin@unit.invalid', password: 'UnitTest#Password42' })
    expect(res.token).toBeTypeOf('string')
    expect(res.type).toBe('Bearer')
    expect(res.user.id).toBeTypeOf('number')
    expect(res.user.email).toBe('admin@unit.invalid')
    // Type is assignable to the schema DTO.
    expectTypeOf(res).toMatchTypeOf<AuthResponse>()
  })

  it('reservations created without a room are PENDING (ER-10/§3)', async () => {
    const created = await generatedApi.reservations.create({
      guestId: 1,
      checkIn: '2026-08-01',
      checkOut: '2026-08-03',
      adults: 2,
      children: 0,
      roomTypeId: 1,
      roomId: null,
    })
    expect(created.status).toBe('PENDING')
    expectTypeOf(created).toMatchTypeOf<ReservationDto>()
  })

  it('availability/quote returns a QuoteResultDto with nightly breakdown', async () => {
    const quote = await generatedApi.availability.quote({
      checkIn: '2026-08-01',
      checkOut: '2026-08-03',
      roomTypeId: 1,
      adults: 2,
      children: 0,
    })
    expect(Array.isArray(quote.nightly)).toBe(true)
    expect(quote.nightly.length).toBe(2)
    expect(quote.grandTotal).toBeTypeOf('number')
    expect(quote.subtotal).toBeGreaterThan(0)
    expectTypeOf(quote).toMatchTypeOf<QuoteResultDto>()
  })

  it('guests are returned with a masked document number by default (ER-24)', async () => {
    const guest = await generatedApi.guests.get(1)
    // 'PASS123' -> 'P•••••3' (first & last char kept)
    expect(guest.documentNumber).not.toBe('PASS123')
    expect(guest.documentNumber.startsWith('P')).toBe(true)
    expect(guest.documentNumber.endsWith('3')).toBe(true)
    expect(guest.documentNumber).toContain('•')
  })
})
