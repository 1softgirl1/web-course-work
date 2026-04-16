<script setup lang="ts">
import { onMounted, ref } from 'vue'
import Card from '@/components/ui/card.vue'
import DoctorCardContent, { type DoctorProfile } from '@/components/doctor/doctorCardContent.vue'
import { resolveSelectedRegionName } from '@/stores/regionsStore'

const DEFAULT_DOCTOR_PROFILE: Omit<DoctorProfile, 'region'> = {
  fullName: 'Борискова Д.В.',
  email: 'doctor@clinic.ru',
  specialty: 'Кардиолог',
  workplace: 'НМИЦ им. Е.Н. Мешалкина',
}

const doctorData = ref<DoctorProfile | null>(null)

const buildDoctorProfile = (): DoctorProfile => {
  if (typeof window === 'undefined') {
    return {
      ...DEFAULT_DOCTOR_PROFILE,
      region: resolveSelectedRegionName(),
    }
  }

  const fullName = localStorage.getItem('doctorFullName')?.trim() || DEFAULT_DOCTOR_PROFILE.fullName
  const email = localStorage.getItem('doctorEmail')?.trim() || DEFAULT_DOCTOR_PROFILE.email
  const specialty = localStorage.getItem('doctorSpecialty')?.trim() || DEFAULT_DOCTOR_PROFILE.specialty
  const workplace = localStorage.getItem('doctorWorkplace')?.trim() || DEFAULT_DOCTOR_PROFILE.workplace

  return {
    fullName,
    email,
    specialty,
    workplace,
    region: resolveSelectedRegionName(),
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
  <div class="p-4 sm:p-6 lg:p-8">
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
