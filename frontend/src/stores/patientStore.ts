import { reactive } from 'vue'
import type {
  CreatePatientRequest,
  CreatedPatientResponse,
  OperationParametersResponse,
  PatientCardResponse,
  PatientStatus,
  PatientSummaryResponse,
  UpdatePatientRequest,
} from '@/api/patientApi.contract'
import { patientsApi } from '@/api/patientsApi'
import { ApiClientError } from '@/api/httpClient'
import { RegionsStore } from '@/stores/regionsStore'

export interface PatientOperation {
  name: string
  anesthesia: string
  duration: string
  deliverySystem: string
}

export interface ValveDetails {
  name: string
  size: string
  material: string
}

export interface Patient {
  code: string
  birthDate: string
  age: number
  diagnosis: string
  operations: number
  operationDetails: PatientOperation[]
  medications: string
  valve: ValveDetails
  status: PatientStatus
  lastExam: string
  region: string
}

export interface NewPatientInput {
  birthDate: string
  region?: string
  regionId?: number
  diagnosis: string
  operations?: PatientOperation[]
  valve?: ValveDetails
  medications?: string
}

export interface RegionChangeLogEntry {
  id: number
  patientCode: string
  changedAt: string
  changedBy: string
  fromRegion: string
  toRegion: string
}

export interface KnownRegion {
  id: number
  name: string
}

interface PatientState {
  patients: Patient[]
  regionChangeLogs: RegionChangeLogEntry[]
  patientIdsByCode: Record<string, number>
  regionNamesById: Record<number, string>
  cardsByCode: Record<string, PatientCardResponse>
  lastLoadedScope: 'own' | 'all' | null
  loading: boolean
}

const state = reactive<PatientState>({
  patients: [],
  regionChangeLogs: [],
  patientIdsByCode: {},
  regionNamesById: {},
  cardsByCode: {},
  lastLoadedScope: null,
  loading: false,
})

const PATIENT_ID_CACHE_KEY = 'patientIdByCode'
const PATIENT_ID_PROBE_WINDOW = 20
const PATIENT_ID_PROBE_MAX = 2000
const DOCTOR_PATIENTS_PAGE_LIMIT = 100

const parseIsoDate = (value: string): Date | null => {
  const parsed = new Date(value)
  return Number.isNaN(parsed.getTime()) ? null : parsed
}

const formatRuDate = (value: string): string => {
  const parsed = parseIsoDate(value)
  if (!parsed) return value
  return parsed.toLocaleDateString('ru-RU')
}

const calculateAge = (birthDate: string): number => {
  const birth = parseIsoDate(birthDate)
  if (!birth) return 0

  const today = new Date()
  let age = today.getFullYear() - birth.getFullYear()
  const hasBirthdayPassed =
    today.getMonth() > birth.getMonth() ||
    (today.getMonth() === birth.getMonth() && today.getDate() >= birth.getDate())

  if (!hasBirthdayPassed) age -= 1
  return Math.max(age, 0)
}

const toMinutesFromDuration = (duration: string): number => {
  const normalized = duration.toLowerCase()
  const hoursMatch = normalized.match(/(\d+)\s*час/)
  const minutesMatch = normalized.match(/(\d+)\s*мин/)

  const hours = hoursMatch ? Number.parseInt(hoursMatch[1], 10) : 0
  const minutes = minutesMatch ? Number.parseInt(minutesMatch[1], 10) : 0
  const total = hours * 60 + minutes

  return total > 0 ? total : 60
}

const toDurationFromMinutes = (minutes: number): string => {
  const safe = Math.max(0, Math.floor(minutes))
  const hours = Math.floor(safe / 60)
  const mins = safe % 60

  if (hours === 0) return `${mins} минут`
  if (mins === 0) return `${hours} часа`
  return `${hours} часа ${mins} минут`
}

const resolveRegionIdByName = (regionName: string): number => {
  const index = RegionsStore.findIndex(region => region.name === regionName)
  return index >= 0 ? index + 1 : 1
}

const resolveRegionNameById = (regionId: number): string => {
  return state.regionNamesById[regionId] ?? `Регион #${regionId}`
}

const rememberRegionName = (regionId: number, regionName: string | null | undefined) => {
  const normalized = (regionName ?? '').trim()
  if (!Number.isFinite(regionId) || regionId <= 0 || !normalized) return
  state.regionNamesById[regionId] = normalized
}

const createDefaultOperation = (params: OperationParametersResponse): PatientOperation => ({
  name: 'Операция',
  anesthesia: params.anesthesia,
  duration: toDurationFromMinutes(params.durationMinutes),
  deliverySystem: params.deliverySystem,
})

const toPatientFromSummary = (summary: PatientSummaryResponse): Patient => {
  return {
    code: summary.patientCode,
    birthDate: formatRuDate(summary.birthDate),
    age: calculateAge(summary.birthDate),
    diagnosis: summary.diagnosis,
    operations: 1,
    operationDetails: [createDefaultOperation(summary.operationParameters)],
    medications: summary.medications,
    valve: {
      ...summary.valve,
    },
    status: summary.status,
    lastExam: summary.lastExaminationAt ? formatRuDate(summary.lastExaminationAt) : 'Нет данных',
    region: resolveRegionNameById(summary.regionId),
  }
}

const toPatientFromCard = (card: PatientCardResponse, status: PatientStatus = 'RED'): Patient => {
  const lastExamIso = card.vitalsHistory.length > 0 ? card.vitalsHistory[card.vitalsHistory.length - 1].examDate : null
  rememberRegionName(card.regionId, card.regionName)

  return {
    code: card.patientCode,
    birthDate: formatRuDate(card.birthDate),
    age: calculateAge(card.birthDate),
    diagnosis: card.diagnosis,
    operations: 1,
    operationDetails: [createDefaultOperation(card.operationParameters)],
    medications: card.medications,
    valve: {
      ...card.valve,
    },
    status,
    lastExam: lastExamIso ? formatRuDate(lastExamIso) : 'Нет данных',
    region: card.regionName,
  }
}

const setPatientInList = (patient: Patient) => {
  const index = state.patients.findIndex(item => item.code === patient.code)
  if (index >= 0) {
    state.patients.splice(index, 1, patient)
  } else {
    state.patients.push(patient)
  }
}

const toOperationParameters = (operations: PatientOperation[] | undefined): OperationParametersResponse => {
  const first = operations?.[0]
  if (!first) {
    return {
      anesthesia: 'Не указано',
      durationMinutes: 60,
      deliverySystem: 'Не указано',
    }
  }

  return {
    anesthesia: first.anesthesia || 'Не указано',
    durationMinutes: toMinutesFromDuration(first.duration),
    deliverySystem: first.deliverySystem || 'Не указано',
  }
}

const toFallbackPatientFromCreated = (created: CreatedPatientResponse): Patient => {
  return {
    code: created.patientCode,
    birthDate: formatRuDate(created.birthDate),
    age: calculateAge(created.birthDate),
    diagnosis: created.diagnosis,
    operations: 1,
    operationDetails: [createDefaultOperation(created.operationParameters)],
    medications: created.medications,
    valve: { ...created.valve },
    status: 'RED',
    lastExam: 'Нет данных',
    region: resolveRegionNameById(created.regionId),
  }
}

const resolvePatientIdByCode = (patientCode: string): number | null => {
  return state.patientIdsByCode[patientCode] ?? null
}

const readPatientIdsCache = (): Record<string, number> => {
  if (typeof window === 'undefined') return {}
  try {
    const raw = localStorage.getItem(PATIENT_ID_CACHE_KEY)
    if (!raw) return {}
    const parsed = JSON.parse(raw) as Record<string, unknown>
    const normalized: Record<string, number> = {}
    Object.entries(parsed).forEach(([key, value]) => {
      if (typeof value === 'number' && Number.isFinite(value) && value > 0) {
        normalized[key] = value
      }
    })
    return normalized
  } catch {
    return {}
  }
}

const persistPatientIdsCache = () => {
  if (typeof window === 'undefined') return
  const payload: Record<string, number> = {}
  Object.entries(state.patientIdsByCode).forEach(([code, id]) => {
    if (typeof id === 'number' && id > 0) payload[code] = id
  })
  localStorage.setItem(PATIENT_ID_CACHE_KEY, JSON.stringify(payload))
}

const rememberPatientId = (patientCode: string, patientId: number) => {
  if (!patientCode || !Number.isFinite(patientId) || patientId <= 0) return
  state.patientIdsByCode[patientCode] = patientId
  persistPatientIdsCache()
}

const hydratePatientIdsCache = () => {
  const cached = readPatientIdsCache()
  Object.entries(cached).forEach(([code, id]) => {
    state.patientIdsByCode[code] = id
  })
}

hydratePatientIdsCache()

const getCachedPatientIdByCode = (patientCode: string): number | null => {
  const card = state.cardsByCode[patientCode]
  return card?.id ?? null
}

const tryGetOwnCardById = async (
  candidateId: number,
  expectedPatientCode: string,
): Promise<PatientCardResponse | null> => {
  if (!Number.isFinite(candidateId) || candidateId <= 0) return null

  try {
    const card = await patientsApi.getPatientCard(candidateId)
    if (!card.patientCode || card.patientCode !== expectedPatientCode) {
      return null
    }
    return card
  } catch (error) {
    if (error instanceof ApiClientError && (error.status === 403 || error.status === 404)) {
      return null
    }
    throw error
  }
}

const discoverPatientCardByProbing = async (
  userId: number,
  expectedPatientCode: string,
): Promise<PatientCardResponse | null> => {
  const checked = new Set<number>()

  const probeCandidate = async (candidateId: number): Promise<PatientCardResponse | null> => {
    if (!Number.isFinite(candidateId) || candidateId <= 0 || checked.has(candidateId)) {
      return null
    }
    checked.add(candidateId)
    return tryGetOwnCardById(candidateId, expectedPatientCode)
  }

  const direct = await probeCandidate(userId)
  if (direct) return direct

  for (let delta = 1; delta <= PATIENT_ID_PROBE_WINDOW; delta += 1) {
    const backward = await probeCandidate(userId - delta)
    if (backward) return backward

    const forward = await probeCandidate(userId + delta)
    if (forward) return forward
  }

  const upperBound = Math.min(
    Math.max(userId + PATIENT_ID_PROBE_WINDOW, 400),
    PATIENT_ID_PROBE_MAX,
  )
  for (let candidateId = 1; candidateId <= upperBound; candidateId += 1) {
    const card = await probeCandidate(candidateId)
    if (card) return card
  }

  return null
}

const getCurrentPatientContext = async () => {
  const { useAuthStore } = await import('@/stores/authStore')
  const authStore = useAuthStore()
  const user = authStore.user.value

  return {
    role: user?.role ?? null,
    userId: user?.id ?? null,
    patientCode: user?.patientCode ?? null,
  }
}

const ensurePatientIdByCode = async (patientCode: string): Promise<number | null> => {
  const known = resolvePatientIdByCode(patientCode)
  if (known) return known

  const cached = getCachedPatientIdByCode(patientCode)
  if (cached) {
    rememberPatientId(patientCode, cached)
    return cached
  }

  const context = await getCurrentPatientContext()
  if (context.role === 'PATIENT') {
    if (context.patientCode !== patientCode) return null

    return resolveCurrentPatientId()
  }

  await loadPatients('all')
  return resolvePatientIdByCode(patientCode)
}

const loadPatients = async (scope: 'own' | 'all' = 'own') => {
  state.loading = true
  try {
    const summaries: PatientSummaryResponse[] = []
    let page = 0
    let total = Number.POSITIVE_INFINITY

    while (summaries.length < total) {
      const response = await patientsApi.listDoctorPatients({
        scope,
        page,
        limit: DOCTOR_PATIENTS_PAGE_LIMIT,
      })

      total = response.total
      if (response.items.length === 0) break

      summaries.push(...response.items)
      if (summaries.length >= total) break
      if (response.items.length < DOCTOR_PATIENTS_PAGE_LIMIT) break

      page += 1
    }

    const missingRegionSamples = new Map<number, PatientSummaryResponse>()
    summaries.forEach(summary => {
      if (!state.regionNamesById[summary.regionId] && !missingRegionSamples.has(summary.regionId)) {
        missingRegionSamples.set(summary.regionId, summary)
      }
    })

    for (const sample of missingRegionSamples.values()) {
      try {
        const card = await patientsApi.getPatientCard(sample.id)
        state.cardsByCode[card.patientCode] = card
        state.patientIdsByCode[card.patientCode] = card.id
        rememberRegionName(card.regionId, card.regionName)
      } catch (error) {
        if (!(error instanceof ApiClientError && (error.status === 403 || error.status === 404))) {
          throw error
        }
      }
    }

    const mapped = summaries.map(toPatientFromSummary)
    const nextPatientIdsByCode: Record<string, number> = { ...state.patientIdsByCode }
    summaries.forEach(summary => {
      nextPatientIdsByCode[summary.patientCode] = summary.id
    })

    state.patientIdsByCode = nextPatientIdsByCode
    state.patients.splice(0, state.patients.length, ...mapped)

    if (scope === 'own' && summaries.length > 0) {
      const first = summaries[0]
      const { useAuthStore } = await import('@/stores/authStore')
      const authStore = useAuthStore()
      authStore.updateDoctorRegion(first.regionId, resolveRegionNameById(first.regionId))
    }

    state.lastLoadedScope = scope
  } finally {
    state.loading = false
  }
}

const loadKnownRegionsFromDoctorPatients = async () => {
  const summaries: PatientSummaryResponse[] = []
  let page = 0
  let total = Number.POSITIVE_INFINITY

  while (summaries.length < total) {
    const response = await patientsApi.listDoctorPatients({
      scope: 'all',
      page,
      limit: DOCTOR_PATIENTS_PAGE_LIMIT,
    })

    total = response.total
    if (response.items.length === 0) break

    summaries.push(...response.items)
    if (summaries.length >= total) break
    if (response.items.length < DOCTOR_PATIENTS_PAGE_LIMIT) break

    page += 1
  }

  const missingRegionSamples = new Map<number, PatientSummaryResponse>()
  summaries.forEach(summary => {
    if (!state.regionNamesById[summary.regionId] && !missingRegionSamples.has(summary.regionId)) {
      missingRegionSamples.set(summary.regionId, summary)
    }
  })

  for (const sample of missingRegionSamples.values()) {
    try {
      const card = await patientsApi.getPatientCard(sample.id)
      state.cardsByCode[card.patientCode] = card
      state.patientIdsByCode[card.patientCode] = card.id
      rememberRegionName(card.regionId, card.regionName)
    } catch (error) {
      if (!(error instanceof ApiClientError && (error.status === 403 || error.status === 404))) {
        throw error
      }
    }
  }

  persistPatientIdsCache()
}

const loadPatientCardByCode = async (patientCode: string): Promise<Patient | null> => {
  const patientId = await ensurePatientIdByCode(patientCode)
  if (!patientId) return null

  const card = await patientsApi.getPatientCard(patientId)
  state.cardsByCode[patientCode] = card
  state.patientIdsByCode[patientCode] = card.id
  rememberRegionName(card.regionId, card.regionName)

  const knownStatus = state.patients.find(item => item.code === patientCode)?.status ?? 'RED'
  const patient = toPatientFromCard(card, knownStatus)
  setPatientInList(patient)
  return patient
}

const resolveCurrentPatientId = async (): Promise<number | null> => {
  const context = await getCurrentPatientContext()
  if (context.role !== 'PATIENT') return null

  const patientId = context.userId
  const patientCode = context.patientCode
  if (!patientCode) return null
  if (typeof patientId !== 'number' || patientId <= 0) {
    return null
  }

  const known = resolvePatientIdByCode(patientCode)
  if (known) {
    return known
  }

  const cardByDirectId = await tryGetOwnCardById(patientId, patientCode)
  if (cardByDirectId) {
    const knownStatus = state.patients.find(item => item.code === cardByDirectId.patientCode)?.status ?? 'RED'
    state.cardsByCode[cardByDirectId.patientCode] = cardByDirectId
    rememberPatientId(cardByDirectId.patientCode, cardByDirectId.id)
    setPatientInList(toPatientFromCard(cardByDirectId, knownStatus))
    return cardByDirectId.id
  }

  const discovered = await discoverPatientCardByProbing(patientId, patientCode)
  if (!discovered) return null

  const knownStatus = state.patients.find(item => item.code === discovered.patientCode)?.status ?? 'RED'
  state.cardsByCode[discovered.patientCode] = discovered
  rememberPatientId(discovered.patientCode, discovered.id)
  setPatientInList(toPatientFromCard(discovered, knownStatus))
  return discovered.id
}

const loadCurrentPatientCard = async (): Promise<Patient | null> => {
  const patientId = await resolveCurrentPatientId()
  if (!patientId) {
    throw new Error('Не удалось определить идентификатор карточки пациента.')
  }

  const card = await patientsApi.getPatientCard(patientId)
  state.cardsByCode[card.patientCode] = card
  rememberPatientId(card.patientCode, card.id)
  rememberRegionName(card.regionId, card.regionName)

  const knownStatus = state.patients.find(item => item.code === card.patientCode)?.status ?? 'RED'
  const patient = toPatientFromCard(card, knownStatus)
  setPatientInList(patient)
  return patient
}

export const usePatientStore = () => {
  const getKnownRegions = (): KnownRegion[] => {
    return Object.entries(state.regionNamesById)
      .map(([id, name]) => ({
        id: Number.parseInt(id, 10),
        name,
      }))
      .filter(region => Number.isFinite(region.id) && region.id > 0 && Boolean(region.name))
      .sort((a, b) => a.name.localeCompare(b.name, 'ru'))
  }

  const addPatient = async (input: NewPatientInput, syncScope?: 'own' | 'all') => {
    const trimmedOperations = (input.operations ?? [])
      .map(item => ({
        name: item.name.trim(),
        anesthesia: item.anesthesia.trim(),
        duration: item.duration.trim(),
        deliverySystem: item.deliverySystem.trim(),
      }))
      .filter(item => item.name || item.anesthesia || item.duration || item.deliverySystem)

    const resolvedRegionId =
      typeof input.regionId === 'number' && Number.isFinite(input.regionId) && input.regionId > 0
        ? input.regionId
        : input.region
          ? resolveRegionIdByName(input.region)
          : null

    if (!resolvedRegionId) {
      throw new Error('REGION_ID_REQUIRED')
    }

    const payload: CreatePatientRequest = {
      birthDate: input.birthDate,
      diagnosis: input.diagnosis.trim(),
      regionId: resolvedRegionId,
      medications: input.medications?.trim() || '',
      valve: {
        name: input.valve?.name?.trim() || '',
        size: input.valve?.size?.trim() || '',
        material: input.valve?.material?.trim() || '',
      },
      operationParameters: toOperationParameters(trimmedOperations),
    }

    const created = await patientsApi.createPatient(payload)
    state.patientIdsByCode[created.patientCode] = created.id

    let patient = toFallbackPatientFromCreated(created)
    try {
      const loaded = await loadPatientCardByCode(created.patientCode)
      if (loaded) {
        patient = loaded
      }
    } catch {
      setPatientInList(patient)
    }

    const scopeToSync = syncScope ?? state.lastLoadedScope
    if (scopeToSync) {
      await loadPatients(scopeToSync)
      const refreshed = state.patients.find(item => item.code === created.patientCode)
      if (refreshed) {
        patient = refreshed
      }
    }

    return {
      patient,
      password: created.temporaryPassword,
    }
  }

  const transferPatientRegion = async (
    patientCode: string,
    nextRegionId: number,
    changedBy: string,
    nextRegionNameHint?: string,
    syncScope?: 'own' | 'all',
  ) => {
    const patientId = await ensurePatientIdByCode(patientCode)
    if (!patientId) return null

    const target = state.patients.find(patient => patient.code === patientCode)
    if (!target) return null

    if (!Number.isFinite(nextRegionId) || nextRegionId <= 0) {
      return target
    }

    const currentRegionId = state.cardsByCode[patientCode]?.regionId ?? null
    if (currentRegionId === nextRegionId) return target

    const previousRegion = target.region

    const payload: UpdatePatientRequest = {
      regionId: nextRegionId,
    }
    const updatedCard = await patientsApi.updatePatientCard(patientId, payload)

    state.cardsByCode[patientCode] = updatedCard
    state.patientIdsByCode[patientCode] = updatedCard.id
    rememberRegionName(updatedCard.regionId, updatedCard.regionName)

    const knownStatus = state.patients.find(item => item.code === patientCode)?.status ?? 'RED'
    const updatedPatient = toPatientFromCard(updatedCard, knownStatus)
    setPatientInList(updatedPatient)

    state.regionChangeLogs.unshift({
      id: Date.now() + Math.floor(Math.random() * 1000),
      patientCode,
      changedAt: new Date().toLocaleString('ru-RU'),
      changedBy: changedBy.trim() || 'Неизвестный врач',
      fromRegion: previousRegion,
      toRegion: updatedPatient.region || nextRegionNameHint || `Регион #${nextRegionId}`,
    })

    const scopeToSync = syncScope ?? state.lastLoadedScope
    if (scopeToSync) {
      await loadPatients(scopeToSync)
    }

    return updatedPatient
  }

  const updatePatientPassword = async (
    patientCode: string,
    nextPassword: string,
    syncScope?: 'own' | 'all',
  ) => {
    const patientId = await ensurePatientIdByCode(patientCode)
    if (!patientId) {
      throw new Error('PATIENT_NOT_FOUND')
    }

    const normalized = nextPassword.trim()
    if (!normalized) {
      throw new Error('VALIDATION_FAILED')
    }
    if (normalized.length < 6) {
      throw new Error('WEAK_PASSWORD')
    }

    try {
      const updatedCard = await patientsApi.updatePatientCard(patientId, { password: normalized })

      state.cardsByCode[patientCode] = updatedCard
      state.patientIdsByCode[patientCode] = updatedCard.id
      rememberRegionName(updatedCard.regionId, updatedCard.regionName)

      const knownStatus = state.patients.find(item => item.code === patientCode)?.status ?? 'RED'
      setPatientInList(toPatientFromCard(updatedCard, knownStatus))

      const scopeToSync = syncScope ?? state.lastLoadedScope
      if (scopeToSync) {
        await loadPatients(scopeToSync)
      }
    } catch (error) {
      if (error instanceof ApiClientError) {
        if (error.status === 400) throw new Error('VALIDATION_FAILED')
        if (error.status === 401 || error.status === 403) throw new Error('ACCESS_DENIED')
        if (error.status === 404) throw new Error('PATIENT_NOT_FOUND')
      }
      throw error
    }
  }

  const getPatientRegionLogs = (patientCode: string) => {
    return state.regionChangeLogs.filter(entry => entry.patientCode === patientCode)
  }

  return {
    patients: state.patients,
    regionChangeLogs: state.regionChangeLogs,
    loading: state.loading,
    addPatient,
    transferPatientRegion,
    updatePatientPassword,
    getPatientRegionLogs,
    getKnownRegions,
    loadKnownRegionsFromDoctorPatients,
    loadPatients,
    loadPatientCardByCode,
    loadCurrentPatientCard,
    resolvePatientIdByCode,
    ensurePatientIdByCode,
    resolveCurrentPatientId,
  }
}
