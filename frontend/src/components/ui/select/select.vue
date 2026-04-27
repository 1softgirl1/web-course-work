<script setup lang="ts">
import { getCurrentInstance, onBeforeUnmount, onMounted, ref } from 'vue'
import { provideSelect } from './select'

const props = defineProps<{
  modelValue?: string
  open?: boolean
  placeholder?: string
}>()

const emit = defineEmits(['update:modelValue', 'update:open'])

const rootEl = ref<HTMLElement | null>(null)
const instance = getCurrentInstance()
const vnodeProps = instance?.vnode.props ?? {}
const isOpenControlled = Object.prototype.hasOwnProperty.call(vnodeProps, 'open')
  || Object.prototype.hasOwnProperty.call(vnodeProps, 'onUpdate:open')
const select = provideSelect(props, emit, isOpenControlled)

const isInside = (event: Event, element: HTMLElement | null): boolean => {
  if (!element) return false
  const path = typeof event.composedPath === 'function' ? event.composedPath() : []
  if (path.length > 0) return path.includes(element)

  const target = event.target as Node | null
  return Boolean(target && element.contains(target))
}

const onDocumentClick = (event: Event) => {
  if (!select.open.value) return

  const clickedInsideRoot = isInside(event, rootEl.value)
  const clickedInsideContent = isInside(event, select.contentEl.value)

  if (!clickedInsideRoot && !clickedInsideContent) {
    select.open.value = false
  }
}

const onDocumentKeyDown = (event: KeyboardEvent) => {
  if (event.key === 'Escape' && select.open.value) {
    select.open.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', onDocumentClick)
  document.addEventListener('keydown', onDocumentKeyDown)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', onDocumentClick)
  document.removeEventListener('keydown', onDocumentKeyDown)
})
</script>

<template>
  <div ref="rootEl" class="relative">
    <slot />
  </div>
</template>


