import { useMutation } from '@tanstack/react-query'
import { availabilityClient } from '../api/generated/client'
import type { QuoteRequest, QuoteResultDto } from '../api/generated/schema'

export function useQuote() {
  return useMutation({
    mutationFn: (data: QuoteRequest) => availabilityClient.quote(data),
  })
}

export type { QuoteRequest, QuoteResultDto }
