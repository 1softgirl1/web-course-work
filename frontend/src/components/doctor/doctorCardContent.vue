<script setup lang="ts">
import { ref, watch } from 'vue'
import Card from '@/components/ui/card.vue'
import Button from '@/components/ui/button.vue'
import Input from '@/components/ui/input.vue'
import { Hospital, Mail, MapPin, Stethoscope, User } from 'lucide-vue-next'
import Badge from "@/components/ui/badge.vue";

export interface DoctorProfile {
  fullName: string
  email: string
  specialty: string
  workplace: string
  region: string
}

const props = defineProps<{
  doctor: DoctorProfile
}>()

const emit = defineEmits<{
  (e: 'update-email', value: string): void
}>()

const isEmailEditing = ref(false)
const emailDraft = ref(props.doctor.email)
const emailError = ref('')

watch(
  () => props.doctor.email,
  (nextEmail) => {
    if (!isEmailEditing.value) {
      emailDraft.value = nextEmail
    }
  },
  { immediate: true }
)

const startEmailEditing = () => {
  emailDraft.value = props.doctor.email
  emailError.value = ''
  isEmailEditing.value = true
}

const cancelEmailEditing = () => {
  emailDraft.value = props.doctor.email
  emailError.value = ''
  isEmailEditing.value = false
}

const saveEmail = () => {
  const normalizedEmail = emailDraft.value.trim().toLowerCase()
  const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

  if (!emailPattern.test(normalizedEmail)) {
    emailError.value = 'Введите корректный email'
    return
  }

  emit('update-email', normalizedEmail)
  emailError.value = ''
  isEmailEditing.value = false
}
</script>

<template>
  <Card
    class="w-full max-w-none"
    title="Персональные данные"
    description="Основная информация о враче"
  >
    <div class="space-y-6">
      <div class="space-y-4">
        <div class="flex items-start gap-3">
          <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-primary/10">
            <User class="h-5 w-5 text-primary" />
          </div>
          <div>
            <p class="text-sm text-muted-foreground">ФИО</p>
            <p class="font-medium text-foreground">{{ doctor.fullName || 'Нет данных' }}</p>
          </div>
        </div>

        <div class="flex items-start gap-3">
          <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-primary/10">
            <Mail class="h-5 w-5 text-primary" />
          </div>
          <div class="min-w-0 flex-1">
            <p class="text-sm text-muted-foreground">Email</p>

            <div v-if="isEmailEditing" class="mt-1 space-y-2">
              <Input
                v-model="emailDraft"
                type="email"
                placeholder="doctor@clinic.ru"
                class="h-10"
              />
              <p v-if="emailError" class="text-sm text-destructive">{{ emailError }}</p>
              <div class="flex flex-wrap gap-2">
                <Button size="sm" @click="saveEmail">Сохранить</Button>
                <Button size="sm" variant="outline" @click="cancelEmailEditing">Отменить</Button>
              </div>
            </div>

            <div v-else class="mt-1 flex flex-wrap items-center gap-2">
              <p class="font-medium text-foreground break-all">{{ doctor.email || 'Нет данных' }}</p>
              <Badge variant="outline" @click="startEmailEditing">Изменить</Badge>
            </div>
          </div>
        </div>

        <div class="flex items-start gap-3">
          <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-primary/10">
            <Stethoscope class="h-5 w-5 text-primary" />
          </div>
          <div>
            <p class="text-sm text-muted-foreground">Специальность</p>
            <p class="font-medium text-foreground">{{ doctor.specialty || 'Нет данных' }}</p>
          </div>
        </div>
      </div>

      <div class="space-y-4">
        <div class="flex items-start gap-3">
          <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-primary/10">
            <Hospital class="h-5 w-5 text-primary" />
          </div>
          <div>
            <p class="text-sm text-muted-foreground">Место работы</p>
            <p class="font-medium text-foreground">{{ doctor.workplace || 'Нет данных' }}</p>
          </div>
        </div>

        <div class="flex items-start gap-3">
          <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-primary/10">
            <MapPin class="h-5 w-5 text-primary" />
          </div>
          <div>
            <p class="text-sm text-muted-foreground">Регион</p>
            <p class="font-medium text-foreground">{{ doctor.region || 'Нет данных' }}</p>
          </div>
        </div>
      </div>
    </div>
  </Card>
</template>
