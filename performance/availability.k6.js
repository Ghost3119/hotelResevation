import http from 'k6/http'
import { check, sleep } from 'k6'

const API_BASE_URL = __ENV.API_BASE_URL || 'http://localhost:5173/api'
const EMAIL = __ENV.ADMIN_EMAIL
const PASSWORD = __ENV.ADMIN_PASSWORD

if (!EMAIL || !PASSWORD) {
  throw new Error('ADMIN_EMAIL and ADMIN_PASSWORD are required for the load test')
}

export const options = {
  scenarios: {
    availability_search: {
      executor: 'ramping-vus',
      stages: [
        { duration: '20s', target: 10 },
        { duration: '40s', target: 10 },
        { duration: '20s', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<1000', 'p(99)<2000'],
    checks: ['rate>0.99'],
  },
}

function futureDate(daysAhead) {
  const d = new Date()
  d.setDate(d.getDate() + daysAhead)
  return d.toISOString().slice(0, 10)
}

export function setup() {
  const res = http.post(
    `${API_BASE_URL}/auth/login`,
    JSON.stringify({ email: EMAIL, password: PASSWORD }),
    { headers: { 'Content-Type': 'application/json' } },
  )

  check(res, {
    'login succeeds': (r) => r.status === 200 && Boolean(r.json('token')),
  })

  return { token: res.json('token') }
}

export default function (data) {
  const checkIn = futureDate(7)
  const checkOut = futureDate(10)
  const res = http.get(
    `${API_BASE_URL}/availability?checkIn=${checkIn}&checkOut=${checkOut}&guests=2`,
    { headers: { Authorization: `Bearer ${data.token}` } },
  )

  check(res, {
    'availability status is 200': (r) => r.status === 200,
    'availability returns an array': (r) => Array.isArray(r.json()),
  })

  sleep(1)
}
