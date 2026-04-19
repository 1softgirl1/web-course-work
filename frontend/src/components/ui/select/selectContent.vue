<script setup lang="ts">
import { watchEffect, ref } from 'vue'
import { useSelect } from './select'

const select = useSelect()
const contentRef = ref<HTMLElement | null>(null)

watchEffect(() => {
  select.setContentEl(contentRef.value)
})
</script>

<template>
  <div
    v-if="select.open.value"
    ref="contentRef"
    role="listbox"
    class="relative z-[3000] mt-1 max-h-64 w-full overflow-y-auto rounded-md border bg-background p-1 shadow-md"
  >
    <slot />
  </div>
</template>


