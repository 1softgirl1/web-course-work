<script setup lang="ts">
import { ref, watch } from "vue"
import { XIcon } from "lucide-vue-next"

const props = defineProps({
  modelValue: { type: Boolean, default: false }, // v-model:open
  showCloseButton: { type: Boolean, default: true },
  overlayClass: { type: String, default: '' },
  contentClass: { type: String, default: '' },
})

const emit = defineEmits(['update:modelValue', 'open', 'close'])

const open = ref(props.modelValue)

// Синхронизация внешнего v-model
watch(() => props.modelValue, (val) => { open.value = val })
watch(open, (val) => { emit('update:modelValue', val); if (val) emit('open'); else emit('close') })

const onClose = () => { open.value = false }
</script>

<template>
  <Teleport to="body">
    <div v-if="open">
      <!-- Overlay -->
      <div
        data-slot="dialog-overlay"
        :class="['fixed inset-0 z-50 bg-black/50 transition-opacity duration-200', overlayClass]"
        @click="onClose"
      />

      <!-- Content -->
      <div
        data-slot="dialog-content"
        :class="[
          'bg-background fixed left-1/2 top-1/2 z-50 grid w-[calc(100%-1rem)] max-w-lg max-h-[calc(100vh-2rem)] -translate-x-1/2 -translate-y-1/2 gap-4 overflow-y-auto rounded-lg border p-4 shadow-lg transition-transform duration-200 sm:w-full sm:p-6',
          contentClass
        ]"
      >
        <slot />

        <!-- Close Button -->
        <button
          v-if="showCloseButton"
          data-slot="dialog-close"
          class="absolute right-3 top-3 rounded-xs opacity-70 hover:opacity-100 focus:ring-2 focus:ring-offset-2 sm:right-4 sm:top-4"
          @click="onClose"
        >
          <XIcon />
          <span class="sr-only">Close</span>
        </button>
      </div>
    </div>
  </Teleport>
</template>