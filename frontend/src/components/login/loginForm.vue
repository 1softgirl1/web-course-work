<script setup lang="ts">
import Card from '@/components/ui/card.vue'
import Tabs from '@/components/ui/tabs.vue'

import Input from '@/components/ui/input.vue'
import Button from '@/components/ui/button.vue'
import { Mail, Lock } from 'lucide-vue-next'
import { computed, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import Field from "@/components/ui/field/field.vue";
import FieldLabel from "@/components/ui/field/field-label.vue";
import FieldGroup from "@/components/ui/field/field-group.vue";

const route = useRoute()
const router = useRouter()

const queryRole = computed(() => String(route.query.role ?? 'patient'))
const activeTab = ref(queryRole.value === 'doctor' ? 'doctor' : 'patient')

const tabItems = [
  { value: 'patient', label: 'Пациент' },
  { value: 'doctor', label: 'Врач' },
]

function handleLogin() {
  if (activeTab.value === 'patient') {
    router.push('/patient')
  } else {
    if (typeof window !== 'undefined') {
      localStorage.setItem('doctorFullName', 'Борискова Д.В.')
    }
    router.push('/doctor')
  }
}
</script>

<template>


    <Card
      class="w-full max-w-md border-border shadow-lg"
      title="Добро пожаловать!"
      description="Войдите для доступа к личному кабинету"
      header-class="gap-1 pb-0"
      title-class="text-center text-lg"
      description-class="text-center"
      content-class="pt-6"
    >
      <Tabs
        v-model="activeTab"
        :items="tabItems"
        list-class="mb-6 grid h-11 w-full grid-cols-2 sm:h-12"
        trigger-class="gap-1.5 text-xs sm:gap-2 sm:text-sm data-[state=active]:bg-primary data-[state=active]:text-primary-foreground"
      />

      <form v-if="activeTab === 'patient'" @submit.prevent="handleLogin">
        <FieldGroup>
          <Field>
            <FieldLabel for="patient-email">Email</FieldLabel>
            <div class="relative">
              <Mail class="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                id="patient-email"
                type="email"
                placeholder="patient@example.com"
                class="h-11 pl-10"
                required
              />
            </div>
          </Field>
          <Field>
            <FieldLabel for="patient-password">Пароль</FieldLabel>
            <div class="relative">
              <Lock class="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                id="patient-password"
                type="password"
                placeholder="Введите пароль"
                class="h-11 pl-10"
                required
              />
            </div>
          </Field>
        </FieldGroup>

        <Button type="submit" class="mt-4 h-11 w-full text-base" size="lg">
          Войти как пациент
        </Button>
      </form>

      <form v-else @submit.prevent="handleLogin">
        <FieldGroup>
          <Field>
            <FieldLabel for="doctor-email">Email</FieldLabel>
            <div class="relative">
              <Mail class="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                id="doctor-email"
                type="email"
                placeholder="doctor@clinic.ru"
                class="h-11 pl-10"
                required
              />
            </div>
          </Field>
          <Field>
            <FieldLabel for="doctor-password">Пароль</FieldLabel>
            <div class="relative">
              <Lock class="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                id="doctor-password"
                type="password"
                placeholder="Введите пароль"
                class="h-11 pl-10"
                required
              />
            </div>
          </Field>
        </FieldGroup>

        <Button type="submit" class="mt-4 h-11 w-full text-base" size="lg">
          Войти как врач
        </Button>
      </form>

      <div class="mt-6 rounded-xl border border-border bg-secondary/50 p-4">
        <p class="text-center text-sm text-muted-foreground">
          Регистрация осуществляется администраторами клиники.
          Если у вас нет доступа, обратитесь в медицинское учреждение.
        </p>
      </div>
    </Card>


</template>

<style scoped>

</style>