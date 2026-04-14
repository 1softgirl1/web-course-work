<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
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
import { resolveSelectedRegionName } from '@/stores/regionsStore'
import { Search, Download, Calendar, MapPin, Plus, Funnel } from 'lucide-vue-next'

type ExamStatus = 'green' | 'yellow' | 'red'

const patientStore = usePatientStore()
const route = useRoute()
const router = useRouter()

const getQueryString = (key: string, fallback: string): string => {
  const value = route.query[key]
  return typeof value === 'string' ? value : fallback
}

const getQueryPage = (): number => {
  const raw = route.query.page
  if (typeof raw !== 'string') return 1
  const parsed = Number.parseInt(raw, 10)
  return Number.isNaN(parsed) || parsed < 1 ? 1 : parsed
}

const searchQuery = ref<string>(getQueryString('q', ''))
const diagnosisQuery = ref<string>(getQueryString('diagnosis', 'all'))
const currentPage = ref<number>(getQueryPage())
const itemsPerPage = 300

const doctorRegionName = computed<string>(() => resolveSelectedRegionName())

const regionPatients = computed<Patient[]>(() => {
  return patientStore.patients.filter(patient => patient.region === doctorRegionName.value)
})

const diagnosisOptions = computed<string[]>(() => {
  return [...new Set(regionPatients.value.map(patient => patient.diagnosis))]
})

function parseExamDate(value: string): Date | null {
  const [day, month, year] = value.split('.').map(Number)
  if (!day || !month || !year) return null
  const date = new Date(year, month - 1, day)
  return Number.isNaN(date.getTime()) ? null : date
}

const filteredData = computed<Patient[]>(() => {
  const q = searchQuery.value.toLowerCase().trim()

  const filteredPatients = regionPatients.value.filter(patient => {
    const matchesSearch =
      patient.code.toLowerCase().includes(q) ||
      patient.fullName.toLowerCase().includes(q) ||
      patient.diagnosis.toLowerCase().includes(q) ||
      patient.region.toLowerCase().includes(q)

    const matchesDiagnosis = diagnosisQuery.value === 'all' || patient.diagnosis === diagnosisQuery.value
    return matchesSearch && matchesDiagnosis
  })

  return [...filteredPatients].sort((a, b) => {
    const aTime = parseExamDate(a.lastExam)?.getTime() ?? 0
    const bTime = parseExamDate(b.lastExam)?.getTime() ?? 0
    return bTime - aTime
  })
})

const totalPages = computed<number>(() => {
  return Math.max(1, Math.ceil(filteredData.value.length / itemsPerPage))
})

const paginatedData = computed<Patient[]>(() => {
  const safePage = Math.min(currentPage.value, totalPages.value)
  const start = (safePage - 1) * itemsPerPage
  return filteredData.value.slice(start, start + itemsPerPage)
})

const pageNumbers = computed<number[]>(() => {
  return Array.from({ length: totalPages.value }, (_, index) => index + 1)
})

watch([searchQuery, diagnosisQuery], () => {
  currentPage.value = 1
})

watch(totalPages, newTotalPages => {
  if (currentPage.value > newTotalPages) {
    currentPage.value = newTotalPages
  }
})

const goToPage = (page: number) => {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
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

const openPatientCard = (code: string) => {
  router.push({
    path: `/doctor/patientCard/${code}`,
    query: {
      from: 'myPatients',
      q: searchQuery.value || undefined,
      diagnosis: diagnosisQuery.value !== 'all' ? diagnosisQuery.value : undefined,
      page: String(currentPage.value),
    },
  })
}
</script>

<template>
  <div class="p-4 sm:p-6 lg:p-8">
    <div class="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h1 class="text-2xl font-bold text-foreground">Мои пациенты</h1>
        <p class="text-muted-foreground">{{ doctorRegionName }} — {{ filteredData.length }} пациентов</p>
      </div>

      <div class="flex items-center gap-2">
        <router-link to="/doctor/myPatients/addPatient">
          <Button variant="default">
            <Plus class="mr-2 h-4 w-4" />
            Добавить пациента
          </Button>
        </router-link>

        <Button variant="outline">
          <Download class="mr-2 h-4 w-4" />
          Выгрузить в Excel
        </Button>
      </div>
    </div>

    <div class="mb-6">
      <div class="grid grid-cols-1 gap-3 md:grid-cols-3">
        <div class="relative">
          <Search class="absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-muted-foreground" />
          <Input
            v-model="searchQuery"
            type="text"
            placeholder="Поиск по коду, ФИО, диагнозу или региону..."
            class="pl-10"
          />
        </div>

        <Select v-model="diagnosisQuery">
          <Funnel class="absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-muted-foreground" />
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
      </div>
    </div>

    <div class="mb-4 flex items-center gap-6 text-sm text-muted-foreground">
      <div class="flex items-center gap-2">
        <span class="inline-block h-2.5 w-2.5 rounded-full" :class="getExamStatusDotClass('green')" />
        <p>Обследование менее 3 месяцев назад</p>
      </div>
      <div class="flex items-center gap-2">
        <span class="inline-block h-2.5 w-2.5 rounded-full" :class="getExamStatusDotClass('yellow')" />
        <p>Обследование 3-6 месяцев назад</p>
      </div>
      <div class="flex items-center gap-2">
        <span class="inline-block h-2.5 w-2.5 rounded-full" :class="getExamStatusDotClass('red')" />
        <p>Обследование более 6 месяцев назад</p>
      </div>
    </div>

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
                v-for="patient in paginatedData"
                :key="patient.code"
                class="cursor-pointer"
                @click="openPatientCard(patient.code)"
              >
                <TableCell>
                  <Badge variant="outline">
                    {{ patient.code }}
                  </Badge>
                </TableCell>

                <TableCell>{{ patient.fullName }}</TableCell>
                <TableCell>{{ patient.age }} лет</TableCell>
                <TableCell class="max-w-[200px] truncate">{{ patient.diagnosis }}</TableCell>
                <TableCell>{{ patient.operations }}</TableCell>

                <TableCell>
                  <span class="flex items-center gap-2 text-sm">
                    <span
                      class="inline-block h-2.5 w-2.5 rounded-full"
                      :class="getExamStatusDotClass(getExamStatus(patient.lastExam))"
                    />
                    <Calendar class="h-3 w-3" />
                    {{ patient.lastExam }}
                  </span>
                </TableCell>

                <TableCell>
                  <span class="flex items-center gap-1 text-sm">
                    <MapPin class="h-3 w-3" />
                    {{ patient.region }}
                  </span>
                </TableCell>
              </TableRow>
            </TableBody>
          </Table>
        </div>

        <div class="flex items-center justify-between border-t border-border px-4 py-3">
          <p class="text-sm text-muted-foreground">Страница {{ currentPage }} из {{ totalPages }}</p>
          <div class="flex items-center gap-2">
            <Button
              type="button"
              variant="outline"
              size="sm"
              :disabled="currentPage === 1"
              @click="goToPage(currentPage - 1)"
            >
              Назад
            </Button>

            <Button
              v-for="page in pageNumbers"
              :key="page"
              type="button"
              size="sm"
              :variant="page === currentPage ? 'default' : 'outline'"
              @click="goToPage(page)"
            >
              {{ page }}
            </Button>

            <Button
              type="button"
              variant="outline"
              size="sm"
              :disabled="currentPage === totalPages"
              @click="goToPage(currentPage + 1)"
            >
              Вперед
            </Button>
          </div>
        </div>
      </div>
    </Card>
  </div>
</template>
