import type { ApiErrorResponse, AuthResponse, RefreshTokenRequest } from '@/api/patientApi.contract'
import {
  applyAuthResponseToSession,
  clearSessionFromBridge,
  getSessionSnapshot,
} from '@/api/sessionBridge'

const DEFAULT_API_BASE_URL = 'http://localhost:8080'

let refreshPromise: Promise<string | null> | null = null

export class ApiClientError extends Error {
  status: number

  details: string[]

  rawError: string

  constructor(params: { status: number; message: string; details?: string[]; rawError?: string }) {
    super(params.message)
    this.name = 'ApiClientError'
    this.status = params.status
    this.details = params.details ?? []
    this.rawError = params.rawError ?? ''
  }
}

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PATCH' | 'PUT' | 'DELETE'
  query?: Record<string, string | number | boolean | undefined | null>
  body?: unknown
  auth?: boolean
  retryOn401?: boolean
}

const getBaseUrl = () => {
  const configured = import.meta.env.VITE_API_BASE_URL
  if (configured === '') return ''

  const raw = String(configured ?? DEFAULT_API_BASE_URL).trim()
  if (!raw) return DEFAULT_API_BASE_URL
  return raw.endsWith('/') ? raw.slice(0, -1) : raw
}

const toQueryString = (query?: RequestOptions['query']) => {
  if (!query) return ''

  const params = new URLSearchParams()
  Object.entries(query).forEach(([key, value]) => {
    if (value === null || value === undefined || value === '') return
    params.set(key, String(value))
  })

  const raw = params.toString()
  return raw ? `?${raw}` : ''
}

const parseError = async (response: Response): Promise<ApiClientError> => {
  let payload: ApiErrorResponse | null = null
  try {
    payload = (await response.json()) as ApiErrorResponse
  } catch {
    payload = null
  }

  const message = payload?.message || `HTTP ${response.status}`
  return new ApiClientError({
    status: response.status,
    message,
    details: payload?.details ?? [],
    rawError: payload?.error ?? response.statusText,
  })
}

const isPublicAuthEndpoint = (path: string) =>
  path === '/auth/login' || path === '/auth/refresh' || path === '/auth/logout'

const buildHeaders = (auth: boolean): Headers => {
  const headers = new Headers()
  headers.set('Accept', 'application/json')

  const snapshot = getSessionSnapshot()
  if (auth && snapshot.accessToken) {
    headers.set('Authorization', `Bearer ${snapshot.accessToken}`)
  }

  return headers
}

const refreshSession = async (): Promise<string | null> => {
  if (refreshPromise) return refreshPromise

  refreshPromise = (async () => {
    const snapshot = getSessionSnapshot()
    if (!snapshot.refreshToken) {
      clearSessionFromBridge()
      return null
    }

    const baseUrl = getBaseUrl()
    const payload: RefreshTokenRequest = { refreshToken: snapshot.refreshToken }

    const response = await fetch(`${baseUrl}/auth/refresh`, {
      method: 'POST',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    })

    if (!response.ok) {
      clearSessionFromBridge()
      return null
    }

    const nextAuth = (await response.json()) as AuthResponse
    applyAuthResponseToSession(nextAuth)
    return nextAuth.accessToken
  })().finally(() => {
    refreshPromise = null
  })

  return refreshPromise
}

const maybeRedirectToLogin = () => {
  if (typeof window === 'undefined') return
  if (window.location.hash.startsWith('#/login')) return
  window.location.hash = '#/login'
}

const sendRequest = async <T>(
  path: string,
  options: RequestOptions,
  allowRetry: boolean,
): Promise<T> => {
  const baseUrl = getBaseUrl()
  const query = toQueryString(options.query)
  const url = `${baseUrl}${path}${query}`

  const headers = buildHeaders(options.auth ?? true)
  let body: string | undefined

  if (options.body !== undefined) {
    headers.set('Content-Type', 'application/json')
    body = JSON.stringify(options.body)
  }

  const response = await fetch(url, {
    method: options.method ?? 'GET',
    headers,
    body,
  })

  if (response.status === 401 && allowRetry && !isPublicAuthEndpoint(path) && (options.auth ?? true)) {
    const nextAccessToken = await refreshSession()
    if (nextAccessToken) {
      return sendRequest<T>(path, options, false)
    }

    maybeRedirectToLogin()
    throw new ApiClientError({ status: 401, message: 'Сессия истекла. Войдите заново.' })
  }

  if (!response.ok) {
    if (response.status === 401 && !isPublicAuthEndpoint(path)) {
      clearSessionFromBridge()
      maybeRedirectToLogin()
    }
    throw await parseError(response)
  }

  if (response.status === 204) {
    return undefined as T
  }

  return (await response.json()) as T
}

export const apiRequest = async <T>(path: string, options: RequestOptions = {}): Promise<T> => {
  return sendRequest<T>(path, options, options.retryOn401 ?? true)
}

export const toHumanErrorMessage = (error: unknown): string => {
  if (error instanceof ApiClientError) {
    if (error.status === 401) return 'Необходимо войти в систему снова.'
    if (error.status === 403) return 'Недостаточно прав для выполнения действия.'
    if (error.status === 404) return 'Запрашиваемые данные не найдены.'
    if (error.status === 409) return 'Конфликт данных: запись уже существует или была изменена.'
    if (error.status === 400 && error.details.length > 0) return error.details.join('; ')
    return error.message || 'Ошибка запроса к API.'
  }

  if (error instanceof Error) return error.message
  return 'Неизвестная ошибка.'
}
