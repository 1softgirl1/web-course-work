<script setup lang="ts">
import { useSelect } from './select'

const select = useSelect()
</script>

<template>
  <div v-if="select.open.value" class="absolute z-50 mt-1 w-full rounded-md border bg-white p-1 shadow-md">
    <slot />
  </div>
</template>