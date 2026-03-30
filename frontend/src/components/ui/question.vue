<template>
  <div
      class="bg-card card border border-gray-300 rounded-xl px-4 py-3 cursor-pointer "
      @click="toggle"

  >
    <h3 class="font-medium justify-between flex items-start">{{ question }}
      <ChevronDownIcon
          class="text-muted-foreground pointer-events-none size-4 shrink-0 translate-y-0.5 transition-transform duration-200"
          :class="{ 'rotate-180': isOpen }"
      /></h3>
    <p v-if="isOpen" class="mt-2 text-gray-600">{{ answer }}</p>
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