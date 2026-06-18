import axios from 'axios'
import { normalizeError } from '../utils/error'
import type { RefreshResponse } from './types'

export const TOKEN_KEY = 'hotel.token'

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 20000,
  withCredentials: true,
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

let isRefreshing = false
let failedQueue: Array<() => void> = []

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
          failedQueue.push(() => {
            originalRequest._retry = true
            api(originalRequest).then(resolve).catch(reject)
          })
        })
      }
      isRefreshing = true
      originalRequest._retry = true
      try {
        const res = await api.post<RefreshResponse>('/auth/refresh')
        localStorage.setItem(TOKEN_KEY, res.data.token)
        failedQueue.forEach((cb) => cb())
        failedQueue = []
        originalRequest.headers.Authorization = `Bearer ${res.data.token}`
        return api(originalRequest)
      } catch (refreshErr) {
        failedQueue = []
        localStorage.removeItem(TOKEN_KEY)
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
