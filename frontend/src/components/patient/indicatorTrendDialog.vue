<script setup lang="ts">
import { computed } from 'vue'
import Dialog from '@/components/ui/dialog.vue'

interface ExamLike {
  id: number
  date: string
  indicators: number[]
}

const props = withDefaults(defineProps<{
  modelValue: boolean
  indicatorIndex: number | null
  exams: ExamLike[]
}>(), {
  modelValue: false,
  indicatorIndex: null,
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
  if (props.indicatorIndex === null) return 'Показатель'
  return `Показатель ${props.indicatorIndex + 1}`
})

const series = computed(() => {
  const indicatorIndex = props.indicatorIndex
  if (indicatorIndex === null) return []

  return [...props.exams]
    .map((exam) => {
      const date = parseExamDate(exam.date)
      const value = Number(exam.indicators[indicatorIndex] ?? NaN)
      return {
        examId: exam.id,
        dateLabel: exam.date,
        date,
        value,
      }
    })
    .filter(item => item.date && Number.isFinite(item.value))
    .sort((left, right) => (left.date?.getTime() ?? 0) - (right.date?.getTime() ?? 0))
})

const chartWidth = 680
const chartHeight = 300
const padding = { top: 24, right: 16, bottom: 44, left: 52 }

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

const polylinePoints = computed(() => {
  return points.value.map(point => `${point.x},${point.y}`).join(' ')
})
</script>

<template>
  <Dialog :model-value="modelValue" @update:modelValue="emit('update:modelValue', $event)" content-class="sm:max-w-5xl">
    <div class="space-y-4">
      <div class="flex items-center justify-between gap-3">
        <h3 class="text-lg font-semibold text-foreground">Динамика: {{ indicatorTitle }}</h3>

      </div>

      <div v-if="series.length > 0" class="rounded-xl border border-border/80 bg-gradient-to-b from-background to-muted/30 p-3 sm:p-4">
        <svg :viewBox="`0 0 ${chartWidth} ${chartHeight}`" class="h-[320px] w-full">
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

          <polyline
            v-if="points.length > 1"
            :points="polylinePoints"
            fill="none"
            stroke="rgb(239 68 68)"
            stroke-width="3"
            stroke-linecap="round"
            stroke-linejoin="round"
          />

          <g v-for="point in points" :key="`point-${point.examId}`">
            <circle :cx="point.x" :cy="point.y" r="6" fill="white" stroke="rgb(239 68 68)" stroke-width="3" />
            <text :x="point.x" :y="chartHeight - 16" text-anchor="middle" class="fill-muted-foreground text-[11px]">
              {{ point.dateLabel }}
            </text>
          </g>
        </svg>
      </div>

      <div v-else class="rounded-lg border border-border p-4 text-sm text-muted-foreground">
        Недостаточно данных для построения графика.
      </div>
    </div>
  </Dialog>
</template>
