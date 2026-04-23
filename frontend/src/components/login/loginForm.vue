<script setup lang="ts">
import Card from '@/components/ui/card.vue'
import Tabs from '@/components/ui/tabs.vue'

import Input from '@/components/ui/input.vue'
import Button from '@/components/ui/button.vue'
import { Eye, EyeOff, Mail, Lock } from 'lucide-vue-next'
import { computed, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import Field from "@/components/ui/field/field.vue";
import FieldLabel from "@/components/ui/field/field-label.vue";
import FieldGroup from "@/components/ui/field/field-group.vue";

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const queryRole = computed(() => String(route.query.role ?? authStore.selectedLoginRole.value))
const activeTab = ref(queryRole.value === 'doctor' ? 'doctor' : 'patient')

watch(
  activeTab,
  (nextRole) => {
    authStore.setSelectedLoginRole(nextRole === 'doctor' ? 'doctor' : 'patient')
  },
  { immediate: true }
)

const tabItems = [
  { value: 'patient', label: 'Пациент' },
  { value: 'doctor', label: 'Врач' },
]

const patientLogin = ref('')
const patientPassword = ref('')
const doctorLogin = ref('')
const doctorPassword = ref('')
const isPatientPasswordVisible = ref(false)
const isDoctorPasswordVisible = ref(false)
const authError = ref('')

async function handleLogin() {
  authError.value = ''
  authStore.setSelectedLoginRole(activeTab.value === 'doctor' ? 'doctor' : 'patient')

  const payload = activeTab.value === 'patient'
    ? { username: patientLogin.value.trim(), password: patientPassword.value }
    : { username: doctorLogin.value.trim(), password: doctorPassword.value }

  if (!payload.username || !payload.password) {
    authError.value = 'Введите логин и пароль'
    return
  }

  try {
    const response = await authStore.login(payload)
    const expectedRole = activeTab.value === 'patient' ? 'PATIENT' : 'DOCTOR'
    const isDoctorRole = response.user.role === 'DOCTOR' || response.user.role === 'DOCTOR_EXTENDED'

    if (
      (expectedRole === 'PATIENT' && response.user.role !== 'PATIENT')
      || (expectedRole === 'DOCTOR' && !isDoctorRole)
    ) {
      await authStore.logout()
      authError.value = 'Выбранная роль не соответствует роли учетной записи. Перейдите на другую вкладку и повторите попытку.'
      return
    }

    if (typeof window !== 'undefined') {
      localStorage.setItem('doctorFullName', response.user.displayName)
    }

    if (response.user.role === 'PATIENT') {
      const patientCode = response.user.patientCode
      router.push(patientCode ? `/patient/myCard/${patientCode}` : '/patient')
      return
    }

    router.push('/doctor')
  } catch {
    authError.value = 'Не удалось выполнить вход. Проверьте учетные данные.'
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
            <FieldLabel for="patient-code">Код пациента</FieldLabel>
            <div class="relative">
              <Mail class="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                v-model="patientLogin"
                id="patient-code"
                type="text"
                placeholder="Код пациента, например PT-7GZVL7PT"
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
                v-model="patientPassword"
                id="patient-password"
                :type="isPatientPasswordVisible ? 'text' : 'password'"
                placeholder="Введите пароль"
                class="h-11 pl-10 pr-10"
                required
              />
              <button
                type="button"
                class="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground transition-colors hover:text-foreground"
                @click="isPatientPasswordVisible = !isPatientPasswordVisible"
              >
                <EyeOff v-if="isPatientPasswordVisible" class="h-4 w-4" />
                <Eye v-else class="h-4 w-4" />
              </button>
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
                v-model="doctorLogin"
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
                v-model="doctorPassword"
                id="doctor-password"
                :type="isDoctorPasswordVisible ? 'text' : 'password'"
                placeholder="Введите пароль"
                class="h-11 pl-10 pr-10"
                required
              />
              <button
                type="button"
                class="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground transition-colors hover:text-foreground"
                @click="isDoctorPasswordVisible = !isDoctorPasswordVisible"
              >
                <EyeOff v-if="isDoctorPasswordVisible" class="h-4 w-4" />
                <Eye v-else class="h-4 w-4" />
              </button>
            </div>
          </Field>
        </FieldGroup>

        <Button type="submit" class="mt-4 h-11 w-full text-base" size="lg">
          Войти как врач
        </Button>
      </form>

      <p v-if="authError" class="mt-3 text-sm text-destructive">
        {{ authError }}
      </p>

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
