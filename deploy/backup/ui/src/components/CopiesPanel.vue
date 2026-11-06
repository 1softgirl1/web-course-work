<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Cloud, HardDrive, Lock, RefreshCw } from '@lucide/vue'
import { api, formatDate, formatSize, TIER_LABELS, type Copies } from '../api'

const copies = ref<Copies | null>(null)
const loading = ref(false)
const diskCopies = computed(() => [
  { label: 'На сервере', copies: copies.value?.local, error: '' },
  ...(copies.value?.network.configured
    ? [{ label: 'На хранилище NFS (/opt1)', copies: copies.value.network.copies, error: copies.value.network.error }]
    : []),
])

async function load(refresh = false) {
  loading.value = true
  try {
    copies.value = await api.copies(refresh)
  } finally {
    loading.value = false
  }
}

const tierOf = (key: string) => TIER_LABELS[key.split('/')[0]] ?? key.split('/')[0]
const fileOf = (key: string) => key.split('/').slice(1).join('/')

onMounted(() => load())
</script>

<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between">
      <p class="text-sm text-muted-foreground">
        Копии зашифрованы открытым ключом; расшифровать их можно только закрытым ключом, которого на сервере нет.
      </p>
      <button class="inline-flex items-center gap-2 rounded-lg border border-border bg-card px-3 py-1.5 text-sm hover:bg-muted" :disabled="loading" @click="load(true)">
        <RefreshCw class="size-4" :class="loading && 'animate-spin'" /> Обновить
      </button>
    </div>

    <section v-for="store in diskCopies" :key="store.label" class="rounded-xl border border-border bg-card">
      <h2 class="flex items-center gap-2 border-b border-border p-4 font-semibold">
        <HardDrive class="size-4" /> {{ store.label }}
        <span class="text-sm font-normal text-muted-foreground">{{ store.copies?.length ?? '…' }}</span>
      </h2>
      <p v-if="store.error" class="p-4 text-sm text-destructive">Хранилище недоступно: {{ store.error }}</p>
      <div v-else class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead class="text-left text-muted-foreground">
            <tr><th class="px-4 py-2">Тип</th><th class="px-4 py-2">Файл</th><th class="px-4 py-2">Размер</th><th class="px-4 py-2">Создан</th></tr>
          </thead>
          <tbody>
            <tr v-for="c in store.copies" :key="c.tier + c.file" class="border-t border-border">
              <td class="px-4 py-2">{{ TIER_LABELS[c.tier] ?? c.tier }}</td>
              <td class="px-4 py-2 break-all">{{ c.file }}</td>
              <td class="px-4 py-2 whitespace-nowrap">{{ formatSize(c.sizeBytes) }}</td>
              <td class="px-4 py-2 whitespace-nowrap">{{ formatDate(c.createdAt) }}</td>
            </tr>
            <tr v-if="store.copies && !store.copies.length"><td colspan="4" class="px-4 py-6 text-center text-muted-foreground">Копий пока нет</td></tr>
          </tbody>
        </table>
      </div>
    </section>

    <section class="rounded-xl border border-border bg-card">
      <h2 class="flex items-center gap-2 border-b border-border p-4 font-semibold">
        <Cloud class="size-4" /> В облаке
        <span class="text-sm font-normal text-muted-foreground">{{ copies?.cloud.copies.length ?? '…' }}</span>
      </h2>
      <p v-if="copies && !copies.cloud.configured" class="p-4 text-sm text-muted-foreground">Облако не настроено (S3_ENDPOINT).</p>
      <p v-else-if="copies?.cloud.error" class="p-4 text-sm text-destructive">Не удалось получить список: {{ copies.cloud.error }}</p>
      <div v-else class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead class="text-left text-muted-foreground">
            <tr><th class="px-4 py-2">Тип</th><th class="px-4 py-2">Файл</th><th class="px-4 py-2">Размер</th><th class="px-4 py-2">Загружен</th><th class="px-4 py-2">Защищён от удаления до</th></tr>
          </thead>
          <tbody>
            <tr v-for="c in copies?.cloud.copies" :key="c.key" class="border-t border-border">
              <td class="px-4 py-2">{{ tierOf(c.key) }}</td>
              <td class="px-4 py-2 break-all">{{ fileOf(c.key) }}</td>
              <td class="px-4 py-2 whitespace-nowrap">{{ formatSize(c.sizeBytes) }}</td>
              <td class="px-4 py-2 whitespace-nowrap">{{ formatDate(c.uploadedAt) }}</td>
              <td class="px-4 py-2 whitespace-nowrap">
                <span v-if="c.lockedUntil" class="inline-flex items-center gap-1" :title="c.lockMode">
                  <Lock class="size-3.5 text-success" /> {{ formatDate(c.lockedUntil) }}
                </span>
                <span v-else class="text-muted-foreground">срок вышел</span>
              </td>
            </tr>
            <tr v-if="copies && !copies.cloud.copies.length"><td colspan="5" class="px-4 py-6 text-center text-muted-foreground">Копий пока нет</td></tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>
