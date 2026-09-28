import { apiRequest, ApiClientError } from '@/api/httpClient'
import { getSessionSnapshot } from '@/api/sessionBridge'

/**
 * Client telemetry contract (mirrors TelemetryEvent.clientKeys on the backend).
 * Only facts and counts: never diagnoses, dates of birth, medications, values, comments, search text or patient codes.
 */
export type TelemetryEventName =
  | 'dashboard_opened'
  | 'patient_filter_applied'
  | 'search_used'
  | 'dynamics_chart_opened'
  | 'characteristics_table_opened'
  | 'examination_form_opened'
  | 'patient_move_clicked'
  | 'region_change_requested'
  | 'region_change_cancelled'
  | 'page_load_failed'
  | 'validation_error'
  | PublicTelemetryEventName

// The public zone works without a token; the backend accepts only these anonymously (with a rate limit).
type PublicTelemetryEventName =
  | 'public_page_opened'
  | 'public_region_detected'
  | 'public_region_confirmed'
  | 'public_region_selected_manual'
  | 'regional_contacts_shown'
  | 'public_login_clicked'
  | 'login_page_opened'
  | 'help_opened'

const PUBLIC_EVENTS = new Set<TelemetryEventName>([
  'public_page_opened',
  'public_region_detected',
  'public_region_confirmed',
  'public_region_selected_manual',
  'regional_contacts_shown',
  'public_login_clicked',
  'login_page_opened',
  'help_opened',
])

type TelemetryValue = string | number | boolean

interface TrackOptions {
  patientId?: number | null
  routeTemplate?: string
  metadata?: Record<string, TelemetryValue>
}

export const track = (eventName: TelemetryEventName, options: TrackOptions = {}) => {
  const authenticated = Boolean(getSessionSnapshot().accessToken)
  // User, role and region come from the token on the server; without a token only public-zone events make sense.
  if (!authenticated && !PUBLIC_EVENTS.has(eventName)) return

  apiRequest<void>('/api/telemetry/events', {
    method: 'POST',
    auth: authenticated,
    retryOn401: false,
    body: {
      eventName,
      patientId: options.patientId ?? undefined,
      routeTemplate: options.routeTemplate,
      frontendVersion: __APP_VERSION__,
      metadata: options.metadata ?? {},
    },
  }).catch(() => {
    // Telemetry must never break the user scenario.
  })
}

/** Filters change on every keystroke; report only the settled state. */
export const createDebouncedTracker = (delayMs = 1000) => {
  let timer: ReturnType<typeof setTimeout> | null = null
  return (eventName: TelemetryEventName, options: TrackOptions = {}) => {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => track(eventName, options), delayMs)
  }
}

export const trackPageLoadFailed = (screen: string, routeTemplate: string | undefined, error: unknown) => {
  track('page_load_failed', {
    routeTemplate,
    metadata: {
      screen,
      error_code: error instanceof ApiClientError ? error.status : 'client_error',
    },
  })
}

/** Client-side form checks that stopped a submit: field codes only, never the entered values. */
export const trackFormValidation = (formName: string, fieldCodes: string[]) => {
  track('validation_error', {
    metadata: {
      form_name: formName,
      field_codes: fieldCodes.join(','),
      errors_count: Math.max(fieldCodes.length, 1),
    },
  })
}

export const deviceType = (): string => (window.matchMedia('(max-width: 767px)').matches ? 'mobile' : 'desktop')

/** Where the visitor came from, without the referrer URL itself. */
export const referrerSource = (): string => {
  if (!document.referrer) return 'direct'
  try {
    return new URL(document.referrer).host === window.location.host ? 'internal' : 'external'
  } catch {
    return 'unknown'
  }
}

// ---------------- Statistics page (DOCTOR_EXTENDED) ----------------

export interface TelemetrySummary {
  days: number
  from: string
  overview: {
    activeDoctors: number
    activePatients: number
    sessions: number
    publicVisits: number
    patientsCreated: number
    examinationsCreated: number
    loginFailures: number
    accessDenied: number
    apiErrors: number
  }
  events: { eventName: string; eventGroup: string; events: number; users: number }[]
  doctorFunnel: { step: string; users: number; events: number | null }[]
  patientFunnel: { step: string; users: number; events: number | null }[]
  patientListLoad: { listType: string | null; loads: number; p50Ms: number; p95Ms: number; maxTotal: number }[]
  daily: { day: string; events: number; users: number }[]
  patientStatuses: { status: string; patients: number }[]
  regions: { regionId: number; regionName: string; patients: number; transfersIn: number; transfersOut: number }[]
}

export interface TelemetryEventView {
  id: number
  occurredAt: string
  eventName: string
  eventGroup: string
  source: string
  actorUserId: number | null
  actorRole: string | null
  actorRegionId: number | null
  routeTemplate: string | null
  entityType: string | null
  entityId: number | null
  patientId: number | null
  result: string | null
  durationMs: number | null
  metadata: Record<string, unknown>
}

export const telemetryStatsApi = {
  summary(days: number) {
    return apiRequest<TelemetrySummary>('/api/telemetry/summary', { query: { days } })
  },

  events(params: { limit: number; offset?: number; group?: string; eventName?: string; role?: string }) {
    return apiRequest<TelemetryEventView[]>('/api/telemetry/events', { query: params })
  },
}
