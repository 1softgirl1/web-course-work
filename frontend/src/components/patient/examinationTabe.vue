<script setup lang="ts">
import { computed } from 'vue'
import Card from '@/components/ui/card.vue'
import Table from '@/components/ui/table/table.vue'
import TableBody from '@/components/ui/table/tableBody.vue'
import TableCell from '@/components/ui/table/tableCell.vue'
import TableHead from '@/components/ui/table/tableHead.vue'
import TableHeader from '@/components/ui/table/tableHeader.vue'
import TableRow from '@/components/ui/table/tableRow.vue'
import { useExaminationStore, type Examination } from '@/stores/examinationStore'
import Badge from "@/components/ui/badge.vue";

const { examinations } = useExaminationStore()

const parseExamDate = (value: string): Date | null => {
  const [day, month, year] = value.split('.').map(Number)
  if (!day || !month || !year) return null
  const date = new Date(year, month - 1, day)
  return Number.isNaN(date.getTime()) ? null : date
}

const sortedExaminations = computed<Examination[]>(() => {
  return [...examinations].sort((a, b) => {
    const aTime = parseExamDate(a.date)?.getTime() ?? 0
    const bTime = parseExamDate(b.date)?.getTime() ?? 0
    return bTime - aTime
  })
})

const indicatorColumns = 30

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
            <TableHead></TableHead>
            <TableHead v-for="index in indicatorIndexes" :key="`head-${index}`" class="indicator-head-cell text-center">
              <span class="indicator-head-text">Показатель {{ index + 1 }}</span>
            </TableHead>
          </TableRow>
        </TableHeader>

        <TableBody>
          <TableRow v-for="exam in sortedExaminations" :key="`exam-row-${exam.id}`" class="text-center">
            <TableCell class="font-medium">
              <Badge variant="outline" >
                {{ exam.date }}
              </Badge>
             </TableCell>
            <TableCell v-for="index in indicatorIndexes" :key="`cell-${exam.id}-${index}`">
              {{ exam.indicators[index] ?? '—' }}
            </TableCell>
          </TableRow>

          <TableRow v-if="sortedExaminations.length === 0">
            <TableCell :colspan="indicatorColumns + 1" class="text-center text-muted-foreground py-6">
              Обследования отсутствуют
            </TableCell>
          </TableRow>
        </TableBody>
      </Table>
    </div>
  </Card>
</template>

<style scoped>
.indicator-head-cell {
  width: 30px;
  min-width: 30px;
  height: 104px;
  padding: 2px 1px;
  vertical-align: bottom;
}

.indicator-head-text {
  display: inline-block;
  writing-mode: vertical-rl;
  transform: rotate(180deg);
  text-orientation: mixed;
  white-space: nowrap;
  line-height: 1;
  font-size: 11px;
}
</style>
