<script setup lang="ts">
import { ref, watchEffect } from 'vue'
import { ChevronDown } from 'lucide-vue-next'
import { cn } from '@/lib/utils'
import { useSelect } from './select'

const select = useSelect()
const triggerRef = ref<HTMLElement | null>(null)

watchEffect(() => {
  select.setTriggerEl(triggerRef.value)
})
</script>

<template>
  <button
      ref="triggerRef"
      type="button"
      @click="select.open.value = !select.open.value"
      :aria-expanded="select.open.value"
      aria-haspopup="listbox"
      :class="cn(
      'flex w-full items-center justify-between rounded-md border px-3 py-2 text-sm',
      'bg-transparent border-input focus:ring-2 focus:ring-ring'
    )"
  >
    <slot />
    <ChevronDown class="w-4 h-4 opacity-50" />
  </button>
</template>


