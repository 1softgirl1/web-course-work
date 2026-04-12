<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import Card from '@/components/ui/card.vue'
import { usePatientStore } from '@/stores/patientStore'
import PatientCardContent from '@/components/patient/patientCardContent.vue'

const patientStore = usePatientStore()
const route = useRoute()

const patientData = computed(() => {
  const code = typeof route.params.code === 'string' ? route.params.code : ''
  if (!code) return patientStore.patients[0]
  return patientStore.patients.find(patient => patient.code === code)
})
</script>

<template>
  <div class="p-4 sm:p-6 lg:p-8">
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-foreground">Моя карточка</h1>
      <p class="text-muted-foreground">Персональные данные и медицинская информация</p>
    </div>

    <PatientCardContent
      v-if="patientData"
      :patient="patientData"
      :can-show-patient-full-name="true"
    />

    <Card v-else class="max-w-xl">
      <div class="p-6 text-muted-foreground">Данные пациента отсутствуют</div>
    </Card>
  </div>
</template>
