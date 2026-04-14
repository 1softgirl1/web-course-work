<script setup lang="ts" >
import { computed, ref } from "vue"
import { useRoute } from 'vue-router'
import Card from "@/components/ui/card.vue";
import Input from '@/components/ui/input.vue'
import Button from '@/components/ui/button.vue'
import Field from "@/components/ui/field/field.vue";
import FieldLabel from "@/components/ui/field/field-label.vue";
import {CheckCircle, Heart, Undo2} from "lucide-vue-next"
import { useExaminationStore, toRuExamDate } from '@/stores/examinationStore'


const submitted = ref(false)
const route = useRoute()
const examDate = ref("")
const conclusion = ref("")
const indicatorValues = ref<string[]>(Array.from({ length: 50 }, () => ""))
const { examinations, addExamination, updateExamination } = useExaminationStore()
const indicatorLabels = Array.from({ length: 50 }, (_, index) => `Показатель ${index + 1}`)

const patientCode = computed(() => {
  return typeof route.params.code === 'string' ? route.params.code : ''
})

const resolvedDoctorFullName = computed(() => {
  if (typeof window === 'undefined') return 'Борискова Д.В.'
  const saved = localStorage.getItem('doctorFullName')
  return saved && saved.trim() ? saved.trim() : 'Борискова Д.В.'
})

const editId = computed<number | null>(() => {
  const value = route.query.editId
  if (typeof value !== 'string') return null
  const parsed = Number(value)
  return Number.isNaN(parsed) ? null : parsed
})

const editingExam = computed(() => {
  if (!editId.value) return null
  return examinations.find(exam => exam.id === editId.value && exam.patientCode === patientCode.value) ?? null
})

if (editingExam.value) {
  const [day, month, year] = editingExam.value.date.split('.')
  examDate.value = `${year}-${month}-${day}`
  conclusion.value = editingExam.value.conclusion
  indicatorValues.value = indicatorLabels.map((_, index) => String(editingExam.value?.indicators[index] ?? ''))
}

const backToPatientCardPath = computed(() => {
  const code = typeof route.params.code === 'string' ? route.params.code : ''
  return code ? `/doctor/patientCard/${code}` : '/doctor/myPatients'
})

const resetForm = () => {
  submitted.value = false
  if (editingExam.value) {
    const [day, month, year] = editingExam.value.date.split('.')
    examDate.value = `${year}-${month}-${day}`
    conclusion.value = editingExam.value.conclusion
    indicatorValues.value = indicatorLabels.map((_, index) => String(editingExam.value?.indicators[index] ?? ''))
    return
  }
  examDate.value = ""
  conclusion.value = ""
  indicatorValues.value = Array.from({ length: 50 }, () => "")
}

const handleSubmit = (e: Event) => {
  e.preventDefault()

  if (!examDate.value) {
    alert('Заполните обязательное поле: дату обследования')
    return
  }

  if (!editingExam.value && !patientCode.value) {
    alert('Не удалось определить пациента для сохранения обследования')
    return
  }

  const indicators = indicatorValues.value.map(value => Number(value.trim()))
  const hasInvalidIndicator = indicators.some(value => Number.isNaN(value))

  if (hasInvalidIndicator) {
    alert('Заполните все 50 показателей числовыми значениями')
    return
  }

  const doctorFullName = resolvedDoctorFullName.value
  if (!doctorFullName) {
    alert('Не удалось определить ФИО врача из личного кабинета')
    return
  }

  if (editingExam.value) {
    updateExamination({
      id: editingExam.value.id,
      date: toRuExamDate(examDate.value),
      doctor: doctorFullName,
      conclusion: conclusion.value.trim(),
      indicators,
    })
  } else {
    addExamination({
      patientCode: patientCode.value,
      date: toRuExamDate(examDate.value),
      doctor: doctorFullName,
      conclusion: conclusion.value.trim(),
      indicators,
    })
  }

  submitted.value = true
}
</script>

<template>
  <div class="p-4 sm:p-6 lg:p-8">
    <!-- Успешная отправка -->
    <Card v-if="submitted" class="max-w-xl flex items-center">
      <div class="p-8 text-center ">
        <div class="w-16 h-16 rounded-full bg-green-100 flex items-center justify-center mx-auto mb-4">
          <CheckCircle class="w-8 h-8 text-green-600" />
        </div>
        <h2 class="text-xl font-semibold text-foreground mb-2">Обследование успешно загружено</h2>
        <Button @click="resetForm">
          Добавить еще
        </Button>
      </div>
    </Card>

    <!-- Форма загрузки -->
    <div v-else>

      <div class="mb-6">
        <div class="flex items-s gap-4 mb-2">
          <router-link :to="backToPatientCardPath">
            <Undo2 class="mt-1"></Undo2>
          </router-link>
          <div>
            <h1 class="text-2xl font-bold text-foreground">Загрузить обследование</h1>
            <p class="text-muted-foreground">Внесите данные о результатах обследования</p>
          </div>
        </div>
      </div>




      <Card class="max-w-3xl" >

        <template #header>
          <div class="flex items-start gap-3">
            <!-- Иконка -->
            <div class="w-10 h-10 rounded-xl bg-red-500/10 flex items-center justify-center shrink-0">
              <Heart class="w-5 h-5 text-red-500" />
            </div>

            <!-- Текст -->
            <div class="flex-col ">
              <div class="font-semibold leading-none ">
                {{ editingExam ? 'Редактирование обследования' : 'Новое обследование' }}
              </div>
              <div class="text-sm text-muted-foreground">
                Укажите параметры и загрузите файлы обследования
              </div>
            </div>
          </div>
        </template>



          <form @submit.prevent="handleSubmit" class="space-y-6">
            <div class="grid sm:grid-cols-1 gap-4">
              <Field>
                <FieldLabel>Дата обследования *</FieldLabel>
                <Input v-model="examDate" type="date" required />
              </Field>
            </div>

            <Field>
              <FieldLabel>Заключение врача</FieldLabel>
              <Input v-model="conclusion" placeholder="Текст заключения" />
            </Field>

            <div class="p-4 bg-secondary/30 rounded-xl space-y-4">
              <h3 class="font-medium text-foreground flex items-center gap-2">
                <Heart class="w-4 h-4 text-red-500" />
                Численные показатели обследования
              </h3>

              <div class="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
                <Field v-for="(label, index) in indicatorLabels" :key="label">
                  <FieldLabel>{{ label }} *</FieldLabel>
                  <Input
                    v-model="indicatorValues[index]"
                    type="number"
                    placeholder="Введите число"
                    required
                  />
                </Field>
              </div>
            </div>

            <Button type="submit" class="w-full" size="lg">
              {{ editingExam ? 'Сохранить изменения' : 'Загрузить обследование' }}
            </Button>
          </form>

      </Card>
    </div>
  </div>
</template>