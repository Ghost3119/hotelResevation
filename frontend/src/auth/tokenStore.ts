type TokenListener = (token: string | null) => void

let accessToken: string | null = null
const listeners = new Set<TokenListener>()

export function getAccessToken(): string | null {
  return accessToken
}

export function setAccessToken(token: string | null): void {
  accessToken = token
  listeners.forEach((listener) => listener(token))
}

export function subscribeAccessToken(listener: TokenListener): () => void {
  listeners.add(listener)
  return () => listeners.delete(listener)
}
