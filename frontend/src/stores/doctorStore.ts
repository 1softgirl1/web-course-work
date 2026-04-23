import { computed, reactive } from 'vue'
import { doctorsApi } from '@/api/doctorsApi'
import { ApiClientError } from '@/api/httpClient'
import type {
  CreateDoctorRequest,
  DoctorResponse,
  UpdateDoctorRequest,
  UserRole,
  UserStatus,
} from '@/api/patientApi.contract'

export interface UiDoctor {
  id: number
  fullName: string
  email: string
  specialty: string
  workplace: string
  role: UserRole
  regionId: number
  regionName: string
  status: 'ACTIVE' | 'INACTIVE'
}

interface CreateDoctorInput {
  lastName: string
  firstName: string
  middleName: string | null
  email: string
  specialty: string
  workplace: string
  regionId: number
  role?: UserRole
}

interface CurrentDoctorContext {
  email: string | null
  canQueryDirectory: boolean
}

interface LoadDoctorsParams {
  search?: string
  regionId?: number
  role?: UserRole
  status?: UserStatus
}

const state = reactive({
  doctors: [] as UiDoctor[],
  page: 0,
  limit: 20,
  total: 0,
  loading: false,
})

const DOCTOR_PROFILE_CACHE_KEY = 'doctorProfileByEmail'
const DOCTOR_PAGE_LIMIT = 100

const readDoctorProfileCache = (): Record<string, UiDoctor> => {
  if (typeof window === 'undefined') return {}

  try {
    const raw = localStorage.getItem(DOCTOR_PROFILE_CACHE_KEY)
    if (!raw) return {}
    return JSON.parse(raw) as Record<string, UiDoctor>
  } catch {
    return {}
  }
}

const writeDoctorProfileCache = (cache: Record<string, UiDoctor>) => {
  if (typeof window === 'undefined') return
  localStorage.setItem(DOCTOR_PROFILE_CACHE_KEY, JSON.stringify(cache))
}

const rememberDoctorProfile = (doctor: UiDoctor) => {
  const email = doctor.email.trim().toLowerCase()
  if (!email) return
  const cache = readDoctorProfileCache()
  cache[email] = doctor
  writeDoctorProfileCache(cache)
}

const getCachedDoctorByEmail = (email: string): UiDoctor | null => {
  const normalized = email.trim().toLowerCase()
  if (!normalized) return null
  const cache = readDoctorProfileCache()
  return cache[normalized] ?? null
}

const toUiDoctor = (doctor: DoctorResponse): UiDoctor => {
  const fullName = [doctor.lastName, doctor.firstName, doctor.middleName ?? ''].filter(Boolean).join(' ')
  return {
    id: doctor.id,
    fullName,
    email: doctor.username,
    specialty: doctor.specialization,
    workplace: doctor.workplace,
    role: doctor.role,
    regionId: doctor.regionId,
    regionName: doctor.regionName,
    status: doctor.status,
  }
}

const setDoctor = (doctor: UiDoctor) => {
  const index = state.doctors.findIndex(item => item.id === doctor.id)
  if (index >= 0) {
    state.doctors.splice(index, 1, doctor)
  } else {
    state.doctors.push(doctor)
  }
  rememberDoctorProfile(doctor)
}

export const useDoctorStore = () => {
  const loadDoctors = async (params: LoadDoctorsParams = {}) => {
    state.loading = true
    try {
      const normalizedSearch = params.search?.trim()
      const search = normalizedSearch ? normalizedSearch : undefined

      const allItems: DoctorResponse[] = []
      let page = 0
      let total = Number.POSITIVE_INFINITY

      while (allItems.length < total) {
        const response = await doctorsApi.listDoctors({
          page,
          limit: DOCTOR_PAGE_LIMIT,
          search,
          regionId: params.regionId,
          role: params.role,
          status: params.status,
        })

        total = response.total
        if (response.items.length === 0) break

        allItems.push(...response.items)
        if (allItems.length >= total) break
        if (response.items.length < DOCTOR_PAGE_LIMIT) break

        page += 1
      }

      const mapped = allItems.map(toUiDoctor)
      state.doctors.splice(0, state.doctors.length, ...mapped)
      state.page = 0
      state.limit = DOCTOR_PAGE_LIMIT
      state.total = Number.isFinite(total) ? total : mapped.length
    } finally {
      state.loading = false
    }
  }

  const createDoctor = async (input: CreateDoctorInput) => {
    const payload: CreateDoctorRequest = {
      username: input.email.trim().toLowerCase(),
      role: input.role ?? 'DOCTOR',
      lastName: input.lastName.trim(),
      firstName: input.firstName.trim(),
      middleName: input.middleName?.trim() || null,
      specialization: input.specialty.trim(),
      workplace: input.workplace.trim(),
      regionId: input.regionId,
    }

    try {
      const created = await doctorsApi.createDoctor(payload)
      const doctor = toUiDoctor(created.doctor)
      setDoctor(doctor)

      return {
        doctor,
        temporaryPassword: created.temporaryPassword,
      }
    } catch (error) {
      if (error instanceof ApiClientError) {
        if (error.status === 400) throw new Error('VALIDATION_FAILED')
        if (error.status === 403) throw new Error('ACCESS_DENIED')
        if (error.status === 404) throw new Error('REGION_NOT_FOUND')
        if (error.status === 409) throw new Error('USERNAME_EXISTS')
      }
      throw error
    }
  }

  const updateDoctor = async (doctorId: number, payload: UpdateDoctorRequest) => {
    const updated = await doctorsApi.updateDoctor(doctorId, payload)
    const doctor = toUiDoctor(updated)
    setDoctor(doctor)
    return doctor
  }

  const resetDoctorPassword = async (doctorId: number) => {
    try {
      return await doctorsApi.resetDoctorPassword(doctorId)
    } catch (error) {
      if (error instanceof ApiClientError) {
        if (error.status === 401 || error.status === 403) throw new Error('ACCESS_DENIED')
        if (error.status === 404) throw new Error('DOCTOR_NOT_FOUND')
      }
      throw error
    }
  }

  const getDoctorById = (doctorId: number): UiDoctor | null => {
    return state.doctors.find(item => item.id === doctorId) ?? null
  }

  const getDoctorByEmail = (email: string): UiDoctor | null => {
    const normalized = email.trim().toLowerCase()
    const inMemory = state.doctors.find(item => item.email.toLowerCase() === normalized) ?? null
    if (inMemory) return inMemory
    return getCachedDoctorByEmail(normalized)
  }

  const resolveCurrentDoctorProfile = async (context: CurrentDoctorContext): Promise<UiDoctor | null> => {
    const normalizedEmail = context.email?.trim().toLowerCase() ?? ''
    if (!normalizedEmail) return null

    const known = getDoctorByEmail(normalizedEmail)
    if (known) return known

    try {
      const me = await doctorsApi.getCurrentDoctor()
      const current = toUiDoctor(me)
      setDoctor(current)
      if (current.email.toLowerCase() === normalizedEmail) {
        return current
      }
    } catch {
      // Fallback to cached/profile directory paths below.
    }

    if (!context.canQueryDirectory) return null

    const response = await doctorsApi.listDoctors({ page: 0, limit: 100, search: normalizedEmail })
    const mapped = response.items.map(toUiDoctor)
    state.doctors.splice(0, state.doctors.length, ...mapped)
    state.page = response.page
    state.limit = response.limit
    state.total = response.total

    const exact = mapped.find(item => item.email.toLowerCase() === normalizedEmail) ?? null
    if (exact) {
      rememberDoctorProfile(exact)
      return exact
    }

    return getCachedDoctorByEmail(normalizedEmail)
  }

  const doctors = computed(() => state.doctors)

  return {
    doctors,
    loading: computed(() => state.loading),
    total: computed(() => state.total),
    loadDoctors,
    createDoctor,
    updateDoctor,
    resetDoctorPassword,
    getDoctorById,
    getDoctorByEmail,
    resolveCurrentDoctorProfile,
  }
}
