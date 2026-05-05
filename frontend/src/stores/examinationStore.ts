import { reactive } from 'vue'
import type {
  CreateExaminationRequest,
  ExaminationResponse,
  UpdateExaminationMeasurementRequest,
  UpdateExaminationRequest,
} from '@/api/patientApi.contract'
import { patientsApi } from '@/api/patientsApi'
import { usePatientStore } from '@/stores/patientStore'

export interface ExaminationMetric {
  characteristicId: number
  characteristicCode: string
  characteristicName: string
  value: number
  unit: string
  comment: string | null
}

export interface Examination {
  id: number
  patientCode: string
  date: string
  doctor: string
  conclusion: string
  metrics: ExaminationMetric[]
  status: string
}

export interface EditableMetricInput {
  characteristicCode: string
  value: number
  comment: string | null
}

export interface MetricDescriptor {
  characteristicCode: string
  characteristicName: string
  unit: string
}

const FIXED_METRIC_COUNT = 50

export interface NewExamination {
  patientCode: string
  date: string
  doctor: string
  conclusion: string
  metrics: EditableMetricInput[]
}

export interface UpdateExamination {
  id: number
  date: string
  doctor: string
  conclusion: string
  metrics: EditableMetricInput[]
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
  const normalized = value.trim()

  const ruMatch = normalized.match(/^(\d{1,2})\.(\d{1,2})\.(\d{4})$/)
  if (ruMatch) {
    const day = ruMatch[1].padStart(2, '0')
    const month = ruMatch[2].padStart(2, '0')
    const year = ruMatch[3]
    return `${day}.${month}.${year}`
  }

  const isoMatch = normalized.match(/^(\d{4})-(\d{1,2})-(\d{1,2})/)
  if (isoMatch) {
    const year = isoMatch[1]
    const month = isoMatch[2].padStart(2, '0')
    const day = isoMatch[3].padStart(2, '0')
    return `${day}.${month}.${year}`
  }

  const sourceDate = new Date(normalized)
  if (Number.isNaN(sourceDate.getTime())) return value
  const day = String(sourceDate.getDate()).padStart(2, '0')
  const month = String(sourceDate.getMonth() + 1).padStart(2, '0')
  const year = String(sourceDate.getFullYear())
  return `${day}.${month}.${year}`
}

const normalizeCode = (value: string): string => value.trim()

const normalizeComment = (value: string | null | undefined): string | null => {
  const trimmed = (value ?? '').trim()
  return trimmed ? trimmed : null
}

const sanitizeMetricInputs = (metrics: EditableMetricInput[]): EditableMetricInput[] => {
  const sanitized: EditableMetricInput[] = []
  const seen = new Set<string>()

  metrics.forEach(metric => {
    const characteristicCode = normalizeCode(metric.characteristicCode)
    if (!characteristicCode) return
    if (!Number.isFinite(metric.value)) return

    if (seen.has(characteristicCode)) {
      throw new Error(`METRIC_DUPLICATE:${characteristicCode}`)
    }
    seen.add(characteristicCode)

    sanitized.push({
      characteristicCode,
      value: metric.value,
      comment: normalizeComment(metric.comment),
    })
  })

  return sanitized
}

const toStoreMetric = (measurement: ExaminationResponse['measurements'][number]): ExaminationMetric => {
  const numeric = Number(measurement.value)

  return {
    characteristicId: measurement.characteristicId,
    characteristicCode: measurement.characteristicCode,
    characteristicName: measurement.characteristicName,
    value: Number.isFinite(numeric) ? numeric : 0,
    unit: measurement.unit,
    comment: measurement.comment,
  }
}

const toStoreExam = (exam: ExaminationResponse, patientCode: string): Examination => {
  return {
    id: exam.examId,
    patientCode,
    date: toRuExamDate(exam.examDate),
    doctor: 'Не указано',
    conclusion: exam.comment ?? '',
    metrics: exam.measurements.map(toStoreMetric),
    status: 'Просмотрено',
  }
}

const applyPatientExaminations = (patientCode: string, responseItems: ExaminationResponse[]) => {
  const mapped = responseItems.map((exam) => toStoreExam(exam, patientCode))

  const latestExamId = mapped.length > 0
    ? [...mapped]
      .sort((left, right) => {
        const leftTime = parseExamDate(left.date)?.getTime() ?? 0
        const rightTime = parseExamDate(right.date)?.getTime() ?? 0
        if (rightTime !== leftTime) return rightTime - leftTime
        return right.id - left.id
      })[0]?.id
    : null

  const withStatus = mapped.map((exam) => ({
    ...exam,
    status: latestExamId === exam.id ? 'Новое' : 'Просмотрено',
  }))

  const filtered = state.examinations.filter((exam) => exam.patientCode !== patientCode)
  state.examinations.splice(0, state.examinations.length, ...filtered, ...withStatus)
}

const buildCreatePayload = (exam: {
  date: string
  conclusion: string
  metrics: EditableMetricInput[]
}): CreateExaminationRequest => {
  const metrics = sanitizeMetricInputs(exam.metrics)
  if (metrics.length === 0) {
    throw new Error('METRICS_REQUIRED')
  }

  return {
    title: 'Обследование',
    examDate: toIsoDate(exam.date),
    comment: normalizeComment(exam.conclusion),
    measurements: metrics.map(metric => ({
      characteristicCode: metric.characteristicCode,
      value: metric.value,
      comment: metric.comment,
    })),
  }
}

const buildUpdatePayload = (current: Examination, next: {
  date: string
  conclusion: string
  metrics: EditableMetricInput[]
}): UpdateExaminationRequest => {
  const payload: UpdateExaminationRequest = {}
  const nextExamDate = toIsoDate(next.date)
  const currentExamDate = toIsoDate(current.date)
  if (nextExamDate !== currentExamDate) {
    payload.examDate = nextExamDate
  }

  const nextComment = normalizeComment(next.conclusion)
  const currentComment = normalizeComment(current.conclusion)
  if (nextComment !== currentComment) {
    payload.comment = nextComment
  }

  const nextMetrics = sanitizeMetricInputs(next.metrics)
  const currentByCode = new Map<string, ExaminationMetric>(
    current.metrics.map(metric => [metric.characteristicCode, metric]),
  )
  const measurementChanges: UpdateExaminationMeasurementRequest[] = []

  nextMetrics.forEach(metric => {
    const existing = currentByCode.get(metric.characteristicCode)
    if (!existing) {
      measurementChanges.push({
        characteristicCode: metric.characteristicCode,
        value: metric.value,
        comment: metric.comment,
      })
      return
    }

    const valueChanged = metric.value !== existing.value
    const commentChanged = normalizeComment(metric.comment) !== normalizeComment(existing.comment)
    if (!valueChanged && !commentChanged) return

    measurementChanges.push({
      characteristicCode: metric.characteristicCode,
      value: valueChanged ? metric.value : undefined,
      comment: commentChanged ? normalizeComment(metric.comment) : undefined,
    })
  })

  if (measurementChanges.length > 0) {
    payload.measurements = measurementChanges
  }

  return payload
}

const hasPatchChanges = (payload: UpdateExaminationRequest): boolean => {
  return Boolean(
    payload.title !== undefined ||
      payload.examDate !== undefined ||
      payload.comment !== undefined ||
      (payload.measurements && payload.measurements.length > 0),
  )
}

const buildFallbackMetricCatalog = (): MetricDescriptor[] => {
  return Array.from({ length: FIXED_METRIC_COUNT }, (_, index) => {
    const ordinal = String(index + 1).padStart(2, '0')
    return {
      characteristicCode: `metric_${ordinal}`,
      characteristicName: `Metric ${ordinal}`,
      unit: `unit_${ordinal}`,
    }
  })
}

export const buildMetricCatalogFromExams = (exams: Examination[]): MetricDescriptor[] => {
  const byCode = new Map<string, MetricDescriptor>()

  buildFallbackMetricCatalog().forEach((metric) => {
    byCode.set(metric.characteristicCode, metric)
  })

  exams.forEach(exam => {
    exam.metrics.forEach(metric => {
      const existing = byCode.get(metric.characteristicCode)
      if (!existing) return
      byCode.set(metric.characteristicCode, {
        characteristicCode: metric.characteristicCode,
        characteristicName: metric.characteristicName || existing?.characteristicName || metric.characteristicCode,
        unit: metric.unit || existing?.unit || '',
      })
    })
  })

  return [...byCode.values()].sort((left, right) =>
    left.characteristicCode.localeCompare(right.characteristicCode),
  )
}

export const useExaminationStore = () => {
  const patientStore = usePatientStore()

  const loadByPatientCode = async (patientCode: string) => {
    const patientId = await patientStore.ensurePatientIdByCode(patientCode)
    if (!patientId) return false

    const response = await patientsApi.listPatientExaminations(patientId)
    applyPatientExaminations(patientCode, response.items)
    return true
  }

  const loadCurrentPatientExaminations = async () => {
    const { useAuthStore } = await import('@/stores/authStore')
    const authStore = useAuthStore()
    const patientCode = authStore.user.value?.patientCode
    if (authStore.user.value?.role !== 'PATIENT') return false
    if (!patientCode) {
      throw new Error('Не удалось определить код пациента в сессии.')
    }
    const patientId = await patientStore.resolveCurrentPatientId()
    if (!patientId) {
      throw new Error('Не удалось определить идентификатор карточки пациента.')
    }

    const response = await patientsApi.listPatientExaminations(patientId)
    applyPatientExaminations(patientCode, response.items)
    return true
  }

  const addExamination = async (exam: NewExamination) => {
    const patientId = await patientStore.ensurePatientIdByCode(exam.patientCode)
    if (!patientId) return false

    await patientsApi.addPatientExamination(patientId, buildCreatePayload(exam))
    await loadByPatientCode(exam.patientCode)
    return true
  }

  const updateExamination = async (payload: UpdateExamination) => {
    const targetExam = state.examinations.find(exam => exam.id === payload.id)
    if (!targetExam) return false

    const patientId = await patientStore.ensurePatientIdByCode(targetExam.patientCode)
    if (!patientId) return false

    const patchPayload = buildUpdatePayload(targetExam, payload)
    if (!hasPatchChanges(patchPayload)) return true

    await patientsApi.updatePatientExamination(
      patientId,
      payload.id,
      patchPayload,
    )

    await loadByPatientCode(targetExam.patientCode)
    return true
  }

  return {
    examinations: state.examinations,
    addExamination,
    updateExamination,
    loadByPatientCode,
    loadCurrentPatientExaminations,
  }
}
