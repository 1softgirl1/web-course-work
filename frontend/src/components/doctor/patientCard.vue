<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Card from '@/components/ui/card.vue'
import { Undo2 } from 'lucide-vue-next'
import { usePatientStore } from '@/stores/patientStore'
import { RegionsStore } from '@/stores/regionsStore.ts'
import PatientCardContent from '@/components/patient/patientCardContent.vue'

const patientStore = usePatientStore()
const route = useRoute()
const router = useRouter()

const patientData = computed(() => {
  const code = typeof route.params.code === 'string' ? route.params.code : ''
  if (!code) return undefined
  return patientStore.patients.find(patient => patient.code === code)
})

const doctorRegionName = computed(() => {
  const savedRegionId = typeof window !== 'undefined' ? localStorage.getItem('selectedRegion') : null
  if (savedRegionId) {
    const region = RegionsStore.find(item => item.id === savedRegionId)
    if (region) return region.name
  }
  return 'Кемерово'
})

const canShowPatientFullName = computed(() => {
  if (!patientData.value) return false
  return patientData.value.region === doctorRegionName.value
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
</script>

<template>
  <div class="p-4 sm:p-6 lg:p-8">

    <div class="mb-6">
      <div class="flex items-s gap-4 mb-2">

          <Undo2 class="mt-1" @click="goBackToList"></Undo2>
        <div>
          <h1 class="text-2xl font-bold text-foreground">Карточка пациента</h1>
          <p class="text-muted-foreground">Персональные данные и медицинская информация</p>
        </div>
      </div>
    </div>

    <PatientCardContent
      v-if="patientData"
      :patient="patientData"
      :can-show-patient-full-name="canShowPatientFullName"
    />

    <Card v-else class="max-w-xl">
      <div class="p-6 text-muted-foreground">Данные пациента отсутствуют</div>
    </Card>
  </div>
</template>
