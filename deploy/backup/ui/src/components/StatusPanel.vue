<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { CircleCheck, CircleX, Clock, Loader2, Play, RefreshCw, Send, ShieldCheck } from '@lucide/vue'
import { api, ApiError, CHECK_LABELS, formatDate, type Status } from '../api'

const status = ref<Status | null>(null)
const output = ref<string[]>([])
const message = ref('')
const busy = ref(false)
let timer: number | undefined

const running = computed(() => status.value?.running)
// "Бэкап запущен" stays only while the run is going; the result card then speaks for itself.
watch(running, (now, before) => {
  if (before && !now) message.value = ''
})
const checks = computed(() => status.value ? Object.values(status.value.checks) : [])
const pending = computed(() => checks.value.length === 0 && !status.value?.journalError)
const allOk = computed(() => checks.value.length > 0 && !status.value?.journalError && checks.value.every((c) => c.ok))

async function load() {
  status.value = await api.status()
  if (status.value.running || output.value.length === 0) output.value = (await api.output()).lines
}

async function act(fn: () => Promise<{ message: string } | unknown>, done: string) {
  busy.value = true
  message.value = ''
  try {
    const res = (await fn()) as { message?: string }
    message.value = res?.message ?? done
  } catch (e) {
    message.value = e instanceof ApiError ? e.message : 'Не удалось выполнить'
  } finally {
    busy.value = false
    await load()
  }
}

onMounted(() => {
  load()
  // Faster refresh while a run is in progress, so the log moves.
  const tick = async () => {
    await load().catch(() => {})
    timer = window.setTimeout(tick, running.value ? 2000 : 15000)
  }
  timer = window.setTimeout(tick, 2000)
})
onUnmounted(() => clearTimeout(timer))
</script>

<template>
  <div v-if="status" class="space-y-4">
    <p v-if="status.journalError" role="alert" class="rounded-xl border border-destructive/40 bg-destructive/5 p-4 text-sm text-destructive">
      Журнал операций не записывается: {{ status.journalError }}. Проверьте свободное место и права записи.
    </p>

    <section
      class="flex flex-wrap items-center gap-3 rounded-xl border p-4"
      :class="pending ? 'border-border bg-card' : allOk ? 'border-success/30 bg-success/5' : 'border-destructive/30 bg-destructive/5'"
    >
      <component :is="pending ? Clock : allOk ? CircleCheck : CircleX" class="size-7" :class="pending ? 'text-muted-foreground' : allOk ? 'text-success' : 'text-destructive'" />
      <div class="flex-1">
        <p class="font-semibold">{{ pending ? 'Ожидается первая проверка' : allOk ? 'Всё работает' : 'Есть проблемы' }}</p>
        <p class="text-sm text-muted-foreground">Проверки выполняются раз в минуту</p>
      </div>
      <button class="btn-secondary" :disabled="busy" @click="act(api.runChecks, 'Проверено')">
        <RefreshCw class="size-4" /> Проверить сейчас
      </button>
    </section>

    <section class="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
      <div v-for="(check, name) in status.checks" :key="name" class="rounded-xl border border-border bg-card p-4">
        <div class="flex items-center gap-2">
          <span class="size-2.5 rounded-full" :class="check.ok ? 'bg-success' : 'bg-destructive'" />
          <span class="font-medium">{{ CHECK_LABELS[name] ?? name }}</span>
        </div>
        <p class="mt-2 text-sm" :class="check.ok ? 'text-muted-foreground' : 'text-destructive'">{{ check.detail }}</p>
        <p class="mt-1 text-xs text-muted-foreground">{{ formatDate(check.checkedAt) }}</p>
      </div>
    </section>

    <section class="grid gap-3 lg:grid-cols-3">
      <div class="rounded-xl border border-border bg-card p-4 lg:col-span-2">
        <h2 class="font-semibold">Последний бэкап</h2>
        <template v-if="status.lastBackup">
          <p class="mt-2 flex items-center gap-2">
            <component :is="status.lastBackup.result === 'ok' ? CircleCheck : CircleX" class="size-5" :class="status.lastBackup.result === 'ok' ? 'text-success' : 'text-destructive'" />
            {{ formatDate(status.lastBackup.time) }}
            <span class="text-sm text-muted-foreground">
              · {{ status.lastBackup.trigger === 'manual' ? 'вручную' : 'по расписанию' }}
              <template v-if="status.lastBackup.durationSec != null">· {{ status.lastBackup.durationSec }} с</template>
            </span>
          </p>
          <p class="mt-1 text-sm text-muted-foreground">{{ status.lastBackup.text }}</p>
          <p v-if="status.lastBackup.result !== 'ok' && status.lastSuccess" class="mt-1 text-sm">
            Последний успешный: {{ formatDate(status.lastSuccess.time) }}
          </p>
        </template>
        <p v-else class="mt-2 text-sm text-muted-foreground">Бэкапов ещё не было</p>

        <div class="mt-4 flex flex-wrap gap-2">
          <button class="btn-primary" :disabled="busy || !!running" @click="act(api.runBackup, 'Бэкап запущен')">
            <Loader2 v-if="running?.kind === 'backup'" class="size-4 animate-spin" />
            <Play v-else class="size-4" />
            {{ running?.kind === 'backup' ? 'Выполняется…' : 'Бэкап сейчас' }}
          </button>
          <button class="btn-secondary" :disabled="busy || !!running" @click="act(api.runVerify, 'Проверка запущена')">
            <Loader2 v-if="running?.kind === 'verify'" class="size-4 animate-spin" />
            <ShieldCheck v-else class="size-4" />
            Проверить копии
          </button>
        </div>
        <p v-if="message" class="mt-2 text-sm text-muted-foreground">{{ message }}</p>
      </div>

      <div class="space-y-3">
        <div class="rounded-xl border border-border bg-card p-4">
          <h2 class="flex items-center gap-2 font-semibold"><Clock class="size-4" /> Следующий запуск</h2>
          <p class="mt-2">{{ formatDate(status.nextRun) }}</p>
          <p class="mt-1 text-sm text-muted-foreground">
            Проверка копий: {{ status.lastVerify ? formatDate(status.lastVerify.time) + (status.lastVerify.result === 'ok' ? ' · ок' : ' · ошибка') : 'ещё не было' }}
          </p>
        </div>
        <div class="rounded-xl border border-border bg-card p-4">
          <h2 class="flex items-center gap-2 font-semibold"><Send class="size-4" /> Связь с ботом</h2>
          <p v-if="!status.notifier.configured" class="mt-2 text-sm text-muted-foreground">Не настроена (NOTIFIER_URL)</p>
          <p v-else-if="status.notifier.lastError" class="mt-2 text-sm text-destructive">Ошибка: {{ status.notifier.lastError }}</p>
          <p v-else class="mt-2 text-sm">Последний сигнал: {{ formatDate(status.notifier.lastOkAt) }}</p>
        </div>
      </div>
    </section>

    <details v-if="output.length" class="rounded-xl border border-border bg-card p-4" :open="!!running">
      <summary class="cursor-pointer font-semibold">Ход последнего запуска</summary>
      <pre class="mt-3 max-h-80 overflow-auto rounded-lg bg-muted p-3 text-xs leading-relaxed whitespace-pre-wrap">{{ output.join('\n') }}</pre>
    </details>
  </div>
</template>

<style scoped>
@reference '../style.css';
.btn-primary {
  @apply inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:opacity-90 disabled:opacity-50;
}
.btn-secondary {
  @apply inline-flex items-center gap-2 rounded-lg border border-border bg-card px-4 py-2 text-sm font-medium hover:bg-muted disabled:opacity-50;
}
</style>
