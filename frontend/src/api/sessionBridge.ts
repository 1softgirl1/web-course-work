import type { AuthResponse } from '@/api/patientApi.contract'

export interface SessionSnapshot {
  accessToken: string | null
  refreshToken: string | null
}

interface SessionBridge {
  getSnapshot: () => SessionSnapshot
  applyAuthResponse: (response: AuthResponse) => void
  clearSession: () => void
}

const noopSnapshot = (): SessionSnapshot => ({ accessToken: null, refreshToken: null })

const bridge: SessionBridge = {
  getSnapshot: noopSnapshot,
  applyAuthResponse: () => undefined,
  clearSession: () => undefined,
}

export const configureSessionBridge = (next: SessionBridge) => {
  bridge.getSnapshot = next.getSnapshot
  bridge.applyAuthResponse = next.applyAuthResponse
  bridge.clearSession = next.clearSession
}

export const getSessionSnapshot = (): SessionSnapshot => bridge.getSnapshot()

export const applyAuthResponseToSession = (response: AuthResponse) => {
  bridge.applyAuthResponse(response)
}

export const clearSessionFromBridge = () => {
  bridge.clearSession()
}
