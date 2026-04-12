<script setup lang="ts">
import { computed } from 'vue'
import Card from '@/components/ui/card.vue'
import Badge from '@/components/ui/badge.vue'
import {
  User,
  MapPin,
  Calendar,
  FileText,
  ScanHeartIcon,
  Heart,
  Pill,
  Stethoscope,
  Activity,
} from 'lucide-vue-next'
import type { Patient } from '@/stores/patientStore'

const props = withDefaults(
  defineProps<{
    patient: Patient
    canShowPatientFullName?: boolean
  }>(),
  {
    canShowPatientFullName: true,
  }
)

const fullName = computed(() => {
  const parts = [props.patient.lastName, props.patient.firstName, props.patient.middleName].filter(Boolean)
  if (parts.length > 0) return parts.join(' ')
  return props.patient.fullName || 'Нет данных'
})

const birthDateLabel = computed(() => {
  return props.patient.birthDate || 'Нет данных'
})

const valveLabel = computed(() => {
  const values = [props.patient.valve.name, props.patient.valve.size, props.patient.valve.material].filter(Boolean)
  return values.length > 0 ? values.join(', ') : 'Нет данных'
})
</script>

<template>
  <div class="space-y-6">
    <Card
      class="w-full max-w-none"
      title="Персональные данные"
      description="Основная информация о пациенте"
    >
      <div class="grid gap-6 lg:grid-cols-2">
        <div class="space-y-4">
          <div class="flex items-start gap-3">
            <div class="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center shrink-0">
              <User class="w-5 h-5 text-primary" />
            </div>
            <div>
              <p class="text-sm text-muted-foreground">ФИО</p>
              <p class="font-medium text-foreground">
                {{ canShowPatientFullName ? fullName : 'Скрыто для другого региона' }}
              </p>
            </div>
          </div>

          <div class="flex items-start gap-3">
            <div class="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center shrink-0">
              <Calendar class="w-5 h-5 text-primary" />
            </div>
            <div>
              <p class="text-sm text-muted-foreground">Дата рождения</p>
              <div class="flex items-baseline gap-2">
                <p class="font-medium text-foreground">{{ birthDateLabel }}</p>
                <p class="text-sm text-muted-foreground">{{ patient.age }} лет</p>
              </div>
            </div>
          </div>

          <div class="flex items-start gap-3">
            <div class="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center shrink-0">
              <MapPin class="w-5 h-5 text-primary" />
            </div>
            <div>
              <p class="text-sm text-muted-foreground">Регион</p>
              <p class="font-medium text-foreground">{{ patient.region }}</p>
            </div>
          </div>

          <div class="flex items-start gap-3">
            <div class="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center shrink-0">
              <FileText class="w-5 h-5 text-primary" />
            </div>
            <div>
              <p class="text-sm text-muted-foreground">Последнее обследование</p>
              <p class="font-medium text-foreground">{{ patient.lastExam }}</p>
            </div>
          </div>
        </div>

        <div class="space-y-4">
          <div class="flex items-start gap-3">
            <div class="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center shrink-0">
              <ScanHeartIcon class="w-5 h-5 text-primary" />
            </div>
            <div>
              <p class="text-sm text-muted-foreground">Код пациента</p>
              <Badge variant="outline" class="text-xs">
                {{ patient.code }}
              </Badge>
            </div>
          </div>
        </div>
      </div>
    </Card>

    <Card
      class="w-full max-w-none"
      title="Медицинская информация"
      description="Диагноз, операции и медикаменты"
    >
      <div class="space-y-6">
        <div class="flex items-start gap-3">
          <div class="w-10 h-10 rounded-xl bg-red-500/10 flex items-center justify-center shrink-0">
            <Heart class="w-5 h-5 text-red-500" />
          </div>
          <div>
            <p class="text-sm text-muted-foreground">Диагноз</p>
            <p class="font-medium text-foreground">{{ patient.diagnosis || 'Нет данных' }}</p>
          </div>
        </div>

        <div>
          <div class="flex items-start gap-3">
            <div class="w-10 h-10 rounded-xl bg-red-500/10 flex items-center justify-center shrink-0">
              <Activity class="w-5 h-5 text-red-500" />
            </div>
            <div class="mb-5">
              <p class="text-sm text-muted-foreground">Операции</p>
              <p class="font-medium text-foreground">Всего операций: {{ patient.operations || 'Нет данных' }}</p>
            </div>
          </div>

          <div v-if="patient.operationDetails.length > 0" class="space-y-2">
            <div
              v-for="(operation, index) in patient.operationDetails"
              :key="`operation-${index}`"
              class="rounded-lg border border-border p-3"
            >
              <p class="font-medium text-foreground">{{ operation.name }}</p>
              <p class="text-sm text-muted-foreground mt-1">
                - Наркоз: {{ operation.anesthesia }} <br />
                - Продолжительность: {{ operation.duration }} <br />
                - Система доставки: {{ operation.deliverySystem }}
              </p>
            </div>
          </div>
          <p v-else class="text-sm text-muted-foreground">Нет данных</p>
        </div>

        <div>
          <div class="flex items-start gap-3 mb-3">
            <div class="w-10 h-10 rounded-xl bg-red-500/10 flex items-center justify-center shrink-0">
              <Stethoscope class="w-5 h-5 text-red-500" />
            </div>
            <div>
              <p class="text-sm text-muted-foreground">Характеристики клапана</p>
              <p class="font-medium text-foreground">{{ valveLabel }}</p>
            </div>
          </div>
        </div>

        <div class="flex items-start gap-3">
          <div class="w-10 h-10 rounded-xl bg-red-500/10 flex items-center justify-center shrink-0">
            <Pill class="w-5 h-5 text-red-500" />
          </div>
          <div class="mb-5">
            <p class="text-sm text-muted-foreground">Медикаменты</p>
            <p class="font-medium text-foreground">{{ patient.medications || 'Нет данных' }}</p>
          </div>
        </div>
      </div>
    </Card>
  </div>
</template>

