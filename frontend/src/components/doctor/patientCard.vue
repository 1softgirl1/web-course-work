<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Card from '@/components/ui/card.vue'
import Button from '@/components/ui/button.vue'
import Badge from '@/components/ui/badge.vue'
import Dialog from '@/components/ui/dialog.vue'
import {Undo2, Calendar, FileText, Eye, Plus, Pencil} from 'lucide-vue-next'
import { usePatientStore } from '@/stores/patientStore'
import { useExaminationStore, type Examination, parseExamDate } from '@/stores/examinationStore'
import { RegionsStore } from '@/stores/regionsStore.ts'
import PatientCardContent from '@/components/patient/patientCardContent.vue'
import ExaminationTabe from "@/components/patient/examinationTabe.vue";

const patientStore = usePatientStore()
const route = useRoute()
const router = useRouter()
const { examinations } = useExaminationStore()
const selectedExam = ref<Examination | null>(null)
const isDetailsOpen = ref(false)

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

const patientCode = computed(() => {
  return typeof route.params.code === 'string' ? route.params.code : ''
})

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

const openDetails = (exam: Examination) => {
  selectedExam.value = exam
  isDetailsOpen.value = true
}

const addExaminationPath = computed(() => {
  const code = typeof route.params.code === 'string' ? route.params.code : ''
  return code ? `/doctor/patientCard/${code}/addExamination` : '/doctor/myPatients'
})

const editExaminationPath = (examId: number) => {
  const code = typeof route.params.code === 'string' ? route.params.code : ''
  return code
    ? { path: `/doctor/patientCard/${code}/addExamination`, query: { editId: String(examId) } }
    : { path: '/doctor/myPatients' }
}
</script>

<template>
  <div class="p-4 sm:p-6 lg:p-8">

    <div class="mb-6 ">
      <div class="flex gap-4 mb-2   ">

          <Undo2 class="mt-1" @click="goBackToList"></Undo2>

        <div>
          <h1 class="text-2xl font-bold text-foreground">Карточка пациента</h1>
          <p class="text-muted-foreground">Персональные данные и медицинская информация</p>
        </div>
        <div v-if="patientData && canShowPatientFullName" class="ml-auto flex items-end gap-2">
          <router-link :to="addExaminationPath">
            <Button variant="default">
              <Plus class="w-4 h-4 mr-2" />
              Добавить обследование
            </Button>
          </router-link>
        </div>



      </div>


    </div>

    <div v-if="patientData" class="space-y-6">
      <PatientCardContent
        :patient="patientData"
        :can-show-patient-full-name="canShowPatientFullName"
      />

      <Card
        class="w-full max-w-none"
        title="Обследования пациента"
        description="История обследований пациента"
      >

        <div class="space-y-3" v-if="patientExaminations.length > 0">
          <div
            v-for="exam in patientExaminations"
            :key="`doctor-patient-exam-${exam.id}`"
            class="rounded-lg border border-border p-3"
          >
            <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-1">
              <div class="flex items-start gap-4">
                <div class="w-12 h-12 rounded-xl bg-primary/10 flex items-center justify-center">
                  <FileText class="w-6 h-6 text-primary stroke-1" />
                </div>
                <div>
                  <div class="flex items-center gap-2 mb-1">
                    <h3 class="font-semibold text-foreground truncate">
                      Обследование #{{ exam.id }}
                    </h3>
                    <Badge v-if="latestExamId === exam.id" variant="default" class="text-xs">Новое</Badge>
                  </div>
                  <p class="text-sm text-muted-foreground">Врач: {{ exam.doctor }}</p>
                </div>
              </div>

              <div class="flex items-center gap-3 sm:gap-4">
                <div class="flex items-center gap-2 text-sm text-muted-foreground">
                  <Calendar class="w-4 h-4" />
                  <span>{{ exam.date }}</span>
                </div>
                <router-link :to="editExaminationPath(exam.id)">
                  <Button type="button" variant="ghost" size="icon">
                    <Pencil class="w-4 h-4" />
                  </Button>
                </router-link>
                <Button type="button" variant="ghost" size="icon" @click="openDetails(exam)">
                  <Eye class="w-4 h-4" />
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

      <Dialog v-model="isDetailsOpen" content-class="sm:max-w-4xl">
        <div v-if="selectedExam" class="space-y-4">
          <div>
            <h3 class="text-lg font-semibold text-foreground">Обследование #{{ selectedExam.id }}</h3>
            <p class="text-sm text-muted-foreground">{{ selectedExam.date }} · Врач: {{ selectedExam.doctor }}</p>
            <p class="text-sm text-muted-foreground mt-1">Заключение: {{ selectedExam.conclusion || 'Нет данных' }}</p>
          </div>

          <div>
            <p class="text-sm text-muted-foreground mb-3">Численные показатели</p>
            <div class="details-scroll max-h-[65vh] overflow-y-auto pr-2">
              <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2">
                <div
                  v-for="(value, index) in selectedExam.indicators"
                  :key="`doctor-modal-${selectedExam.id}-indicator-${index}`"
                  class="text-sm rounded-md bg-secondary/40 px-3 py-2"
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

