<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import Card from '@/components/ui/card.vue'
import Table from '@/components/ui/table/table.vue'
import TableBody from '@/components/ui/table/tableBody.vue'
import TableCell from '@/components/ui/table/tableCell.vue'
import TableHead from '@/components/ui/table/tableHead.vue'
import TableHeader from '@/components/ui/table/tableHeader.vue'
import TableRow from '@/components/ui/table/tableRow.vue'
import { mockPatientApi } from '@/mocks/openapi/mockPatientApi'
import { MapPin } from 'lucide-vue-next'

const router = useRouter()
const doctors = computed(() => mockPatientApi.listDoctors())

const openDoctorCard = (doctorId: number) => {
  router.push(`/doctor/allDoctors/${doctorId}`)
}
</script>

<template>
  <div class="p-4 sm:p-6 lg:p-8">
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-foreground">Все врачи</h1>
      <p class="text-muted-foreground">Список врачей системы</p>
    </div>

    <Card>
      <div v-if="doctors.length > 0" class="overflow-x-auto">
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
              v-for="doctor in doctors"
              :key="doctor.id"
              class="cursor-pointer"
              @click="openDoctorCard(doctor.id)"
            >
              <TableCell class="font-medium">{{ doctor.fullName }}</TableCell>
              <TableCell>{{ doctor.email }}</TableCell>
              <TableCell>
                {{ doctor.specialty }}
              </TableCell>
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

      <div v-else class="p-4 text-sm text-muted-foreground">
        Нет доступа к списку врачей.
      </div>
    </Card>
  </div>
</template>
