<script setup lang="ts">
import { onMounted, ref } from 'vue'
import Card from '@/components/ui/card.vue'
import DoctorCardContent, { type DoctorProfile } from '@/components/doctor/doctorCardContent.vue'
import { useAuthStore } from '@/stores/authStore'
import { mockPatientApi } from '@/mocks/openapi/mockPatientApi'

const DEFAULT_DOCTOR_PROFILE: Omit<DoctorProfile, 'region'> = {
  fullName: 'Борискова Д.В.',
  email: 'doctor@clinic.ru',
  specialty: 'Кардиолог',
  workplace: 'НМИЦ им. Е.Н. Мешалкина',
}

const doctorData = ref<DoctorProfile | null>(null)
const authStore = useAuthStore()

const buildDoctorProfile = (): DoctorProfile => {
  const doctor = mockPatientApi.getCurrentDoctor()
  const email = authStore.user.value?.email?.trim() || doctor?.email || DEFAULT_DOCTOR_PROFILE.email

  return {
    fullName: authStore.user.value?.displayName || doctor?.fullName || DEFAULT_DOCTOR_PROFILE.fullName,
    email,
    specialty: doctor?.specialty || DEFAULT_DOCTOR_PROFILE.specialty,
    workplace: doctor?.workplace || DEFAULT_DOCTOR_PROFILE.workplace,
    region: authStore.doctorRegionName.value,
  }
}

const handleUpdateEmail = (nextEmail: string) => {
  const normalizedEmail = nextEmail.trim().toLowerCase()
  if (!normalizedEmail || !doctorData.value) return

  doctorData.value.email = normalizedEmail
  if (typeof window !== 'undefined') {
    localStorage.setItem('doctorEmail', normalizedEmail)
  }
}

onMounted(() => {
  doctorData.value = buildDoctorProfile()
})
</script>

<template>
  <div class="p-4 sm:p-6 lg:p-8 space-y-6">
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-foreground">Моя карточка</h1>
      <p class="text-muted-foreground">Персональные данные</p>
    </div>

    <DoctorCardContent
      v-if="doctorData"
      :doctor="doctorData"
      @update-email="handleUpdateEmail"
    />

    <Card v-else class="max-w-xl">
      <div class="p-6 text-muted-foreground">Данные врача отсутствуют</div>
    </Card>

  </div>
</template>
