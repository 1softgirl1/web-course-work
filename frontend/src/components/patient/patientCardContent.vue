<script setup lang="ts">
import { computed, ref, useSlots } from 'vue'
import Card from '@/components/ui/card.vue'
import Badge from '@/components/ui/badge.vue'
import Dialog from '@/components/ui/dialog.vue'
import ChangePasswordCard from '@/components/profile/changePasswordCard.vue'
import {
  Activity,
  Calendar,
  FileText,
  Heart,
  KeyRound,
  MapPin,
  Pill,
  ScanHeartIcon,
  Stethoscope,
  User,
} from 'lucide-vue-next'
import type { Patient } from '@/stores/patientStore'

const props = withDefaults(
  defineProps<{
    patient: Patient
    canShowPatientFullName?: boolean
    showRegionHelpBadge?: boolean
    canChangeOwnPassword?: boolean
  }>(),
  {
    canShowPatientFullName: true,
    showRegionHelpBadge: false,
    canChangeOwnPassword: false,
  }
)

const fullName = computed(() => {
  const parts = [props.patient.lastName, props.patient.firstName, props.patient.middleName].filter(Boolean)
  if (parts.length > 0) return parts.join(' ')
  return props.patient.fullName || 'Нет данных'
})

const birthDateLabel = computed(() => props.patient.birthDate || 'Нет данных')

const valveName = computed(() => props.patient.valve.name || 'Не указано')
const valveSize = computed(() => props.patient.valve.size || 'Не указано')
const valveMaterial = computed(() => props.patient.valve.material || 'Не указано')

const isRegionChangeOpen = ref(false)
const isPasswordModalOpen = ref(false)
const slots = useSlots()

const canShowPasswordBlock = computed(() => {
  return props.canChangeOwnPassword || Boolean(slots['password-action'])
})
</script>

<template>
  <div class="min-w-0 space-y-6">
    <Card
      class="w-full max-w-none min-w-0 overflow-hidden"
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
              <p class="font-medium text-foreground break-words">
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
                <p class="font-medium text-foreground break-words">{{ patient.region }}</p>
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
              <Badge variant="outline" class="text-xs">{{ patient.code }}</Badge>
            </div>
          </div>

          <div v-if="canShowPasswordBlock" class="flex items-start gap-3">
            <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-primary/10">
              <KeyRound class="h-5 w-5 text-primary" />
            </div>
            <div class="flex items-end gap-2">
              <div>
                <p class="text-sm text-muted-foreground">Пароль</p>
                <p class="font-medium text-foreground">********</p>
              </div>
              <slot name="password-action">
                <Badge v-if="canChangeOwnPassword" variant="outline" class="cursor-pointer" @click="isPasswordModalOpen = true">Изменить</Badge>
              </slot>
            </div>
          </div>
        </div>
      </div>
    </Card>

    <Card
      class="w-full max-w-none min-w-0 overflow-hidden"
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
            <p class="font-medium text-foreground break-words">{{ patient.diagnosis || 'Нет данных' }}</p>
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

          <div v-if="patient.operationDetails.length > 0" class="space-y-3">
            <div
              v-for="(operation, index) in patient.operationDetails"
              :key="`operation-${index}`"
              class="rounded-xl border border-border/80 bg-muted/20 p-4"
            >

              <div class="mb-3 flex items-center  gap-3">
                <Badge variant="outline" class="text-xs">Операция {{ index + 1 }}</Badge>
                <p class="font-semibold text-foreground break-words">{{ operation.name }}</p>
              </div>

              <div class="grid gap-2 sm:grid-cols-3">
                <div class="rounded-lg bg-background px-3 py-2">
                  <p class="text-xs text-muted-foreground">Наркоз</p>
                  <p class="text-sm font-medium text-foreground">{{ operation.anesthesia || 'Не указано' }}</p>
                </div>

                <div class="rounded-lg bg-background px-3 py-2">
                  <p class="text-xs text-muted-foreground">Продолжительность</p>
                  <p class="text-sm font-medium text-foreground">{{ operation.duration || 'Не указано' }}</p>
                </div>

                <div class="rounded-lg bg-background px-3 py-2">
                  <p class="text-xs text-muted-foreground">Система доставки</p>
                  <p class="text-sm font-medium text-foreground break-words">{{ operation.deliverySystem || 'Не указано' }}</p>
                </div>
              </div>
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
              <div class="mt-2 grid gap-2 sm:grid-cols-3">
                <div class="rounded-lg bg-background px-3 py-2">
                  <p class="text-xs text-muted-foreground">Название</p>
                  <p class="text-sm font-medium text-foreground">{{ valveName }}</p>
                </div>
                <div class="rounded-lg bg-background px-3 py-2">
                  <p class="text-xs text-muted-foreground">Размер</p>
                  <p class="text-sm font-medium text-foreground">{{ valveSize }}</p>
                </div>
                <div class="rounded-lg bg-background px-3 py-2">
                  <p class="text-xs text-muted-foreground">Материал</p>
                  <p class="text-sm font-medium text-foreground">{{ valveMaterial }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="flex items-start gap-3">
          <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-red-500/10">
            <Pill class="h-5 w-5 text-red-500" />
          </div>
          <div class="mb-5">
            <p class="text-sm text-muted-foreground">Медикаменты</p>
            <p class="font-medium text-foreground break-words">{{ patient.medications || 'Нет данных' }}</p>
          </div>
        </div>
      </div>
    </Card>

    <Dialog v-if="canChangeOwnPassword" v-model="isPasswordModalOpen" content-class="sm:max-w-md">
      <div class="space-y-4">
        <h3 class="text-lg font-semibold text-foreground">Изменение пароля</h3>
        <ChangePasswordCard />
      </div>
    </Dialog>

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
