import { computed, reactive } from 'vue'
import type { AuthResponse, ChangePasswordRequest, LoginRequest, UserRole } from '@/api/patientApi.contract'
import { mockPatientApi } from '@/mocks/openapi/mockPatientApi'
import { DEFAULT_REGION_ID, getRegionNameById, RegionsStore } from '@/stores/regionsStore'

const AUTH_STORAGE_KEY = 'authSession'

interface AuthState {
  accessToken: string | null
  expiresAt: string | null
  user: AuthResponse['user'] | null
}

const state = reactive<AuthState>({
  accessToken: null,
  expiresAt: null,
  user: null,
})

const persistSession = () => {
  if (typeof window === 'undefined') return

  if (!state.user || !state.accessToken) {
    localStorage.removeItem(AUTH_STORAGE_KEY)
    return
  }

  localStorage.setItem(
    AUTH_STORAGE_KEY,
    JSON.stringify({
      accessToken: state.accessToken,
      expiresAt: state.expiresAt,
      user: state.user,
    })
  )
}

const applySession = (response: AuthResponse) => {
  state.accessToken = response.accessToken
  state.expiresAt = response.expiresAt
  state.user = response.user

  mockPatientApi.restoreSession({
    id: response.user.id,
    role: response.user.role,
    displayName: response.user.displayName,
    email: response.user.email,
    patientCode: response.user.patientCode,
    doctorRegionId: response.user.doctorRegionId ?? null,
  })

  persistSession()
}

const clearSession = () => {
  state.accessToken = null
  state.expiresAt = null
  state.user = null
  mockPatientApi.clearSession()
  persistSession()
}

const hydrateSession = () => {
  if (typeof window === 'undefined') return
  const raw = localStorage.getItem(AUTH_STORAGE_KEY)
  if (!raw) return

  try {
    const parsed = JSON.parse(raw) as AuthState
    if (!parsed?.accessToken || !parsed?.user) {
      clearSession()
      return
    }

    state.accessToken = parsed.accessToken
    state.expiresAt = parsed.expiresAt
    state.user = parsed.user

    mockPatientApi.restoreSession({
      id: parsed.user.id,
      role: parsed.user.role,
      displayName: parsed.user.displayName,
      email: parsed.user.email,
      patientCode: parsed.user.patientCode,
      doctorRegionId: parsed.user.doctorRegionId ?? null,
    })
  } catch {
    clearSession()
  }
}

hydrateSession()

export const useAuthStore = () => {
  const isAuthenticated = computed(() => Boolean(state.accessToken && state.user))
  const role = computed<UserRole | null>(() => state.user?.role ?? null)
  const isDoctor = computed(() => role.value === 'DOCTOR' || role.value === 'DOCTOR_EXTENDED')
  const isDoctorExtended = computed(() => role.value === 'DOCTOR_EXTENDED')
  const isPatient = computed(() => role.value === 'PATIENT')
  const doctorRegionName = computed<string>(() => {
    const regionName = state.user?.doctorRegionName
    if (regionName) return regionName

    const regionId = state.user?.doctorRegionId
    if (typeof regionId === 'number' && regionId > 0) {
      return getRegionNameById(RegionsStore[regionId - 1]?.id ?? DEFAULT_REGION_ID)
    }

    return getRegionNameById(DEFAULT_REGION_ID)
  })

  const login = (payload: LoginRequest) => {
    const response = mockPatientApi.login(payload)
    applySession(response)
    return response
  }

  const logout = () => {
    clearSession()
  }

  const changePassword = (payload: ChangePasswordRequest) => {
    return mockPatientApi.changeOwnPassword(payload)
  }

  const updateDoctorRegion = (regionId: number, regionName: string) => {
    if (!state.user) return
    if (state.user.role !== 'DOCTOR' && state.user.role !== 'DOCTOR_EXTENDED') return

    state.user = {
      ...state.user,
      doctorRegionId: regionId,
      doctorRegionName: regionName,
    }
    persistSession()
    mockPatientApi.restoreSession({
      id: state.user.id,
      role: state.user.role,
      displayName: state.user.displayName,
      email: state.user.email,
      patientCode: state.user.patientCode,
      doctorRegionId: regionId,
    })
  }

  return {
    accessToken: computed(() => state.accessToken),
    user: computed(() => state.user),
    role,
    isAuthenticated,
    isDoctor,
    isDoctorExtended,
    isPatient,
    doctorRegionName,
    login,
    changePassword,
    updateDoctorRegion,
    logout,
  }
}

