<script setup lang="ts">
import { useAttrs } from 'vue'
import { cn } from '@/lib/utils'

defineOptions({
  inheritAttrs: false,
})

const props = withDefaults(defineProps<{
  class?: string
}>(), {
  class: '',
})

const attrs = useAttrs()
</script>

<template>
  <div
    data-slot="table-container"
    class="relative w-full min-w-0 overflow-x-auto"
  >
    <table
      data-slot="table"
      :class="cn('w-full caption-bottom text-sm', props.class)"
      v-bind="attrs"
    >
      <slot />
    </table>
  </div>
</template>

