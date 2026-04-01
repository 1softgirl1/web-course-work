<script setup lang="ts">
import { cn } from '@/lib/utils'

const props = withDefaults(defineProps<{
  for?: string
  class?: string
}>(), {
  for: undefined,
  class: '',
})
</script>

<template>
  <label :for="props.for" :class="cn('text-sm font-medium text-foreground', props.class)">
    <slot />
  </label>
</template>

