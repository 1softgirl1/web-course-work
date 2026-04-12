<script setup lang="ts">
import { ref, watch, defineProps, defineEmits } from "vue"
import Button from '../ui/button.vue'
import Dialog from '../ui/dialog.vue'
import { MapPin, LucideHospital } from "lucide-vue-next"
import type { Region } from "../../stores/regionsStore.ts"
import { RegionsStore } from "../../stores/regionsStore.ts"

const props = defineProps<{ open: boolean }>()
const emit = defineEmits<{
  (e: 'update:open', val: boolean): void
  (e: 'select', region: Region): void
}>()

const localOpen = ref(props.open)

// Синхронизация с пропсом
watch(() => props.open, val => localOpen.value = val)
watch(localOpen, val => emit('update:open', val))

const handleSelect = (region: Region) => {
  emit('select', region)
  localOpen.value = false
}
</script>

<template>
  <Dialog v-model="localOpen" content-class="sm:max-w-lg">
    <div class="p-1 sm:p-2">
      <div class="mb-2 flex items-center gap-3">
        <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/10">
          <MapPin class="h-5 w-5 text-primary"/>
        </div>
        <h3 class="text-lg font-semibold sm:text-xl">Выберите ваш регион</h3>
      </div>
      <p class="mb-4 text-sm text-muted-foreground">
        Выберите регион, чтобы увидеть контактную информацию ближайших клиник.
      </p>

      <div class="grid gap-2">
        <Button
            v-for="region in RegionsStore"
            :key="region.id"
            variant="outline"
            class="h-auto justify-start px-3 py-3 sm:px-4"
            @click="handleSelect(region)"
        >
          <LucideHospital class="mr-3 h-4 w-4 shrink-0"/>
          <div class="min-w-0 text-left">
            <div class="font-medium wrap-break-word">{{ region.name }}</div>
            <div class="text-xs">{{ region.clinics.length }} {{ region.clinics.length === 1 ? "клиника" : "клиники" }}</div>
          </div>
        </Button>
      </div>
    </div>
  </Dialog>
</template>