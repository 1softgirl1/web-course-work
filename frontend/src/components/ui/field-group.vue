<script setup lang="ts">
import {cn} from "@/lib/utils.ts";


const props = withDefaults(defineProps<{
  class?: string
}>(), {
  class: '',
})
</script>

<template>
  <div :class="cn('grid gap-4', props.class)">
    <slot />
  </div>
</template>

