<script setup lang="ts">

import { useSlots, onMounted } from 'vue'
import { Check } from 'lucide-vue-next'
import { useSelect } from './select'

const props = defineProps<{
  value: string
}>()

const select = useSelect()
const slots = useSlots()

onMounted(() => {
  const label = slots.default?.()[0]?.children || ''
  select.registerItem(props.value, label)
})
</script>

<template>
  <div
      @click="select.selectValue(props.value)"
      class="flex items-center justify-between px-2 py-1.5 text-sm cursor-pointer hover:bg-gray-100 rounded"
  >
    <span><slot /></span>

    <Check v-if="select.modelValue.value === props.value" class="w-4 h-4" />
  </div>
</template>