<script setup lang="ts">
import { ChevronDown } from 'lucide-vue-next'
import { cn } from '@/lib/utils'
import { useSelect } from './select'

const select = useSelect()
</script>

<template>
  <button
      type="button"
      @click="select.open.value = !select.open.value"
      :class="cn(
      'flex w-full items-center justify-between rounded-md border px-3 py-2 text-sm',
      'bg-transparent border-input focus:ring-2 focus:ring-ring'
    )"
  >
    <slot />
    <ChevronDown class="w-4 h-4 opacity-50" />
  </button>
</template>