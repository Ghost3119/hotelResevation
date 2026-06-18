import { describe, it, expect } from 'vitest'
import { roomTypesApi } from './roomTypes.api'

describe('roomTypesApi', () => {
  it('list() extrae .content de la respuesta PageDto paginada (BUG-1)', async () => {
    const result = await roomTypesApi.list()

    // El contrato real del backend es PageDto<RoomTypeDto>; la API del frontend
    // debe devolver el array plano (.content), no el objeto paginado.
    expect(Array.isArray(result)).toBe(true)
    expect(result.length).toBe(2)
    expect(result.map((r) => r.name).sort()).toEqual(['Doble', 'Suite'])
  })

  it('list() con active=true devuelve solo tipos activos', async () => {
    const result = await roomTypesApi.list({ active: true })
    expect(Array.isArray(result)).toBe(true)
    expect(result.every((r) => r.active)).toBe(true)
  })

  it('list() expone basePrice como número (no string)', async () => {
    const result = await roomTypesApi.list()
    const doble = result.find((r) => r.name === 'Doble')
    expect(doble).toBeDefined()
    expect(typeof doble!.basePrice).toBe('number')
    expect(doble!.basePrice).toBe(120)
  })
})
