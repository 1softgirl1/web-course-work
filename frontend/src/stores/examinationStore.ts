import { reactive } from 'vue'

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

const createIndicators = (startValue: number): number[] => {
  return Array.from({ length: 50 }, (_, index) => startValue + index)
}

const initialExaminations: Examination[] = [
  {
    id: 1,
    patientCode: 'PT-7GZVL7PT',
    date: "10.03.2026",
    doctor: "Петрова А.В.",
    conclusion: "Стабильное состояние, рекомендовано плановое наблюдение.",
    indicators: createIndicators(10),
    status: "Новое",
  },
  {
    id: 2,
    patientCode: 'PT-7GZVL7PT',
    date: "15.02.2026",
    doctor: "Сидоров В.И.",
    conclusion: "Положительная динамика, продолжить текущую терапию.",
    indicators: createIndicators(20),
    status: "Просмотрено",
  },
  {
    id: 3,
    patientCode: 'PT-K9M2Q4XR',
    date: "10.02.2026",
    doctor: "Петрова А.В.",
    conclusion: "Требуется контроль показателей через 3 месяца.",
    indicators: createIndicators(30),
    status: "Просмотрено",
  },
  {
    id: 4,
    patientCode: 'PT-3HWT8LNC',
    date: "05.01.2026",
    doctor: "Козлова М.Н.",
    conclusion: "Без признаков ухудшения, наблюдение в стандартном режиме.",
    indicators: createIndicators(40),
    status: "Просмотрено",
  },
  {
    id: 5,
    patientCode: 'PT-2QNF9ZTA',
    date: "05.01.2026",
    doctor: "Петрова А.В.",
    conclusion: "Рекомендована коррекция медикаментозной терапии.",
    indicators: createIndicators(50),
    status: "Просмотрено",
  },
]

const state = reactive({
  examinations: initialExaminations,
})

export function parseExamDate(value: string): Date | null {
  const [day, month, year] = value.split('.').map(Number)
  if (!day || !month || !year) return null
  const date = new Date(year, month - 1, day)
  return Number.isNaN(date.getTime()) ? null : date
}

export function toRuExamDate(value: string): string {
  const sourceDate = new Date(value)
  if (Number.isNaN(sourceDate.getTime())) return value
  return sourceDate.toLocaleDateString('ru-RU')
}

export const useExaminationStore = () => {
  const addExamination = (exam: NewExamination) => {
    const newId = Math.max(...state.examinations.map(e => e.id), 0) + 1
    state.examinations.unshift({
      id: newId,
      ...exam,
      status: 'Новое',
    })
  }

  const updateExamination = (payload: UpdateExamination) => {
    const target = state.examinations.find(exam => exam.id === payload.id)
    if (!target) return false

    target.date = payload.date
    target.doctor = payload.doctor
    target.conclusion = payload.conclusion
    target.indicators = [...payload.indicators]
    target.status = 'Новое'
    return true
  }

  return {
    examinations: state.examinations,
    addExamination,
    updateExamination,
  }
}

