<script setup lang="ts" >
import { ref } from "vue"
import Card from "@/components/ui/card.vue";
import Input from '@/components/ui/input.vue'
import Button from '@/components/ui/button.vue'
import Select from '@/components/ui/select/select.vue'


import Field from "@/components/ui/field/field.vue";
import FieldLabel from "@/components/ui/field/field-label.vue";
import { Upload, FileText, X, CheckCircle, Heart } from "lucide-vue-next"
import SelectTrigger from "@/components/ui/select/selectTrigger.vue";
import SelectValue from "@/components/ui/select/selectValue.vue";
import SelectContent from "@/components/ui/select/selectContent.vue";
import SelectItem from "@/components/ui/select/selectItem.vue";
import { useExaminationStore } from '../../stores/examinationStore'

// Типы обследований
const examTypes = [
  "ЭхоКГ",
  "МРТ сердца",
  "КТ сердца",
  "ЭКГ",
  "Холтеровское мониторирование",
  "Нагрузочный тест",
  "Анализ крови",
  "BNP / NT-proBNP",
  "Другое",
]



// Состояние формы
const files = ref<File[]>([])
const submitted = ref(false)
const examType = ref("")
const examDate = ref("")
const clinic = ref("")
const doctor = ref("")
const { addExamination } = useExaminationStore()

// Обработчики
const handleFileChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  if (target.files) {
    files.value = [...files.value, ...Array.from(target.files)]
  }
}

const removeFile = (index: number) => {
  files.value = files.value.filter((_, i) => i !== index)
}

const handleSubmit = (e: Event) => {
  e.preventDefault()
  if (!examType.value || !examDate.value || !clinic.value.trim() || !doctor.value.trim()) {
    alert('Заполните обязательные поля: тип обследования, дату, медицинское учреждение и врача')
    return
  }
  addExamination({
    type: examType.value,
    date: new Date(examDate.value).toLocaleDateString('ru-RU'),
    doctor: doctor.value,
    clinic: clinic.value,
    hasFile: files.value.length > 0,
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
        <Button @click="() => { submitted = false; files = [] }">
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
            <!-- Тип и дата обследования -->
            <div class="grid sm:grid-cols-2 gap-4">
              <Field>
                <FieldLabel>Тип обследования *</FieldLabel>
                <Select v-model="examType">
                  <SelectTrigger>
                    <SelectValue placeholder="Выберите тип" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem v-for="type in examTypes" :key="type" :value="type">
                      {{ type }}
                    </SelectItem>
                  </SelectContent>
                </Select>
              </Field>

              <Field>
                <FieldLabel>Дата обследования *</FieldLabel>
                <Input v-model="examDate" type="date" required />
              </Field>
            </div>

            <Field>
              <FieldLabel>Медицинское учреждение *</FieldLabel>
              <Input v-model="clinic" placeholder="Название клиники или больницы" required />
            </Field>

            <Field>
              <FieldLabel>Врач *</FieldLabel>
              <Input v-model="doctor" placeholder="ФИО врача" required />
            </Field>


            <div
                v-if="['ЭхоКГ'].includes(examType)"
                class="p-4 bg-secondary/30 rounded-xl space-y-4"
            >
              <h3 class="font-medium text-foreground flex items-center gap-2">
                <Heart class="w-4 h-4 text-red-500" />
                Параметры {{ examType }}
              </h3>

              <div class="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
                <Field>
                  <FieldLabel>Фракция выброса ПЖ (%)</FieldLabel>
                  <Input type="number" placeholder="напр. 45" min="0" max="100" />
                </Field>
                <Field>
                  <FieldLabel>Конечно-диастолический объём ПЖ (мл)</FieldLabel>
                  <Input type="number" placeholder="напр. 150" min="0" />
                </Field>
                <Field>
                  <FieldLabel>Конечно-систолический объём ПЖ (мл)</FieldLabel>
                  <Input type="number" placeholder="напр. 80" min="0" />
                </Field>
                <!-- … остальные поля сердца … -->
              </div>
            </div>

            <!-- Файлы -->
            <div>
              <FieldLabel class="mb-2 block">Прикрепить файлы</FieldLabel>
              <div
                  class="border-2 border-dashed border-border rounded-xl p-8 text-center hover:border-primary/50 transition-colors"
              >
                <input
                    type="file"
                    multiple
                    accept=".pdf,.jpg,.jpeg,.png"
                    id="file-upload"
                    class="hidden"
                    @change="handleFileChange"
                />
                <label for="file-upload" class="cursor-pointer">
                  <Upload class="w-10 h-10 text-muted-foreground mx-auto mb-4" />
                  <p class="text-foreground font-medium mb-1">
                    Нажмите для загрузки или перетащите файлы
                  </p>
                  <p class="text-sm text-muted-foreground">PDF, JPG, PNG до 50 МБ</p>
                </label>
              </div>

              <!-- Список файлов -->
              <div v-if="files.length" class="mt-4 space-y-2">
                <div
                    v-for="(file, index) in files"
                    :key="index"
                    class="flex items-center justify-between p-3 bg-secondary/50 rounded-lg"
                >
                  <div class="flex items-center gap-3">
                    <FileText class="w-5 h-5 text-primary" />
                    <span class="text-sm text-foreground">{{ file.name }}</span>
                    <span class="text-xs text-muted-foreground">
                      {{ (file.size / 1024 / 1024).toFixed(2) }} МБ
                    </span>
                  </div>
                  <button
                      type="button"
                      @click="removeFile(index)"
                      class="p-1 hover:bg-secondary rounded"
                  >
                    <X class="w-4 h-4 text-muted-foreground" />
                  </button>
                </div>
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