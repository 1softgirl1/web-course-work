export type CheckResult = { ok: boolean; detail: string; checkedAt: string }
export type JournalEntry = {
  time: string
  type: 'backup' | 'verify' | 'settings' | 'service' | 'restore'
  result: 'ok' | 'failed' | 'info'
  text: string
  trigger?: string
  durationSec?: number
}
export type RunState = { kind: 'backup' | 'verify' | 'restore' | 'switch' | 'drop'; trigger: string; startedAt: string }
export type Status = {
  checks: Record<string, CheckResult>
  running?: RunState
  lastBackup?: JournalEntry
  lastSuccess?: JournalEntry
  lastVerify?: JournalEntry
  nextRun?: string
  notifier: { configured: boolean; lastOkAt?: string; lastError?: string }
  journalError?: string
}
export type Settings = {
  dailyTime: string
  keepDaily: number
  keepWeekly: number
  keepMonthly: number
  weeklyDay: number
  monthlyDay: number
}
export type LocalCopy = { tier: string; file: string; sizeBytes: number; createdAt: string }
export type CloudCopy = { key: string; sizeBytes: number; uploadedAt?: string; lockedUntil?: string; lockMode?: string }
export type DatabaseInfo = {
  name: string
  role: 'live' | 'restored' | 'broken'
  sizeBytes: number
  patients?: number
  examinations?: number
  lastExam?: string
  schemaVersion?: number
}
export type Copies = {
  local: LocalCopy[]
  cloud: { configured: boolean; copies: CloudCopy[]; error?: string }
  network: { configured: boolean; copies: LocalCopy[]; error?: string }
}

export class ApiError extends Error {
  constructor(public status: number, message: string, public fields: Record<string, string> = {}) {
    super(message)
  }
}

/** Called on 401 so the app can show the login screen. */
export let onUnauthorized = () => {}
export const setUnauthorizedHandler = (fn: () => void) => (onUnauthorized = fn)

const csrf = () => document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]+)/)?.[1] ?? ''

async function request<T>(method: string, path: string, body?: unknown, form = false): Promise<T> {
  const headers: Record<string, string> = { 'X-XSRF-TOKEN': decodeURIComponent(csrf()) }
  if (body !== undefined && !form) headers['Content-Type'] = 'application/json'
  const response = await fetch(path, {
    method,
    headers,
    credentials: 'same-origin',
    body: form ? (body as URLSearchParams) : body !== undefined ? JSON.stringify(body) : undefined,
  })
  if (response.status === 401 && path !== '/api/login') onUnauthorized()
  if (!response.ok) {
    const data = await response.json().catch(() => ({}))
    throw new ApiError(response.status, data.message ?? `Ошибка ${response.status}`, data.fields)
  }
  return response.status === 204 ? (undefined as T) : response.json()
}

export const api = {
  // Any request first, so the server sets the XSRF-TOKEN cookie the login POST needs.
  me: () => request<{ user: string }>('GET', '/api/me'),
  login: async (username: string, password: string) => {
    if (!csrf()) await fetch('/api/me', { credentials: 'same-origin' })
    return request<void>('POST', '/api/login', new URLSearchParams({ username, password }), true)
  },
  logout: () => request<void>('POST', '/api/logout'),
  status: () => request<Status>('GET', '/api/status'),
  copies: (refresh = false) => request<Copies>('GET', `/api/copies?refresh=${refresh}`),
  settings: () => request<Settings>('GET', '/api/settings'),
  saveSettings: (s: Settings) => request<{ settings: Settings; changes: string[] }>('PUT', '/api/settings', s),
  journal: (type = '') => request<JournalEntry[]>('GET', `/api/journal?limit=200&type=${type}`),
  output: () => request<{ running?: RunState; lines: string[] }>('GET', '/api/output'),
  runBackup: () => request<{ message: string }>('POST', '/api/backup'),
  runVerify: () => request<{ message: string }>('POST', '/api/verify'),
  databases: () => request<DatabaseInfo[]>('GET', '/api/databases'),
  restore: (body: { source: 'local' | 'cloud' | 'network'; file: string; target: string; key: string }) =>
    request<{ message: string }>('POST', '/api/restore', body),
  switchDatabase: (name: string, confirm: string) =>
    request<{ message: string }>('POST', `/api/databases/${encodeURIComponent(name)}/switch`, { confirm }),
  dropDatabase: (name: string, confirm: string) =>
    request<{ message: string }>('POST', `/api/databases/${encodeURIComponent(name)}/drop`, { confirm }),
  runChecks: () => request<Record<string, CheckResult>>('POST', '/api/checks'),
}

export const formatDate = (iso?: string) =>
  iso ? new Date(iso).toLocaleString('ru-RU', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' }) : '—'

export const formatSize = (bytes: number) =>
  bytes < 1024 ? `${bytes} Б` : bytes < 1024 ** 2 ? `${(bytes / 1024).toFixed(1)} КБ` : `${(bytes / 1024 ** 2).toFixed(1)} МБ`

export const CHECK_LABELS: Record<string, string> = {
  db: 'База данных',
  backend: 'Бэкенд',
  backup: 'Последний бэкап',
  copies: 'Целостность копий',
  network: 'Хранилище NFS',
  journal: 'Журнал операций',
}

export const TYPE_LABELS: Record<string, string> = {
  backup: 'Бэкап',
  verify: 'Проверка копий',
  settings: 'Настройки',
  service: 'Сервис',
  restore: 'Восстановление',
}

export const TIER_LABELS: Record<string, string> = { daily: 'Ежедневная', weekly: 'Недельная', monthly: 'Месячная' }

export const WEEKDAYS = ['Понедельник', 'Вторник', 'Среда', 'Четверг', 'Пятница', 'Суббота', 'Воскресенье']
