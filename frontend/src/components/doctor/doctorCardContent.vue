<script setup lang="ts">
import { ref } from 'vue'
import Card from '@/components/ui/card.vue'
import Badge from '@/components/ui/badge.vue'
import Dialog from '@/components/ui/dialog.vue'
import ChangePasswordCard from '@/components/profile/changePasswordCard.vue'
import { Hospital, KeyRound, Mail, MapPin, Stethoscope, User } from 'lucide-vue-next'

export interface DoctorProfile {
  fullName: string
  email: string
  specialty: string
  workplace: string
  region: string
}

defineProps<{
  doctor: DoctorProfile
}>()

const isPasswordModalOpen = ref(false)
</script>

<template>
  <Card
    class="w-full max-w-none"
    title="Персональные данные"
    description="Основная информация о враче"
  >
    <div class="grid gap-6 lg:grid-cols-2">
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
            <Stethoscope class="h-5 w-5 text-primary" />
          </div>
          <div>
            <p class="text-sm text-muted-foreground">Специальность</p>
            <p class="font-medium text-foreground">{{ doctor.specialty || 'Нет данных' }}</p>
          </div>
        </div>

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

      <div class="space-y-4">
        <div class="flex items-start gap-3">
          <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-primary/10">
            <Mail class="h-5 w-5 text-primary" />
          </div>
          <div class="min-w-0 flex-1">
            <p class="text-sm text-muted-foreground">Email</p>
            <p class="mt-1 font-medium text-foreground break-all">{{ doctor.email || 'Нет данных' }}</p>
          </div>
        </div>

        <div class="flex items-start gap-3">
          <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-primary/10">
            <KeyRound class="h-5 w-5 text-primary" />
          </div>

          <div>
            <p class="text-sm text-muted-foreground">Пароль</p>
            <div class="flex flex-wrap items-end gap-2">
              <p class="font-medium text-foreground">********</p>
              <Badge variant="outline" class="cursor-pointer" @click="isPasswordModalOpen = true">Изменить</Badge>
            </div>
          </div>
        </div>
      </div>
    </div>
  </Card>

  <Dialog v-model="isPasswordModalOpen" content-class="sm:max-w-md">
    <div class="space-y-4">
      <h3 class="text-lg font-semibold text-foreground">Изменение пароля</h3>
      <ChangePasswordCard />
    </div>
  </Dialog>
</template>

