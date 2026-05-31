<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import Card from '@/components/ui/card.vue'
import Input from '@/components/ui/input.vue'
import Button from '@/components/ui/button.vue'
import Badge from '@/components/ui/badge.vue'
import Dialog from '@/components/ui/dialog.vue'
import Field from '@/components/ui/field/field.vue'
import FieldLabel from '@/components/ui/field/field-label.vue'
import { CheckCircle, Heart, Undo2 } from 'lucide-vue-next'
import {
  buildMetricCatalogFromExams,
  useExaminationStore,
  toRuExamDate,
  type EditableMetricInput,
  type MetricDescriptor,
} from '@/stores/examinationStore'
import { useAuthStore } from '@/stores/authStore'
import { usePatientStore } from '@/stores/patientStore'

interface FormMetricRow {
  characteristicCode: string
  value: string
  comment: string
}

const submitted = ref(false)
const formError = ref('')
const route = useRoute()
const authStore = useAuthStore()
const patientStore = usePatientStore()
const examDate = ref('')
const conclusion = ref('')
const metricRows = ref<FormMetricRow[]>([])
const examinationStore = useExaminationStore()
const { examinations, addExamination, updateExamination } = examinationStore
const loadError = ref('')

const patientCode = computed(() => {
  return typeof route.params.code === 'string' ? route.params.code : ''
})

const resolvedDoctorFullName = computed(() => {
  return authStore.user.value?.displayName || 'Врач'
})

const knownMetricCatalog = computed(() => {
  const patientExams = examinations.filter(exam => exam.patientCode === patientCode.value)
  return buildMetricCatalogFromExams(patientExams)
})

const knownMetricByCode = computed(() => {
  return knownMetricCatalog.value.reduce<Record<string, MetricDescriptor>>((acc, metric) => {
    acc[metric.characteristicCode] = metric
    return acc
  }, {})
})

const canEditPatientExaminations = computed(() => {
  if (authStore.isDoctorExtended.value) return true
  const patient = patientStore.patients.find(item => item.code === patientCode.value)
  if (!patient) return false
  return patient.region === authStore.doctorRegionName.value
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

const createRowByCode = (characteristicCode: string): FormMetricRow => ({
  characteristicCode,
  value: '',
  comment: '',
})

const createFixedMetricRows = (): FormMetricRow[] => {
  return knownMetricCatalog.value.map(metric => createRowByCode(metric.characteristicCode))
}

const hydrateFormFromEditing = () => {
  metricRows.value = createFixedMetricRows()

  if (!editingExam.value) return
  const [day, month, year] = editingExam.value.date.split('.')
  examDate.value = `${year}-${month}-${day}`
  conclusion.value = editingExam.value.conclusion

  const rowByCode = new Map(metricRows.value.map(row => [row.characteristicCode, row]))
  editingExam.value.metrics.forEach(metric => {
    const row = rowByCode.get(metric.characteristicCode)
    if (!row) return
    row.value = String(metric.value)
    row.comment = metric.comment ?? ''
  })
}

const backToPatientCardPath = computed(() => {
  const code = typeof route.params.code === 'string' ? route.params.code : ''
  return code ? `/doctor/patientCard/${code}` : '/doctor/myPatients'
})

const parseMetricRows = (): EditableMetricInput[] => {
  const parsed: EditableMetricInput[] = []
  const seenCodes = new Set<string>()

  metricRows.value.forEach(row => {
    const code = row.characteristicCode.trim()
    const valueRaw = row.value.trim()
    const comment = row.comment.trim()

    const hasAnyInput = Boolean(valueRaw || comment)
    if (!hasAnyInput) return

    const numeric = Number(valueRaw)
    if (!Number.isFinite(numeric)) {
      throw new Error('METRIC_VALUE_INVALID')
    }

    if (seenCodes.has(code)) {
      throw new Error('METRIC_CODE_DUPLICATE')
    }
    seenCodes.add(code)

    parsed.push({
      characteristicCode: code,
      value: numeric,
      comment: comment || null,
    })
  })

  return parsed
}

const resetForm = () => {
  if (editingExam.value) {
    hydrateFormFromEditing()
    return
  }
  examDate.value = ''
  conclusion.value = ''
  metricRows.value = createFixedMetricRows()
}

const closeSuccessDialog = () => {
  submitted.value = false
  resetForm()
}

const showMetricValidationError = (error: unknown) => {
  if (!(error instanceof Error)) {
    formError.value = 'Не удалось сохранить обследование. Попробуйте снова.'
    return
  }

  if (error.message === 'METRIC_VALUE_INVALID') {
    formError.value = 'Если показатель заполнен, значение должно быть числом.'
    return
  }

  if (error.message === 'METRIC_CODE_DUPLICATE' || error.message.startsWith('METRIC_DUPLICATE:')) {
    formError.value = 'Коды показателей не должны повторяться.'
    return
  }

  if (error.message === 'METRICS_REQUIRED') {
    formError.value = 'Добавьте минимум один показатель.'
    return
  }

  formError.value = 'Не удалось сохранить обследование. Попробуйте снова.'
}

const handleSubmit = async (e: Event) => {
  e.preventDefault()
  formError.value = ''

  if (!examDate.value) {
    formError.value = 'Заполните обязательное поле: дату обследования.'
    return
  }

  if (!editingExam.value && !patientCode.value) {
    formError.value = 'Не удалось определить пациента для сохранения обследования.'
    return
  }

  if (!canEditPatientExaminations.value) {
    formError.value = 'Редактирование обследований пациента из другого региона для обычного врача недоступно.'
    return
  }

  const doctorFullName = resolvedDoctorFullName.value
  if (!doctorFullName) {
    formError.value = 'Не удалось определить ФИО врача из личного кабинета.'
    return
  }

  let metrics: EditableMetricInput[]
  try {
    metrics = parseMetricRows()
  } catch (error) {
    showMetricValidationError(error)
    return
  }

  if (!editingExam.value && metrics.length === 0) {
    formError.value = 'Добавьте минимум один показатель.'
    return
  }

  try {
    if (editingExam.value) {
      await updateExamination({
        id: editingExam.value.id,
        date: toRuExamDate(examDate.value),
        doctor: doctorFullName,
        conclusion: conclusion.value.trim(),
        metrics,
      })
    } else {
      await addExamination({
        patientCode: patientCode.value,
        date: toRuExamDate(examDate.value),
        doctor: doctorFullName,
        conclusion: conclusion.value.trim(),
        metrics,
      })
    }

    submitted.value = true
  } catch (error) {
    showMetricValidationError(error)
  }
}

onMounted(async () => {
  metricRows.value = createFixedMetricRows()
  if (!patientCode.value) return
  try {
    await patientStore.loadPatientCardByCode(patientCode.value)
    await examinationStore.loadByPatientCode(patientCode.value)
    hydrateFormFromEditing()
    loadError.value = ''
  } catch {
    loadError.value = 'Не удалось загрузить данные пациента.'
  }
})
</script>

<template>
  <div class="p-4 sm:p-6 lg:p-8">
    <Dialog v-model="submitted" content-class="sm:max-w-xl">
      <div class="p-4 text-center sm:p-8">
        <div class="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-green-100">
          <CheckCircle class="h-8 w-8 text-green-600" />
        </div>
        <h2 class="mb-6 text-xl font-semibold text-foreground">Обследование успешно загружено</h2>
        <div class="flex flex-col gap-2 sm:flex-row sm:justify-center">
          <router-link :to="backToPatientCardPath" class="w-full sm:w-auto">
            <Button variant="outline" class="w-full sm:w-auto">Назад</Button>
          </router-link>
          <Button class="w-full sm:w-auto" @click="closeSuccessDialog">
            {{ editingExam ? 'Вернуться к редактированию' : 'Добавить еще' }}
          </Button>
        </div>
      </div>
    </Dialog>

    <div>
      <div class="mb-6">
        <div class="mb-2 flex items-start gap-4">
          <router-link :to="backToPatientCardPath">
            <Undo2 class="mt-1" />
          </router-link>
          <div>
            <h1 class="text-2xl font-bold text-foreground">
              {{ editingExam ? 'Редактировать обследование' : 'Загрузить обследование' }}
            </h1>
            <p class="text-muted-foreground">Внесите данные о результатах обследования</p>
          </div>
        </div>
      </div>

      <p v-if="loadError" class="mb-4 text-sm text-destructive">{{ loadError }}</p>

      <Card class="w-full max-w-4xl">
        <p v-if="formError" class="px-6 pt-6 text-sm text-destructive">{{ formError }}</p>
        <template #header>
          <div class="flex items-start gap-3">
            <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-red-500/10">
              <Heart class="h-5 w-5 text-red-500" />
            </div>
            <div>
              <div class="font-semibold leading-none">
                {{ editingExam ? 'Редактирование обследования' : 'Новое обследование' }}
              </div>
              <div class="text-sm text-muted-foreground">
                Укажите дату, комментарий и нужные показатели
              </div>
            </div>
          </div>
        </template>

        <form v-if="canEditPatientExaminations" class="space-y-6" @submit.prevent="handleSubmit">
          <Field>
            <FieldLabel>Дата обследования *</FieldLabel>
            <Input v-model="examDate" type="date" required />
          </Field>

          <Field>
            <FieldLabel>Заключение врача</FieldLabel>
            <Input v-model="conclusion" placeholder="Текст заключения" />
          </Field>

          <div class="space-y-3 rounded-xl bg-secondary/30 p-4">
            <div class="flex flex-wrap items-center justify-between gap-2">
              <h3 class="flex items-center gap-2 font-medium text-foreground">
                <Heart class="h-4 w-4 text-red-500" />
                Показатели обследования
              </h3>
            </div>

            <p class="text-xs text-muted-foreground">
              Для нового обследования заполните минимум один показатель. Пустые поля не отправляются.
            </p>

            <div class="space-y-3">
              <div
                v-for="row in metricRows"
                :key="row.characteristicCode"
                class="grid grid-cols-1 gap-2 rounded-lg border border-border bg-background p-3 md:grid-cols-12"
              >
                <div class="md:col-span-5 flex flex-col justify-start gap-1.5 pt-1">
                  <p class="text-sm font-medium leading-tight text-foreground break-words">
                    {{ knownMetricByCode[row.characteristicCode]?.characteristicName || '—' }}
                  </p>
                  <Badge
                    v-if="knownMetricByCode[row.characteristicCode]?.unit"
                    variant="outline"
                    class="text-xs w-fit"
                  >
                    {{ knownMetricByCode[row.characteristicCode]?.unit }}
                  </Badge>
                </div>

                <div class="md:col-span-3">
                  <FieldLabel>Значение</FieldLabel>
                  <Input
                    v-model="row.value"
                    type="number"
                    :placeholder="knownMetricByCode[row.characteristicCode]?.placeholder ?? ''"
                  />
                </div>

                <div class="md:col-span-4">
                  <FieldLabel>Комментарий</FieldLabel>
                  <Input v-model="row.comment" placeholder="Опционально" />
                </div>
              </div>
            </div>
          </div>

          <Button type="submit" class="w-full" size="lg">
            {{ editingExam ? 'Сохранить изменения' : 'Загрузить обследование' }}
          </Button>
        </form>

        <div v-else class="rounded-lg border border-destructive/20 bg-destructive/5 p-4 text-sm text-foreground">
          Редактирование обследований пациента из другого региона для обычного врача недоступно.
        </div>
      </Card>
    </div>
  </div>
</template>
