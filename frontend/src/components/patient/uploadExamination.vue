<script setup lang="ts" >
import { ref } from "vue"
import Card from "@/components/ui/card.vue";
import Input from '@/components/ui/input.vue'
import Button from '@/components/ui/button.vue'
import Field from "@/components/ui/field/field.vue";
import FieldLabel from "@/components/ui/field/field-label.vue";
import { CheckCircle, Heart } from "lucide-vue-next"
import { useExaminationStore } from '../../stores/examinationStore'

const submitted = ref(false)
const examDate = ref("")
const doctor = ref("")
const conclusion = ref("")
const indicatorValues = ref<string[]>(Array.from({ length: 50 }, () => ""))
const { addExamination } = useExaminationStore()

const indicatorLabels = Array.from({ length: 50 }, (_, index) => `Показатель ${index + 1}`)

const resetForm = () => {
  submitted.value = false
  examDate.value = ""
  doctor.value = ""
  conclusion.value = ""
  indicatorValues.value = Array.from({ length: 50 }, () => "")
}

const handleSubmit = (e: Event) => {
  e.preventDefault()

  if (!examDate.value || !doctor.value.trim()) {
    alert('Заполните обязательные поля: дату обследования и врача')
    return
  }

  const indicators = indicatorValues.value.map(value => Number(value))
  const hasInvalidIndicator = indicators.some(value => Number.isNaN(value))

  if (hasInvalidIndicator) {
    alert('Заполните все 50 показателей числовыми значениями')
    return
  }

  addExamination({
    date: new Date(examDate.value).toLocaleDateString('ru-RU'),
    doctor: doctor.value.trim(),
    conclusion: conclusion.value.trim(),
    indicators,
  })

  submitted.value = true
}
</script>

<template>
  <div class="p-4 sm:p-6 lg:p-8">
    <!-- Успешная отправка -->
    <Card v-if="submitted" class="max-w-xl mx-auto">
      <div class="p-8 text-center">
        <div class="w-16 h-16 rounded-full bg-green-100 flex items-center justify-center mx-auto mb-4">
          <CheckCircle class="w-8 h-8 text-green-600" />
        </div>
        <h2 class="text-xl font-semibold text-foreground mb-2">Обследование успешно загружено</h2>
        <p class="text-muted-foreground mb-6">
          Данные отправлены и будут рассмотрены лечащим врачом.
        </p>
        <Button @click="resetForm">
          Загрузить ещё
        </Button>
      </div>
    </Card>

    <!-- Форма загрузки -->
    <div v-else>
      <div class="mb-6">
        <h1 class="text-2xl font-bold text-foreground">Загрузить обследование</h1>
        <p class="text-muted-foreground">Заполните форму и прикрепите результаты обследования</p>
      </div>

      <Card class="max-w-3xl" >

        <template #header>
          <div class="flex items-start gap-3">
            <!-- Иконка -->
            <div class="w-10 h-10 rounded-xl bg-red-500/10 flex items-center justify-center shrink-0">
              <Heart class="w-5 h-5 text-red-500" />
            </div>

            <!-- Текст -->
            <div class="flex-col ">
              <div class="font-semibold leading-none ">
                Новое обследование
              </div>
              <div class="text-sm text-muted-foreground">
                Укажите параметры и загрузите файлы обследования
              </div>
            </div>
          </div>
        </template>



          <form @submit.prevent="handleSubmit" class="space-y-6">
            <div class="grid sm:grid-cols-2 gap-4">
              <Field>
                <FieldLabel>Дата обследования *</FieldLabel>
                <Input v-model="examDate" type="date" required />
              </Field>

              <Field>
                <FieldLabel>Врач *</FieldLabel>
                <Input v-model="doctor" placeholder="ФИО врача" required />
              </Field>
            </div>

            <Field>
              <FieldLabel>Заключение врача</FieldLabel>
              <Input v-model="conclusion" placeholder="Текст заключения" />
            </Field>

            <div class="p-4 bg-secondary/30 rounded-xl space-y-4">
              <h3 class="font-medium text-foreground flex items-center gap-2">
                <Heart class="w-4 h-4 text-red-500" />
                Численные показатели обследования
              </h3>

              <div class="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
                <Field v-for="(label, index) in indicatorLabels" :key="label">
                  <FieldLabel>{{ label }} *</FieldLabel>
                  <Input
                    v-model="indicatorValues[index]"
                    type="number"
                    placeholder="Введите число"
                    required
                  />
                </Field>
              </div>
            </div>

            <Button type="submit" class="w-full" size="lg">
              Загрузить обследование
            </Button>
          </form>

      </Card>
    </div>
  </div>
</template>