<script setup lang="ts">
import { computed } from 'vue'
import { cn } from '../../lib/utils'

const props = withDefaults(defineProps<{
  class?: string
  modelValue?: string | number
}>(), {
  class: '',
  modelValue: '',
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const classes = computed(() =>
  cn(
    'border-input bg-background ring-offset-background placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-ring/50 flex w-full rounded-md border px-3 py-2 text-sm shadow-xs transition-[color,box-shadow] focus-visible:ring-[3px] focus-visible:outline-none disabled:cursor-not-allowed disabled:opacity-50',
    props.class,
  )
)
</script>

<template>
  <input
      data-slot="input"
      :class="classes"
      :value="props.modelValue"
      @input="emit('update:modelValue', ($event.target as HTMLInputElement).value)"
      v-bind="$attrs"
  />
</template>
