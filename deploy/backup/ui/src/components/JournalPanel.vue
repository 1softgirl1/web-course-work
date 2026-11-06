<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { api, formatDate, TYPE_LABELS, type JournalEntry } from '../api'

const type = ref('')
const entries = ref<JournalEntry[]>([])

const load = async () => (entries.value = await api.journal(type.value))
watch(type, load)
onMounted(load)

const resultClass = (r: string) => (r === 'ok' ? 'text-success' : r === 'failed' ? 'text-destructive' : 'text-muted-foreground')
const resultLabel = (r: string) => (r === 'ok' ? 'успешно' : r === 'failed' ? 'ошибка' : 'инфо')
</script>

<template>
  <section class="rounded-xl border border-border bg-card">
    <div class="flex flex-wrap items-center gap-3 border-b border-border p-4">
      <h2 class="flex-1 font-semibold">Журнал</h2>
      <select v-model="type" class="rounded-lg border border-border px-3 py-1.5 text-sm">
        <option value="">Все события</option>
        <option v-for="(label, key) in TYPE_LABELS" :key="key" :value="key">{{ label }}</option>
      </select>
    </div>
    <div class="overflow-x-auto">
      <table class="w-full text-sm">
        <thead class="text-left text-muted-foreground">
          <tr><th class="px-4 py-2">Время</th><th class="px-4 py-2">Событие</th><th class="px-4 py-2">Итог</th><th class="px-4 py-2">Подробности</th></tr>
        </thead>
        <tbody>
          <tr v-for="(e, i) in entries" :key="i" class="border-t border-border align-top">
            <td class="px-4 py-2 whitespace-nowrap">{{ formatDate(e.time) }}</td>
            <td class="px-4 py-2 whitespace-nowrap">
              {{ TYPE_LABELS[e.type] ?? e.type }}
              <span v-if="e.trigger" class="text-muted-foreground">· {{ e.trigger === 'manual' ? 'вручную' : 'по расписанию' }}</span>
            </td>
            <td class="px-4 py-2 whitespace-nowrap" :class="resultClass(e.result)">{{ resultLabel(e.result) }}</td>
            <td class="px-4 py-2">
              {{ e.text }}
              <span v-if="e.durationSec != null" class="text-muted-foreground">· {{ e.durationSec }} с</span>
            </td>
          </tr>
          <tr v-if="!entries.length"><td colspan="4" class="px-4 py-6 text-center text-muted-foreground">Записей нет</td></tr>
        </tbody>
      </table>
    </div>
  </section>
</template>
