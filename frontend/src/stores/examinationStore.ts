import { reactive } from 'vue'

export interface Examination {
  id: number
  date: string
  doctor: string
  conclusion: string
  indicators: number[]
  status: string
}

export interface NewExamination {
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
    date: "10.03.2026",
    doctor: "Петрова А.В.",
    conclusion: "Стабильное состояние, рекомендовано плановое наблюдение.",
    indicators: createIndicators(10),
    status: "Новое",
  },
  {
    id: 2,
    date: "15.02.2026",
    doctor: "Сидоров В.И.",
    conclusion: "Положительная динамика, продолжить текущую терапию.",
    indicators: createIndicators(20),
    status: "Просмотрено",
  },
  {
    id: 3,
    date: "10.02.2026",
    doctor: "Петрова А.В.",
    conclusion: "Требуется контроль показателей через 3 месяца.",
    indicators: createIndicators(30),
    status: "Просмотрено",
  },
  {
    id: 4,
    date: "05.01.2026",
    doctor: "Козлова М.Н.",
    conclusion: "Без признаков ухудшения, наблюдение в стандартном режиме.",
    indicators: createIndicators(40),
    status: "Просмотрено",
  },
  {
    id: 5,
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

export const useExaminationStore = () => {
  const addExamination = (exam: NewExamination) => {
    const newId = Math.max(...state.examinations.map(e => e.id), 0) + 1
    state.examinations.unshift({
      id: newId,
      ...exam,
      status: 'Новое',
    })
  }

  return {
    examinations: state.examinations,
    addExamination,
  }
}

