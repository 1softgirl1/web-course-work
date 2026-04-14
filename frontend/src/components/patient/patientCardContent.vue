<script setup lang="ts">
import { computed, ref } from 'vue'
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
import Dialog from "@/components/ui/dialog.vue";

const props = withDefaults(
  defineProps<{
    patient: Patient
    canShowPatientFullName?: boolean
    showRegionHelpBadge?: boolean
  }>(),
  {
    canShowPatientFullName: true,
    showRegionHelpBadge: false,
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

const isRegionChangeOpen = ref(false)
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
                <Badge
                  v-if="showRegionHelpBadge"
                  variant="default"
                  class="cursor-pointer text-xs"
                  role="button"
                  tabindex="0"
                  @click="isRegionChangeOpen = true"
                  @keydown.enter="isRegionChangeOpen = true"
                  @keydown.space.prevent="isRegionChangeOpen = true"
                >
                  Как сменить регион?
                </Badge>
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



    <Dialog v-model="isRegionChangeOpen" content-class="sm:max-w-2xl">
      <div class="space-y-4">
        <div >
          <div class="mb-1 flex items-center gap-2 ">
            <h3 class="text-lg font-semibold text-foreground">Я переехал, как изменить регион?</h3>
          </div>
          <div class="flex flex-row items-center gap-2 font-medium ">
            <div class="bg-primary/10 flex h-10 w-10 shrink-0 items-center justify-center rounded-full my-4 ">
              <p class="text-primary font-semibold">1</p>
            </div>
            <p>Отправьте письмо на электронную почту</p>
          </div>
          <p class="text-sm text-muted-foreground">
            Напишите письмо на адрес
            <span class="text-primary">
              support@your-organization.ru
            </span> с запросом на смену региона по следующему шаблону:
          </p>

          <div class="bg-primary/10 my-4 rounded-xl p-4 text-sm text-muted-foreground">
            <p><span class="text-black">Тема: Запрос на смену региона</span></p>
            <p>Здравствуйте! Прошу изменить мой регион обслуживания на: [укажите нужный регион].</p>
            <p>ФИО: [ваше ФИО]</p>
            <p>Код пациента: [ваш код пациента]</p>
            <p>Дата рождения: [дд.мм.гггг]</p>
            <p>Контактный телефон: [номер телефона]</p>
            <p>Спасибо!</p>
          </div>


          <div class="flex flex-row items-center gap-2 font-medium ">
            <div class="bg-primary/10 flex h-10 w-10 shrink-0 items-center justify-center rounded-full my-4 ">
              <p class="text-primary font-semibold">2</p>
            </div>
            <p>Позвоните и подтвердите заявку</p>
          </div>
          <p class="text-sm text-muted-foreground">
            Позвоните по номеру:
            <span class="text-primary"> 8 (800) 000-00-00 </span><br>
            Сообщите, что вы отправили письмо с заявкой на смену региона, и попросите подтвердить её получение.
          </p>

          <div class="flex flex-row items-center gap-2 font-medium ">
            <div class="bg-primary/10 flex h-10 w-10 shrink-0 items-center justify-center rounded-full my-4 ">
              <p class="text-primary font-semibold">3</p>
            </div>
            <p>Ожидайте и при необходимости напомните</p>
          </div>
          <p class="text-sm text-muted-foreground">
            Ожидайте изменения региона в течение 5  рабочих дней.
            Если по истечении этого срока регион не изменился — позвоните повторно и уточните статус заявки.
          </p>



        </div>

      </div>
    </Dialog>

  </div>
</template>
