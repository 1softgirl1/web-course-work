<script setup lang="ts">
import { provideSelect } from './select'

const props = defineProps<{
  modelValue?: string
  open?: boolean
  placeholder?: string
}>()

const emit = defineEmits(['update:modelValue', 'update:open'])

provideSelect(props, emit)
</script>

<template>
  <div class="relative">
    <slot />
  </div>
</template>