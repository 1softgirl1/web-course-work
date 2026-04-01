<script setup lang="ts">
import { ref, watch, defineProps, defineEmits } from "vue"
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
          'bg-background fixed top-1/2 left-1/2 z-50 grid w-full max-w-lg -translate-x-1/2 -translate-y-1/2 gap-4 rounded-lg border p-6 shadow-lg sm:max-w-lg transition-transform duration-200',
          contentClass
        ]"
      >
        <slot />

        <!-- Close Button -->
        <button
            v-if="showCloseButton"
            data-slot="dialog-close"
            class="absolute top-4 right-4 opacity-70 hover:opacity-100 focus:ring-2 focus:ring-offset-2 rounded-xs"
            @click="onClose"
        >
          <XIcon />
          <span class="sr-only">Close</span>
        </button>
      </div>
    </div>
  </Teleport>
</template>