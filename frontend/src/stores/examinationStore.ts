import { reactive } from 'vue'

export interface Examination {
  id: number
  type: string
  date: string
  doctor: string
  clinic: string
  status: string
  hasFile: boolean
}

export interface NewExamination {
  type: string
  date: string
  doctor: string
  clinic: string
  hasFile: boolean
}

const initialExaminations: Examination[] = [
  {
    id: 1,
    type: "ЭхоКГ",
    date: "10.03.2026",
    doctor: "Петрова А.В.",
    clinic: "НМИЦ им. В.А. Алмазова",
    status: "Новое",
    hasFile: true,
  },
  {
    id: 2,
    type: "МРТ сердца",
    date: "15.02.2026",
    doctor: "Сидоров В.И.",
    clinic: "НМИЦ им. В.А. Алмазова",
    status: "Просмотрено",
    hasFile: true,
  },
  {
    id: 3,
    type: "ЭКГ",
    date: "10.02.2026",
    doctor: "Петрова А.В.",
    clinic: "Поликлиника №5",
    status: "Просмотрено",
    hasFile: true,
  },
  {
    id: 4,
    type: "Холтеровское мониторирование",
    date: "05.01.2026",
    doctor: "Козлова М.Н.",
    clinic: "НМИЦ им. В.А. Алмазова",
    status: "Просмотрено",
    hasFile: true,
  },
  {
    id: 5,
    type: "Анализ крови",
    date: "05.01.2026",
    doctor: "Петрова А.В.",
    clinic: "Поликлиника №5",
    status: "Просмотрено",
    hasFile: true,
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

