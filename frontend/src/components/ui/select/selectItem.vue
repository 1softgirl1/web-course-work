<script setup lang="ts">
import { useSlots, watchEffect } from 'vue'
import { Check } from 'lucide-vue-next'
import { useSelect } from './select'

const props = defineProps<{
  value: string
}>()

const select = useSelect()
const slots = useSlots()

const slotText = (node: any): string => {
  if (!node) return ''
  if (typeof node === 'string') return node
  if (Array.isArray(node)) return node.map(slotText).join('')
  if (typeof node.children === 'string') return node.children
  if (Array.isArray(node.children)) return node.children.map(slotText).join('')
  return ''
}

watchEffect(() => {
  const rendered = slots.default?.() ?? []
  const label = slotText(rendered).trim()
  select.registerItem(props.value, label)
})
</script>

<template>
  <div
      role="option"
      :aria-selected="select.modelValue.value === props.value"
      tabindex="0"
      @click="select.selectValue(props.value)"
      @keydown.enter.prevent="select.selectValue(props.value)"
      @keydown.space.prevent="select.selectValue(props.value)"
      class="flex items-center justify-between rounded px-2 py-1.5 text-sm cursor-pointer hover:bg-muted"
  >
    <span><slot /></span>

    <Check v-if="select.modelValue.value === props.value" class="w-4 h-4" />
  </div>
</template>


