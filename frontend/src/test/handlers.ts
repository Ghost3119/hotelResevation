import { http, HttpResponse } from 'msw'
import type {
  DashboardDto,
  GuestDto,
  PaymentDto,
  ReservationDto,
  RoomDto,
  RoomTypeDto,
  UserDto,
} from '../api/types'

export const API = 'http://localhost:8080/api'

export const adminUser: UserDto = {
  id: 1,
  email: 'admin@hotel.test',
  fullName: 'Admin User',
  role: 'ADMIN',
  active: true,
}

export const recepUser: UserDto = {
  id: 2,
  email: 'recep@hotel.test',
  fullName: 'Recep User',
  role: 'RECEPCIONISTA',
  active: true,
}

const NOW = '2026-06-17T10:00:00'

const allGuests: GuestDto[] = [
  {
    id: 1,
    firstName: 'John',
    lastName: 'Doe',
    email: 'john@test.com',
    phone: '555-0100',
    documentNumber: 'PASS123',
    nationality: 'USA',
    createdAt: NOW,
  },
  {
    id: 2,
    firstName: 'Jane',
    lastName: 'Smith',
    email: 'jane@test.com',
    phone: '555-0200',
    documentNumber: 'DNI456',
    nationality: 'España',
    createdAt: NOW,
  },
]

const roomTypes: RoomTypeDto[] = [
  {
    id: 1,
    name: 'Doble',
    description: 'Habitación doble',
    maxCapacity: 2,
    basePrice: 120.0,
    amenities: ['WiFi', 'TV'],
    active: true,
  },
  {
    id: 2,
    name: 'Suite',
    description: 'Suite premium',
    maxCapacity: 4,
    basePrice: 250.0,
    amenities: ['WiFi', 'TV', 'Jacuzzi'],
    active: true,
  },
]

const allRooms: RoomDto[] = [
  { id: 1, number: '101', floor: 1, roomTypeId: 1, roomTypeName: 'Doble', status: 'AVAILABLE', observations: null },
  { id: 2, number: '102', floor: 1, roomTypeId: 1, roomTypeName: 'Doble', status: 'OCCUPIED', observations: null },
  { id: 3, number: '201', floor: 2, roomTypeId: 2, roomTypeName: 'Suite', status: 'AVAILABLE', observations: null },
]

const dashboard: DashboardDto = {
  arrivalsToday: 2,
  departuresToday: 1,
  occupiedRooms: 5,
  availableRooms: 10,
  cleaningRooms: 2,
  occupancyRate: 33.3,
  incomePeriod: 1250.0,
  recentReservations: [
    {
      id: 1,
      status: 'CONFIRMED',
      guestId: 1,
      guestName: 'John Doe',
      checkIn: '2026-07-01',
      checkOut: '2026-07-03',
      nights: 2,
      totalAmount: 240.0,
      balance: 240.0,
      roomTypeName: 'Doble',
    },
  ],
}

let meUser: UserDto = adminUser

interface Store {
  reservations: Record<number, ReservationDto>
  paymentsByRes: Record<number, PaymentDto[]>
  reservationSeq: number
  paymentSeq: number
}

function freshStore(): Store {
  const r1: ReservationDto = {
    id: 1,
    status: 'CONFIRMED',
    guestId: 1,
    guestName: 'John Doe',
    checkIn: '2026-07-01',
    checkOut: '2026-07-03',
    nights: 2,
    adults: 2,
    children: 0,
    roomTypeId: 1,
    roomTypeName: 'Doble',
    rooms: [],
    nightlyPrice: 120.0,
    totalAmount: 240.0,
    paidAmount: 0.0,
    balance: 240.0,
    notes: null,
    specialRequests: null,
    checkInAt: null,
    checkOutAt: null,
    createdAt: NOW,
  }
  const r2: ReservationDto = {
    id: 2,
    status: 'CHECKED_IN',
    guestId: 2,
    guestName: 'Jane Smith',
    checkIn: '2026-06-15',
    checkOut: '2026-06-20',
    nights: 5,
    adults: 1,
    children: 0,
    roomTypeId: 2,
    roomTypeName: 'Suite',
    rooms: [{ roomId: 3, roomNumber: '201', checkIn: '2026-06-15', checkOut: '2026-06-20' }],
    nightlyPrice: 250.0,
    totalAmount: 1250.0,
    paidAmount: 1250.0,
    balance: 0.0,
    notes: null,
    specialRequests: null,
    checkInAt: '2026-06-15T14:00:00',
    checkOutAt: null,
    createdAt: NOW,
  }
  return {
    reservations: { 1: r1, 2: r2 },
    paymentsByRes: {
      1: [],
      2: [
        {
          id: 100,
          reservationId: 2,
          amount: 1250.0,
          method: 'CASH',
          status: 'COMPLETED',
          reference: 'ref-2',
          paidAt: '2026-06-15T14:05:00',
        },
      ],
    },
    reservationSeq: 100,
    paymentSeq: 200,
  }
}

let store: Store = freshStore()

export function setMeUser(user: UserDto) {
  meUser = user
}

export function resetDb() {
  meUser = adminUser
  store = freshStore()
}

function paginate<T>(content: T[], page: number, size: number) {
  const start = page * size
  const slice = content.slice(start, start + size)
  return {
    content: slice,
    page,
    size,
    totalElements: content.length,
    totalPages: Math.max(1, Math.ceil(content.length / size)),
  }
}

function errorResponse(
  status: number,
  code: string,
  message: string,
  fieldErrors: { field: string; message: string }[] = []
) {
  return HttpResponse.json(
    {
      timestamp: NOW,
      status,
      error: '',
      code,
      message,
      path: '',
      fieldErrors,
    },
    { status }
  )
}

function guestName(id: number): string {
  const g = allGuests.find((x) => x.id === id)
  return g ? `${g.firstName} ${g.lastName}` : '—'
}

function roomNumber(id: number): string {
  return allRooms.find((r) => r.id === id)?.number ?? String(id)
}

export const handlers = [
  // Auth
  http.post(`${API}/auth/login`, async ({ request }) => {
    const body = (await request.json()) as { email: string; password: string }
    if (!body.email || !body.password) {
      return errorResponse(401, 'BAD_CREDENTIALS', 'Credenciales no válidas.')
    }
    return HttpResponse.json({
      token: 'test-jwt-token',
      type: 'Bearer',
      expiresIn: 900,
      user: meUser,
    })
  }),
  http.get(`${API}/auth/me`, () => HttpResponse.json(meUser)),
  http.post(`${API}/auth/refresh`, () =>
    HttpResponse.json({ token: 'new-test-token', type: 'Bearer', expiresIn: 900 }),
  ),
  http.post(`${API}/auth/logout`, () => new HttpResponse(null, { status: 204 })),

  // Dashboard
  http.get(`${API}/dashboard`, () => HttpResponse.json(dashboard)),

  // Guests
  http.get(`${API}/guests`, ({ request }) => {
    const url = new URL(request.url)
    const search = (url.searchParams.get('search') || '').toLowerCase()
    const page = Number(url.searchParams.get('page') || 0)
    const size = Number(url.searchParams.get('size') || 10)
    const filtered = search
      ? allGuests.filter((g) =>
          `${g.firstName} ${g.lastName} ${g.email ?? ''} ${g.documentNumber}`
            .toLowerCase()
            .includes(search)
        )
      : allGuests
    return HttpResponse.json(paginate(filtered, page, size))
  }),
  http.get(`${API}/guests/:id`, ({ params }) => {
    const id = Number(params.id)
    const g = allGuests.find((x) => x.id === id)
    if (!g) return errorResponse(404, 'RESOURCE_NOT_FOUND', 'Huésped no encontrado.')
    return HttpResponse.json(g)
  }),
  http.post(`${API}/guests`, async ({ request }) => {
    const body = (await request.json()) as Partial<GuestDto>
    const g: GuestDto = {
      id: 999,
      firstName: body.firstName ?? '',
      lastName: body.lastName ?? '',
      email: body.email ?? null,
      phone: body.phone ?? null,
      documentNumber: body.documentNumber ?? '',
      nationality: body.nationality ?? '',
      createdAt: NOW,
    }
    return HttpResponse.json(g, { status: 201 })
  }),
  http.put(`${API}/guests/:id`, async ({ params, request }) => {
    const id = Number(params.id)
    const g = allGuests.find((x) => x.id === id)
    if (!g) return errorResponse(404, 'RESOURCE_NOT_FOUND', 'Huésped no encontrado.')
    const body = (await request.json()) as Partial<GuestDto>
    return HttpResponse.json({ ...g, ...body })
  }),
  http.get(`${API}/guests/:id/reservations`, ({ params }) => {
    const id = Number(params.id)
    const list = Object.values(store.reservations).filter((r) => r.guestId === id)
    return HttpResponse.json(list)
  }),

  // Room types
  http.get(`${API}/room-types`, ({ request }) => {
    const url = new URL(request.url)
    const active = url.searchParams.get('active')
    let list = roomTypes
    if (active === 'true') list = roomTypes.filter((rt) => rt.active)
    return HttpResponse.json({
      content: list,
      page: 0,
      size: 100,
      totalElements: list.length,
      totalPages: 1,
    })
  }),

  // Rooms
  http.get(`${API}/rooms`, ({ request }) => {
    const url = new URL(request.url)
    const page = Number(url.searchParams.get('page') || 0)
    const size = Number(url.searchParams.get('size') || 10)
    const floor = url.searchParams.get('floor')
    const roomTypeId = url.searchParams.get('roomTypeId')
    const status = url.searchParams.get('status')
    let filtered = allRooms
    if (floor) filtered = filtered.filter((r) => r.floor === Number(floor))
    if (roomTypeId) filtered = filtered.filter((r) => r.roomTypeId === Number(roomTypeId))
    if (status) filtered = filtered.filter((r) => r.status === status)
    return HttpResponse.json(paginate(filtered, page, size))
  }),

  // Users
  http.get(`${API}/users`, ({ request }) => {
    const url = new URL(request.url)
    const page = Number(url.searchParams.get('page') || 0)
    const size = Number(url.searchParams.get('size') || 10)
    const list = [adminUser, recepUser]
    return HttpResponse.json(paginate(list, page, size))
  }),

  // Availability
  http.get(`${API}/availability`, ({ request }) => {
    const url = new URL(request.url)
    const roomTypeId = url.searchParams.get('roomTypeId')
    const guests = Number(url.searchParams.get('guests') || 1)
    let result = allRooms
      .filter((r) => r.status === 'AVAILABLE')
      .map((r) => {
        const rt = roomTypes.find((t) => t.id === r.roomTypeId)!
        return {
          roomId: r.id,
          number: r.number,
          floor: r.floor,
          roomTypeId: r.roomTypeId,
          roomTypeName: r.roomTypeName,
          maxCapacity: rt.maxCapacity,
          basePrice: rt.basePrice,
        }
      })
    if (roomTypeId) result = result.filter((r) => r.roomTypeId === Number(roomTypeId))
    result = result.filter((r) => r.maxCapacity >= guests)
    return HttpResponse.json(result)
  }),

  // Reservations
  http.get(`${API}/reservations`, ({ request }) => {
    const url = new URL(request.url)
    const page = Number(url.searchParams.get('page') || 0)
    const size = Number(url.searchParams.get('size') || 10)
    const status = url.searchParams.get('status')
    let list = Object.values(store.reservations)
    if (status) list = list.filter((r) => r.status === status)
    list = [...list].sort((a, b) => b.id - a.id)
    return HttpResponse.json(paginate(list, page, size))
  }),
  http.get(`${API}/reservations/:id`, ({ params }) => {
    const id = Number(params.id)
    const r = store.reservations[id]
    if (!r) return errorResponse(404, 'RESOURCE_NOT_FOUND', 'Reserva no encontrada.')
    return HttpResponse.json(r)
  }),
  http.post(`${API}/reservations`, async ({ request }) => {
    const body = (await request.json()) as {
      guestId: number
      checkIn: string
      checkOut: string
      adults: number
      children: number
      roomTypeId: number
      roomId?: number | null
      notes?: string | null
      specialRequests?: string | null
    }
    if (body.checkOut <= body.checkIn) {
      return errorResponse(400, 'INVALID_DATES', 'La fecha de salida debe ser posterior a la de entrada.', [
        { field: 'checkOut', message: 'La salida debe ser posterior a la entrada.' },
      ])
    }
    const nights =
      Math.round((Date.parse(body.checkOut) - Date.parse(body.checkIn)) / 86400000)
    const rt = roomTypes.find((t) => t.id === body.roomTypeId)
    const nightlyPrice = rt?.basePrice ?? 0
    const total = nights * nightlyPrice
    const id = ++store.reservationSeq
    const res: ReservationDto = {
      id,
      status: 'CONFIRMED',
      guestId: body.guestId,
      guestName: guestName(body.guestId),
      checkIn: body.checkIn,
      checkOut: body.checkOut,
      nights,
      adults: body.adults,
      children: body.children,
      roomTypeId: body.roomTypeId,
      roomTypeName: rt?.name ?? '',
      rooms: body.roomId
        ? [{ roomId: body.roomId, roomNumber: roomNumber(body.roomId), checkIn: body.checkIn, checkOut: body.checkOut }]
        : [],
      nightlyPrice,
      totalAmount: total,
      paidAmount: 0,
      balance: total,
      notes: body.notes ?? null,
      specialRequests: body.specialRequests ?? null,
      checkInAt: null,
      checkOutAt: null,
      createdAt: NOW,
    }
    store.reservations[id] = res
    store.paymentsByRes[id] = []
    return HttpResponse.json(res, { status: 201 })
  }),
  http.post(`${API}/reservations/:id/cancel`, ({ params }) => {
    const id = Number(params.id)
    const r = store.reservations[id]
    if (!r) return errorResponse(404, 'RESOURCE_NOT_FOUND', 'Reserva no encontrada.')
    if (r.status === 'CANCELLED') {
      return errorResponse(409, 'CANCEL_NOT_ALLOWED', 'La reserva ya está cancelada.')
    }
    r.status = 'CANCELLED'
    r.rooms = []
    return HttpResponse.json(r)
  }),
  http.post(`${API}/reservations/:id/assign-room`, async ({ params, request }) => {
    const id = Number(params.id)
    const r = store.reservations[id]
    if (!r) return errorResponse(404, 'RESOURCE_NOT_FOUND', 'Reserva no encontrada.')
    const body = (await request.json()) as { roomId: number }
    r.rooms = [{ roomId: body.roomId, roomNumber: roomNumber(body.roomId), checkIn: r.checkIn, checkOut: r.checkOut }]
    return HttpResponse.json(r)
  }),
  http.post(`${API}/reservations/:id/check-in`, ({ params }) => {
    const id = Number(params.id)
    const r = store.reservations[id]
    if (!r) return errorResponse(404, 'RESOURCE_NOT_FOUND', 'Reserva no encontrada.')
    if (r.status === 'CHECKED_IN') {
      return errorResponse(409, 'DUPLICATE_CHECKIN', 'El check-in ya fue realizado.')
    }
    r.status = 'CHECKED_IN'
    r.checkInAt = NOW
    if (r.rooms.length === 0) {
      const room = allRooms.find((rm) => rm.roomTypeId === r.roomTypeId && rm.status === 'AVAILABLE')
      if (room) {
        r.rooms = [{ roomId: room.id, roomNumber: room.number, checkIn: r.checkIn, checkOut: r.checkOut }]
      }
    }
    return HttpResponse.json(r)
  }),
  http.post(`${API}/reservations/:id/check-out`, ({ params }) => {
    const id = Number(params.id)
    const r = store.reservations[id]
    if (!r) return errorResponse(404, 'RESOURCE_NOT_FOUND', 'Reserva no encontrada.')
    r.status = 'CHECKED_OUT'
    r.checkOutAt = NOW
    return HttpResponse.json(r)
  }),

  // Payments
  http.get(`${API}/reservations/:id/payments`, ({ params }) => {
    const id = Number(params.id)
    return HttpResponse.json(store.paymentsByRes[id] ?? [])
  }),
  http.post(`${API}/reservations/:id/payments`, async ({ params, request }) => {
    const id = Number(params.id)
    const r = store.reservations[id]
    if (!r) return errorResponse(404, 'RESOURCE_NOT_FOUND', 'Reserva no encontrada.')
    const body = (await request.json()) as { amount: number; method: string; reference?: string | null }
    const pay: PaymentDto = {
      id: ++store.paymentSeq,
      reservationId: id,
      amount: body.amount,
      method: body.method as PaymentDto['method'],
      status: 'COMPLETED',
      reference: body.reference ?? null,
      paidAt: NOW,
    }
    store.paymentsByRes[id] = [...(store.paymentsByRes[id] ?? []), pay]
    r.paidAmount = Number((r.paidAmount + pay.amount).toFixed(2))
    r.balance = Number((r.totalAmount - r.paidAmount).toFixed(2))
    return HttpResponse.json(pay, { status: 201 })
  }),
  http.patch(`${API}/payments/:id/status`, async ({ params, request }) => {
    const id = Number(params.id)
    const body = (await request.json()) as { status: string }
    for (const resId of Object.keys(store.paymentsByRes)) {
      const list = store.paymentsByRes[Number(resId)]
      const pay = list.find((p) => p.id === id)
      if (pay) {
        pay.status = body.status as PaymentDto['status']
        const r = store.reservations[Number(resId)]
        if (r) {
          const completed = list.filter((p) => p.status === 'COMPLETED').reduce((s, p) => s + p.amount, 0)
          r.paidAmount = Number(completed.toFixed(2))
          r.balance = Number((r.totalAmount - r.paidAmount).toFixed(2))
        }
        return HttpResponse.json(pay)
      }
    }
    return errorResponse(404, 'RESOURCE_NOT_FOUND', 'Pago no encontrado.')
  }),
]
