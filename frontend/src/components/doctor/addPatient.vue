<script setup lang="ts" >
import { computed, onMounted, ref } from "vue"
import { useRoute } from 'vue-router'
import Card from "@/components/ui/card.vue";
import Input from '@/components/ui/input.vue'
import Button from '@/components/ui/button.vue'
import Select from '@/components/ui/select/select.vue'
import Dialog from '@/components/ui/dialog.vue'


import Field from "@/components/ui/field/field.vue";
import FieldLabel from "@/components/ui/field/field-label.vue";
import { CheckCircle, User, Undo2, Plus, Trash2 } from "lucide-vue-next"
import SelectTrigger from "@/components/ui/select/selectTrigger.vue";
import SelectValue from "@/components/ui/select/selectValue.vue";
import SelectContent from "@/components/ui/select/selectContent.vue";
import SelectItem from "@/components/ui/select/selectItem.vue";
import Badge from "@/components/ui/badge.vue";
import { usePatientStore } from '@/stores/patientStore'
import { useAuthStore } from '@/stores/authStore'

const submitted = ref(false)
const route = useRoute()
const birthDate = ref("")
const regionId = ref("")
const diagnosis = ref("")
const valveName = ref("")
const valveSize = ref("")
const valveMaterial = ref("")
const createdPatientCode = ref("")
const createdPatientPassword = ref("")
const backPath = computed(() => {
  return route.query.from === 'allPatients' ? '/doctor/allPatients' : '/doctor/myPatients'
})
const currentListScope = computed<'own' | 'all'>(() => {
  return route.query.from === 'allPatients' ? 'all' : 'own'
})

interface OperationItem {
  name: string
  anesthesia: string
  duration: string
  deliverySystem: string
}

const createEmptyOperation = (): OperationItem => ({
  name: "",
  anesthesia: "",
  duration: "",
  deliverySystem: "",
})

const operations = ref<OperationItem[]>([createEmptyOperation()])
const medications = ref("")

const patientStore = usePatientStore()
const authStore = useAuthStore()

const availableRegions = computed(() => {
  const unique = new Map<number, string>()
  patientStore.getKnownRegions().forEach(region => {
    unique.set(region.id, region.name)
  })

  const doctorRegionId = authStore.doctorRegionId.value
  const doctorRegionName = authStore.doctorRegionName.value
  if (typeof doctorRegionId === 'number' && doctorRegionId > 0 && doctorRegionName && !unique.has(doctorRegionId)) {
    unique.set(doctorRegionId, doctorRegionName)
  }

  return [...unique.entries()]
    .map(([id, name]) => ({ id, name }))
    .sort((a, b) => a.name.localeCompare(b.name, 'ru'))
})

const addOperation = () => {
  operations.value.push(createEmptyOperation())
}

const removeOperation = (index: number) => {
  if (operations.value.length === 1) {
    operations.value[0] = createEmptyOperation()
    return
  }
  operations.value.splice(index, 1)
}

const getOperationProgress = (operation: OperationItem): number => {
  const values = [operation.name, operation.anesthesia, operation.duration, operation.deliverySystem]
  return values.filter(value => value.trim()).length
}

const getOperationStatusLabel = (operation: OperationItem): string => {
  const progress = getOperationProgress(operation)
  if (progress === 0) return 'Не заполнено'
  if (progress < 4) return 'Заполняется'
  return 'Заполнено'
}

const getOperationStatusClass = (operation: OperationItem): string => {
  const progress = getOperationProgress(operation)
  if (progress === 0) return 'bg-muted text-muted-foreground'
  if (progress < 4) return 'bg-yellow-100 text-yellow-800'
  return 'bg-green-100 text-green-700'
}

const resetForm = () => {
  submitted.value = false
  birthDate.value = ""
  regionId.value = ""
  diagnosis.value = ""
  valveName.value = ""
  valveSize.value = ""
  valveMaterial.value = ""
  operations.value = [createEmptyOperation()]
  medications.value = ""
  createdPatientCode.value = ""
  createdPatientPassword.value = ""
}

const closeSuccessDialog = () => {
  submitted.value = false
  resetForm()
}

const handleSubmit = async (e: Event) => {
  e.preventDefault()

  if (!birthDate.value || !regionId.value || !diagnosis.value) {
    alert('Заполните обязательные поля: дату рождения, регион и диагноз')
    return
  }

  const parsedRegionId = Number.parseInt(regionId.value, 10)
  if (!Number.isFinite(parsedRegionId) || parsedRegionId <= 0) {
    alert('Некорректный регион')
    return
  }

  const normalizedOperations = operations.value
    .map(item => ({
      name: item.name.trim(),
      anesthesia: item.anesthesia.trim(),
      duration: item.duration.trim(),
      deliverySystem: item.deliverySystem.trim(),
    }))

  const hasPartiallyFilledOperation = normalizedOperations.some(item => {
    const values = [item.name, item.anesthesia, item.duration, item.deliverySystem]
    return values.some(Boolean) && values.some(value => !value)
  })

  if (hasPartiallyFilledOperation) {
    alert('Заполните все параметры операции или удалите незаполненную запись')
    return
  }

  const filledOperations = normalizedOperations.filter(item => {
    return item.name && item.anesthesia && item.duration && item.deliverySystem
  })

  const filledMedications = medications.value.trim()

  try {
    const { patient, password } = await patientStore.addPatient(
      {
        birthDate: birthDate.value,
        regionId: parsedRegionId,
        diagnosis: diagnosis.value,
        operations: filledOperations,
        medications: filledMedications,
        valve: {
          name: valveName.value.trim(),
          size: valveSize.value.trim(),
          material: valveMaterial.value.trim(),
        },
      },
      currentListScope.value,
    )

    createdPatientCode.value = patient.code
    createdPatientPassword.value = password
    submitted.value = true
  } catch {
    alert('Не удалось создать пациента. Проверьте заполнение формы и попробуйте снова.')
  }
}

onMounted(async () => {
  try {
    await patientStore.loadKnownRegionsFromDoctorPatients()
  } catch {
    // keep currently available region options
  }

  const doctorRegionId = authStore.doctorRegionId.value
  if (!regionId.value && typeof doctorRegionId === 'number' && doctorRegionId > 0) {
    regionId.value = String(doctorRegionId)
  }
})


</script>

<template>
  <div class="p-4 sm:p-6 lg:p-8">
    <Dialog v-model="submitted" content-class="sm:max-w-xl">
      <div class="p-4 text-center sm:p-8">
        <div class="w-16 h-16 rounded-full bg-green-100 flex items-center justify-center mx-auto mb-4">
          <CheckCircle class="w-8 h-8 text-green-600" />
        </div>
        <h2 class="text-xl font-semibold text-foreground mb-2">Пациент успешно создан</h2>
        <div class="flex flex-col items-center text-muted-foreground mb-6 gap-1">
          <div class="flex flex-wrap items-center justify-center gap-2 sm:gap-4">
            <p class="text-muted-foreground">
              Код пациента:
            </p>
            <Badge variant="outline" class="text-sm font-medium" >
              {{ createdPatientCode }}
            </Badge>
          </div>
          <div class="flex flex-wrap items-center justify-center gap-2 sm:gap-4">
            <p class="text-muted-foreground">
              Пароль пациента:
            </p>
            <p class="text-foreground font-medium">{{ createdPatientPassword }}</p>
          </div>
        </div>
        <div class="flex flex-col gap-2 sm:flex-row sm:justify-center">
          <router-link :to="backPath" class="w-full sm:w-auto">
            <Button variant="outline" class="w-full sm:w-auto">Назад</Button>
          </router-link>
          <Button class="w-full sm:w-auto" @click="closeSuccessDialog">
            Добавить еще
          </Button>
        </div>
      </div>
    </Dialog>

    <!-- Форма загрузки -->
    <div>
      <div class="mb-6">
        <div class="mb-2 flex items-start gap-3 sm:gap-4">
          <router-link :to="backPath">
            <Undo2 class="mt-1"></Undo2>
          </router-link>
          <div>
            <h1 class="text-2xl font-bold text-foreground">Добавить пациента</h1>
            <p class="text-muted-foreground">Добавьте информацию о новом пациенте</p>
          </div>
        </div>
      </div>

      <Card class="mx-auto w-full max-w-3xl md:mx-0" >

        <template #header>
          <div class="flex items-start gap-3">
            <div class="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center shrink-0">
              <User class="w-5 h-5 text-primary" />
            </div>
            <div class="flex-col ">
              <div class="font-semibold leading-none ">
                Новый пациент
              </div>
              <div class="text-sm text-muted-foreground">
                Укажите данные о новом пациенте
              </div>
            </div>
          </div>
        </template>



        <form @submit.prevent="handleSubmit" class="space-y-4">
          <div class="grid sm:grid-cols-2 gap-4">
            <Field>
              <FieldLabel>Дата рождения *</FieldLabel>
              <Input v-model="birthDate" type="date" required />
            </Field>
            <Field>
              <FieldLabel>Регион *</FieldLabel>
              <Select v-model="regionId">
                <SelectTrigger>
                  <SelectValue placeholder="Выберите регион" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem v-for="item in availableRegions" :key="item.id" :value="String(item.id)">
                    {{ item.name }}
                  </SelectItem>
                </SelectContent>
              </Select>
            </Field>
          </div>

          <Field>
            <FieldLabel>Диагноз *</FieldLabel>
            <Input v-model="diagnosis" placeholder="Диагноз" required />
          </Field>

          <div class="space-y-4">
            <div class="flex flex-col gap-3 rounded-xl border border-border bg-muted/30 px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <FieldLabel>Перенесенные операции</FieldLabel>
                <p class="text-xs text-muted-foreground mt-1">Добавьте одну или несколько операций пациента</p>
              </div>
              <Button type="button" variant="outline" size="sm" class="w-full sm:w-auto" @click="addOperation">
                <Plus class="w-4 h-4 mr-1" />
                Добавить операцию
              </Button>
            </div>

            <Card v-for="(operation, index) in operations" :key="index" class="overflow-hidden border border-border/80 shadow-none">
              <div class="flex flex-col gap-2 border-b border-border bg-card px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
                <div class="flex items-center gap-2">
                  <div class="flex h-6 w-6 items-center justify-center rounded-full bg-primary/10 text-xs font-semibold text-primary">
                    {{ index + 1 }}
                  </div>
                  <p class="text-sm font-medium">Операция</p>
                </div>
                <div class="flex items-center justify-between gap-2 sm:justify-start">
                  <span class="rounded-full px-2 py-1 text-xs font-medium" :class="getOperationStatusClass(operation)">
                    {{ getOperationStatusLabel(operation) }}
                  </span>
                  <Button type="button" variant="outline" size="sm" class="text-muted-foreground hover:text-destructive" @click="removeOperation(index)">
                    <Trash2 class="w-4 h-4" />
                </Button>
                </div>
              </div>

              <div class="bg-muted/20 p-4 space-y-3">
                <Field>
                  <FieldLabel>Название операции</FieldLabel>
                  <Input v-model="operation.name" placeholder="Например: Протезирование клапана" />
                </Field>

                <div class="grid sm:grid-cols-3 gap-3">
                  <Field>
                    <FieldLabel>Наркоз</FieldLabel>
                    <Input v-model="operation.anesthesia" placeholder="Общий / местный" />
                  </Field>
                  <Field>
                    <FieldLabel>Продолжительность</FieldLabel>
                    <Input v-model="operation.duration" placeholder="Например: 2 часа" />
                  </Field>
                  <Field>
                    <FieldLabel>Система доставки</FieldLabel>
                    <Input v-model="operation.deliverySystem" placeholder="Например: Катетерная" />
                  </Field>
                </div>
              </div>
            </Card>
          </div>

          <div class="space-y-3">
            <FieldLabel>Характеристики клапана</FieldLabel>
            <div class="grid sm:grid-cols-3 gap-3">
              <Field>
                <FieldLabel>Название</FieldLabel>
                <Input v-model="valveName" placeholder="Название клапана" />
              </Field>
              <Field>
                <FieldLabel>Размер</FieldLabel>
                <Input v-model="valveSize" placeholder="Размер" />
              </Field>
              <Field>
                <FieldLabel>Материал</FieldLabel>
                <Input v-model="valveMaterial" placeholder="Материал" />
              </Field>
            </div>
          </div>

          <div class="space-y-3">
            <Field>
              <FieldLabel>Список медикаментов</FieldLabel>
              <Input v-model="medications" placeholder="Например: Бисопролол 2.5 мг, Аспирин 75 мг" />
            </Field>
          </div>

          <Button type="submit" class="w-full" size="lg">
            Добавить пациента
          </Button>
        </form>

      </Card>
    </div>
  </div>
</template>
