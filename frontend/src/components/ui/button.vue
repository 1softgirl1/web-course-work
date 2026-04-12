<script setup>
import { computed } from 'vue'
import { cva } from 'class-variance-authority'
import { cn } from '@/lib/utils'

// props
const props = defineProps({
  variant: {
    type: String,
    default: 'default',
  },
  size: {
    type: String,
    default: 'default',
  },
  asChild: {
    type: Boolean,
    default: false,
  },
  class: {
    type: String,
    default: '',
  },
})

// variants
const buttonVariants = cva(
    "inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-md text-sm font-medium transition-all disabled:pointer-events-none disabled:opacity-50 [&_svg]:pointer-events-none [&_svg:not([class*='size-'])]:size-4 shrink-0 [&_svg]:shrink-0 outline-none focus-visible:border-ring focus-visible:ring-ring/50 focus-visible:ring-[3px] aria-invalid:ring-destructive/20 dark:aria-invalid:ring-destructive/40 aria-invalid:border-destructive",
    {
      variants: {
        variant: {
          default: 'bg-primary text-primary-foreground hover:bg-primary/90',
          destructive:
              'bg-destructive text-white hover:bg-destructive/90',
          outline:
              'border bg-white shadow-xs hover:bg-background ',
          secondary:
              'bg-secondary text-secondary-foreground hover:bg-secondary/80',
          ghost:
              'hover:bg-accent hover:text-accent-foreground',
          link: 'text-primary underline-offset-4 hover:underline',
        },
        size: {
          default: 'h-9 px-4 py-2',
          sm: 'h-8 rounded-md px-3',
          lg: 'h-10 rounded-md px-6',
          icon: 'size-9',
          'icon-sm': 'size-8',
          'icon-lg': 'size-10',
        },
      },
      defaultVariants: {
        variant: 'default',
        size: 'default',
      },
    }
)

// вычисление классов
const classes = computed(() =>
    cn(buttonVariants({ variant: props.variant, size: props.size }), props.class)
)
</script>

<template>
  <component
      :is="asChild ? 'span' : 'button'"
      data-slot="button"
      :class="classes"
      v-bind="$attrs"
  >
    <slot />
  </component>
</template>