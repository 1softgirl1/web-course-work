<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Save } from '@lucide/vue'
import { api, ApiError, WEEKDAYS, type Settings } from '../api'

const form = ref<Settings | null>(null)
const errors = ref<Record<string, string>>({})
const result = ref<{ ok: boolean; lines: string[] } | null>(null)
const saving = ref(false)

async function save() {
  if (!form.value) return
  saving.value = true
  errors.value = {}
  result.value = null
  try {
    const res = await api.saveSettings(form.value)
    form.value = res.settings
    result.value = { ok: true, lines: res.changes.length ? res.changes : ['Изменений нет'] }
  } catch (e) {
    if (e instanceof ApiError) errors.value = e.fields
    result.value = { ok: false, lines: [e instanceof ApiError ? e.message : 'Не удалось сохранить'] }
  } finally {
    saving.value = false
  }
}

onMounted(async () => (form.value = await api.settings()))
</script>

<template>
  <form v-if="form" class="max-w-2xl space-y-4" @submit.prevent="save">
    <section class="rounded-xl border border-border bg-card p-4">
      <h2 class="font-semibold">Ежедневный бэкап</h2>
      <label class="mt-3 flex flex-wrap items-center gap-3 text-sm">
        <span class="w-56">Время запуска (по серверу)</span>
        <input v-model="form.dailyTime" type="time" required class="rounded-lg border border-border px-3 py-2 outline-none focus:border-primary" />
      </label>
      <p v-if="errors.dailyTime" class="mt-1 text-sm text-destructive">{{ errors.dailyTime }}</p>
      <p class="mt-2 text-sm text-muted-foreground">Лучше ночью, когда в регистре никто не работает. Бэкап занимает несколько секунд.</p>
    </section>

    <details class="rounded-xl border border-border bg-card p-4">
      <summary class="cursor-pointer font-semibold">Расширенные настройки</summary>
      <p class="mt-2 text-sm text-muted-foreground">
        Сколько копий хранить. Копия в облаке защищена от удаления ровно на этот срок; уменьшение срока не снимет защиту с уже загруженных копий.
      </p>
      <div class="mt-3 grid gap-3 text-sm">
        <label class="flex flex-wrap items-center gap-3">
          <span class="w-56">Ежедневных копий</span>
          <input v-model.number="form.keepDaily" type="number" min="1" max="60" class="w-24 rounded-lg border border-border px-3 py-2" />
          <span class="text-muted-foreground">от 1 до 60</span>
        </label>
        <p v-if="errors.keepDaily" class="text-destructive">{{ errors.keepDaily }}</p>
        <label class="flex flex-wrap items-center gap-3">
          <span class="w-56">Недельных копий</span>
          <input v-model.number="form.keepWeekly" type="number" min="1" max="52" class="w-24 rounded-lg border border-border px-3 py-2" />
          <span class="text-muted-foreground">от 1 до 52</span>
        </label>
        <p v-if="errors.keepWeekly" class="text-destructive">{{ errors.keepWeekly }}</p>
        <label class="flex flex-wrap items-center gap-3">
          <span class="w-56">День недельной копии</span>
          <select v-model.number="form.weeklyDay" class="rounded-lg border border-border px-3 py-2">
            <option v-for="(day, i) in WEEKDAYS" :key="day" :value="i + 1">{{ day }}</option>
          </select>
        </label>
        <label class="flex flex-wrap items-center gap-3">
          <span class="w-56">Месячных копий</span>
          <input v-model.number="form.keepMonthly" type="number" min="1" max="60" class="w-24 rounded-lg border border-border px-3 py-2" />
          <span class="text-muted-foreground">от 1 до 60</span>
        </label>
        <p v-if="errors.keepMonthly" class="text-destructive">{{ errors.keepMonthly }}</p>
        <label class="flex flex-wrap items-center gap-3">
          <span class="w-56">День месячной копии</span>
          <input v-model.number="form.monthlyDay" type="number" min="1" max="28" class="w-24 rounded-lg border border-border px-3 py-2" />
          <span class="text-muted-foreground">от 1 до 28</span>
        </label>
        <p v-if="errors.monthlyDay" class="text-destructive">{{ errors.monthlyDay }}</p>
      </div>
    </details>

    <div class="flex flex-wrap items-start gap-3">
      <button class="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:opacity-90 disabled:opacity-50" :disabled="saving">
        <Save class="size-4" /> Сохранить
      </button>
      <ul v-if="result" class="text-sm" :class="result.ok ? 'text-muted-foreground' : 'text-destructive'">
        <li v-for="line in result.lines" :key="line">{{ line }}</li>
      </ul>
    </div>
    <p class="text-sm text-muted-foreground">Изменения записываются в журнал и приходят в Telegram.</p>
  </form>
</template>
