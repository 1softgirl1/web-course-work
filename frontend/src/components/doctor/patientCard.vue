<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Card from '@/components/ui/card.vue'
import Button from '@/components/ui/button.vue'
import Badge from '@/components/ui/badge.vue'
import Dialog from '@/components/ui/dialog.vue'
import Select from '@/components/ui/select/select.vue'
import SelectTrigger from '@/components/ui/select/selectTrigger.vue'
import SelectValue from '@/components/ui/select/selectValue.vue'
import SelectContent from '@/components/ui/select/selectContent.vue'
import SelectItem from '@/components/ui/select/selectItem.vue'
import { Undo2, Calendar, FileText, Eye, Plus, Pencil } from 'lucide-vue-next'
import { usePatientStore } from '@/stores/patientStore'
import { useExaminationStore, type Examination, parseExamDate } from '@/stores/examinationStore'
import { RegionsStore, resolveSelectedRegionName } from '@/stores/regionsStore'
import PatientCardContent from '@/components/patient/patientCardContent.vue'
import ExaminationTabe from '@/components/patient/examinationTabe.vue'

const patientStore = usePatientStore()
const route = useRoute()
const router = useRouter()
const { examinations } = useExaminationStore()

const selectedExam = ref<Examination | null>(null)
const isDetailsOpen = ref(false)
const isRegionPickerOpen = ref(false)
const isRegionSelectOpen = ref(false)
const isTransferConfirmOpen = ref(false)
const isTransferCountdownOpen = ref(false)
const targetRegion = ref('')
const countdownSeconds = ref(10)

let countdownIntervalId: ReturnType<typeof setInterval> | null = null
let countdownTimeoutId: ReturnType<typeof setTimeout> | null = null

const patientCode = computed(() => {
  return typeof route.params.code === 'string' ? route.params.code : ''
})

const patientData = computed(() => {
  if (!patientCode.value) return undefined
  return patientStore.patients.find(patient => patient.code === patientCode.value)
})

const doctorRegionName = computed(() => resolveSelectedRegionName())

const currentDoctorFullName = computed(() => {
  if (typeof window === 'undefined') return 'Неизвестный врач'
  return localStorage.getItem('doctorFullName')?.trim() || 'Неизвестный врач'
})

const canShowPatientFullName = computed(() => {
  if (!patientData.value) return false
  return patientData.value.region === doctorRegionName.value
})

const canChangeRegion = computed(() => {
  return Boolean(patientData.value && canShowPatientFullName.value)
})

const availableRegions = computed(() => {
  return RegionsStore.filter(region => region.name !== patientData.value?.region)
})


const backNavigation = computed(() => {
  const from = route.query.from === 'allPatients' ? '/doctor/allPatients' : '/doctor/myPatients'
  const query: Record<string, string> = {}

  if (typeof route.query.q === 'string' && route.query.q) query.q = route.query.q
  if (typeof route.query.diagnosis === 'string' && route.query.diagnosis) query.diagnosis = route.query.diagnosis
  if (typeof route.query.region === 'string' && route.query.region) query.region = route.query.region
  if (typeof route.query.page === 'string' && route.query.page) query.page = route.query.page

  return { path: from, query }
})

const goBackToList = () => {
  router.push(backNavigation.value)
}

const patientExaminations = computed(() => {
  return examinations
    .filter(exam => exam.patientCode === patientCode.value)
    .sort((a, b) => {
      const aTime = parseExamDate(a.date)?.getTime() ?? 0
      const bTime = parseExamDate(b.date)?.getTime() ?? 0

      if (bTime !== aTime) return bTime - aTime
      return b.id - a.id
    })
})

const latestExamId = computed<number | null>(() => {
  return patientExaminations.value.length > 0 ? patientExaminations.value[0].id : null
})

const addExaminationPath = computed(() => {
  return patientCode.value ? `/doctor/patientCard/${patientCode.value}/addExamination` : '/doctor/myPatients'
})

const editExaminationPath = (examId: number) => {
  return patientCode.value
    ? { path: `/doctor/patientCard/${patientCode.value}/addExamination`, query: { editId: String(examId) } }
    : { path: '/doctor/myPatients' }
}

const openDetails = (exam: Examination) => {
  selectedExam.value = exam
  isDetailsOpen.value = true
}

const clearTransferTimers = () => {
  if (countdownIntervalId) {
    clearInterval(countdownIntervalId)
    countdownIntervalId = null
  }

  if (countdownTimeoutId) {
    clearTimeout(countdownTimeoutId)
    countdownTimeoutId = null
  }
}

const resetTransferFlow = () => {
  clearTransferTimers()
  countdownSeconds.value = 10
  isRegionPickerOpen.value = false
  isRegionSelectOpen.value = false
  isTransferConfirmOpen.value = false
  isTransferCountdownOpen.value = false
  targetRegion.value = ''
}

const openRegionPicker = () => {
  if (!patientData.value || !canChangeRegion.value) return
  targetRegion.value = ''
  isRegionSelectOpen.value = false
  isRegionPickerOpen.value = true
}

const proceedToRegionConfirmation = () => {
  if (!targetRegion.value || !patientData.value) return
  isRegionPickerOpen.value = false
  isTransferConfirmOpen.value = true
}

const finalizeRegionTransfer = () => {
  if (!patientData.value || !targetRegion.value) {
    resetTransferFlow()
    return
  }

  patientStore.transferPatientRegion(patientData.value.code, targetRegion.value, currentDoctorFullName.value)
  const shouldReturnToOwnList = route.query.from !== 'allPatients'
  resetTransferFlow()

  if (shouldReturnToOwnList) {
    router.push(backNavigation.value)
  }
}

const confirmRegionTransfer = () => {
  if (!targetRegion.value) return

  isTransferConfirmOpen.value = false
  isTransferCountdownOpen.value = true
  countdownSeconds.value = 10
  clearTransferTimers()

  countdownIntervalId = setInterval(() => {
    countdownSeconds.value = Math.max(countdownSeconds.value - 1, 0)
  }, 1000)

  countdownTimeoutId = setTimeout(() => {
    clearTransferTimers()
    finalizeRegionTransfer()
  }, 10000)
}

const cancelRegionTransfer = () => {
  resetTransferFlow()
}

watch(
  () => patientData.value?.code,
  () => {
    resetTransferFlow()
  }
)

onBeforeUnmount(() => {
  clearTransferTimers()
})
</script>

<template>
  <div class="p-4 sm:p-6 lg:p-8">
    <div class="mb-6">
      <div class="mb-2 flex gap-4">
        <Undo2 class="mt-1 cursor-pointer" @click="goBackToList" />

        <div>
          <h1 class="text-2xl font-bold text-foreground">Карточка пациента</h1>
          <p class="text-muted-foreground">Персональные данные и медицинская информация</p>
        </div>

        <div v-if="patientData && canShowPatientFullName" class="ml-auto flex items-end gap-2">
          <router-link :to="addExaminationPath">
            <Button variant="default">
              <Plus class="mr-2 h-4 w-4" />
              Добавить обследование
            </Button>
          </router-link>
        </div>
      </div>
    </div>


    <div v-if="patientData" class="space-y-6">
      <PatientCardContent :patient="patientData" :can-show-patient-full-name="canShowPatientFullName">

        <template #region-action>
          <Badge
              v-if="canChangeRegion"
              variant="default"
              class="cursor-pointer text-xs"
              role="button"
              tabindex="0"
              @click="openRegionPicker"
          >
            Изменить регион
          </Badge>
        </template>
      </PatientCardContent>


      <Card
        class="w-full max-w-none"
        title="Обследования пациента"
        description="История обследований пациента"
      >
        <div v-if="patientExaminations.length > 0" class="space-y-3">
          <div
            v-for="exam in patientExaminations"
            :key="`doctor-patient-exam-${exam.id}`"
            class="rounded-lg border border-border p-3"
          >
            <div class="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
              <div class="flex items-start gap-4">
                <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-primary/10">
                  <FileText class="h-6 w-6 stroke-1 text-primary" />
                </div>
                <div>
                  <div class="mb-1 flex items-center gap-2">
                    <h3 class="truncate font-semibold text-foreground">Обследование #{{ exam.id }}</h3>
                    <Badge v-if="latestExamId === exam.id" variant="default" class="text-xs">Новое</Badge>
                  </div>
                  <p class="text-sm text-muted-foreground">Врач: {{ exam.doctor }}</p>
                </div>
              </div>

              <div class="flex items-center gap-3 sm:gap-4">
                <div class="flex items-center gap-2 text-sm text-muted-foreground">
                  <Calendar class="h-4 w-4" />
                  <span>{{ exam.date }}</span>
                </div>
                <router-link :to="editExaminationPath(exam.id)">
                  <Button type="button" variant="ghost" size="icon">
                    <Pencil class="h-4 w-4" />
                  </Button>
                </router-link>
                <Button type="button" variant="ghost" size="icon" @click="openDetails(exam)">
                  <Eye class="h-4 w-4" />
                </Button>
              </div>
            </div>
          </div>
        </div>

        <p v-else class="text-sm text-muted-foreground">Обследования не найдены</p>
      </Card>

      <div class="mt-8 w-full">
        <ExaminationTabe :examinations="patientExaminations" />
      </div>

      <Dialog
        v-model="isRegionPickerOpen"
        :content-class="isRegionSelectOpen ? 'details-scroll sm:max-w-md sm:min-h-[26rem]' : 'details-scroll sm:max-w-md'"
      >
        <div class="space-y-4">
          <div>
            <h3 class="text-lg font-semibold text-foreground">Изменить регион пациента</h3>
            <p class="text-sm text-muted-foreground">Выберите новый регион для пациента</p>
          </div>

          <Select v-model="targetRegion" v-model:open="isRegionSelectOpen">
            <SelectTrigger>
              <SelectValue placeholder="Выберите регион" />
            </SelectTrigger>
            <SelectContent class="details-scroll max-h-56 overflow-y-auto">
              <SelectItem v-for="region in availableRegions" :key="region.id" :value="region.name">
                {{ region.name }}
              </SelectItem>
            </SelectContent>
          </Select>

          <div class="flex justify-end gap-2">
            <Button type="button" variant="outline" @click="cancelRegionTransfer">Отмена</Button>
            <Button type="button" :disabled="!targetRegion" @click="proceedToRegionConfirmation">Продолжить</Button>
          </div>
        </div>
      </Dialog>

      <Dialog v-model="isTransferConfirmOpen" content-class="sm:max-w-md">
        <div class="space-y-4">
          <div>
            <h3 class="text-lg font-semibold text-foreground">Подтверждение перевода</h3>
            <p class="text-sm text-muted-foreground">
              Вы уверены, что хотите перевести пациента в регион {{ targetRegion }}?
            </p>
          </div>

          <div class="flex justify-end gap-2">
            <Button type="button" variant="outline" @click="cancelRegionTransfer">Нет</Button>
            <Button type="button" @click="confirmRegionTransfer">Да, перевести</Button>
          </div>
        </div>
      </Dialog>

      <Dialog v-model="isTransferCountdownOpen" content-class="sm:max-w-md">
        <div class="space-y-4">
          <div>
            <h3 class="text-lg font-semibold text-foreground">Перевод будет выполнен через {{ countdownSeconds }} сек.</h3>
            <p class="text-sm text-muted-foreground">
              До завершения отсчёта можно отменить перевод пациента в регион {{ targetRegion }}.
            </p>
          </div>

          <div class="h-2 overflow-hidden rounded-full bg-secondary">
            <div
              class="h-full bg-primary transition-all duration-1000"
              :style="{ width: `${(countdownSeconds / 10) * 100}%` }"
            />
          </div>

          <div class="flex justify-end">
            <Button type="button" variant="outline" @click="cancelRegionTransfer">Отменить</Button>
          </div>
        </div>
      </Dialog>

      <Dialog v-model="isDetailsOpen" content-class="sm:max-w-4xl">
        <div v-if="selectedExam" class="space-y-4">
          <div>
            <h3 class="text-lg font-semibold text-foreground">Обследование #{{ selectedExam.id }}</h3>
            <p class="text-sm text-muted-foreground">{{ selectedExam.date }} · Врач: {{ selectedExam.doctor }}</p>
            <p class="mt-1 text-sm text-muted-foreground">Заключение: {{ selectedExam.conclusion || 'Нет данных' }}</p>
          </div>

          <div>
            <p class="mb-3 text-sm text-muted-foreground">Численные показатели</p>
            <div class="details-scroll max-h-[65vh] overflow-y-auto pr-2">
              <div class="grid grid-cols-1 gap-2 sm:grid-cols-2 lg:grid-cols-3">
                <div
                  v-for="(value, index) in selectedExam.indicators"
                  :key="`doctor-modal-${selectedExam.id}-indicator-${index}`"
                  class="rounded-md bg-secondary/40 px-3 py-2 text-sm"
                >
                  <span class="text-muted-foreground">Показатель {{ index + 1 }}:</span>
                  <span class="ml-1 font-medium text-foreground">{{ value }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </Dialog>
    </div>

    <Card v-else class="max-w-xl">
      <div class="p-6 text-muted-foreground">Данные пациента отсутствуют</div>
    </Card>
  </div>
</template>

<style scoped>
:global(.details-scroll) {
  scrollbar-width: thin;
  scrollbar-color: rgb(148 163 184) transparent;
}

:global(.details-scroll::-webkit-scrollbar) {
  width: 10px;
}

:global(.details-scroll::-webkit-scrollbar-track) {
  background: transparent;
}

:global(.details-scroll::-webkit-scrollbar-thumb) {
  background: linear-gradient(180deg, rgb(148 163 184), rgb(100 116 139));
  border-radius: 9999px;
  border: 2px solid transparent;
  background-clip: padding-box;
}

:global(.details-scroll::-webkit-scrollbar-thumb:hover) {
  background: linear-gradient(180deg, rgb(100 116 139), rgb(71 85 105));
  background-clip: padding-box;
}
</style>
