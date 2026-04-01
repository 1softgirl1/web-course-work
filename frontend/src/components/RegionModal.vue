<script setup lang="ts">
import { ref, watch, defineProps, defineEmits } from "vue"
import Button from './ui/button.vue'
import Dialog from './ui/dialog.vue'
import { MapPin, LucideHospital } from "lucide-vue-next"
import type { Region } from "../data/regions"
import { REGIONS } from "../data/regions"

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
  <Dialog v-model="localOpen" class="sm:max-w-lg">
    <div class="p-4" v-if="localOpen">
      <div class="flex items-center gap-3 mb-2">
        <div class="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center">
          <MapPin class="w-5 h-5 text-primary"/>
        </div>
        <h3 class="text-xl font-semibold">Выберите ваш регион</h3>
      </div>
      <p class="text-sm text-muted-foreground mb-4">
        Выберите регион, чтобы увидеть контактную информацию ближайших клиник.
      </p>

      <div class="grid gap-2">
        <Button
            v-for="region in REGIONS"
            :key="region.id"
            variant="outline"
            class="justify-start h-auto py-3 px-4"
            @click="handleSelect(region)"
        >
          <LucideHospital class="w-4 h-4 mr-3"/>
          <div class="text-left">
            <div class="font-medium">{{ region.name }}</div>
            <div class="text-xs">{{ region.clinics.length }} {{ region.clinics.length === 1 ? "клиника" : "клиники" }}</div>
          </div>
        </Button>
      </div>
    </div>
  </Dialog>
</template>