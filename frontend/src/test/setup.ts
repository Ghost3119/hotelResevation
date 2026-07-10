import '@testing-library/jest-dom/vitest'
import { afterAll, afterEach, beforeAll } from 'vitest'
import { cleanup } from '@testing-library/react'
import { server } from './server'
import { resetDb } from './handlers'
import { setAccessToken } from '../auth/tokenStore'

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))

afterEach(() => {
  cleanup()
  server.resetHandlers()
  resetDb()
  setAccessToken(null)
  window.localStorage.clear()
})

afterAll(() => server.close())
