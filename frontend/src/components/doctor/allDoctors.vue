<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import Card from '@/components/ui/card.vue'
import Button from '@/components/ui/button.vue'
import Input from '@/components/ui/input.vue'
import Table from '@/components/ui/table/table.vue'
import TableBody from '@/components/ui/table/tableBody.vue'
import TableCell from '@/components/ui/table/tableCell.vue'
import TableHead from '@/components/ui/table/tableHead.vue'
import TableHeader from '@/components/ui/table/tableHeader.vue'
import TableRow from '@/components/ui/table/tableRow.vue'
import { useDoctorStore } from '@/stores/doctorStore'
import { MapPin, Plus, Search } from 'lucide-vue-next'

const router = useRouter()
const doctorStore = useDoctorStore()
const doctors = computed(() => doctorStore.doctors.value)
const searchQuery = ref('')
const loadError = ref('')
let searchDebounceId: ReturnType<typeof setTimeout> | null = null

const filteredDoctors = computed(() => doctors.value)

const openDoctorCard = (doctorId: number) => {
  router.push(`/doctor/allDoctors/${doctorId}`)
}

const loadDoctors = async (search?: string) => {
  loadError.value = ''
  try {
    await doctorStore.loadDoctors({ search })
  } catch {
    loadError.value = 'Не удалось загрузить список врачей.'
  }
}

onMounted(async () => {
  await loadDoctors()
})

watch(searchQuery, (nextQuery) => {
  if (searchDebounceId) {
    clearTimeout(searchDebounceId)
    searchDebounceId = null
  }

  searchDebounceId = setTimeout(async () => {
    await loadDoctors(nextQuery)
    searchDebounceId = null
  }, 350)
})

onBeforeUnmount(() => {
  if (!searchDebounceId) return
  clearTimeout(searchDebounceId)
  searchDebounceId = null
})
</script>

<template>
  <div class="p-4 sm:p-6 lg:p-8">
    <div class="mb-6 flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
      <div>
        <h1 class="text-2xl font-bold text-foreground">Все врачи</h1>
        <p class="text-muted-foreground">Список врачей системы</p>
      </div>
      <router-link to="/doctor/allDoctors/addDoctor" class="w-full sm:w-auto">
        <Button class="w-full sm:w-auto">
          <Plus class="mr-2 h-4 w-4" />
          Добавить врача
        </Button>
      </router-link>
    </div>

    <div class="mb-6">
      <div class="relative w-full sm:max-w-xl">
        <Search class="absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-muted-foreground" />
        <Input
          v-model="searchQuery"
          type="text"
          placeholder="Поиск по ФИО, email, специальности, месту работы, региону..."
          class="pl-10"
        />
      </div>
    </div>

    <Card>
      <p v-if="loadError" class="p-4 text-sm text-destructive">{{ loadError }}</p>
      <div v-if="doctors.length > 0 && filteredDoctors.length > 0">
        <div class="space-y-3 p-3 md:hidden">
          <button
            v-for="doctor in filteredDoctors"
            :key="doctor.id"
            type="button"
            class="w-full rounded-lg border border-border p-3 text-left transition-colors hover:bg-muted/40"
            @click="openDoctorCard(doctor.id)"
          >
            <p class="font-semibold text-foreground">{{ doctor.fullName }}</p>
            <p class="mt-1 text-sm text-muted-foreground">{{ doctor.email }}</p>
            <p class="mt-2 text-sm"><span class="text-muted-foreground">Специальность:</span> {{ doctor.specialty }}</p>
            <p class="mt-1 text-sm"><span class="text-muted-foreground">Место работы:</span> {{ doctor.workplace }}</p>
            <p class="mt-1 inline-flex items-center gap-1 text-sm">
              <MapPin class="h-4 w-4 text-primary" />
              {{ doctor.regionName }}
            </p>
          </button>
        </div>

        <div class="hidden overflow-x-auto md:block">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>ФИО</TableHead>
                <TableHead>Email</TableHead>
                <TableHead>Специальность</TableHead>
                <TableHead>Место работы</TableHead>
                <TableHead>Регион</TableHead>
              </TableRow>
            </TableHeader>

            <TableBody>
              <TableRow
                v-for="doctor in filteredDoctors"
                :key="doctor.id"
                class="cursor-pointer"
                @click="openDoctorCard(doctor.id)"
              >
                <TableCell class="font-medium">{{ doctor.fullName }}</TableCell>
                <TableCell>{{ doctor.email }}</TableCell>
                <TableCell>{{ doctor.specialty }}</TableCell>
                <TableCell>
                  <span class="block max-w-70 truncate" :title="doctor.workplace">{{ doctor.workplace }}</span>
                </TableCell>
                <TableCell>
                  <span class="inline-flex items-center gap-1">
                    <MapPin class="h-4 w-4 text-primary" />
                    {{ doctor.regionName }}
                  </span>
                </TableCell>
              </TableRow>
            </TableBody>
          </Table>
        </div>
      </div>

      <div v-else-if="searchQuery.trim()" class="p-4 text-sm text-muted-foreground">
        По вашему запросу врачи не найдены.
      </div>

      <div v-else class="p-4 text-sm text-muted-foreground">
        Нет доступа к списку врачей.
      </div>
    </Card>
  </div>
</template>
