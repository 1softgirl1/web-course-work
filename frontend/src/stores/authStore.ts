import { computed, reactive, ref } from 'vue'
import { authApi } from '@/api/authApi'
import { doctorsApi } from '@/api/doctorsApi'
import type { AuthResponse, ChangePasswordRequest, LoginRequest, RefreshTokenRequest, UserRole } from '@/api/patientApi.contract'
import { ApiClientError } from '@/api/httpClient'
import { configureSessionBridge, type SessionSnapshot } from '@/api/sessionBridge'

const AUTH_STORAGE_KEY = 'authSession'
const LOGIN_ROLE_STORAGE_KEY = 'loginRole'

export type LoginRole = 'patient' | 'doctor'

interface AuthState {
  accessToken: string | null
  accessTokenExpiresAt: string | null
  refreshToken: string | null
  refreshTokenExpiresAt: string | null
  user: AuthResponse['user'] | null
  doctorRegionId: number | null
  doctorRegionName: string | null
}

const state = reactive<AuthState>({
  accessToken: null,
  accessTokenExpiresAt: null,
  refreshToken: null,
  refreshTokenExpiresAt: null,
  user: null,
  doctorRegionId: null,
  doctorRegionName: null,
})

const normalizeLoginRole = (value: unknown): LoginRole => (value === 'doctor' ? 'doctor' : 'patient')

const readLoginRoleFromStorage = (): LoginRole => {
  if (typeof window === 'undefined') return 'patient'
  return normalizeLoginRole(localStorage.getItem(LOGIN_ROLE_STORAGE_KEY))
}

const selectedLoginRoleState = ref<LoginRole>(readLoginRoleFromStorage())

const writeLoginRoleToStorage = (role: LoginRole) => {
  selectedLoginRoleState.value = role
  if (typeof window === 'undefined') return
  localStorage.setItem(LOGIN_ROLE_STORAGE_KEY, role)
}

const persistSession = () => {
  if (typeof window === 'undefined') return

  if (!state.user || !state.accessToken || !state.refreshToken) {
    localStorage.removeItem(AUTH_STORAGE_KEY)
    return
  }

  localStorage.setItem(
    AUTH_STORAGE_KEY,
    JSON.stringify({
      accessToken: state.accessToken,
      accessTokenExpiresAt: state.accessTokenExpiresAt,
      refreshToken: state.refreshToken,
      refreshTokenExpiresAt: state.refreshTokenExpiresAt,
      user: state.user,
      doctorRegionId: state.doctorRegionId,
      doctorRegionName: state.doctorRegionName,
    }),
  )
}

const clearSessionState = () => {
  state.accessToken = null
  state.accessTokenExpiresAt = null
  state.refreshToken = null
  state.refreshTokenExpiresAt = null
  state.user = null
  state.doctorRegionId = null
  state.doctorRegionName = null
  persistSession()
}

const applySession = (response: AuthResponse) => {
  state.accessToken = response.accessToken
  state.accessTokenExpiresAt = response.accessTokenExpiresAt
  state.refreshToken = response.refreshToken
  state.refreshTokenExpiresAt = response.refreshTokenExpiresAt
  state.user = response.user

  if (response.user.role === 'DOCTOR' || response.user.role === 'DOCTOR_EXTENDED') {
    state.doctorRegionId = state.doctorRegionId ?? null
    state.doctorRegionName = state.doctorRegionName ?? null
  } else {
    state.doctorRegionId = null
    state.doctorRegionName = null
  }

  persistSession()
}

const hydrateSession = () => {
  if (typeof window === 'undefined') return
  const raw = localStorage.getItem(AUTH_STORAGE_KEY)
  if (!raw) return

  try {
    const parsed = JSON.parse(raw) as Partial<AuthState>
    if (!parsed?.accessToken || !parsed?.refreshToken || !parsed?.user) {
      clearSessionState()
      return
    }

    state.accessToken = parsed.accessToken
    state.accessTokenExpiresAt = parsed.accessTokenExpiresAt ?? null
    state.refreshToken = parsed.refreshToken
    state.refreshTokenExpiresAt = parsed.refreshTokenExpiresAt ?? null
    state.user = parsed.user
    state.doctorRegionId = parsed.doctorRegionId ?? null
    state.doctorRegionName = parsed.doctorRegionName ?? null
  } catch {
    clearSessionState()
  }
}

hydrateSession()

const getSessionSnapshot = (): SessionSnapshot => ({
  accessToken: state.accessToken,
  refreshToken: state.refreshToken,
})

configureSessionBridge({
  getSnapshot: getSessionSnapshot,
  applyAuthResponse: (response) => {
    applySession(response)
  },
  clearSession: () => {
    clearSessionState()
  },
})

export const useAuthStore = () => {
  const selectedLoginRole = computed<LoginRole>(() => selectedLoginRoleState.value)
  const isAuthenticated = computed(() => Boolean(state.accessToken && state.user))
  const role = computed<UserRole | null>(() => state.user?.role ?? null)
  const isDoctor = computed(() => role.value === 'DOCTOR' || role.value === 'DOCTOR_EXTENDED')
  const isDoctorExtended = computed(() => role.value === 'DOCTOR_EXTENDED')
  const isPatient = computed(() => role.value === 'PATIENT')

  const doctorRegionName = computed<string>(() => {
    if (state.doctorRegionName) return state.doctorRegionName

    if (typeof state.doctorRegionId === 'number' && state.doctorRegionId > 0) {
      return `Регион #${state.doctorRegionId}`
    }

    return 'Регион не указан'
  })

  const syncDoctorRegionAfterLogin = async () => {
    if (!state.user) return
    if (state.user.role !== 'DOCTOR' && state.user.role !== 'DOCTOR_EXTENDED') return

    try {
      const doctor = await doctorsApi.getCurrentDoctor()
      state.doctorRegionId = doctor.regionId
      state.doctorRegionName = doctor.regionName
      persistSession()
    } catch {
      // Preserve successful login even if region sync fails.
    }
  }

  const login = async (payload: LoginRequest) => {
    const response = await authApi.login(payload)
    applySession(response)
    await syncDoctorRegionAfterLogin()
    writeLoginRoleToStorage(response.user.role === 'PATIENT' ? 'patient' : 'doctor')
    return response
  }

  const logout = async () => {
    const refreshToken = state.refreshToken
    clearSessionState()

    if (!refreshToken) return

    try {
      const payload: RefreshTokenRequest = { refreshToken }
      await authApi.logout(payload)
    } catch {
      // logout should be idempotent for UI
    }
  }

  const changePassword = async (payload: Omit<ChangePasswordRequest, 'refreshToken'>) => {
    if (!state.refreshToken) {
      throw new Error('REFRESH_TOKEN_MISSING')
    }

    try {
      const response = await authApi.changeOwnPassword({
        ...payload,
        refreshToken: state.refreshToken,
      })
      applySession(response)
      return response
    } catch (error) {
      if (error instanceof ApiClientError) {
        const message = error.message.toLowerCase()

        if (error.status === 401) {
          if (message.includes('invalid credentials')) {
            throw new Error('INVALID_CURRENT_PASSWORD')
          }
          throw new Error('SESSION_EXPIRED')
        }

        if (error.status === 400) {
          if (message.includes('at least 6 characters') || message.includes('newpassword')) {
            throw new Error('WEAK_PASSWORD')
          }
          throw new Error('VALIDATION_FAILED')
        }

        if (error.status === 403) {
          throw new Error('SESSION_EXPIRED')
        }
      }

      throw error
    }
  }

  const updateDoctorRegion = (regionId: number, regionName: string) => {
    if (!state.user) return
    if (state.user.role !== 'DOCTOR' && state.user.role !== 'DOCTOR_EXTENDED') return

    state.doctorRegionId = regionId
    state.doctorRegionName = regionName
    persistSession()
  }

  const setSelectedLoginRole = (roleValue: LoginRole) => {
    writeLoginRoleToStorage(normalizeLoginRole(roleValue))
  }

  return {
    accessToken: computed(() => state.accessToken),
    refreshToken: computed(() => state.refreshToken),
    user: computed(() => state.user),
    role,
    isAuthenticated,
    isDoctor,
    isDoctorExtended,
    isPatient,
    selectedLoginRole,
    doctorRegionName,
    doctorRegionId: computed(() => state.doctorRegionId),
    login,
    setSelectedLoginRole,
    changePassword,
    updateDoctorRegion,
    logout,
    applySession,
    clearSessionState,
  }
}


