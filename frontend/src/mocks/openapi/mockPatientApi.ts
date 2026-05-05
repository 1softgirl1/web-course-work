import {
  type AuthResponse,
  type ChangePasswordRequest,
  type CreateExaminationRequest,
  type CreateDoctorRequest,
  type CreatePatientRequest,
  type CreatedPatientResponse,
  type CreatedDoctorResponse,
  type ExaminationListResponse,
  type ExaminationResponse,
  type ListDoctorPatientsParams,
  type LoginRequest,
  type OperationParametersResponse,
  type PatientCardResponse,
  type PatientListResponse,
  type PatientStatus,
  type UserRole,
  type UpdatePatientRequest,
  type ValveResponse,
} from '@/api/patientApi.contract'
import { DEFAULT_REGION_ID, RegionsStore } from '@/stores/regionsStore'

export interface MockOperationHistoryItem {
  name: string
  anesthesia: string
  duration: string
  deliverySystem: string
}

export interface MockPatientRecord {
  id: number
  patientCode: string
  lastName: string
  firstName: string
  middleName: string | null
  birthDate: string
  diagnosis: string
  regionId: number
  regionName: string
  valve: ValveResponse
  operationParameters: OperationParametersResponse
  operationHistory: MockOperationHistoryItem[]
  medications: string
  createdAt: string
  temporaryPassword: string
  password?: string
}

export interface MockExaminationRecord extends ExaminationResponse {
  patientId: number
  doctorName: string
}

export interface MockDoctorRecord {
  id: number
  lastName: string
  firstName: string
  middleName: string | null
  fullName: string
  email: string
  specialty: string
  workplace: string
  role: UserRole
  regionId: number
  regionName: string
  password?: string
}

const CHARS = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'
const MOCK_DEFAULT_PASSWORD = '123456'

const toRegionIdByName = (regionName: string): number => {
  const index = RegionsStore.findIndex(region => region.name === regionName)
  return index >= 0 ? index + 1 : toRegionIdByStoreKey(DEFAULT_REGION_ID)
}

const toRegionIdByStoreKey = (storeKey: string): number => {
  const index = RegionsStore.findIndex(region => region.id === storeKey)
  return index >= 0 ? index + 1 : 1
}

const toRegionNameById = (regionId: number): string => {
  return RegionsStore[regionId - 1]?.name ?? RegionsStore[0]?.name ?? ''
}

const parseRuDate = (value: string): Date | null => {
  const [day, month, year] = value.split('.').map(Number)
  if (!day || !month || !year) return null
  const date = new Date(year, month - 1, day)
  return Number.isNaN(date.getTime()) ? null : date
}

const toIsoDateFromRu = (value: string): string => {
  const parsed = parseRuDate(value)
  if (!parsed) return new Date().toISOString().slice(0, 10)
  const year = parsed.getFullYear()
  const month = String(parsed.getMonth() + 1).padStart(2, '0')
  const day = String(parsed.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const toRuDateFromIso = (value: string): string => {
  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) return value
  return parsed.toLocaleDateString('ru-RU')
}

const randomFromChars = (length: number): string => {
  let result = ''
  for (let i = 0; i < length; i += 1) {
    const index = Math.floor(Math.random() * CHARS.length)
    result += CHARS[index]
  }
  return result
}

const generatePatientCode = (existingCodes: Set<string>): string => {
  let code = ''
  do {
    code = `PT-${randomFromChars(8)}`
  } while (existingCodes.has(code))
  return code
}

const generatePassword = (): string => `${randomFromChars(4)}@${randomFromChars(8)}`

const createMeasurements = (startValue: number) => {
  return Array.from({ length: 50 }, (_, index) => ({
    characteristicId: index + 1,
    characteristicCode: `CHAR_${String(index + 1).padStart(2, '0')}`,
    characteristicName: `Показатель ${index + 1}`,
    value: String(startValue + index),
    unit: 'ед.',
    comment: null,
  }))
}

const nowIso = new Date().toISOString()

const patientSeed: MockPatientRecord[] = [
  {
    id: 1,
    patientCode: 'PT-7GZVL7PT',
    lastName: 'Иванов',
    firstName: 'Артем',
    middleName: 'Сергеевич',
    birthDate: '2010-03-15',
    diagnosis: 'Тетрада Фалло',
    regionId: toRegionIdByName('Кемерово'),
    regionName: 'Кемерово',
    valve: { name: 'Medtronic Melody', size: '22 мм', material: 'Биологический' },
    operationParameters: { anesthesia: 'Общий', durationMinutes: 200, deliverySystem: 'Хирургический доступ' },
    operationHistory: [
      { name: 'Протезирование клапана легочной артерии', anesthesia: 'Общий', duration: '3 часа 20 минут', deliverySystem: 'Хирургический доступ' },
      { name: 'Баллонная дилатация', anesthesia: 'Седация', duration: '1 час 15 минут', deliverySystem: 'Катетерная' },
    ],
    medications: 'Бисопролол 2.5 мг, Аспирин 75 мг, Спиронолактон 25 мг',
    createdAt: nowIso,
    temporaryPassword: generatePassword(),
  },
  {
    id: 2,
    patientCode: 'PT-K9M2Q4XR',
    lastName: 'Петрова',
    firstName: 'Анна',
    middleName: 'Дмитриевна',
    birthDate: '2018-08-22',
    diagnosis: 'Атрезия легочной артерии',
    regionId: toRegionIdByName('Кемерово'),
    regionName: 'Кемерово',
    valve: { name: 'Carpentier-Edwards', size: '19 мм', material: 'Биологический' },
    operationParameters: { anesthesia: 'Общий', durationMinutes: 160, deliverySystem: 'Хирургический доступ' },
    operationHistory: [
      { name: 'Пластика выходного тракта правого желудочка', anesthesia: 'Общий', duration: '2 часа 40 минут', deliverySystem: 'Хирургический доступ' },
      { name: 'Реваскуляризация легочной артерии', anesthesia: 'Общий', duration: '3 часа 10 минут', deliverySystem: 'Хирургический доступ' },
      { name: 'Контрольная катетеризация', anesthesia: 'Седация', duration: '1 час', deliverySystem: 'Катетерная' },
    ],
    medications: 'Фуросемид 20 мг, Эналаприл 2.5 мг',
    createdAt: nowIso,
    temporaryPassword: generatePassword(),
  },
  {
    id: 3,
    patientCode: 'PT-3HWT8LNC',
    lastName: 'Смирнов',
    firstName: 'Максим',
    middleName: 'Олегович',
    birthDate: '2014-11-03',
    diagnosis: 'Общий артериальный ствол',
    regionId: toRegionIdByName('Казань'),
    regionName: 'Казань',
    valve: { name: 'Contegra', size: '20 мм', material: 'Биологический' },
    operationParameters: { anesthesia: 'Общий', durationMinutes: 245, deliverySystem: 'Хирургический доступ' },
    operationHistory: [
      { name: 'Радикальная коррекция порока', anesthesia: 'Общий', duration: '4 часа 5 минут', deliverySystem: 'Хирургический доступ' },
      { name: 'Стентирование легочной артерии', anesthesia: 'Седация', duration: '55 минут', deliverySystem: 'Катетерная' },
    ],
    medications: 'Бисопролол 1.25 мг, Клопидогрел 75 мг',
    createdAt: nowIso,
    temporaryPassword: generatePassword(),
  },
  {
    id: 4,
    patientCode: 'PT-R5D1YVQK',
    lastName: 'Кузнецова',
    firstName: 'Мария',
    middleName: 'Ильинична',
    birthDate: '2007-06-14',
    diagnosis: 'Тетрада Фалло',
    regionId: toRegionIdByName('Екатеринбург'),
    regionName: 'Екатеринбург',
    valve: { name: 'St. Jude Medical', size: '23 мм', material: 'Механический' },
    operationParameters: { anesthesia: 'Общий', durationMinutes: 180, deliverySystem: 'Хирургический доступ' },
    operationHistory: [
      { name: 'Первичная хирургическая коррекция', anesthesia: 'Общий', duration: '3 часа', deliverySystem: 'Хирургический доступ' },
    ],
    medications: 'Варфарин 2.5 мг',
    createdAt: nowIso,
    temporaryPassword: generatePassword(),
  },
]

const examinationSeed: MockExaminationRecord[] = [
  {
    examId: 1,
    patientId: 1,
    title: 'Контрольное обследование',
    examDate: toIsoDateFromRu('10.03.2026'),
    comment: 'Стабильное состояние, рекомендовано плановое наблюдение.',
    doctorName: 'Петрова А.В.',
    measurements: createMeasurements(10),
  },
  {
    examId: 2,
    patientId: 1,
    title: 'Повторное обследование',
    examDate: toIsoDateFromRu('15.02.2026'),
    comment: 'Положительная динамика, продолжить текущую терапию.',
    doctorName: 'Сидоров В.И.',
    measurements: createMeasurements(20),
  },
  {
    examId: 6,
    patientId: 1,
    title: 'Плановое обследование',
    examDate: toIsoDateFromRu('12.01.2026'),
    comment: 'Состояние стабильное, рекомендовано продолжить наблюдение.',
    doctorName: 'Петрова А.В.',
    measurements: createMeasurements(18),
  },
  {
    examId: 7,
    patientId: 1,
    title: 'Контроль терапии',
    examDate: toIsoDateFromRu('18.11.2025'),
    comment: 'Есть умеренная положительная динамика по ряду показателей.',
    doctorName: 'Сидоров В.И.',
    measurements: createMeasurements(22),
  },
  {
    examId: 8,
    patientId: 1,
    title: 'Промежуточное обследование',
    examDate: toIsoDateFromRu('25.09.2025'),
    comment: 'Нужен контроль через 2-3 месяца.',
    doctorName: 'Петрова А.В.',
    measurements: createMeasurements(24),
  },
  {
    examId: 9,
    patientId: 1,
    title: 'Расширенное обследование',
    examDate: toIsoDateFromRu('14.07.2025'),
    comment: 'Без критических отклонений, рекомендована текущая схема терапии.',
    doctorName: 'Сидоров В.И.',
    measurements: createMeasurements(26),
  },
  {
    examId: 10,
    patientId: 1,
    title: 'Базовое контрольное обследование',
    examDate: toIsoDateFromRu('20.05.2025'),
    comment: 'Исходная точка для последующего сравнения динамики.',
    doctorName: 'Петрова А.В.',
    measurements: createMeasurements(28),
  },
  {
    examId: 11,
    patientId: 1,
    title: 'Внеплановый контроль',
    examDate: toIsoDateFromRu('11.04.2025'),
    comment: 'Контроль после коррекции терапии, выраженных отклонений нет.',
    doctorName: 'Сидоров В.И.',
    measurements: createMeasurements(30),
  },
  {
    examId: 12,
    patientId: 1,
    title: 'Архивное обследование',
    examDate: toIsoDateFromRu('03.02.2025'),
    comment: 'Ретроспективная точка для сравнения долгосрочной динамики.',
    doctorName: 'Петрова А.В.',
    measurements: createMeasurements(32),
  },
  {
    examId: 3,
    patientId: 2,
    title: 'Контроль после терапии',
    examDate: toIsoDateFromRu('10.02.2026'),
    comment: 'Требуется контроль показателей через 3 месяца.',
    doctorName: 'Петрова А.В.',
    measurements: createMeasurements(30),
  },
  {
    examId: 4,
    patientId: 3,
    title: 'Плановое обследование',
    examDate: toIsoDateFromRu('05.01.2026'),
    comment: 'Без признаков ухудшения, наблюдение в стандартном режиме.',
    doctorName: 'Козлова М.Н.',
    measurements: createMeasurements(40),
  },
  {
    examId: 5,
    patientId: 2,
    title: 'Плановое обследование',
    examDate: toIsoDateFromRu('05.01.2026'),
    comment: 'Рекомендована коррекция медикаментозной терапии.',
    doctorName: 'Петрова А.В.',
    measurements: createMeasurements(50),
  },
]

const db = {
  patients: [...patientSeed],
  examinations: [...examinationSeed],
}

const doctorSeed: MockDoctorRecord[] = [
  {
    id: 1,
    lastName: 'Борискова',
    firstName: 'Дарья',
    middleName: 'Викторовна',
    fullName: 'Борискова Дарья Викторовна',
    email: 'doctor@clinic.ru',
    specialty: 'Кардиолог',
    workplace: 'НМИЦ им. Е.Н. Мешалкина',
    role: 'DOCTOR',
    regionId: toRegionIdByStoreKey(DEFAULT_REGION_ID),
    regionName: toRegionNameById(toRegionIdByStoreKey(DEFAULT_REGION_ID)),
  },
  {
    id: 2,
    lastName: 'Шамгунова',
    firstName: 'Алина',
    middleName: 'Денисовна',
    fullName: 'Шамгунова Алина Денисовна',
    email: 'admin@clinic.ru',
    specialty: 'Кардиохирург',
    workplace: 'ФГБУ НМИЦ сердечно-сосудистой хирургии им. А.Н. Бакулева',
    role: 'DOCTOR_EXTENDED',
    regionId: toRegionIdByStoreKey(DEFAULT_REGION_ID),
    regionName: toRegionNameById(toRegionIdByStoreKey(DEFAULT_REGION_ID)),
  },
  {
    id: 3,
    lastName: 'Петрова',
    firstName: 'Анна',
    middleName: 'Викторовна',
    fullName: 'Петрова Анна Викторовна',
    email: 'petrova@clinic.ru',
    specialty: 'Кардиолог',
    workplace: 'Клинический кардиологический диспансер им. Л.С. Барбараша',
    role: 'DOCTOR',
    regionId: toRegionIdByName('Кемерово'),
    regionName: 'Кемерово',
  },
  {
    id: 4,
    lastName: 'Сидоров',
    firstName: 'Владимир',
    middleName: 'Ильич',
    fullName: 'Сидоров Владимир Ильич',
    email: 'sidorov@clinic.ru',
    specialty: 'Врач функциональной диагностики',
    workplace: 'Межрегиональный клинико-диагностический центр',
    role: 'DOCTOR',
    regionId: toRegionIdByName('Казань'),
    regionName: 'Казань',
  },
]

interface MockSession {
  userId: number
  role: UserRole
  displayName: string
  email: string | null
  patientCode: string | null
  doctorRegionId: number | null
}

let currentSession: MockSession | null = null

const isDoctorRole = (role: UserRole | null | undefined) =>
  role === 'DOCTOR' || role === 'DOCTOR_EXTENDED'

const isDoctorExtended = (role: UserRole | null | undefined) => role === 'DOCTOR_EXTENDED'

const resolveDoctorRegionId = (): number => {
  if (currentSession?.doctorRegionId) return currentSession.doctorRegionId
  if (isDoctorRole(currentSession?.role) && currentSession?.email) {
    const doctor = doctorSeed.find(item => item.email.toLowerCase() === currentSession?.email?.toLowerCase())
    if (doctor) return doctor.regionId
  }
  return toRegionIdByStoreKey(DEFAULT_REGION_ID)
}

const canEditPatient = (patient: MockPatientRecord): boolean => {
  const role = currentSession?.role
  if (!isDoctorRole(role)) return false
  if (isDoctorExtended(role)) return true
  return patient.regionId === resolveDoctorRegionId()
}

const canViewFullPatient = (patient: MockPatientRecord): boolean => {
  const role = currentSession?.role
  if (isDoctorExtended(role)) return true
  if (role === 'DOCTOR') return patient.regionId === resolveDoctorRegionId()
  if (role === 'PATIENT') return currentSession?.patientCode === patient.patientCode
  return false
}

const getStatusByExamDate = (examDate: string | null): PatientStatus => {
  if (!examDate) return 'RED'
  const parsed = new Date(examDate)
  if (Number.isNaN(parsed.getTime())) return 'RED'

  const diffMs = Date.now() - parsed.getTime()
  const diffMonths = diffMs / (1000 * 60 * 60 * 24 * 30.44)

  if (diffMonths < 3) return 'GREEN'
  if (diffMonths <= 6) return 'YELLOW'
  return 'RED'
}

const getLatestExamDate = (patientId: number): string | null => {
  const dates = db.examinations
    .filter(exam => exam.patientId === patientId)
    .map(exam => exam.examDate)
    .sort((left, right) => right.localeCompare(left))

  return dates[0] ?? null
}

const toSummary = (patient: MockPatientRecord, anonymized: boolean) => {
  const status = getStatusByExamDate(getLatestExamDate(patient.id))
  return {
    id: patient.id,
    patientCode: patient.patientCode,
    birthDate: patient.birthDate,
    diagnosis: patient.diagnosis,
    regionId: patient.regionId,
    status,
    valve: patient.valve,
    operationParameters: patient.operationParameters,
    medications: patient.medications,
    createdAt: patient.createdAt,
    lastExaminationAt: getLatestExamDate(patient.id),
  }
}

const toCard = (patient: MockPatientRecord, anonymized: boolean): PatientCardResponse => {
  const vitalsHistory = db.examinations
    .filter(exam => exam.patientId === patient.id)
    .sort((left, right) => right.examDate.localeCompare(left.examDate))
    .map(exam => ({
      examId: exam.examId,
      title: exam.title,
      examDate: exam.examDate,
      comment: exam.comment,
      measurements: exam.measurements,
    }))

  return {
    id: patient.id,
    viewMode: anonymized ? 'ANONYMIZED' : 'FULL',
    patientCode: patient.patientCode,
    regionId: patient.regionId,
    regionName: patient.regionName,
    birthDate: patient.birthDate,
    diagnosis: patient.diagnosis,
    valve: patient.valve,
    operationParameters: patient.operationParameters,
    medications: patient.medications,
    createdAt: patient.createdAt,
    vitalsHistory,
  }
}

export const mockPatientApi = {
  login(payload: LoginRequest): AuthResponse {
    const isDoctor = payload.login.includes('@')
    const normalizedLogin = payload.login.trim().toLowerCase()
    const doctorByEmail = isDoctor
      ? doctorSeed.find(doctor => doctor.email.toLowerCase() === normalizedLogin)
      : null
    const role: UserRole = isDoctor
      ? (doctorByEmail?.role ?? (normalizedLogin.includes('admin') || normalizedLogin.includes('extended') ? 'DOCTOR_EXTENDED' : 'DOCTOR'))
      : 'PATIENT'

    const doctorRegionId = isDoctor
      ? (doctorByEmail?.regionId ?? resolveDoctorRegionId())
      : null
    const patientByCode = !isDoctor ? db.patients.find(patient => patient.patientCode.toLowerCase() === normalizedLogin) : null

    if (isDoctor && !doctorByEmail) {
      throw new Error('INVALID_CREDENTIALS')
    }

    if (!isDoctor && !patientByCode) {
      throw new Error('INVALID_CREDENTIALS')
    }

    const accountPassword = isDoctor
      ? (doctorByEmail?.password ?? MOCK_DEFAULT_PASSWORD)
      : (patientByCode?.password ?? MOCK_DEFAULT_PASSWORD)
    if (payload.password !== accountPassword) {
      throw new Error('INVALID_CREDENTIALS')
    }

    const patientCode = patientByCode?.patientCode ?? (!isDoctor ? payload.login : null)

    currentSession = {
      userId: isDoctor ? (doctorByEmail?.id ?? 1) : (patientByCode?.id ?? 100),
      role,
      displayName: isDoctor
        ? (doctorByEmail?.fullName ?? (role === 'DOCTOR_EXTENDED' ? 'Администратор врач' : 'Борискова Д.В.'))
        : (patientByCode ? `${patientByCode.lastName} ${patientByCode.firstName}` : 'Пациент'),
      email: isDoctor ? payload.login : null,
      patientCode,
      doctorRegionId,
    }

    return {
      accessToken: `mock-jwt-${Date.now()}`,
      tokenType: 'Bearer',
      expiresAt: new Date(Date.now() + 60 * 60 * 1000).toISOString(),
      user: {
        id: currentSession.userId,
        role,
        displayName: currentSession.displayName,
        email: currentSession.email,
        patientCode: currentSession.patientCode,
        doctorRegionId: doctorRegionId ?? null,
        doctorRegionName: doctorRegionId ? toRegionNameById(doctorRegionId) : null,
      },
    }
  },

  listDoctorPatients(params: ListDoctorPatientsParams = {}): PatientListResponse {
    if (!isDoctorRole(currentSession?.role)) {
      return { items: [], page: 0, limit: 20, total: 0 }
    }

    const scope = params.scope ?? 'own'
    const doctorRegionId = resolveDoctorRegionId()
    const page = Math.max(0, params.page ?? 0)
    const limit = Math.min(100, Math.max(1, params.limit ?? 20))

    const diagnosisFilter = (params.diagnosis ?? '').trim().toLowerCase()

    let filtered = [...db.patients]
    if (scope === 'own') {
      filtered = filtered.filter(patient => patient.regionId === doctorRegionId)
    }

    if (typeof params.regionId === 'number') {
      filtered = filtered.filter(patient => patient.regionId === params.regionId)
    }

    if (diagnosisFilter) {
      filtered = filtered.filter(patient => patient.diagnosis.toLowerCase().includes(diagnosisFilter))
    }

    const priority: Record<PatientStatus, number> = { RED: 0, YELLOW: 1, GREEN: 2 }
    const itemsWithStatus = filtered.map(patient => ({
      patient,
      status: getStatusByExamDate(getLatestExamDate(patient.id)),
    }))

    itemsWithStatus.sort((left, right) => {
      if (priority[left.status] !== priority[right.status]) {
        return priority[left.status] - priority[right.status]
      }
      return right.patient.id - left.patient.id
    })

    const start = page * limit
    const slice = itemsWithStatus.slice(start, start + limit)

    return {
      items: slice.map(item => toSummary(item.patient, scope === 'all')),
      page,
      limit,
      total: filtered.length,
    }
  },

  getPatientCard(patientId: number): PatientCardResponse | null {
    const patient = db.patients.find(item => item.id === patientId)
    if (!patient) return null
    if (currentSession?.role === 'PATIENT' && currentSession.patientCode !== patient.patientCode) {
      return null
    }
    const anonymized = !canViewFullPatient(patient)
    return toCard(patient, anonymized)
  },

  createPatientCard(payload: CreatePatientRequest): CreatedPatientResponse {
    if (!isDoctorRole(currentSession?.role)) {
      throw new Error('FORBIDDEN')
    }

    const id = Math.max(0, ...db.patients.map(patient => patient.id)) + 1
    const patientCode = generatePatientCode(new Set(db.patients.map(patient => patient.patientCode)))
    const now = new Date().toISOString()

    const record: MockPatientRecord = {
      id,
      patientCode,
      lastName: 'Пациент',
      firstName: patientCode,
      middleName: null,
      birthDate: payload.birthDate,
      diagnosis: payload.diagnosis.trim(),
      regionId: payload.regionId,
      regionName: toRegionNameById(payload.regionId),
      valve: { ...payload.valve },
      operationParameters: { ...payload.operationParameters },
      operationHistory: [
        {
          name: 'Операция при добавлении карточки',
          anesthesia: payload.operationParameters.anesthesia,
          duration: `${payload.operationParameters.durationMinutes} мин`,
          deliverySystem: payload.operationParameters.deliverySystem,
        },
      ],
      medications: payload.medications.trim(),
      createdAt: now,
      temporaryPassword: generatePassword(),
    }

    db.patients.unshift(record)

    return {
      id: record.id,
      patientCode: record.patientCode,
      temporaryPassword: record.temporaryPassword,
      birthDate: record.birthDate,
      diagnosis: record.diagnosis,
      regionId: record.regionId,
      valve: { ...record.valve },
      operationParameters: { ...record.operationParameters },
      medications: record.medications,
      createdAt: record.createdAt,
    }
  },

  updatePatientCard(patientId: number, payload: UpdatePatientRequest): PatientCardResponse | null {
    const patient = db.patients.find(item => item.id === patientId)
    if (!patient) return null
    if (!canEditPatient(patient)) return null

    if (payload.birthDate !== undefined) patient.birthDate = payload.birthDate
    if (payload.diagnosis !== undefined) patient.diagnosis = payload.diagnosis.trim()
    if (payload.regionId !== undefined) {
      patient.regionId = payload.regionId
      patient.regionName = toRegionNameById(payload.regionId)
    }
    if (payload.medications !== undefined) patient.medications = payload.medications.trim()
    if (payload.valve) patient.valve = { ...payload.valve }
    if (payload.operationParameters) {
      patient.operationParameters = { ...payload.operationParameters }
      patient.operationHistory.unshift({
        name: 'Операция (обновление карточки)',
        anesthesia: payload.operationParameters.anesthesia,
        duration: `${payload.operationParameters.durationMinutes} мин`,
        deliverySystem: payload.operationParameters.deliverySystem,
      })
    }

    return toCard(patient, !canViewFullPatient(patient))
  },

  addPatientExamination(patientId: number, payload: CreateExaminationRequest, doctorName: string): ExaminationResponse | null {
    const patient = db.patients.find(item => item.id === patientId)
    if (!patient) return null
    if (!canEditPatient(patient)) return null

    const nextExamId = Math.max(0, ...db.examinations.map(exam => exam.examId)) + 1

    const record: MockExaminationRecord = {
      examId: nextExamId,
      patientId,
      title: payload.title,
      examDate: payload.examDate,
      comment: payload.comment,
      doctorName,
      measurements: payload.measurements.map((measurement, index) => ({
        characteristicId: index + 1,
        characteristicCode: measurement.characteristicCode,
        characteristicName: `Показатель ${index + 1}`,
        value: String(measurement.value),
        unit: 'ед.',
        comment: measurement.comment,
      })),
    }

    db.examinations.unshift(record)

    return {
      examId: record.examId,
      title: record.title,
      examDate: record.examDate,
      comment: record.comment,
      measurements: record.measurements,
    }
  },

  listPatientExaminations(patientId: number): ExaminationListResponse {
    const patient = db.patients.find(item => item.id === patientId)
    if (!patient) return { items: [] }

    if (currentSession?.role === 'PATIENT' && currentSession.patientCode !== patient.patientCode) {
      return { items: [] }
    }

    return {
      items: db.examinations
        .filter(exam => exam.patientId === patientId)
        .sort((left, right) => right.examDate.localeCompare(left.examDate))
        .map(exam => ({
          examId: exam.examId,
          title: exam.title,
          examDate: exam.examDate,
          comment: exam.comment,
          measurements: exam.measurements,
        })),
    }
  },

  // Mock-only helper while backend has no examination PATCH endpoint yet.
  updatePatientExamination(patientId: number, examId: number, payload: CreateExaminationRequest, doctorName: string): ExaminationResponse | null {
    const patient = db.patients.find(item => item.id === patientId)
    if (!patient) return null
    if (!canEditPatient(patient)) return null

    const exam = db.examinations.find(item => item.patientId === patientId && item.examId === examId)
    if (!exam) return null

    exam.title = payload.title
    exam.examDate = payload.examDate
    exam.comment = payload.comment
    exam.doctorName = doctorName
    exam.measurements = payload.measurements.map((measurement, index) => ({
      characteristicId: index + 1,
      characteristicCode: measurement.characteristicCode,
      characteristicName: `Показатель ${index + 1}`,
      value: String(measurement.value),
      unit: 'ед.',
      comment: measurement.comment,
    }))

    return {
      examId: exam.examId,
      title: exam.title,
      examDate: exam.examDate,
      comment: exam.comment,
      measurements: exam.measurements,
    }
  },

  findPatientByCode(code: string): MockPatientRecord | null {
    return db.patients.find(patient => patient.patientCode === code) ?? null
  },

  findPatientById(id: number): MockPatientRecord | null {
    return db.patients.find(patient => patient.id === id) ?? null
  },

  setPatientOperationHistory(patientId: number, history: MockOperationHistoryItem[]): boolean {
    const patient = db.patients.find(item => item.id === patientId)
    if (!patient) return false
    patient.operationHistory = history.map(item => ({ ...item }))
    return true
  },

  restoreSession(session: {
    id: number
    role: UserRole
    displayName: string
    email: string | null
    patientCode: string | null
    doctorRegionId?: number | null
  }) {
    const restoredRegionId = session.doctorRegionId
      ?? (isDoctorRole(session.role) && session.email
        ? (doctorSeed.find(item => item.email.toLowerCase() === session.email?.toLowerCase())?.regionId ?? toRegionIdByStoreKey(DEFAULT_REGION_ID))
        : null)
    currentSession = {
      userId: session.id,
      role: session.role,
      displayName: session.displayName,
      email: session.email,
      patientCode: session.patientCode,
      doctorRegionId: restoredRegionId,
    }
  },

  clearSession() {
    currentSession = null
  },

  listDoctors(): MockDoctorRecord[] {
    if (!isDoctorExtended(currentSession?.role)) return []
    return doctorSeed.map(doctor => ({ ...doctor }))
  },

  getDoctorById(doctorId: number): MockDoctorRecord | null {
    if (!isDoctorExtended(currentSession?.role)) return null
    const doctor = doctorSeed.find(item => item.id === doctorId)
    return doctor ? { ...doctor } : null
  },

  updateDoctorRegion(doctorId: number, regionId: number): MockDoctorRecord | null {
    if (!isDoctorExtended(currentSession?.role)) return null

    const doctor = doctorSeed.find(item => item.id === doctorId)
    if (!doctor) return null

    doctor.regionId = regionId
    doctor.regionName = toRegionNameById(regionId)

    if (currentSession?.userId === doctorId) {
      currentSession.doctorRegionId = regionId
    }

    return { ...doctor }
  },
  createDoctor(payload: CreateDoctorRequest): CreatedDoctorResponse {
    if (!isDoctorExtended(currentSession?.role)) {
      throw new Error('FORBIDDEN')
    }

    const normalizedEmail = payload.email.trim().toLowerCase()
    const existingDoctor = doctorSeed.find(item => item.email.toLowerCase() === normalizedEmail)
    if (existingDoctor) {
      throw new Error('DOCTOR_EMAIL_EXISTS')
    }

    const id = Math.max(0, ...doctorSeed.map(doctor => doctor.id)) + 1
    const temporaryPassword = generatePassword()
    const regionId = payload.regionId
    const lastName = payload.lastName.trim()
    const firstName = payload.firstName.trim()
    const middleName = payload.middleName?.trim() || null
    const fullName = [lastName, firstName, middleName].filter(Boolean).join(' ')

    const record: MockDoctorRecord = {
      id,
      lastName,
      firstName,
      middleName,
      fullName,
      email: normalizedEmail,
      specialty: payload.specialty.trim(),
      workplace: payload.workplace.trim(),
      role: 'DOCTOR',
      regionId,
      regionName: toRegionNameById(regionId),
      password: temporaryPassword,
    }

    doctorSeed.unshift(record)

    return {
      id: record.id,
      fullName: record.fullName,
      lastName: record.lastName,
      firstName: record.firstName,
      middleName: record.middleName,
      email: record.email,
      specialty: record.specialty,
      workplace: record.workplace,
      role: record.role,
      regionId: record.regionId,
      regionName: record.regionName,
      temporaryPassword,
    }
  },
  changeOwnPassword(payload: ChangePasswordRequest): boolean {
    if (!currentSession) throw new Error('UNAUTHORIZED')

    const session = currentSession
    const currentPassword = payload.currentPassword.trim()
    const newPassword = payload.newPassword.trim()
    if (!currentPassword || !newPassword) throw new Error('VALIDATION_ERROR')
    if (newPassword.length < 6) throw new Error('WEAK_PASSWORD')

    if (session.role === 'PATIENT') {
      const patient = db.patients.find(item => item.id === session.userId)
      if (!patient) throw new Error('NOT_FOUND')
      const savedPassword = patient.password ?? MOCK_DEFAULT_PASSWORD
      if (savedPassword !== currentPassword) throw new Error('INVALID_CURRENT_PASSWORD')
      patient.password = newPassword
      return true
    }

    if (isDoctorRole(session.role)) {
      const doctor = doctorSeed.find(item => item.id === session.userId)
      if (!doctor) throw new Error('NOT_FOUND')
      const savedPassword = doctor.password ?? MOCK_DEFAULT_PASSWORD
      if (savedPassword !== currentPassword) throw new Error('INVALID_CURRENT_PASSWORD')
      doctor.password = newPassword
      return true
    }

    throw new Error('FORBIDDEN')
  },

  listAllPatientsRaw(): MockPatientRecord[] {
    return db.patients.map(patient => ({ ...patient, valve: { ...patient.valve }, operationParameters: { ...patient.operationParameters }, operationHistory: patient.operationHistory.map(item => ({ ...item })) }))
  },

  listAllExaminationsRaw(): MockExaminationRecord[] {
    return db.examinations.map(exam => ({ ...exam, measurements: exam.measurements.map(item => ({ ...item })) }))
  },

  getCurrentDoctor(): MockDoctorRecord | null {
    if (!isDoctorRole(currentSession?.role) || !currentSession?.email) return null
    const doctor = doctorSeed.find(item => item.email.toLowerCase() === currentSession?.email?.toLowerCase())
    return doctor ? { ...doctor } : null
  },

  toRuDateFromIso,
}











