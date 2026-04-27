<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import Card from '@/components/ui/card.vue'
import DoctorCardContent, { type DoctorProfile } from '@/components/doctor/doctorCardContent.vue'
import { useAuthStore } from '@/stores/authStore'
import { useDoctorStore, type UiDoctor } from '@/stores/doctorStore'
import { usePatientStore } from '@/stores/patientStore'
import { toHumanErrorMessage } from '@/api/httpClient'

const doctorData = ref<DoctorProfile | null>(null)
const loadError = ref('')

const authStore = useAuthStore()
const doctorStore = useDoctorStore()
const patientStore = usePatientStore()

const resolveRegionLabel = (doctorFromStore: UiDoctor | null): string => {
  if (doctorFromStore?.regionName) return doctorFromStore.regionName

  const knownDoctorRegionId = authStore.doctorRegionId.value
  if (typeof knownDoctorRegionId === 'number' && knownDoctorRegionId > 0) {
    return authStore.doctorRegionName.value
  }

  return 'Нет данных'
}

const buildDoctorProfile = (doctorFromStore: UiDoctor | null): DoctorProfile => {
  const sessionUser = authStore.user.value
  const fallbackEmail = sessionUser?.email?.trim() || ''
  const fallbackName = sessionUser?.displayName?.trim() || ''

  return {
    fullName: doctorFromStore?.fullName || fallbackName || 'Нет данных',
    email: doctorFromStore?.email || fallbackEmail || 'Нет данных',
    specialty: doctorFromStore?.specialty || 'Нет данных',
    workplace: doctorFromStore?.workplace || 'Нет данных',
    region: resolveRegionLabel(doctorFromStore),
  }
}

const canLoadDoctorsDirectory = computed(() => authStore.isDoctorExtended.value)

onMounted(async () => {
  loadError.value = ''
  const sessionEmail = authStore.user.value?.email?.trim() ?? ''

  try {
    await patientStore.loadPatients('own')
  } catch {
    // Doctor card can still be rendered from session data.
  }

  try {
    const doctorFromStore = await doctorStore.resolveCurrentDoctorProfile({
      email: sessionEmail || null,
      canQueryDirectory: canLoadDoctorsDirectory.value,
    })
    doctorData.value = buildDoctorProfile(doctorFromStore)
  } catch (error) {
    loadError.value = toHumanErrorMessage(error)
    doctorData.value = buildDoctorProfile(doctorStore.getDoctorByEmail(sessionEmail))
  }
})
</script>

<template>
  <div class="p-4 sm:p-6 lg:p-8 space-y-6">
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-foreground">Моя карточка</h1>
      <p class="text-muted-foreground">Персональные данные</p>
    </div>

    <p v-if="loadError" class="text-sm text-destructive">{{ loadError }}</p>

    <DoctorCardContent
      v-if="doctorData"
      :doctor="doctorData"
    />

    <Card v-else class="max-w-xl">
      <div class="p-6 text-muted-foreground">Данные врача отсутствуют</div>
    </Card>
  </div>
</template>
