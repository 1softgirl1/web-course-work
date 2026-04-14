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
            <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-primary/10">
              <User class="h-5 w-5 text-primary" />
            </div>
            <div>
              <p class="text-sm text-muted-foreground">ФИО</p>
              <p class="font-medium text-foreground">
                {{ canShowPatientFullName ? fullName : 'Скрыто для другого региона' }}
              </p>
            </div>
          </div>

          <div class="flex items-start gap-3">
            <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-primary/10">
              <Calendar class="h-5 w-5 text-primary" />
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
            <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-primary/10">
              <MapPin class="h-5 w-5 text-primary" />
            </div>
            <div class="min-w-0 flex-1">
              <p class="text-sm text-muted-foreground">Регион</p>
              <div class="flex flex-wrap items-center gap-2">
                <p class="font-medium text-foreground">{{ patient.region }}</p>
                <slot name="region-action" />
              </div>
            </div>
          </div>

          <div class="flex items-start gap-3">
            <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-primary/10">
              <FileText class="h-5 w-5 text-primary" />
            </div>
            <div>
              <p class="text-sm text-muted-foreground">Последнее обследование</p>
              <p class="font-medium text-foreground">{{ patient.lastExam }}</p>
            </div>
          </div>
        </div>

        <div class="space-y-4">
          <div class="flex items-start gap-3">
            <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-primary/10">
              <ScanHeartIcon class="h-5 w-5 text-primary" />
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
          <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-red-500/10">
            <Heart class="h-5 w-5 text-red-500" />
          </div>
          <div>
            <p class="text-sm text-muted-foreground">Диагноз</p>
            <p class="font-medium text-foreground">{{ patient.diagnosis || 'Нет данных' }}</p>
          </div>
        </div>

        <div>
          <div class="flex items-start gap-3">
            <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-red-500/10">
              <Activity class="h-5 w-5 text-red-500" />
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
              <p class="mt-1 text-sm text-muted-foreground">
                - Наркоз: {{ operation.anesthesia }} <br />
                - Продолжительность: {{ operation.duration }} <br />
                - Система доставки: {{ operation.deliverySystem }}
              </p>
            </div>
          </div>
          <p v-else class="text-sm text-muted-foreground">Нет данных</p>
        </div>

        <div>
          <div class="mb-3 flex items-start gap-3">
            <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-red-500/10">
              <Stethoscope class="h-5 w-5 text-red-500" />
            </div>
            <div>
              <p class="text-sm text-muted-foreground">Характеристики клапана</p>
              <p class="font-medium text-foreground">{{ valveLabel }}</p>
            </div>
          </div>
        </div>

        <div class="flex items-start gap-3">
          <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-red-500/10">
            <Pill class="h-5 w-5 text-red-500" />
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
