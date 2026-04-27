<script setup lang="ts">
import { ref, watch } from 'vue'
import Dialog from '../ui/dialog.vue'
import Button from '../ui/button.vue'
import { MapPin } from 'lucide-vue-next'

const props = defineProps<{
  open: boolean
  regionName: string
}>()

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'confirm'): void
  (e: 'reject'): void
}>()

const localOpen = ref(props.open)

watch(() => props.open, (value) => {
  localOpen.value = value
})

watch(localOpen, (value) => {
  emit('update:open', value)
})

const onConfirm = () => {
  emit('confirm')
}

const onReject = () => {
  emit('reject')
}
</script>

<template>
  <Dialog v-model="localOpen" content-class="sm:max-w-md">
    <div class="space-y-4">
      <div class="flex items-center gap-3">
        <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/10">
          <MapPin class="h-5 w-5 text-primary" />
        </div>
        <h3 class="text-lg font-semibold">Вы находитесь в {{ regionName }}?</h3>
      </div>

      <div class="flex flex-col gap-2 sm:flex-row sm:justify-end">
        <Button variant="outline" @click="onReject">Нет, выбрать другой</Button>
        <Button @click="onConfirm">Да</Button>
      </div>
    </div>
  </Dialog>
</template>

