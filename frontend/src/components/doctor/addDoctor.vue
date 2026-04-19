<script setup lang="ts">
import { ref } from 'vue'
import { Undo2, UserRoundPlus, CheckCircle } from 'lucide-vue-next'
import Card from '@/components/ui/card.vue'
import Input from '@/components/ui/input.vue'
import Button from '@/components/ui/button.vue'
import Badge from '@/components/ui/badge.vue'
import Field from '@/components/ui/field/field.vue'
import FieldLabel from '@/components/ui/field/field-label.vue'
import Select from '@/components/ui/select/select.vue'
import SelectTrigger from '@/components/ui/select/selectTrigger.vue'
import SelectValue from '@/components/ui/select/selectValue.vue'
import SelectContent from '@/components/ui/select/selectContent.vue'
import SelectItem from '@/components/ui/select/selectItem.vue'
import { RegionsStore } from '@/stores/regionsStore'
import { mockPatientApi } from '@/mocks/openapi/mockPatientApi'

const submitted = ref(false)

const lastName = ref('')
const firstName = ref('')
const middleName = ref('')
const email = ref('')
const specialty = ref('')
const workplace = ref('')
const regionStoreId = ref('')

const createdDoctorEmail = ref('')
const createdDoctorPassword = ref('')
const formError = ref('')

const resolveRegionIdByStoreId = (storeId: string): number => {
  const index = RegionsStore.findIndex(item => item.id === storeId)
  return index >= 0 ? index + 1 : 1
}

const resetForm = () => {
  submitted.value = false
  lastName.value = ''
  firstName.value = ''
  middleName.value = ''
  email.value = ''
  specialty.value = ''
  workplace.value = ''
  regionStoreId.value = ''
  createdDoctorEmail.value = ''
  createdDoctorPassword.value = ''
  formError.value = ''
}

const handleSubmit = () => {
  formError.value = ''

  if (!lastName.value.trim() || !firstName.value.trim() || !email.value.trim() || !specialty.value.trim() || !workplace.value.trim() || !regionStoreId.value) {
    formError.value = 'Заполните все поля формы'
    return
  }

  const normalizedEmail = email.value.trim().toLowerCase()
  const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailPattern.test(normalizedEmail)) {
    formError.value = 'Введите корректный email'
    return
  }

  try {
    const created = mockPatientApi.createDoctor({
      lastName: lastName.value,
      firstName: firstName.value,
      middleName: middleName.value,
      email: normalizedEmail,
      specialty: specialty.value,
      workplace: workplace.value,
      regionId: resolveRegionIdByStoreId(regionStoreId.value),
    })

    createdDoctorEmail.value = created.email
    createdDoctorPassword.value = created.temporaryPassword
    submitted.value = true
  } catch (error) {
    const code = error instanceof Error ? error.message : 'UNKNOWN_ERROR'
    if (code === 'DOCTOR_EMAIL_EXISTS') {
      formError.value = 'Врач с таким email уже существует'
      return
    }

    if (code === 'FORBIDDEN') {
      formError.value = 'Недостаточно прав для добавления врача'
      return
    }

    formError.value = 'Не удалось добавить врача'
  }
}
</script>

<template>
  <div class="p-4 sm:p-6 lg:p-8">
    <Card v-if="submitted" class="mx-auto flex w-full max-w-xl items-center md:mx-0">
      <div class="p-4 text-center sm:p-8">
        <div class="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-green-100">
          <CheckCircle class="h-8 w-8 text-green-600" />
        </div>
        <h2 class="mb-2 text-xl font-semibold text-foreground">Врач успешно добавлен</h2>


        <div class="mb-6 space-y-2 text-left">
          <div class="flex flex-wrap items-center gap-2 sm:gap-4">
            <p class="text-muted-foreground">Логин (email):</p>
            <Badge variant="outline" class="text-sm font-medium">{{ createdDoctorEmail }}</Badge>
          </div>
          <div class="flex flex-wrap items-center gap-2 sm:gap-4">
            <p class="text-muted-foreground">Временный пароль:</p>
            <p class="font-medium text-foreground">{{ createdDoctorPassword }}</p>
          </div>
        </div>

        <Button @click="resetForm">Добавить еще врача</Button>
      </div>
    </Card>

    <div v-else>
      <div class="mb-6">
        <div class="mb-2 flex items-start gap-3 sm:gap-4">
          <router-link to="/doctor/allDoctors">
            <Undo2 class="mt-1" />
          </router-link>
          <div>
            <h1 class="text-2xl font-bold text-foreground">Добавить врача</h1>
            <p class="text-muted-foreground">Создание нового аккаунта врача</p>
          </div>
        </div>
      </div>

      <Card class="mx-auto w-full max-w-3xl md:mx-0">
        <template #header>
          <div class="flex items-start gap-3">
            <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-primary/10">
              <UserRoundPlus class="h-5 w-5 text-primary" />
            </div>
            <div>
              <div class="font-semibold leading-none">Новый врач</div>
              <div class="text-sm text-muted-foreground">Заполните данные для создания врача</div>
            </div>
          </div>
        </template>

        <form class="space-y-4" @submit.prevent="handleSubmit">
          <div class="grid gap-4 sm:grid-cols-2">
            <Field>
              <FieldLabel>Фамилия *</FieldLabel>
              <Input v-model="lastName" placeholder="Иванов" required />
            </Field>

            <Field>
              <FieldLabel>Имя *</FieldLabel>
              <Input v-model="firstName" placeholder="Иван" required />
            </Field>
          </div>

          <Field>
            <FieldLabel>Отчество</FieldLabel>
            <Input v-model="middleName" placeholder="Иванович (при наличии)" />
          </Field>

          <Field>
            <FieldLabel>Email *</FieldLabel>
            <Input v-model="email" type="email" placeholder="doctor@clinic.ru" required />
          </Field>

          <Field>
            <FieldLabel>Специальность *</FieldLabel>
            <Input v-model="specialty" placeholder="Кардиолог" required />
          </Field>

          <Field>
            <FieldLabel>Место работы *</FieldLabel>
            <Input v-model="workplace" placeholder="НМИЦ им. Е.Н. Мешалкина" required />
          </Field>

          <Field>
            <FieldLabel>Регион *</FieldLabel>
            <Select v-model="regionStoreId">
              <SelectTrigger>
                <SelectValue placeholder="Выберите регион" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem v-for="item in RegionsStore" :key="item.id" :value="item.id">
                  {{ item.name }}
                </SelectItem>
              </SelectContent>
            </Select>
          </Field>

          <p v-if="formError" class="text-sm text-destructive">{{ formError }}</p>

          <Button type="submit" class="w-full" size="lg">Добавить врача</Button>
        </form>
      </Card>
    </div>
  </div>
</template>


