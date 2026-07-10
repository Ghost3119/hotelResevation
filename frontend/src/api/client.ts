import axios from 'axios'
import { normalizeError } from '../utils/error'
import { getAccessToken, setAccessToken } from '../auth/tokenStore'
import type { RefreshResponse } from './types'

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 20000,
  withCredentials: true,
})

api.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

let isRefreshing = false
let failedQueue: Array<{
  resolve: (token: string) => void
  reject: (error: unknown) => void
}> = []

function settleQueue(error: unknown, token: string | null) {
  failedQueue.forEach(({ resolve, reject }) => {
    if (error || !token) reject(error)
    else resolve(token)
  })
  failedQueue = []
}

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config
    if (
      error.response?.status === 401 &&
      !originalRequest._retry &&
      !originalRequest.url?.includes('/auth/')
    ) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({
            resolve: (token) => {
              originalRequest._retry = true
              originalRequest.headers.Authorization = `Bearer ${token}`
              api(originalRequest).then(resolve).catch(reject)
            },
            reject,
          })
        })
      }
      isRefreshing = true
      originalRequest._retry = true
      try {
        const res = await api.post<RefreshResponse>('/auth/refresh')
        setAccessToken(res.data.token)
        settleQueue(null, res.data.token)
        originalRequest.headers.Authorization = `Bearer ${res.data.token}`
        return api(originalRequest)
      } catch (refreshErr) {
        settleQueue(refreshErr, null)
        setAccessToken(null)
        if (typeof window !== 'undefined') {
          window.location.href = '/login'
        }
        return Promise.reject(refreshErr)
      } finally {
        isRefreshing = false
      }
    }
    return Promise.reject(normalizeError(error))
  }
)
