<script setup lang="ts">
import { computed } from 'vue'
import Card from '@/components/ui/card.vue'
import Table from '@/components/ui/table/table.vue'
import TableBody from '@/components/ui/table/tableBody.vue'
import TableCell from '@/components/ui/table/tableCell.vue'
import TableHead from '@/components/ui/table/tableHead.vue'
import TableHeader from '@/components/ui/table/tableHeader.vue'
import TableRow from '@/components/ui/table/tableRow.vue'
import { useExaminationStore, type Examination, parseExamDate } from '@/stores/examinationStore'
import Badge from "@/components/ui/badge.vue";

const props = defineProps<{
  examinations?: Examination[]
}>()

const { examinations } = useExaminationStore()

const sourceExaminations = computed<Examination[]>(() => {
  return props.examinations ?? examinations
})

const sortedExaminations = computed<Examination[]>(() => {
  return [...sourceExaminations.value].sort((a, b) => {
    const aTime = parseExamDate(a.date)?.getTime() ?? 0
    const bTime = parseExamDate(b.date)?.getTime() ?? 0
    if (bTime !== aTime) return bTime - aTime
    return b.id - a.id
  })
})

const indicatorColumns = 50

const indicatorIndexes = computed<number[]>(() => {
  return Array.from({ length: indicatorColumns }, (_, index) => index)
})
</script>

<template>
  <Card class="w-full max-w-none" title="Таблица обследований" description="Сравнение показателей по датам обследований">
    <div class="overflow-x-auto">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead class="sticky left-0 bg-background z-10 min-w-35">Показатель</TableHead>
            <TableHead v-for="exam in sortedExaminations" :key="`head-date-${exam.id}`" class="text-center min-w-35">
              <Badge variant="outline">{{ exam.date }}</Badge>
            </TableHead>
          </TableRow>
        </TableHeader>

        <TableBody>
          <TableRow v-for="index in indicatorIndexes" :key="`indicator-row-${index}`" class="text-center">
            <TableCell class="sticky left-0 bg-background z-10 text-left font-medium">
              Показатель {{ index + 1 }}
            </TableCell>
            <TableCell v-for="exam in sortedExaminations" :key="`cell-${index}-${exam.id}`">
              {{ exam.indicators[index] ?? '—' }}
            </TableCell>
          </TableRow>

          <TableRow v-if="sortedExaminations.length === 0">
            <TableCell :colspan="sortedExaminations.length + 1" class="text-center text-muted-foreground py-6">
              Обследования отсутствуют
            </TableCell>
          </TableRow>
        </TableBody>
      </Table>
    </div>
  </Card>
</template>

