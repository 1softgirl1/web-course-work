<script setup lang="ts">
import { computed } from 'vue'
import { cn } from '../../lib/utils'

type TabItem = {
  value: string
  label: string
}

const props = withDefaults(defineProps<{
  modelValue: string
  items: TabItem[]
  class?: string
  listClass?: string
  triggerClass?: string
  contentClass?: string
}>(), {
  class: '',
  listClass: '',
  triggerClass: '',
  contentClass: '',
})

defineSlots<{
  trigger?: (props: { item: TabItem; active: boolean }) => unknown
  [name: `content-${string}`]: ((props: { item: TabItem; active: boolean }) => unknown) | undefined
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const activeTab = computed(() => props.modelValue)

const setActive = (value: string) => {
  if (value === props.modelValue) return
  emit('update:modelValue', value)
}
</script>

<template>
  <div data-slot="tabs" :class="cn('flex flex-col gap-2', props.class)">
    <div
      data-slot="tabs-list"
      role="tablist"
      :class="cn('bg-muted text-muted-foreground inline-flex h-9 w-fit items-center justify-center rounded-lg p-[3px]', props.listClass)"
    >
      <button
        v-for="item in props.items"
        :key="item.value"
        type="button"
        role="tab"
        data-slot="tabs-trigger"
        :data-state="activeTab === item.value ? 'active' : 'inactive'"
        :aria-selected="activeTab === item.value"
        :class="cn(`data-[state=active]:bg-background dark:data-[state=active]:text-foreground focus-visible:border-ring focus-visible:ring-ring/50 focus-visible:outline-ring dark:data-[state=active]:border-input dark:data-[state=active]:bg-input/30 text-foreground dark:text-muted-foreground inline-flex h-[calc(100%-1px)] flex-1 items-center justify-center gap-1.5 rounded-md border border-transparent px-2 py-1 text-sm font-medium whitespace-nowrap transition-[color,box-shadow] focus-visible:ring-[3px] focus-visible:outline-1 disabled:pointer-events-none disabled:opacity-50 data-[state=active]:shadow-sm [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4`, props.triggerClass)"
        @click="setActive(item.value)"
      >
        <slot name="trigger" :item="item" :active="activeTab === item.value">
          {{ item.label }}
        </slot>
      </button>
    </div>

    <div
      v-for="item in props.items"
      :key="`content-${item.value}`"
      role="tabpanel"
      data-slot="tabs-content"
      :data-state="activeTab === item.value ? 'active' : 'inactive'"
      :class="cn('flex-1 outline-none', props.contentClass)"
      :style="activeTab === item.value ? undefined : { display: 'none' }"
    >
      <slot :name="`content-${item.value}`" :item="item" :active="activeTab === item.value" />
    </div>
  </div>
</template>

