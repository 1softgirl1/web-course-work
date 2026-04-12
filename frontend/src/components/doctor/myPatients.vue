<script setup lang="ts">
import { ref, computed } from 'vue'



import Card from '@/components/ui/card.vue'

import Button from '@/components/ui/button.vue'
import Input from '@/components/ui/input.vue'
import Badge from '@/components/ui/badge.vue'
import Select from '@/components/ui/select/select.vue'
import SelectTrigger from '@/components/ui/select/selectTrigger.vue'
import SelectValue from '@/components/ui/select/selectValue.vue'
import SelectContent from '@/components/ui/select/selectContent.vue'
import SelectItem from '@/components/ui/select/selectItem.vue'

import Table from '@/components/ui/table/table.vue'
import TableBody from '@/components/ui/table/tableBody.vue'
import TableCell from '@/components/ui/table/tableCell.vue'
import TableHead from '@/components/ui/table/tableHead.vue'
import TableHeader from '@/components/ui/table/tableHeader.vue'
import TableRow from '@/components/ui/table/tableRow.vue'
import { usePatientStore, type Patient } from '@/stores/patientStore'

import { Search, Download, Calendar, MapPin, Plus, Funnel } from 'lucide-vue-next';


/* ---------------- TYPES ---------------- */

type ExamStatus = 'green' | 'yellow' | 'red'
const patientStore = usePatientStore()

/* ---------------- STATE ---------------- */

const searchQuery = ref<string>('')
const diagnosisQuery = ref<string>('all')
const regionQuery = ref<string>('all')

const diagnosisOptions = computed<string[]>(() => {
  return [...new Set(patientStore.patients.map(patient => patient.diagnosis))]
})

const regionOptions = computed<string[]>(() => {
  return [...new Set(patientStore.patients.map(patient => patient.region))]
})

/* ---------------- COMPUTED ---------------- */

const filteredData = computed<Patient[]>(() => {
  const q = searchQuery.value.toLowerCase().trim()

  return patientStore.patients.filter(patient => {
    const matchesSearch =
        patient.code.toLowerCase().includes(q) ||
        patient.fullName.toLowerCase().includes(q) ||
        patient.diagnosis.toLowerCase().includes(q) ||
        patient.region.toLowerCase().includes(q)

    const matchesDiagnosis = diagnosisQuery.value === 'all' || patient.diagnosis === diagnosisQuery.value
    const matchesRegion = regionQuery.value === 'all' || patient.region === regionQuery.value

    return matchesSearch && matchesDiagnosis && matchesRegion
  })
})

const parseExamDate = (value: string): Date | null => {
  const [day, month, year] = value.split('.').map(Number)
  if (!day || !month || !year) return null
  const date = new Date(year, month - 1, day)
  return Number.isNaN(date.getTime()) ? null : date
}

const getExamStatus = (lastExam: string): ExamStatus => {
  const examDate = parseExamDate(lastExam)
  if (!examDate) return 'red'

  const now = new Date()
  const diffMs = now.getTime() - examDate.getTime()
  const diffMonths = diffMs / (1000 * 60 * 60 * 24 * 30.44)

  if (diffMonths < 3) return 'green'
  if (diffMonths <= 6) return 'yellow'
  return 'red'
}

const getExamStatusDotClass = (status: ExamStatus): string => {
  if (status === 'green') return 'bg-green-300'
  if (status === 'yellow') return 'bg-yellow-300'
  return 'bg-red-400'
}
</script>

<template>
  <div class="p-4 sm:p-6 lg:p-8">

    <!-- HEADER -->
    <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
      <div>
        <h1 class="text-2xl font-bold text-foreground">Все пациенты</h1>
        <p class="text-muted-foreground">
          Кемерово — {{ filteredData.length }} пациентов
        </p>
      </div>

      <div class="flex items-center gap-2">
        <router-link to="/doctor/myPatients/addPatient">
          <Button variant="default">
            <Plus class="w-4 h-4 mr-2" />
            Добавить пациента
          </Button>
        </router-link>

        <Button variant="outline">
          <Download class="w-4 h-4 mr-2" />
          Выгрузить в Excel
        </Button>
      </div>

    </div>

    <!-- SEARCH -->
    <div class="mb-6">
      <div class="grid grid-cols-1 md:grid-cols-3 gap-3">
        <div class="relative">
          <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
          <Input
              v-model="searchQuery"
              type="text"
              placeholder="Поиск по коду, ФИО, диагнозу или региону..."
              class="pl-10"
          />
        </div>

        <Select v-model="diagnosisQuery">
          <Funnel class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
          <SelectTrigger>
            <SelectValue placeholder="Все диагнозы" class="pl-10" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">Все диагнозы</SelectItem>
            <SelectItem v-for="diagnosis in diagnosisOptions" :key="diagnosis" :value="diagnosis">
              {{ diagnosis }}
            </SelectItem>
          </SelectContent>
        </Select>

        <Select v-model="regionQuery">
          <MapPin class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
          <SelectTrigger>
            <SelectValue placeholder="Все регионы" class="pl-10"/>
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">Все регионы</SelectItem>
            <SelectItem v-for="region in regionOptions" :key="region" :value="region">
              {{ region }}
            </SelectItem>
          </SelectContent>
        </Select>
      </div>
    </div>

    <div class="flex items-center gap-6  text-sm text-muted-foreground mb-4">
      <div class="flex items-center gap-2">
         <span
             class="inline-block w-2.5 h-2.5 rounded-full"
             :class="getExamStatusDotClass('green')"
         />
        <p>Обследование менее 3 месяцев назад</p>
      </div>
      <div class="flex items-center gap-2">
        <span
            class="inline-block w-2.5 h-2.5 rounded-full"
            :class="getExamStatusDotClass('yellow')"
        />
        <p>Обследование менее 3-6 месяцев назад</p>
      </div>
      <div class="flex items-center gap-2">
        <span
            class="inline-block w-2.5 h-2.5 rounded-full"
            :class="getExamStatusDotClass('red')"
        />
        <p>Обследование более 6 месяцев назад</p>
      </div>



    </div>

    <!-- TABLE -->
    <Card>
      <div>
        <div class="overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Код пациента</TableHead>
                <TableHead>ФИО</TableHead>
                <TableHead>Возраст</TableHead>
                <TableHead>Диагноз</TableHead>
                <TableHead>Операции</TableHead>
                <TableHead>Последнее обследование</TableHead>
                <TableHead>Регион</TableHead>
              </TableRow>
            </TableHeader>

            <TableBody>
              <TableRow
                  v-for="patient in filteredData"
                  :key="patient.code"
              >
                <TableCell>
                  <Badge variant="outline" >
                    {{ patient.code }}
                  </Badge>
                </TableCell>

                <TableCell>
                  {{ patient.fullName }}
                </TableCell>

                <TableCell>
                  {{ patient.age }} лет
                </TableCell>

                <TableCell class="max-w-[200px] truncate">
                  {{ patient.diagnosis }}
                </TableCell>


                <TableCell>
                  {{ patient.operations }}
                </TableCell>

                <TableCell>
                  <span class="flex items-center gap-2 text-sm">
                    <span
                        class="inline-block w-2.5 h-2.5 rounded-full"
                        :class="getExamStatusDotClass(getExamStatus(patient.lastExam))"
                    />
                    <Calendar class="w-3 h-3" />
                    {{ patient.lastExam }}
                  </span>
                </TableCell>

                <TableCell>
                  <span class="flex items-center gap-1 text-sm">
                    <MapPin class="w-3 h-3" />
                    {{ patient.region }}
                  </span>
                </TableCell>

              </TableRow>
            </TableBody>
          </Table>
        </div>
      </div>
    </Card>

  </div>
</template>