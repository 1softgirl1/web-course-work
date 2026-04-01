<template>
  <div
      class="card cursor-pointer rounded-xl border border-gray-300 bg-card px-4 py-3 sm:px-5"
      @click="toggle"
  >
    <h3 class="flex items-start justify-between gap-3 text-sm font-medium sm:text-base">
      <span class="text-pretty">{{ question }}</span>
      <ChevronDownIcon
          class="pointer-events-none size-4 shrink-0 translate-y-0.5 text-muted-foreground transition-transform duration-200"
          :class="{ 'rotate-180': isOpen }"
      />
    </h3>
    <p v-if="isOpen" class="mt-2 text-sm text-gray-600 sm:text-base">{{ answer }}</p>
  </div>
</template>

<script setup>
import { ref, defineProps } from 'vue';
import { ChevronDownIcon } from "lucide-vue-next";

const props = defineProps({
  question: String,
  answer: String,
  initiallyOpen: { type: Boolean, default: false },
});

const isOpen = ref(props.initiallyOpen);

function toggle() {
  isOpen.value = !isOpen.value;
}
</script>

<style scoped>
.card {
  transition: all 0.3s ease;
}
</style>