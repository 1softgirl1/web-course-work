<script setup lang="ts">
import { computed, useSlots } from 'vue'
import { cn } from '../../lib/utils'

const props = withDefaults(defineProps<{
  class?: string
  headerClass?: string
  titleClass?: string
  descriptionClass?: string
  contentClass?: string
  footerClass?: string
  title?: string
  description?: string
}>(), {
  class: '',
  headerClass: '',
  titleClass: '',
  descriptionClass: '',
  contentClass: '',
  footerClass: '',
  title: '',
  description: '',
})

const slots = useSlots()

const hasHeader = computed(() => Boolean(slots.header || slots.action || props.title || props.description))
</script>

<template>
  <div
    data-slot="card"
    :class="cn('bg-card text-card-foreground flex flex-col gap-6 rounded-xl border py-6 shadow-sm', props.class)"
  >
    <div
      v-if="hasHeader"
      data-slot="card-header"
      :class="cn('@container/card-header grid auto-rows-min grid-rows-[auto_auto] items-start gap-2 px-6 has-data-[slot=card-action]:grid-cols-[1fr_auto] [.border-b]:pb-6', props.headerClass)"
    >
      <slot name="header">
        <div
          v-if="props.title"
          data-slot="card-title"
          :class="cn('leading-none font-semibold', props.titleClass)"
        >
          {{ props.title }}
        </div>
        <div
          v-if="props.description"
          data-slot="card-description"
          :class="cn('text-muted-foreground text-sm', props.descriptionClass)"
        >
          {{ props.description }}
        </div>
      </slot>

      <div
        v-if="$slots.action"
        data-slot="card-action"
        :class="cn('col-start-2 row-span-2 row-start-1 self-start justify-self-end')"
      >
        <slot name="action" />
      </div>
    </div>

    <div data-slot="card-content" :class="cn('px-6', props.contentClass)">
      <slot />
    </div>

    <div
      v-if="$slots.footer"
      data-slot="card-footer"
      :class="cn('flex items-center px-6 [.border-t]:pt-6', props.footerClass)"
    >
      <slot name="footer" />
    </div>
  </div>
</template>
