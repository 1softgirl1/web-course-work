import { reactive } from 'vue'

export interface Patient {
  code: string
  fullName: string
  age: number
  diagnosis: string
  operations: number
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
  operationsCount?: number
  operations?: Array<{
    name: string
    anesthesia: string
    duration: string
    deliverySystem: string
  }>
  valve?: {
    name: string
    size: string
    material: string
  }
  medications?: string[]
}

const initialPatients: Patient[] = [
  {
    code: 'PT-7GZVL7PT',
    fullName: 'Иванов Артем Сергеевич',
    age: 16,
    diagnosis: 'Тетрада Фалло',
    operations: 2,
    lastExam: '10.03.2026',
    region: 'Санкт-Петербург',
  },
  {
    code: 'PT-K9M2Q4XR',
    fullName: 'Петрова Анна Дмитриевна',
    age: 8,
    diagnosis: 'Атрезия легочной артерии',
    operations: 3,
    lastExam: '08.03.2026',
    region: 'Москва',
  },
  {
    code: 'PT-3HWT8LNC',
    fullName: 'Смирнов Максим Олегович',
    age: 12,
    diagnosis: 'Общий артериальный ствол',
    operations: 2,
    lastExam: '05.03.2026',
    region: 'Казань',
  },
  {
    code: 'PT-R5D1YVQK',
    fullName: 'Кузнецова Мария Ильинична',
    age: 19,
    diagnosis: 'Тетрада Фалло',
    operations: 1,
    lastExam: '01.01.2026',
    region: 'Екатеринбург',
  },
  {
    code: 'PT-B8XU6MJP',
    fullName: 'Васильев Никита Андреевич',
    age: 7,
    diagnosis: 'Двойное отхождение сосудов от ПЖ',
    operations: 2,
    lastExam: '28.02.2026',
    region: 'Новосибирск',
  },
  {
    code: 'PT-2QNF9ZTA',
    fullName: 'Соколова Елизавета Романовна',
    age: 10,
    diagnosis: 'Атрезия легочной артерии с ДМЖП',
    operations: 4,
    lastExam: '05.03.2025',
    region: 'Санкт-Петербург',
  },
  {
    code: 'PT-L4CV7RHM',
    fullName: 'Попов Кирилл Алексеевич',
    age: 17,
    diagnosis: 'Общий артериальный ствол',
    operations: 2,
    lastExam: '28.02.2026',
    region: 'Санкт-Петербург',
  },
  {
    code: 'PT-X1PK6NWD',
    fullName: 'Морозова София Павловна',
    age: 13,
    diagnosis: 'Двойное отхождение сосудов от ПЖ',
    operations: 1,
    lastExam: '20.02.2026',
    region: 'Санкт-Петербург',
  },
]

const state = reactive({
  patients: initialPatients,
})

const CHARS = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'

const randomFromChars = (length: number): string => {
  let result = ''
  for (let i = 0; i < length; i += 1) {
    const index = Math.floor(Math.random() * CHARS.length)
    result += CHARS[index]
  }
  return result
}

const generatePatientCode = (): string => {
  let code = ''
  do {
    code = `PT-${randomFromChars(8)}`
  } while (state.patients.some(patient => patient.code === code))
  return code
}

const generatePassword = (): string => {
  return `${randomFromChars(4)}@${randomFromChars(8)}`
}

const formatDate = (value: Date): string => {
  const day = String(value.getDate()).padStart(2, '0')
  const month = String(value.getMonth() + 1).padStart(2, '0')
  const year = value.getFullYear()
  return `${day}.${month}.${year}`
}

const calculateAge = (birthDate: string): number => {
  const birth = new Date(birthDate)
  if (Number.isNaN(birth.getTime())) return 0

  const today = new Date()
  let age = today.getFullYear() - birth.getFullYear()
  const hasBirthdayPassed =
    today.getMonth() > birth.getMonth() ||
    (today.getMonth() === birth.getMonth() && today.getDate() >= birth.getDate())

  if (!hasBirthdayPassed) {
    age -= 1
  }

  return Math.max(age, 0)
}

export const usePatientStore = () => {
  const addPatient = (input: NewPatientInput) => {
    const fullName = [input.lastName, input.firstName, input.middleName?.trim()]
      .filter(Boolean)
      .join(' ')

    const createdPatient: Patient = {
      code: generatePatientCode(),
      fullName,
      age: calculateAge(input.birthDate),
      diagnosis: input.diagnosis.trim(),
      operations: input.operationsCount ?? 0,
      lastExam: formatDate(new Date()),
      region: input.region,
    }

    state.patients.unshift(createdPatient)

    return {
      patient: createdPatient,
      password: generatePassword(),
    }
  }

  return {
    patients: state.patients,
    addPatient,
  }
}

