<script setup lang="ts">
import { cn } from '@/lib/utils'

const props = withDefaults(defineProps<{
  class?: string
}>(), {
  class: '',
})
</script>

<template>
  <thead
    data-slot="table-header"
    :class="cn('[&_tr]:border-b bg-muted/50', props.class)"
    v-bind="$attrs"
  >
    <slot />
  </thead>
</template>

