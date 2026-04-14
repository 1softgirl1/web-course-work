import { reactive } from 'vue'

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

const initialPatients: Patient[] = [
  {
    code: 'PT-7GZVL7PT',
    fullName: 'Иванов Артем Сергеевич',
    age: 16,
    diagnosis: 'Тетрада Фалло',
    operations: 2,
    operationDetails: [
      {
        name: 'Протезирование клапана легочной артерии',
        anesthesia: 'Общий',
        duration: '3 часа 20 минут',
        deliverySystem: 'Хирургический доступ',
      },
      {
        name: 'Баллонная дилатация',
        anesthesia: 'Седация',
        duration: '1 час 15 минут',
        deliverySystem: 'Катетерная',
      },
    ],
    medications: 'Бисопролол 2.5 мг, Аспирин 75 мг, Спиронолактон 25 мг',
    valve: { name: 'Medtronic Melody', size: '22 мм', material: 'Биологический' },
    lastExam: '10.03.2026',
    region: 'Кемерово',
    lastName: 'Иванов',
    firstName: 'Артем',
    middleName: 'Сергеевич',
    birthDate: '2010-03-15',
  },
  {
    code: 'PT-K9M2Q4XR',
    fullName: 'Петрова Анна Дмитриевна',
    age: 8,
    diagnosis: 'Атрезия легочной артерии',
    operations: 3,
    operationDetails: [
      {
        name: 'Пластика выходного тракта правого желудочка',
        anesthesia: 'Общий',
        duration: '2 часа 40 минут',
        deliverySystem: 'Хирургический доступ',
      },
      {
        name: 'Реваскуляризация легочной артерии',
        anesthesia: 'Общий',
        duration: '3 часа 10 минут',
        deliverySystem: 'Хирургический доступ',
      },
      {
        name: 'Контрольная катетеризация',
        anesthesia: 'Седация',
        duration: '1 час',
        deliverySystem: 'Катетерная',
      },
    ],
    medications: 'Фуросемид 20 мг, Эналаприл 2.5 мг',
    valve: { name: 'Carpentier-Edwards', size: '19 мм', material: 'Биологический' },
    lastExam: '08.03.2026',
    region: 'Кемерово',
    lastName: 'Петрова',
    firstName: 'Анна',
    middleName: 'Дмитриевна',
    birthDate: '2018-08-22',
  },
  {
    code: 'PT-3HWT8LNC',
    fullName: 'Смирнов Максим Олегович',
    age: 12,
    diagnosis: 'Общий артериальный ствол',
    operations: 2,
    operationDetails: [
      {
        name: 'Радикальная коррекция порока',
        anesthesia: 'Общий',
        duration: '4 часа 5 минут',
        deliverySystem: 'Хирургический доступ',
      },
      {
        name: 'Стентирование легочной артерии',
        anesthesia: 'Седация',
        duration: '55 минут',
        deliverySystem: 'Катетерная',
      },
    ],
    medications: 'Бисопролол 1.25 мг, Клопидогрел 75 мг',
    valve: { name: 'Contegra', size: '20 мм', material: 'Биологический' },
    lastExam: '05.03.2026',
    region: 'Казань',
    lastName: 'Смирнов',
    firstName: 'Максим',
    middleName: 'Олегович',
    birthDate: '2014-11-03',
  },
  {
    code: 'PT-R5D1YVQK',
    fullName: 'Кузнецова Мария Ильинична',
    age: 19,
    diagnosis: 'Тетрада Фалло',
    operations: 1,
    operationDetails: [
      {
        name: 'Первичная хирургическая коррекция',
        anesthesia: 'Общий',
        duration: '3 часа',
        deliverySystem: 'Хирургический доступ',
      },
    ],
    medications: 'Варфарин 2.5 мг',
    valve: { name: 'St. Jude Medical', size: '23 мм', material: 'Механический' },
    lastExam: '01.01.2026',
    region: 'Екатеринбург',
    lastName: 'Кузнецова',
    firstName: 'Мария',
    middleName: 'Ильинична',
    birthDate: '2007-06-14',
  },
  {
    code: 'PT-B8XU6MJP',
    fullName: 'Васильев Никита Андреевич',
    age: 7,
    diagnosis: 'Двойное отхождение сосудов от ПЖ',
    operations: 2,
    operationDetails: [
      {
        name: 'Паллиативное вмешательство',
        anesthesia: 'Общий',
        duration: '1 час 50 минут',
        deliverySystem: 'Хирургический доступ',
      },
      {
        name: 'Имплантация кондуита',
        anesthesia: 'Общий',
        duration: '2 часа 30 минут',
        deliverySystem: 'Хирургический доступ',
      },
    ],
    medications: 'Спиронолактон 12.5 мг, Каптоприл 6.25 мг',
    valve: { name: 'Hancock II', size: '18 мм', material: 'Биологический' },
    lastExam: '28.02.2026',
    region: 'Новосибирск',
    lastName: 'Васильев',
    firstName: 'Никита',
    middleName: 'Андреевич',
    birthDate: '2019-01-28',
  },
  {
    code: 'PT-2QNF9ZTA',
    fullName: 'Соколова Елизавета Романовна',
    age: 10,
    diagnosis: 'Атрезия легочной артерии с ДМЖП',
    operations: 4,
    operationDetails: [
      {
        name: 'Системно-легочный шунт',
        anesthesia: 'Общий',
        duration: '2 часа 15 минут',
        deliverySystem: 'Хирургический доступ',
      },
      {
        name: 'Закрытие дефекта межжелудочковой перегородки',
        anesthesia: 'Общий',
        duration: '3 часа 40 минут',
        deliverySystem: 'Хирургический доступ',
      },
      {
        name: 'Реконструкция легочной артерии',
        anesthesia: 'Общий',
        duration: '2 часа 55 минут',
        deliverySystem: 'Хирургический доступ',
      },
      {
        name: 'Контрольная ангиография',
        anesthesia: 'Седация',
        duration: '45 минут',
        deliverySystem: 'Катетерная',
      },
    ],
    medications: 'Торасемид 5 мг, Дигоксин 0.125 мг',
    valve: { name: 'Perimount Magna', size: '21 мм', material: 'Биологический' },
    lastExam: '05.03.2025',
    region: 'Кемерово',
    lastName: 'Соколова',
    firstName: 'Елизавета',
    middleName: 'Романовна',
    birthDate: '2016-05-09',
  },
  {
    code: 'PT-L4CV7RHM',
    fullName: 'Попов Кирилл Алексеевич',
    age: 17,
    diagnosis: 'Общий артериальный ствол',
    operations: 2,
    operationDetails: [
      {
        name: 'Коррекция общего артериального ствола',
        anesthesia: 'Общий',
        duration: '4 часа 20 минут',
        deliverySystem: 'Хирургический доступ',
      },
      {
        name: 'Замена кондуита',
        anesthesia: 'Общий',
        duration: '2 часа 25 минут',
        deliverySystem: 'Хирургический доступ',
      },
    ],
    medications: 'Эналаприл 5 мг, Аспирин 100 мг',
    valve: { name: 'Sorin Mitroflow', size: '24 мм', material: 'Биологический' },
    lastExam: '28.02.2026',
    region: 'Санкт-Петербург',
    lastName: 'Попов',
    firstName: 'Кирилл',
    middleName: 'Алексеевич',
    birthDate: '2009-09-01',
  },
  {
    code: 'PT-X1PK6NWD',
    fullName: 'Морозова София Павловна',
    age: 13,
    diagnosis: 'Двойное отхождение сосудов от ПЖ',
    operations: 1,
    operationDetails: [
      {
        name: 'Пластика межжелудочковой перегородки',
        anesthesia: 'Общий',
        duration: '2 часа 35 минут',
        deliverySystem: 'Хирургический доступ',
      },
    ],
    medications: 'Бисопролол 2.5 мг',
    valve: { name: 'On-X', size: '21 мм', material: 'Механический' },
    lastExam: '20.02.2026',
    region: 'Санкт-Петербург',
    lastName: 'Морозова',
    firstName: 'София',
    middleName: 'Павловна',
    birthDate: '2013-12-17',
  },
]

const state = reactive({
  patients: initialPatients,
  regionChangeLogs: [] as RegionChangeLogEntry[],
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
      lastName: input.lastName.trim(),
      firstName: input.firstName.trim(),
      middleName: input.middleName?.trim() ?? '',
      birthDate: input.birthDate,
      age: calculateAge(input.birthDate),
      diagnosis: input.diagnosis.trim(),
      operations: input.operations?.length ?? 0,
      operationDetails: input.operations ?? [],
      medications: input.medications?.trim() ?? '',
      valve: input.valve ?? { name: '', size: '', material: '' },
      lastExam: formatDate(new Date()),
      region: input.region,
    }

    state.patients.unshift(createdPatient)

    return {
      patient: createdPatient,
      password: generatePassword(),
    }
  }

  const transferPatientRegion = (patientCode: string, nextRegion: string, changedBy: string) => {
    const patient = state.patients.find(item => item.code === patientCode)
    if (!patient) return null

    const trimmedRegion = nextRegion.trim()
    const trimmedChangedBy = changedBy.trim()

    if (!trimmedRegion || patient.region === trimmedRegion) {
      return patient
    }

    const previousRegion = patient.region
    patient.region = trimmedRegion

    state.regionChangeLogs.unshift({
      id: Date.now() + Math.floor(Math.random() * 1000),
      patientCode: patient.code,
      changedAt: new Date().toLocaleString('ru-RU'),
      changedBy: trimmedChangedBy || 'Неизвестный врач',
      fromRegion: previousRegion,
      toRegion: trimmedRegion,
    })

    return patient
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

