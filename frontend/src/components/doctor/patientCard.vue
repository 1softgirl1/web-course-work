<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Card from '@/components/ui/card.vue'
import Button from '@/components/ui/button.vue'
import Input from '@/components/ui/input.vue'
import Badge from '@/components/ui/badge.vue'
import Dialog from '@/components/ui/dialog.vue'
import Select from '@/components/ui/select/select.vue'
import SelectTrigger from '@/components/ui/select/selectTrigger.vue'
import SelectValue from '@/components/ui/select/selectValue.vue'
import SelectContent from '@/components/ui/select/selectContent.vue'
import SelectItem from '@/components/ui/select/selectItem.vue'
import { Undo2, Calendar, FileText, Eye, EyeOff, Plus, Pencil } from 'lucide-vue-next'
import { usePatientStore } from '@/stores/patientStore'
import { useExaminationStore, type Examination, parseExamDate } from '@/stores/examinationStore'
import { useAuthStore } from '@/stores/authStore'
import PatientCardContent from '@/components/patient/patientCardContent.vue'
import ExaminationTabe from '@/components/patient/examinationTabe.vue'
import IndicatorTrendDialog from '@/components/patient/indicatorTrendDialog.vue'

const patientStore = usePatientStore()
const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()
const examinationStore = useExaminationStore()
const { examinations } = examinationStore

const selectedExam = ref<Examination | null>(null)
const isDetailsOpen = ref(false)
const selectedIndicatorCode = ref<string | null>(null)
const selectedIndicatorLabel = ref<string | null>(null)
const isIndicatorTrendOpen = ref(false)
const isRegionPickerOpen = ref(false)
const isRegionSelectOpen = ref(false)
const isTransferConfirmOpen = ref(false)
const isTransferCountdownOpen = ref(false)
const isPasswordDialogOpen = ref(false)
const passwordValue = ref('')
const passwordConfirm = ref('')
const passwordError = ref('')
const passwordSuccess = ref('')
const isPasswordSubmitting = ref(false)
const isPatientNewPasswordVisible = ref(false)
const isPatientConfirmPasswordVisible = ref(false)
const targetRegionId = ref('')
const countdownSeconds = ref(10)
const loadError = ref('')

let countdownIntervalId: ReturnType<typeof setInterval> | null = null
let countdownTimeoutId: ReturnType<typeof setTimeout> | null = null

const patientCode = computed(() => {
  return typeof route.params.code === 'string' ? route.params.code : ''
})

const patientData = computed(() => {
  if (!patientCode.value) return undefined
  return patientStore.patients.find(patient => patient.code === patientCode.value)
})

const doctorRegionName = computed(() => authStore.doctorRegionName.value)

const currentDoctorFullName = computed(() => {
  return authStore.user.value?.displayName || 'Неизвестный врач'
})

const canChangeRegion = computed(() => {
  if (!patientData.value) return false
  if (authStore.isDoctorExtended.value) return true
  return patientData.value.region === doctorRegionName.value
})

const canEditPatientExaminations = computed(() => {
  if (!patientData.value) return false
  if (authStore.isDoctorExtended.value) return true
  return patientData.value.region === doctorRegionName.value
})

const canChangePatientPassword = computed(() => canEditPatientExaminations.value)

const availableRegions = computed(() => {
  return patientStore
    .getKnownRegions()
    .filter(region => region.name !== patientData.value?.region)
})

const selectedTargetRegionName = computed(() => {
  if (!targetRegionId.value) return ''
  const selected = availableRegions.value.find(region => String(region.id) === targetRegionId.value)
  return selected?.name ?? ''
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

const currentListScope = computed<'own' | 'all'>(() => {
  return route.query.from === 'allPatients' ? 'all' : 'own'
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

const openIndicatorTrend = (payload: { code: string; label: string }) => {
  selectedIndicatorCode.value = payload.code
  selectedIndicatorLabel.value = payload.label
  isIndicatorTrendOpen.value = true
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
  targetRegionId.value = ''
}

const resetPasswordFlow = () => {
  isPasswordDialogOpen.value = false
  passwordValue.value = ''
  passwordConfirm.value = ''
  passwordError.value = ''
  passwordSuccess.value = ''
  isPasswordSubmitting.value = false
  isPatientNewPasswordVisible.value = false
  isPatientConfirmPasswordVisible.value = false
}

const openRegionPicker = async () => {
  if (!patientData.value || !canChangeRegion.value) return
  try {
    await patientStore.loadKnownRegionsFromDoctorPatients()
  } catch {
    // Use currently known region names if preload fails.
  }
  targetRegionId.value = ''
  isRegionSelectOpen.value = false
  isRegionPickerOpen.value = true
}

const proceedToRegionConfirmation = () => {
  if (!targetRegionId.value || !patientData.value) return
  isRegionPickerOpen.value = false
  isTransferConfirmOpen.value = true
}

const finalizeRegionTransfer = () => {
  if (!patientData.value || !targetRegionId.value) {
    resetTransferFlow()
    return
  }

  const nextRegionId = Number.parseInt(targetRegionId.value, 10)
  if (!Number.isFinite(nextRegionId) || nextRegionId <= 0) {
    resetTransferFlow()
    return
  }

  patientStore
    .transferPatientRegion(
      patientData.value.code,
      nextRegionId,
      currentDoctorFullName.value,
      selectedTargetRegionName.value,
      currentListScope.value,
    )
    .then(() => {
      const shouldReturnToOwnList = route.query.from !== 'allPatients'
      resetTransferFlow()
      if (shouldReturnToOwnList) {
        router.push(backNavigation.value)
      }
    })
    .catch(() => {
      loadError.value = 'Не удалось изменить регион пациента.'
      resetTransferFlow()
    })
}

const confirmRegionTransfer = () => {
  if (!targetRegionId.value) return

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

const submitPatientPasswordChange = async () => {
  passwordError.value = ''
  passwordSuccess.value = ''

  const next = passwordValue.value.trim()
  const confirm = passwordConfirm.value.trim()
  if (!next || !confirm) {
    passwordError.value = 'Заполните оба поля пароля.'
    return
  }
  if (next !== confirm) {
    passwordError.value = 'Пароли не совпадают.'
    return
  }
  if (next.length < 6) {
    passwordError.value = 'Пароль должен содержать минимум 6 символов.'
    return
  }
  if (!patientCode.value) {
    passwordError.value = 'Пациент не найден.'
    return
  }

  isPasswordSubmitting.value = true
  try {
    await patientStore.updatePatientPassword(patientCode.value, next, currentListScope.value)
    passwordSuccess.value = 'Пароль пациента успешно обновлен.'
    passwordValue.value = ''
    passwordConfirm.value = ''
  } catch (error) {
    const code = error instanceof Error ? error.message : 'UNKNOWN_ERROR'
    if (code === 'WEAK_PASSWORD' || code === 'VALIDATION_FAILED') {
      passwordError.value = 'Проверьте пароль. Минимум 6 символов.'
    } else if (code === 'ACCESS_DENIED') {
      passwordError.value = 'Недостаточно прав для изменения пароля.'
    } else if (code === 'PATIENT_NOT_FOUND') {
      passwordError.value = 'Пациент не найден.'
    } else {
      passwordError.value = 'Не удалось изменить пароль пациента.'
    }
  } finally {
    isPasswordSubmitting.value = false
  }
}

watch(
  () => patientData.value?.code,
  () => {
    resetTransferFlow()
    resetPasswordFlow()
  }
)

const loadPatientContext = async () => {
  if (!patientCode.value) return
  try {
    await patientStore.loadPatientCardByCode(patientCode.value)
    await examinationStore.loadByPatientCode(patientCode.value)
    loadError.value = ''
  } catch {
    loadError.value = 'Не удалось загрузить данные пациента.'
  }
}

watch(
  () => patientCode.value,
  () => {
    void loadPatientContext()
  },
)

onMounted(() => {
  void loadPatientContext()
})

onBeforeUnmount(() => {
  clearTransferTimers()
})
</script>

<template>
  <div class="min-w-0 p-4 sm:p-6 lg:p-8">
    <div class="mb-6">
      <div class="mb-2 flex flex-wrap items-start gap-3 sm:gap-4">
        <Undo2 class="mt-1 cursor-pointer" @click="goBackToList" />

        <div class="min-w-0 flex-1">
          <h1 class="text-xl font-bold text-foreground sm:text-2xl">Карточка пациента</h1>
          <p class="text-muted-foreground">Персональные данные и медицинская информация</p>
        </div>

        <div v-if="patientData && canEditPatientExaminations" class="flex w-full justify-end sm:ml-auto sm:w-auto sm:items-end sm:gap-2">
          <router-link :to="addExaminationPath" class="w-full sm:w-auto">
            <Button variant="default" class="w-full sm:w-auto">
              <Plus class="mr-2 h-4 w-4" />
              Добавить обследование
            </Button>
          </router-link>
        </div>
      </div>
      <p v-if="loadError" class="mt-2 text-sm text-destructive">{{ loadError }}</p>
    </div>


    <div v-if="patientData" class="min-w-0 space-y-6">
      <PatientCardContent :patient="patientData">

        <template #region-action>
          <Badge
              v-if="canChangeRegion"
              variant="default"
              class="cursor-pointer text-xs"
              role="button"
              tabindex="0"
              @click="openRegionPicker"
          >
            Изменить
          </Badge>
        </template>

        <template #password-action>
          <Badge v-if="canChangePatientPassword" variant="outline" class="cursor-pointer" @click="isPasswordDialogOpen = true">
            Изменить
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
            <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between sm:gap-1">
              <div class="flex min-w-0 items-center gap-3 sm:gap-4">
                <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-primary/10">
                  <FileText class="h-6 w-6 stroke-1 text-primary" />
                </div>
                <div class="min-w-0">
                  <div class="mb-1 flex flex-wrap items-center gap-2 sm:flex-nowrap">
                    <h3 class="font-semibold leading-snug text-foreground sm:truncate">Обследование #{{ exam.id }}</h3>
                    <Badge v-if="latestExamId === exam.id" variant="default" class="text-xs">Новое</Badge>
                  </div>
                </div>
              </div>

              <div class="mt-2 flex items-center justify-between gap-3 border-t border-border/60 pt-2 sm:mt-0 sm:justify-start sm:gap-4 sm:border-0 sm:pt-0">
                <div class="flex items-center gap-2 text-sm text-muted-foreground">
                  <Calendar class="h-4 w-4" />
                  <span>{{ exam.date }}</span>
                </div>
                <router-link v-if="canEditPatientExaminations" :to="editExaminationPath(exam.id)">
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

      <div class="mt-8 w-full min-w-0">
        <ExaminationTabe :examinations="patientExaminations" @select-indicator="openIndicatorTrend" />
      </div>

      <Dialog
        v-model="isRegionPickerOpen"
        :content-class="isRegionSelectOpen ? 'details-scroll sm:max-w-md sm:min-h-[26rem]' : 'details-scroll sm:max-w-md'"
      >
        <div class="space-y-4">
          <div>
            <h3 class="text-base font-semibold leading-tight text-foreground sm:text-lg">Изменить регион пациента</h3>
            <p class="text-sm text-muted-foreground">Выберите новый регион для пациента</p>
          </div>

          <Select v-model="targetRegionId" v-model:open="isRegionSelectOpen">
            <SelectTrigger>
              <SelectValue placeholder="Выберите регион" />
            </SelectTrigger>
            <SelectContent class="z-[4000]">
              <SelectItem v-for="region in availableRegions" :key="region.id" :value="String(region.id)">
                {{ region.name }}
              </SelectItem>
            </SelectContent>
          </Select>

          <div class="flex justify-end gap-2">
            <Button type="button" variant="outline" @click="cancelRegionTransfer">Отмена</Button>
            <Button type="button" :disabled="!targetRegionId" @click="proceedToRegionConfirmation">Продолжить</Button>
          </div>
        </div>
      </Dialog>

      <Dialog v-model="isTransferConfirmOpen" content-class="sm:max-w-md">
        <div class="space-y-4">
          <div>
            <h3 class="text-base font-semibold leading-tight text-foreground sm:text-lg">Подтверждение перевода</h3>
            <p class="text-sm text-muted-foreground">
              Вы уверены, что хотите перевести пациента в регион {{ selectedTargetRegionName }}?
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
            <h3 class="text-base font-semibold leading-tight text-foreground sm:text-lg">Перевод будет выполнен через {{ countdownSeconds }} сек.</h3>
            <p class="text-sm text-muted-foreground">
              До завершения отсчёта можно отменить перевод пациента в регион {{ selectedTargetRegionName }}.
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

      <Dialog v-model="isPasswordDialogOpen" content-class="sm:max-w-md">
        <div class="space-y-4">
          <div>
            <h3 class="text-base font-semibold leading-tight text-foreground sm:text-lg">Изменение пароля пациента</h3>
            <p class="text-sm text-muted-foreground">Новый пароль должен содержать минимум 6 символов.</p>
          </div>

          <div class="space-y-2">
            <label class="text-sm text-muted-foreground" for="patient-password-next">Новый пароль</label>
            <div class="relative">
              <Input
                id="patient-password-next"
                v-model="passwordValue"
                :type="isPatientNewPasswordVisible ? 'text' : 'password'"
                autocomplete="new-password"
                class="pr-10"
              />
              <button
                type="button"
                class="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground transition-colors hover:text-foreground"
                @click="isPatientNewPasswordVisible = !isPatientNewPasswordVisible"
              >
                <EyeOff v-if="isPatientNewPasswordVisible" class="h-4 w-4" />
                <Eye v-else class="h-4 w-4" />
              </button>
            </div>
          </div>

          <div class="space-y-2">
            <label class="text-sm text-muted-foreground" for="patient-password-confirm">Подтверждение пароля</label>
            <div class="relative">
              <Input
                id="patient-password-confirm"
                v-model="passwordConfirm"
                :type="isPatientConfirmPasswordVisible ? 'text' : 'password'"
                autocomplete="new-password"
                class="pr-10"
              />
              <button
                type="button"
                class="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground transition-colors hover:text-foreground"
                @click="isPatientConfirmPasswordVisible = !isPatientConfirmPasswordVisible"
              >
                <EyeOff v-if="isPatientConfirmPasswordVisible" class="h-4 w-4" />
                <Eye v-else class="h-4 w-4" />
              </button>
            </div>
          </div>

          <p v-if="passwordError" class="text-sm text-destructive">{{ passwordError }}</p>
          <p v-if="passwordSuccess" class="text-sm text-green-600">{{ passwordSuccess }}</p>

          <div class="flex justify-end gap-2">
            <Button type="button" variant="outline" @click="resetPasswordFlow">Закрыть</Button>
            <Button type="button" :disabled="isPasswordSubmitting" @click="submitPatientPasswordChange">
              {{ isPasswordSubmitting ? 'Сохранение...' : 'Сохранить' }}
            </Button>
          </div>
        </div>
      </Dialog>

      <Dialog v-model="isDetailsOpen" content-class="sm:max-w-4xl">
        <div v-if="selectedExam" class="space-y-4">
          <div>
            <h3 class="text-base font-semibold leading-tight text-foreground sm:text-lg">Обследование #{{ selectedExam.id }}</h3>
            <p class="text-sm text-muted-foreground">{{ selectedExam.date }}</p>
            <p class="mt-1 text-sm text-muted-foreground">Заключение: {{ selectedExam.conclusion || 'Нет данных' }}</p>
          </div>

          <div>
            <p class="mb-3 text-sm text-muted-foreground">Численные показатели</p>
            <div class="details-scroll max-h-[65vh] overflow-y-auto pr-2">
              <div class="grid grid-cols-1 gap-2 sm:grid-cols-2 lg:grid-cols-3">
                <div
                  v-for="metric in selectedExam.metrics"
                  :key="`doctor-modal-${selectedExam.id}-${metric.characteristicCode}`"
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
        :exams="patientExaminations"
      />
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
