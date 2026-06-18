import axios, { AxiosError } from 'axios'
import type { ApiError, NormalizedError } from '../api/types'

export function normalizeError(error: unknown): NormalizedError {
  if (axios.isAxiosError(error)) {
    const axiosError = error as AxiosError<ApiError>
    const body = axiosError.response?.data
    return {
      code: body?.code || `HTTP_${axiosError.response?.status ?? 0}`,
      message: body?.message || axiosError.message || 'Error de red',
      status: axiosError.response?.status,
      fieldErrors: body?.fieldErrors || [],
    }
  }
  if (error instanceof Error) {
    return { code: 'UNKNOWN', message: error.message, fieldErrors: [] }
  }
  return { code: 'UNKNOWN', message: 'Error desconocido', fieldErrors: [] }
}

export function getFieldErrors(error: unknown): Record<string, string> {
  const map: Record<string, string> = {}
  normalizeError(error).fieldErrors.forEach((fe) => {
    map[fe.field] = fe.message
  })
  return map
}
