<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import Card from '@/components/ui/card.vue'
import Button from '@/components/ui/button.vue'
import Dialog from '@/components/ui/dialog.vue'
import ExaminationTabe from '@/components/patient/examinationTabe.vue'
import IndicatorTrendDialog from '@/components/patient/indicatorTrendDialog.vue'
import { useAuthStore } from '@/stores/authStore'
import { FileText, Calendar, Eye } from 'lucide-vue-next'
import { useExaminationStore, type Examination } from '@/stores/examinationStore'
import Badge from '@/components/ui/badge.vue'
import { toHumanErrorMessage } from '@/api/httpClient'

const examinationStore = useExaminationStore()
const { examinations } = examinationStore
const authStore = useAuthStore()
const selectedExam = ref<Examination | null>(null)
const isDetailsOpen = ref(false)
const selectedIndicatorCode = ref<string | null>(null)
const selectedIndicatorLabel = ref<string | null>(null)
const isIndicatorTrendOpen = ref(false)
const loadError = ref('')

const parseExamDate = (value: string): Date | null => {
  const [day, month, year] = value.split('.').map(Number)
  if (!day || !month || !year) return null
  const date = new Date(year, month - 1, day)
  return Number.isNaN(date.getTime()) ? null : date
}

const sortedExaminations = computed<Examination[]>(() => {
  const patientCode = authStore.user.value?.patientCode
  const visibleExaminations = patientCode
    ? examinations.filter(exam => exam.patientCode === patientCode)
    : examinations

  return [...visibleExaminations].sort((a, b) => {
    const aTime = parseExamDate(a.date)?.getTime() ?? 0
    const bTime = parseExamDate(b.date)?.getTime() ?? 0

    if (bTime !== aTime) return bTime - aTime
    return b.id - a.id
  })
})

const latestExamId = computed<number | null>(() => {
  return sortedExaminations.value.length > 0 ? sortedExaminations.value[0].id : null
})

const openDetails = (exam: Examination) => {
  selectedExam.value = exam
  isDetailsOpen.value = true
}

const openIndicatorTrend = (payload: { code: string; label: string }) => {
  selectedIndicatorCode.value = payload.code
  selectedIndicatorLabel.value = payload.label
  isIndicatorTrendOpen.value = true
}

onMounted(async () => {
  try {
    await examinationStore.loadCurrentPatientExaminations()
  } catch (error) {
    loadError.value = toHumanErrorMessage(error)
  }
})
</script>

<template>
  <div class="p-4 sm:p-6 lg:p-8">
    <div class="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h1 class="text-2xl font-bold text-foreground">Обследования</h1>
        <p class="text-muted-foreground">История всех пройденных обследований</p>
      </div>
    </div>

    <p v-if="loadError" class="mb-4 text-sm text-destructive">{{ loadError }}</p>

    <div class="space-y-4">
      <Card
        v-for="exam in sortedExaminations"
        :key="exam.id"
        class="transition-colors lg:h-25 hover:border-primary/30 "
      >
        <div>
          <div class="flex min-w-0 flex-col gap-3 sm:flex-row sm:items-center sm:justify-between sm:gap-1">
            <div class="flex min-w-0 items-center gap-3 sm:gap-4">
              <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-primary/10">
                <FileText class="h-6 w-6 stroke-1 text-primary" />
              </div>
              <div class="min-w-0">
                <div class="mb-1 flex flex-wrap items-center gap-2 sm:flex-nowrap ">
                  <h3 class="truncate font-semibold leading-snug text-foreground">Обследование #{{ exam.id }}</h3>
                  <Badge v-if="latestExamId === exam.id" variant="default" class="text-xs">Новое</Badge>
                </div>
              </div>
            </div>

            <div class="mt-2 flex items-center justify-between gap-3 border-t border-border/60 pt-2 sm:mt-0 sm:justify-start sm:gap-4 sm:border-0 sm:pt-0">
              <div class="flex items-center gap-2 text-sm text-muted-foreground">
                <Calendar class="h-4 w-4" />
                <span>{{ exam.date }}</span>
              </div>
              <Button type="button" variant="ghost" size="icon" @click="openDetails(exam)">
                <Eye class="h-4 w-4" />
              </Button>
            </div>
          </div>
        </div>
      </Card>
    </div>

    <div class="mt-8 w-full">
      <ExaminationTabe @select-indicator="openIndicatorTrend" />
    </div>

    <Dialog v-model="isDetailsOpen" content-class="sm:max-w-4xl">
      <div v-if="selectedExam" class="space-y-4">
        <div>
          <div class="mb-1 flex items-center gap-2">
            <h3 class="text-lg font-semibold text-foreground">Обследование #{{ selectedExam.id }}</h3>
            <Badge variant="outline">{{ selectedExam.date }}</Badge>
          </div>

          <p class="mt-1 text-sm text-muted-foreground">Заключение: {{ selectedExam.conclusion || 'Нет данных' }}</p>
        </div>

        <div>
          <p class="mb-3 text-sm text-muted-foreground">Численные показатели</p>
          <div class="details-scroll max-h-[65vh] overflow-y-auto pr-2">
            <div class="grid grid-cols-1 gap-2 sm:grid-cols-2 lg:grid-cols-3">
              <div
                v-for="metric in selectedExam.metrics"
                :key="`modal-${selectedExam.id}-${metric.characteristicCode}`"
                class="rounded-md bg-secondary/40 px-3 py-2 text-left text-sm"
              >
                <span class="text-muted-foreground">{{ metric.characteristicName || metric.characteristicCode }}:</span>
                <span class="ml-1 font-medium text-foreground">{{ metric.value }}{{ metric.unit ? ` ${metric.unit}` : '' }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Dialog>

    <IndicatorTrendDialog
      v-model="isIndicatorTrendOpen"
      :indicator-code="selectedIndicatorCode"
      :indicator-label="selectedIndicatorLabel"
      :exams="sortedExaminations"
    />
  </div>
</template>

<style scoped>
.details-scroll {
  scrollbar-width: thin;
  scrollbar-color: rgb(148 163 184) transparent;
}

.details-scroll::-webkit-scrollbar {
  width: 10px;
}

.details-scroll::-webkit-scrollbar-track {
  background: transparent;
}

.details-scroll::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, rgb(148 163 184), rgb(100 116 139));
  border-radius: 9999px;
  border: 2px solid transparent;
  background-clip: padding-box;
}

.details-scroll::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(180deg, rgb(100 116 139), rgb(71 85 105));
  background-clip: padding-box;
}
</style>
