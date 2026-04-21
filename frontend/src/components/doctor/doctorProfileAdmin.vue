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
import { MapPin, Undo2, Mail, Stethoscope, Hospital, User } from 'lucide-vue-next'
import { mockPatientApi, type MockDoctorRecord } from '@/mocks/openapi/mockPatientApi'
import { RegionsStore } from '@/stores/regionsStore'
import { useAuthStore } from '@/stores/authStore'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

interface SelectableRegion {
  regionId: number
  name: string
}

const doctorData = ref<MockDoctorRecord | null>(null)
const isRegionPickerOpen = ref(false)
const isRegionSelectOpen = ref(false)
const isTransferConfirmOpen = ref(false)
const isTransferCountdownOpen = ref(false)
const targetRegionId = ref('')
const countdownSeconds = ref(10)

let countdownIntervalId: ReturnType<typeof setInterval> | null = null
let countdownTimeoutId: ReturnType<typeof setTimeout> | null = null

const doctorId = computed(() => {
  const rawId = route.params.id
  if (typeof rawId !== 'string') return null
  const parsed = Number.parseInt(rawId, 10)
  return Number.isNaN(parsed) ? null : parsed
})

const availableRegions = computed<SelectableRegion[]>(() => {
  if (!doctorData.value) {
    return RegionsStore.map((region, index) => ({ regionId: index + 1, name: region.name }))
  }
  return RegionsStore
    .map((region, index) => ({ regionId: index + 1, name: region.name }))
    .filter(region => region.regionId !== doctorData.value?.regionId)
})

const backToDoctorsList = () => {
  router.push('/doctor/allDoctors')
}

const loadDoctor = () => {
  if (!doctorId.value) {
    doctorData.value = null
    return
  }
  doctorData.value = mockPatientApi.getDoctorById(doctorId.value)
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

const openRegionPicker = () => {
  if (!doctorData.value) return
  targetRegionId.value = ''
  isRegionSelectOpen.value = false
  isRegionPickerOpen.value = true
}

const proceedToRegionConfirmation = () => {
  if (!targetRegionId.value || !doctorData.value) return
  isRegionPickerOpen.value = false
  isTransferConfirmOpen.value = true
}

const finalizeRegionTransfer = () => {
  if (!doctorData.value || !targetRegionId.value || !doctorId.value) {
    resetTransferFlow()
    return
  }

  const nextRegionId = Number.parseInt(targetRegionId.value, 10)
  if (Number.isNaN(nextRegionId)) {
    resetTransferFlow()
    return
  }

  const updated = mockPatientApi.updateDoctorRegion(doctorId.value, nextRegionId)
  if (updated) {
    doctorData.value = updated
    if (authStore.user.value?.id === updated.id) {
      authStore.updateDoctorRegion(updated.regionId, updated.regionName)
    }
  }

  resetTransferFlow()
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

const selectedRegionName = computed(() => {
  if (!targetRegionId.value) return ''
  const regionId = Number.parseInt(targetRegionId.value, 10)
  if (Number.isNaN(regionId)) return ''
  return RegionsStore[regionId - 1]?.name ?? ''
})

watch(
  () => doctorId.value,
  () => {
    resetTransferFlow()
    loadDoctor()
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  clearTransferTimers()
})
</script>

<template>
  <div class="p-4 sm:p-6 lg:p-8">
    <div class="mb-6">
      <div class="mb-2 flex items-start gap-3 sm:gap-4">
        <Undo2 class="mt-1 cursor-pointer" @click="backToDoctorsList" />
        <div>
          <h1 class="text-2xl font-bold text-foreground">Карточка врача</h1>
          <p class="text-muted-foreground">Персональные данные врача</p>
        </div>
      </div>
    </div>

    <Card v-if="doctorData" class="w-full max-w-none" title="Персональные данные" description="Основная информация о враче">
      <div class="space-y-6">
        <div class="grid gap-6 lg:grid-cols-2">
          <div class="space-y-4">
            <div class="flex items-start gap-3">
              <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-primary/10">
                <User class="h-5 w-5 text-primary" />
              </div>
              <div>
                <p class="text-sm text-muted-foreground">ФИО</p>
                <p class="font-medium text-foreground">{{ doctorData.fullName }}</p>
              </div>
            </div>
            <div class="flex items-start gap-3">
              <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-primary/10">
                <Stethoscope class="h-5 w-5 text-primary" />
              </div>
              <div>
                <p class="text-sm text-muted-foreground">Специальность</p>
                <p class="font-medium text-foreground">{{ doctorData.specialty }}</p>
              </div>
            </div>

            <div class="flex items-start gap-3">
              <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-primary/10">
                <Hospital class="h-5 w-5 text-primary" />
              </div>
              <div>
                <p class="text-sm text-muted-foreground">Место работы</p>
                <p class="font-medium text-foreground">{{ doctorData.workplace }}</p>
              </div>
            </div>

            <div class="flex items-start gap-3">
              <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-primary/10">
                <MapPin class="h-5 w-5 text-primary" />
              </div>
              <div>
                <p class="text-sm text-muted-foreground">Регион</p>
                <div class="flex flex-wrap items-center gap-2">
                  <p class="font-medium text-foreground">{{ doctorData.regionName }}</p>
                  <Badge variant="default" class="cursor-pointer text-xs" role="button" tabindex="0" @click="openRegionPicker">
                    Изменить
                  </Badge>
                </div>
              </div>
            </div>


          </div>

          <div class="space-y-4">


            <div class="flex items-start gap-3">
              <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-primary/10">
                <Mail class="h-5 w-5 text-primary" />
              </div>
              <div>
                <p class="text-sm text-muted-foreground">Email</p>
                <p class="font-medium text-foreground break-all">{{ doctorData.email }}</p>
              </div>
            </div>

          </div>
        </div>
      </div>
    </Card>

    <Card v-else class="max-w-xl">
      <div class="p-6 text-muted-foreground">Данные врача отсутствуют</div>
    </Card>

    <Dialog v-model="isRegionPickerOpen" :content-class="isRegionSelectOpen ? 'details-scroll sm:max-w-md sm:min-h-[26rem]' : 'details-scroll sm:max-w-md'">
      <div class="space-y-4">
        <div>
          <h3 class="text-base font-semibold leading-tight text-foreground sm:text-lg">Изменить регион врача</h3>
          <p class="text-sm text-muted-foreground">Выберите новый регион для врача</p>
        </div>

        <Select v-model="targetRegionId" v-model:open="isRegionSelectOpen">
          <SelectTrigger>
            <SelectValue placeholder="Выберите регион" />
          </SelectTrigger>
          <SelectContent class="z-[4000]">
            <SelectItem v-for="region in availableRegions" :key="region.regionId" :value="String(region.regionId)">
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
            Вы уверены, что хотите перевести врача в регион {{ selectedRegionName }}?
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
            До завершения отсчёта можно отменить перевод врача в регион {{ selectedRegionName }}.
          </p>
        </div>

        <div class="h-2 overflow-hidden rounded-full bg-secondary">
          <div class="h-full bg-primary transition-all duration-1000" :style="{ width: `${(countdownSeconds / 10) * 100}%` }" />
        </div>

        <div class="flex justify-end">
          <Button type="button" variant="outline" @click="cancelRegionTransfer">Отменить</Button>
        </div>
      </div>
    </Dialog>
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
