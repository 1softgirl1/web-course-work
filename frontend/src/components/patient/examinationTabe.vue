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
import Badge from '@/components/ui/badge.vue'

interface IndicatorSelection {
  code: string
  label: string
}

interface MetricRow {
  characteristicCode: string
  characteristicName: string
}

const props = defineProps<{
  examinations?: Examination[]
}>()

const emit = defineEmits<{
  (e: 'select-indicator', payload: IndicatorSelection): void
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

const metricRows = computed<MetricRow[]>(() => {
  const map = new Map<string, MetricRow>()
  sortedExaminations.value.forEach(exam => {
    exam.metrics.forEach(metric => {
      if (map.has(metric.characteristicCode)) return
      map.set(metric.characteristicCode, {
        characteristicCode: metric.characteristicCode,
        characteristicName: metric.characteristicName || metric.characteristicCode,
      })
    })
  })

  return [...map.values()].sort((left, right) =>
    left.characteristicCode.localeCompare(right.characteristicCode),
  )
})

const horizontalScrollThreshold = 7

const shouldShowHorizontalScroll = computed<boolean>(() => {
  return sortedExaminations.value.length > horizontalScrollThreshold
})

const tableMinWidth = computed<string | undefined>(() => {
  if (!shouldShowHorizontalScroll.value) return undefined
  const firstColumnWidth = 260
  const examColumnWidth = 165
  return `${firstColumnWidth + sortedExaminations.value.length * examColumnWidth}px`
})

const getMetricForExam = (exam: Examination, characteristicCode: string) => {
  return exam.metrics.find(metric => metric.characteristicCode === characteristicCode) ?? null
}

const selectIndicator = (row: MetricRow) => {
  emit('select-indicator', {
    code: row.characteristicCode,
    label: row.characteristicName,
  })
}
</script>

<template>
  <Card class="w-full max-w-none" title="Таблица обследований" description="Сравнение показателей по датам обследований">
    <div class="space-y-3 sm:hidden">
      <div
        v-for="exam in sortedExaminations"
        :key="`mobile-exam-${exam.id}`"
        class="rounded-lg border bg-card p-3"
      >
        <div class="mb-2 flex items-center justify-between gap-2">
          <p class="text-sm font-medium">Обследование #{{ exam.id }}</p>
          <Badge variant="outline">{{ exam.date }}</Badge>
        </div>

        <p class="mb-2 text-xs text-muted-foreground">Врач: {{ exam.doctor }}</p>

        <div class="max-h-52 space-y-1 overflow-y-auto pr-1">
          <button
            v-for="metric in exam.metrics"
            :key="`mobile-cell-${exam.id}-${metric.characteristicCode}`"
            type="button"
            class="flex w-full items-start justify-between gap-3 rounded bg-secondary/40 px-2 py-1.5 text-xs transition-colors hover:bg-secondary/70"
            @click="selectIndicator({ characteristicCode: metric.characteristicCode, characteristicName: metric.characteristicName })"
          >
            <span class="text-left text-muted-foreground">{{ metric.characteristicName || metric.characteristicCode }}</span>
            <span class="font-medium text-right">{{ metric.value }}{{ metric.unit ? ` ${metric.unit}` : '' }}</span>
          </button>
        </div>
      </div>

      <div v-if="sortedExaminations.length === 0" class="py-6 text-center text-sm text-muted-foreground">
        Обследования отсутствуют
      </div>
    </div>

    <div class="hidden w-full min-w-0 sm:block">
      <Table :style="tableMinWidth ? { minWidth: tableMinWidth } : undefined" :class="tableMinWidth ? 'w-max' : ''">
        <TableHeader>
          <TableRow>
            <TableHead class="sticky left-0 z-10 min-w-56 bg-background">Показатель</TableHead>
            <TableHead v-for="exam in sortedExaminations" :key="`head-date-${exam.id}`" class="min-w-36 text-center">
              <Badge variant="outline">{{ exam.date }}</Badge>
            </TableHead>
          </TableRow>
        </TableHeader>

        <TableBody>
          <TableRow v-for="metric in metricRows" :key="`metric-row-${metric.characteristicCode}`" class="text-center">
            <TableCell class="sticky left-0 z-10 bg-background text-left font-medium">
              <button
                type="button"
                class="w-full text-left transition-colors hover:text-primary"
                @click="selectIndicator(metric)"
              >
                {{ metric.characteristicName }}
                <span class="ml-1 text-xs text-muted-foreground">({{ metric.characteristicCode }})</span>
              </button>
            </TableCell>

            <TableCell v-for="exam in sortedExaminations" :key="`cell-${metric.characteristicCode}-${exam.id}`">
              <template v-if="getMetricForExam(exam, metric.characteristicCode)">
                <button
                  type="button"
                  class="w-full rounded px-1 py-1 transition-colors hover:bg-secondary/40"
                  @click="selectIndicator(metric)"
                >
                  {{ getMetricForExam(exam, metric.characteristicCode)?.value }}
                  {{ getMetricForExam(exam, metric.characteristicCode)?.unit ? ` ${getMetricForExam(exam, metric.characteristicCode)?.unit}` : '' }}
                </button>
              </template>
              <span v-else>—</span>
            </TableCell>
          </TableRow>

          <TableRow v-if="metricRows.length === 0">
            <TableCell :colspan="sortedExaminations.length + 1" class="py-6 text-center text-muted-foreground">
              Обследования отсутствуют
            </TableCell>
          </TableRow>
        </TableBody>
      </Table>
    </div>
  </Card>
</template>
