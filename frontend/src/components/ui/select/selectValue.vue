<script setup lang="ts">
import { computed } from 'vue'
import { useSelect } from './select'

const props = withDefaults(defineProps<{
  placeholder?: string
}>(), {
  placeholder: 'Выберите тип',
})

const select = useSelect()
const displayValue = computed(() => {
  return select.selectedLabel.value || props.placeholder
})
</script>

<template>
  <span class="truncate text-sm" :class="select.selectedLabel.value ? 'text-foreground' : 'text-muted-foreground'">
    {{ displayValue }}
  </span>
</template>