<script setup lang="ts">
import { provideSelect } from './select'

const props = defineProps<{
  modelValue?: string
  placeholder?: string
}>()

const emit = defineEmits(['update:modelValue'])

provideSelect(props, emit)
</script>

<template>
  <div class="relative">
    <slot />
  </div>
</template>