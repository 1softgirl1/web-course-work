<script setup lang="ts">
import { computed } from 'vue'
import Dialog from '@/components/ui/dialog.vue'
import type { Examination } from '@/stores/examinationStore'

const props = withDefaults(defineProps<{
  modelValue: boolean
  indicatorCode: string | null
  indicatorLabel: string | null
  exams: Examination[]
}>(), {
  modelValue: false,
  indicatorCode: null,
  indicatorLabel: null,
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const parseExamDate = (value: string): Date | null => {
  const [day, month, year] = value.split('.').map(Number)
  if (!day || !month || !year) return null
  const date = new Date(year, month - 1, day)
  return Number.isNaN(date.getTime()) ? null : date
}

const indicatorTitle = computed(() => {
  if (!props.indicatorCode) return 'Показатель'
  return props.indicatorLabel || props.indicatorCode
})

const series = computed(() => {
  if (!props.indicatorCode) return []

  return [...props.exams]
    .map((exam) => {
      const date = parseExamDate(exam.date)
      const metric = exam.metrics.find(item => item.characteristicCode === props.indicatorCode)
      return {
        examId: exam.id,
        dateLabel: exam.date,
        date,
        value: metric?.value ?? NaN,
      }
    })
    .filter(item => item.date && Number.isFinite(item.value))
    .sort((left, right) => (left.date?.getTime() ?? 0) - (right.date?.getTime() ?? 0))
})

const chartWidth = 920
const chartHeight = 360
const padding = { top: 24, right: 22, bottom: 68, left: 64 }
const maxXLabels = 6

const valueRange = computed(() => {
  if (series.value.length === 0) {
    return { min: 0, max: 100 }
  }

  let min = Math.min(...series.value.map(item => item.value))
  let max = Math.max(...series.value.map(item => item.value))

  if (min === max) {
    min -= 1
    max += 1
  }

  const pad = (max - min) * 0.12
  return {
    min: min - pad,
    max: max + pad,
  }
})

const yTicks = computed(() => {
  const tickCount = 5
  const { min, max } = valueRange.value
  const span = max - min

  return Array.from({ length: tickCount }, (_, index) => {
    const ratio = index / (tickCount - 1)
    const value = max - span * ratio
    const y = padding.top + (chartHeight - padding.top - padding.bottom) * ratio
    return { value, y }
  })
})

const points = computed(() => {
  if (series.value.length === 0) return []

  const innerWidth = chartWidth - padding.left - padding.right
  const innerHeight = chartHeight - padding.top - padding.bottom
  const { min, max } = valueRange.value

  return series.value.map((item, index) => {
    const x = series.value.length === 1
      ? padding.left + innerWidth / 2
      : padding.left + (innerWidth * index) / (series.value.length - 1)

    const ratio = (item.value - min) / (max - min)
    const y = padding.top + innerHeight - ratio * innerHeight

    return {
      ...item,
      x,
      y,
    }
  })
})

const xAxisY = computed(() => chartHeight - padding.bottom)

const chartAreaPoints = computed(() => {
  if (points.value.length === 0) return ''
  const first = points.value[0]
  const last = points.value[points.value.length - 1]
  const linePoints = points.value.map(point => `${point.x},${point.y}`).join(' ')
  return `${first.x},${xAxisY.value} ${linePoints} ${last.x},${xAxisY.value}`
})

const xLabelIndexes = computed(() => {
  const length = points.value.length
  if (length <= maxXLabels) return Array.from({ length }, (_, index) => index)

  const step = Math.ceil((length - 1) / (maxXLabels - 1))
  const indexes = new Set<number>([0, length - 1])
  for (let index = step; index < length - 1; index += step) {
    indexes.add(index)
  }
  return [...indexes].sort((left, right) => left - right)
})

const xLabelPoints = computed(() => {
  return xLabelIndexes.value.map(index => points.value[index]).filter(Boolean)
})

const latestPoint = computed(() => {
  if (points.value.length === 0) return null
  return points.value[points.value.length - 1]
})

const summary = computed(() => {
  if (series.value.length === 0) return null

  const values = series.value.map(item => item.value)
  const first = values[0]
  const last = values[values.length - 1]
  const min = Math.min(...values)
  const max = Math.max(...values)
  const delta = last - first

  return { min, max, last, delta, first }
})

const formatDelta = (value: number) => {
  if (value > 0) return `+${value.toFixed(1)}`
  if (value < 0) return value.toFixed(1)
  return '0.0'
}

const deltaClass = computed(() => {
  if (!summary.value) return 'text-muted-foreground'
  if (summary.value.delta > 0) return 'text-emerald-600'
  if (summary.value.delta < 0) return 'text-rose-600'
  return 'text-muted-foreground'
})
</script>

<template>
  <Dialog :model-value="modelValue" @update:modelValue="emit('update:modelValue', $event)" content-class="sm:max-w-6xl">
    <div class="space-y-5">
      <div class="space-y-3">
        <h3 class="text-lg font-semibold text-foreground sm:text-xl">Динамика: {{ indicatorTitle }}</h3>
        <div v-if="summary" class="grid grid-cols-2 gap-2 sm:grid-cols-5">
          <div class="rounded-lg border border-border bg-secondary/30 px-3 py-2">
            <p class="text-[11px] uppercase tracking-wide text-muted-foreground">Первое</p>
            <p class="text-sm font-semibold text-foreground">{{ summary.first.toFixed(1) }}</p>
          </div>
          <div class="rounded-lg border border-border bg-secondary/30 px-3 py-2">
            <p class="text-[11px] uppercase tracking-wide text-muted-foreground">Последнее</p>
            <p class="text-sm font-semibold text-foreground">{{ summary.last.toFixed(1) }}</p>
          </div>
          <div class="rounded-lg border border-border bg-secondary/30 px-3 py-2">
            <p class="text-[11px] uppercase tracking-wide text-muted-foreground">Мин</p>
            <p class="text-sm font-semibold text-foreground">{{ summary.min.toFixed(1) }}</p>
          </div>
          <div class="rounded-lg border border-border bg-secondary/30 px-3 py-2">
            <p class="text-[11px] uppercase tracking-wide text-muted-foreground">Макс</p>
            <p class="text-sm font-semibold text-foreground">{{ summary.max.toFixed(1) }}</p>
          </div>
          <div class="rounded-lg border border-border bg-secondary/30 px-3 py-2">
            <p class="text-[11px] uppercase tracking-wide text-muted-foreground">Изменение</p>
            <p class="text-sm font-semibold" :class="deltaClass">{{ formatDelta(summary.delta) }}</p>
          </div>
        </div>
      </div>

      <div v-if="series.length > 0" class="rounded-xl border border-border/80 bg-gradient-to-b from-background to-muted/30 p-3 sm:p-4">
        <div class="overflow-x-auto">
          <svg :viewBox="`0 0 ${chartWidth} ${chartHeight}`" class="h-[360px] min-w-[700px] w-full">
            <defs>
              <linearGradient id="trend-fill" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="rgb(59 130 246)" stop-opacity="0.28" />
                <stop offset="100%" stop-color="rgb(59 130 246)" stop-opacity="0.03" />
              </linearGradient>
            </defs>

            <g>
              <line
                v-for="tick in yTicks"
                :key="`grid-${tick.y}`"
                :x1="padding.left"
                :x2="chartWidth - padding.right"
                :y1="tick.y"
                :y2="tick.y"
                stroke="rgb(203 213 225)"
                stroke-width="1"
                stroke-dasharray="4 4"
                opacity="0.7"
              />

              <text
                v-for="tick in yTicks"
                :key="`label-${tick.y}`"
                :x="padding.left - 10"
                :y="tick.y + 4"
                text-anchor="end"
                class="fill-muted-foreground text-[11px]"
              >
                {{ tick.value.toFixed(1) }}
              </text>
            </g>

            <line
              :x1="padding.left"
              :x2="chartWidth - padding.right"
              :y1="xAxisY"
              :y2="xAxisY"
              stroke="rgb(148 163 184)"
              stroke-width="1.4"
              opacity="0.9"
            />

            <line
              v-for="point in xLabelPoints"
              :key="`x-grid-${point.examId}`"
              :x1="point.x"
              :x2="point.x"
              :y1="padding.top"
              :y2="xAxisY"
              stroke="rgb(226 232 240)"
              stroke-width="1"
              stroke-dasharray="3 4"
              opacity="0.7"
            />

            <polygon
              v-if="points.length > 1"
              :points="chartAreaPoints"
              fill="url(#trend-fill)"
            />

            <polyline
              v-if="points.length > 1"
              :points="points.map(point => `${point.x},${point.y}`).join(' ')"
              fill="none"
              stroke="rgb(37 99 235)"
              stroke-width="3.2"
              stroke-linecap="round"
              stroke-linejoin="round"
            />

            <g v-for="point in points" :key="`point-${point.examId}`">
              <circle :cx="point.x" :cy="point.y" r="5.5" fill="white" stroke="rgb(37 99 235)" stroke-width="2.5" />
              <title>{{ point.dateLabel }}: {{ point.value.toFixed(1) }}</title>
            </g>

            <g v-if="latestPoint">
              <circle :cx="latestPoint.x" :cy="latestPoint.y" r="9.5" fill="none" stroke="rgb(37 99 235)" stroke-opacity="0.25" stroke-width="3" />
              <circle :cx="latestPoint.x" :cy="latestPoint.y" r="6" fill="white" stroke="rgb(37 99 235)" stroke-width="3" />
            </g>

            <g v-for="point in xLabelPoints" :key="`date-label-${point.examId}`">
              <text
                :x="point.x"
                :y="chartHeight - 14"
                text-anchor="end"
                class="fill-muted-foreground text-[11px]"
                transform-origin="center"
                :transform="`rotate(-30 ${point.x} ${chartHeight - 14})`"
              >
                {{ point.dateLabel }}
              </text>
            </g>
          </svg>
        </div>
      </div>

      <div v-else class="rounded-lg border border-border p-4 text-sm text-muted-foreground">
        Недостаточно данных для построения графика.
      </div>
    </div>
  </Dialog>
</template>
