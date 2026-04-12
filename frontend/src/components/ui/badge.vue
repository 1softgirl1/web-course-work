<script setup lang="ts">
import { computed, useAttrs } from 'vue'
import { cn } from '@/lib/utils'

const props = withDefaults(defineProps<{
  variant?: 'default' | 'secondary' | 'destructive' | 'outline'
  asChild?: boolean
  class?: string
}>(), {
  variant: 'default',
  asChild: false,
  class: '',
})

const attrs = useAttrs()

const badgeVariants = (variant: string) => {
  const base =
      'inline-flex items-center justify-center rounded-md border px-2 py-0.5 text-xs font-medium w-fit whitespace-nowrap shrink-0 gap-1 overflow-hidden transition-[color,box-shadow]'

  const variants: Record<string, string> = {
    default:
        'border-transparent bg-primary text-primary-foreground',
    secondary:
        'border-transparent bg-secondary text-secondary-foreground',
    destructive:
        'border-transparent bg-destructive text-white',
    outline:
        'text-foreground',
  }

  return cn(base, variants[variant] || variants.default)
}

const classes = computed(() =>
    cn(badgeVariants(props.variant), props.class)
)
</script>

<template>
  <component
      :is="asChild ? 'slot' : 'span'"
      data-slot="badge"
      :class="classes"
      v-bind="attrs"
  >
    <slot />
  </component>
</template>