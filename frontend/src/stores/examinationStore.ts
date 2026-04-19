import { reactive } from 'vue'
import type { CreateExaminationRequest } from '@/api/patientApi.contract'
import { mockPatientApi } from '@/mocks/openapi/mockPatientApi'

export interface Examination {
  id: number
  patientCode: string
  date: string
  doctor: string
  conclusion: string
  indicators: number[]
  status: string
}

export interface NewExamination {
  patientCode: string
  date: string
  doctor: string
  conclusion: string
  indicators: number[]
}

export interface UpdateExamination {
  id: number
  date: string
  doctor: string
  conclusion: string
  indicators: number[]
}

const state = reactive({
  examinations: [] as Examination[],
})

export function parseExamDate(value: string): Date | null {
  const [day, month, year] = value.split('.').map(Number)
  if (!day || !month || !year) return null
  const date = new Date(year, month - 1, day)
  return Number.isNaN(date.getTime()) ? null : date
}

const toIsoDate = (value: string): string => {
  const parsed = parseExamDate(value)
  if (!parsed) return value
  const year = parsed.getFullYear()
  const month = String(parsed.getMonth() + 1).padStart(2, '0')
  const day = String(parsed.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export function toRuExamDate(value: string): string {
  const sourceDate = new Date(value)
  if (Number.isNaN(sourceDate.getTime())) return value
  return sourceDate.toLocaleDateString('ru-RU')
}

const getIndicatorsFromMeasurements = (examId: number): number[] => {
  const all = mockPatientApi.listAllExaminationsRaw()
  const exam = all.find(item => item.examId === examId)
  if (!exam) return []

  return exam.measurements.map(measurement => {
    const numeric = Number(measurement.value)
    return Number.isNaN(numeric) ? 0 : numeric
  })
}

const syncExaminationsFromMock = () => {
  const patientsById = new Map(
    mockPatientApi.listAllPatientsRaw().map(patient => [patient.id, patient.patientCode])
  )

  const allRaw = mockPatientApi.listAllExaminationsRaw().map(exam => ({
    id: exam.examId,
    patientCode: patientsById.get(exam.patientId) ?? '',
    date: mockPatientApi.toRuDateFromIso(exam.examDate),
    doctor: exam.doctorName,
    conclusion: exam.comment ?? '',
    indicators: exam.measurements.map(item => Number(item.value) || 0),
  }))

  const latestByPatient = new Map<string, number>()
  const sortedByDate = [...allRaw].sort((left, right) => {
    const leftTime = parseExamDate(left.date)?.getTime() ?? 0
    const rightTime = parseExamDate(right.date)?.getTime() ?? 0
    return rightTime - leftTime
  })

  for (const exam of sortedByDate) {
    if (!latestByPatient.has(exam.patientCode)) {
      latestByPatient.set(exam.patientCode, exam.id)
    }
  }

  const mapped: Examination[] = allRaw.map(exam => ({
    ...exam,
    status: latestByPatient.get(exam.patientCode) === exam.id ? 'Новое' : 'Просмотрено',
  }))

  state.examinations.splice(0, state.examinations.length, ...mapped)
}

syncExaminationsFromMock()

const toApiPayload = (exam: {
  date: string
  conclusion: string
  indicators: number[]
}): CreateExaminationRequest => {
  return {
    title: 'Обследование',
    examDate: toIsoDate(exam.date),
    comment: exam.conclusion.trim() || null,
    measurements: exam.indicators.map((value, index) => ({
      characteristicCode: `CHAR_${String(index + 1).padStart(2, '0')}`,
      value,
      comment: null,
    })),
  }
}

export const useExaminationStore = () => {
  const addExamination = (exam: NewExamination) => {
    const patient = mockPatientApi.findPatientByCode(exam.patientCode)
    if (!patient) return false

    mockPatientApi.addPatientExamination(patient.id, toApiPayload(exam), exam.doctor)
    syncExaminationsFromMock()
    return true
  }

  const updateExamination = (payload: UpdateExamination) => {
    const targetExam = state.examinations.find(exam => exam.id === payload.id)
    if (!targetExam) return false

    const patient = mockPatientApi.findPatientByCode(targetExam.patientCode)
    if (!patient) return false

    const nextIndicators = payload.indicators.length > 0
      ? payload.indicators
      : getIndicatorsFromMeasurements(payload.id)

    const updated = mockPatientApi.updatePatientExamination(
      patient.id,
      payload.id,
      toApiPayload({
        date: payload.date,
        conclusion: payload.conclusion,
        indicators: nextIndicators,
      }),
      payload.doctor
    )

    if (!updated) return false

    syncExaminationsFromMock()
    return true
  }

  return {
    examinations: state.examinations,
    addExamination,
    updateExamination,
  }
}
