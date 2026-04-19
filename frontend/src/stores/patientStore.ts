import { reactive } from 'vue'
import type { CreatePatientRequest, OperationParametersResponse } from '@/api/patientApi.contract'
import { mockPatientApi } from '@/mocks/openapi/mockPatientApi'
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
  fullName: string
  lastName: string
  firstName: string
  middleName: string
  birthDate: string
  age: number
  diagnosis: string
  operations: number
  operationDetails: PatientOperation[]
  medications: string
  valve: ValveDetails
  lastExam: string
  region: string
}

export interface NewPatientInput {
  lastName: string
  firstName: string
  middleName?: string
  birthDate: string
  region: string
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

const state = reactive({
  patients: [] as Patient[],
  regionChangeLogs: [] as RegionChangeLogEntry[],
})

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

const buildPatientFromRaw = (patientId: number): Patient | null => {
  const raw = mockPatientApi.findPatientById(patientId)
  if (!raw) return null

  const exams = mockPatientApi.listPatientExaminations(raw.id).items
  const lastExamIso = exams[0]?.examDate ?? null

  const middleName = raw.middleName ?? ''
  const fullName = [raw.lastName, raw.firstName, middleName].filter(Boolean).join(' ')

  return {
    code: raw.patientCode,
    fullName,
    lastName: raw.lastName,
    firstName: raw.firstName,
    middleName,
    birthDate: raw.birthDate,
    age: calculateAge(raw.birthDate),
    diagnosis: raw.diagnosis,
    operations: raw.operationHistory.length,
    operationDetails: raw.operationHistory.map(item => ({ ...item })),
    medications: raw.medications,
    valve: { ...raw.valve },
    lastExam: lastExamIso ? formatRuDate(lastExamIso) : 'Нет данных',
    region: raw.regionName,
  }
}

const syncPatientsFromMock = () => {
  const mapped = mockPatientApi
    .listAllPatientsRaw()
    .map(patient => buildPatientFromRaw(patient.id))
    .filter((patient): patient is Patient => Boolean(patient))

  state.patients.splice(0, state.patients.length, ...mapped)
}

syncPatientsFromMock()

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

export const usePatientStore = () => {
  const addPatient = (input: NewPatientInput) => {
    const trimmedOperations = (input.operations ?? [])
      .map(item => ({
        name: item.name.trim(),
        anesthesia: item.anesthesia.trim(),
        duration: item.duration.trim(),
        deliverySystem: item.deliverySystem.trim(),
      }))
      .filter(item => item.name || item.anesthesia || item.duration || item.deliverySystem)

    const payload: CreatePatientRequest = {
      lastName: input.lastName.trim(),
      firstName: input.firstName.trim(),
      middleName: input.middleName?.trim() || null,
      birthDate: input.birthDate,
      diagnosis: input.diagnosis.trim(),
      regionId: resolveRegionIdByName(input.region),
      medications: input.medications?.trim() || '',
      valve: {
        name: input.valve?.name?.trim() || '',
        size: input.valve?.size?.trim() || '',
        material: input.valve?.material?.trim() || '',
      },
      operationParameters: toOperationParameters(trimmedOperations),
    }

    const created = mockPatientApi.createPatientCard(payload)

    if (trimmedOperations.length > 0) {
      mockPatientApi.setPatientOperationHistory(
        created.id,
        trimmedOperations.map(operation => ({ ...operation }))
      )
    } else {
      mockPatientApi.setPatientOperationHistory(created.id, [
        {
          name: 'Операция не указана',
          anesthesia: payload.operationParameters.anesthesia,
          duration: toDurationFromMinutes(payload.operationParameters.durationMinutes),
          deliverySystem: payload.operationParameters.deliverySystem,
        },
      ])
    }

    syncPatientsFromMock()

    const createdPatient = state.patients.find(patient => patient.code === created.patientCode)

    return {
      patient: createdPatient ?? {
        code: created.patientCode,
        fullName: [created.lastName, created.firstName, created.middleName ?? ''].filter(Boolean).join(' '),
        lastName: created.lastName,
        firstName: created.firstName,
        middleName: created.middleName ?? '',
        birthDate: created.birthDate,
        age: calculateAge(created.birthDate),
        diagnosis: created.diagnosis,
        operations: trimmedOperations.length,
        operationDetails: trimmedOperations,
        medications: created.medications,
        valve: { ...created.valve },
        lastExam: 'Нет данных',
        region: RegionsStore[created.regionId - 1]?.name ?? input.region,
      },
      password: created.temporaryPassword,
    }
  }

  const transferPatientRegion = (patientCode: string, nextRegion: string, changedBy: string) => {
    const target = mockPatientApi.findPatientByCode(patientCode)
    if (!target) return null

    const nextRegionName = nextRegion.trim()
    if (!nextRegionName || nextRegionName === target.regionName) {
      return state.patients.find(patient => patient.code === patientCode) ?? null
    }

    const previousRegion = target.regionName

    mockPatientApi.updatePatientCard(target.id, {
      regionId: resolveRegionIdByName(nextRegionName),
    })

    state.regionChangeLogs.unshift({
      id: Date.now() + Math.floor(Math.random() * 1000),
      patientCode,
      changedAt: new Date().toLocaleString('ru-RU'),
      changedBy: changedBy.trim() || 'Неизвестный врач',
      fromRegion: previousRegion,
      toRegion: nextRegionName,
    })

    syncPatientsFromMock()

    return state.patients.find(patient => patient.code === patientCode) ?? null
  }

  const getPatientRegionLogs = (patientCode: string) => {
    return state.regionChangeLogs.filter(entry => entry.patientCode === patientCode)
  }

  return {
    patients: state.patients,
    regionChangeLogs: state.regionChangeLogs,
    addPatient,
    transferPatientRegion,
    getPatientRegionLogs,
  }
}
