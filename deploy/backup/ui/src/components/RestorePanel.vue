<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { AlertTriangle, ArchiveRestore, Database, Loader2, RefreshCw, Trash2, Undo2 } from '@lucide/vue'
import { api, ApiError, formatDate, formatSize, TIER_LABELS, type Copies, type DatabaseInfo, type RunState } from '../api'

const copies = ref<Copies | null>(null)
const databases = ref<DatabaseInfo[]>([])
const running = ref<RunState | undefined>()
const output = ref<string[]>([])

// Step 1: restore into a new database
const source = ref<'local' | 'cloud' | 'network'>('cloud')
const file = ref('')
const target = ref(`restored_${new Date().toISOString().slice(0, 10).replaceAll('-', '')}`)
const key = ref('')
const restoreMessage = ref<{ ok: boolean; text: string } | null>(null)

// Steps 2-3: switch / drop with a typed confirmation
const action = ref<{ kind: 'switch' | 'drop'; db: DatabaseInfo } | null>(null)
const confirm = ref('')
const actionMessage = ref<{ ok: boolean; text: string } | null>(null)
const busy = ref(false)
let timer: number | undefined

const files = computed(() => {
  if (!copies.value) return []
  if (source.value === 'cloud')
    return copies.value.cloud.copies.map((c) => ({ value: c.key, label: `${TIER_LABELS[c.key.split('/')[0]] ?? ''} · ${formatDate(c.uploadedAt)} · ${formatSize(c.sizeBytes)}` }))
  const list = source.value === 'network' ? copies.value.network.copies : copies.value.local
  return list.map((c) => ({ value: `${c.tier}/${c.file}`, label: `${TIER_LABELS[c.tier]} · ${formatDate(c.createdAt)} · ${formatSize(c.sizeBytes)}` }))
})
const live = computed(() => databases.value.find((d) => d.role === 'live'))
const restoring = computed(() => running.value?.kind === 'restore')

watch(files, (list) => {
  if (!list.some((f) => f.value === file.value)) file.value = list[0]?.value ?? ''
})

const loadingCopies = ref(false)
async function loadCopies() {
  loadingCopies.value = true
  try {
    copies.value = await api.copies(true) // always fresh: a copy made a minute ago must be in the list
  } finally {
    loadingCopies.value = false
  }
}

async function loadDatabases() {
  databases.value = await api.databases()
}

async function poll() {
  const out = await api.output()
  const wasRestoring = restoring.value
  running.value = out.running
  if (out.running?.kind === 'restore' || wasRestoring) output.value = out.lines
  if (wasRestoring && !out.running) {
    const last = (await api.journal('restore'))[0]
    restoreMessage.value = last ? { ok: last.result === 'ok', text: last.text } : null
    await loadDatabases()
  }
}

async function startRestore() {
  restoreMessage.value = null
  try {
    await api.restore({ source: source.value, file: file.value, target: target.value, key: key.value })
    output.value = []
    running.value = { kind: 'restore', trigger: 'manual', startedAt: new Date().toISOString() }
  } catch (e) {
    restoreMessage.value = { ok: false, text: e instanceof ApiError ? e.message : 'Не удалось запустить' }
  } finally {
    key.value = '' // the key is used once and not kept in the page
  }
}

function ask(kind: 'switch' | 'drop', db: DatabaseInfo) {
  action.value = { kind, db }
  confirm.value = ''
  actionMessage.value = null
}

async function runAction() {
  if (!action.value) return
  busy.value = true
  const { kind, db } = action.value
  try {
    const res = kind === 'switch' ? await api.switchDatabase(db.name, confirm.value) : await api.dropDatabase(db.name, confirm.value)
    actionMessage.value = { ok: true, text: res.message }
    action.value = null
  } catch (e) {
    actionMessage.value = { ok: false, text: e instanceof ApiError ? e.message : 'Не удалось выполнить' }
  } finally {
    busy.value = false
    await loadDatabases()
  }
}

const roleLabel = (r: string) => (r === 'live' ? 'рабочая' : r === 'broken' ? 'прежняя (до переключения)' : 'восстановленная')
const roleClass = (r: string) => (r === 'live' ? 'bg-success/10 text-success' : r === 'broken' ? 'bg-muted text-muted-foreground' : 'bg-primary/10 text-primary')

onMounted(async () => {
  await Promise.all([loadCopies(), loadDatabases()])
  await poll()
  const tick = async () => {
    await poll().catch(() => {})
    timer = window.setTimeout(tick, restoring.value ? 1500 : 5000)
  }
  timer = window.setTimeout(tick, 1500)
})
onUnmounted(() => clearTimeout(timer))
</script>

<template>
  <div class="space-y-4">
    <section class="rounded-xl border border-border bg-card p-4">
      <h2 class="flex items-center gap-2 font-semibold"><ArchiveRestore class="size-4" /> 1. Восстановить копию в новую базу</h2>
      <p class="mt-1 text-sm text-muted-foreground">
        Рабочая база не меняется: копия разворачивается рядом, в отдельную базу. Сайт переключается на неё только на шаге 2.
      </p>
      <form class="mt-4 grid gap-3 text-sm" @submit.prevent="startRestore">
        <div class="flex flex-wrap items-center gap-3">
          <span class="w-40">Откуда</span>
          <label class="flex items-center gap-1.5"><input v-model="source" type="radio" value="cloud" /> из облака</label>
          <label class="flex items-center gap-1.5"><input v-model="source" type="radio" value="local" /> с сервера</label>
          <label v-if="copies?.network.configured" class="flex items-center gap-1.5"><input v-model="source" type="radio" value="network" /> с NFS (/opt1)</label>
        </div>
        <label class="flex flex-wrap items-center gap-3">
          <span class="w-40">Копия</span>
          <select v-model="file" required class="min-w-72 flex-1 rounded-lg border border-border px-3 py-2">
            <option v-if="loadingCopies && !files.length" value="">загружается…</option>
            <option v-for="f in files" :key="f.value" :value="f.value">{{ f.label }}</option>
          </select>
          <button type="button" class="inline-flex items-center gap-1.5 rounded-lg border border-border px-3 py-2 hover:bg-muted" :disabled="loadingCopies" @click="loadCopies">
            <RefreshCw class="size-4" :class="loadingCopies && 'animate-spin'" /> Обновить
          </button>
          <span class="text-muted-foreground">{{ files.length }} {{ source === 'cloud' ? 'в облаке' : source === 'network' ? 'на NFS' : 'на сервере' }}</span>
        </label>
        <p v-if="source === 'network' && copies?.network.error" class="text-destructive">Хранилище недоступно: {{ copies.network.error }}</p>
        <label class="flex flex-wrap items-center gap-3">
          <span class="w-40">Новая база</span>
          <input v-model="target" required pattern="[a-z][a-z0-9_]{2,50}" class="w-72 rounded-lg border border-border px-3 py-2" />
          <span class="text-muted-foreground">латиница, цифры и _</span>
        </label>
        <label class="flex flex-wrap items-center gap-3">
          <span class="w-40">Закрытый ключ</span>
          <input
            v-model="key"
            type="password"
            required
            autocomplete="off"
            placeholder="AGE-SECRET-KEY-…"
            class="min-w-72 flex-1 rounded-lg border border-border px-3 py-2 font-mono"
          />
        </label>
        <p class="text-muted-foreground">Ключ используется один раз и нигде не сохраняется — ни на сервере, ни в браузере.</p>
        <div class="flex flex-wrap items-center gap-3">
          <button
            class="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 font-medium text-primary-foreground hover:opacity-90 disabled:opacity-50"
            :disabled="!!running || !file"
          >
            <Loader2 v-if="restoring" class="size-4 animate-spin" />
            <ArchiveRestore v-else class="size-4" />
            {{ restoring ? 'Восстанавливается…' : 'Восстановить' }}
          </button>
          <span v-if="running && !restoring" class="text-muted-foreground">Сейчас выполняется: {{ running.kind }}</span>
          <span v-if="restoreMessage" :class="restoreMessage.ok ? 'text-success' : 'text-destructive'">{{ restoreMessage.text }}</span>
        </div>
      </form>
      <pre v-if="output.length" class="mt-3 max-h-48 overflow-auto rounded-lg bg-muted p-3 text-xs whitespace-pre-wrap">{{ output.join('\n') }}</pre>
    </section>

    <section class="rounded-xl border border-border bg-card">
      <div class="flex items-center gap-2 border-b border-border p-4">
        <h2 class="flex flex-1 items-center gap-2 font-semibold"><Database class="size-4" /> 2. Базы данных</h2>
        <button class="inline-flex items-center gap-1.5 rounded-lg border border-border px-3 py-1.5 text-sm hover:bg-muted" @click="loadDatabases">
          <RefreshCw class="size-4" /> Обновить
        </button>
      </div>
      <div class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead class="text-left text-muted-foreground">
            <tr>
              <th class="px-4 py-2">База</th><th class="px-4 py-2">Пациентов</th><th class="px-4 py-2">Обследований</th>
              <th class="px-4 py-2">Последнее</th><th class="px-4 py-2">Схема</th><th class="px-4 py-2">Размер</th><th class="px-4 py-2"></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="db in databases" :key="db.name" class="border-t border-border">
              <td class="px-4 py-2">
                <div class="font-medium">{{ db.name }}</div>
                <span class="rounded-full px-2 py-0.5 text-xs" :class="roleClass(db.role)">{{ roleLabel(db.role) }}</span>
              </td>
              <td class="px-4 py-2">{{ db.patients ?? '—' }}</td>
              <td class="px-4 py-2">{{ db.examinations ?? '—' }}</td>
              <td class="px-4 py-2 whitespace-nowrap">{{ db.lastExam ?? '—' }}</td>
              <td class="px-4 py-2" :class="db.role !== 'live' && live && db.schemaVersion !== live.schemaVersion ? 'text-destructive' : ''">
                {{ db.schemaVersion ?? '—' }}
              </td>
              <td class="px-4 py-2 whitespace-nowrap">{{ formatSize(db.sizeBytes) }}</td>
              <td class="px-4 py-2">
                <div class="flex flex-wrap justify-end gap-2">
                <button
                  v-if="db.role !== 'live'"
                  class="inline-flex items-center gap-1 whitespace-nowrap rounded-lg bg-primary px-3 py-1.5 text-primary-foreground hover:opacity-90 disabled:opacity-50"
                  :disabled="!!running"
                  @click="ask('switch', db)"
                >
                  <Undo2 class="size-4" /> {{ db.role === 'broken' ? 'Вернуть рабочей' : 'Сделать рабочей' }}
                </button>
                <button
                  v-if="db.role !== 'live'"
                  class="inline-flex items-center gap-1 whitespace-nowrap rounded-lg border border-border px-3 py-1.5 hover:bg-muted disabled:opacity-50"
                  :disabled="!!running"
                  @click="ask('drop', db)"
                >
                  <Trash2 class="size-4" /> Удалить
                </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="action" class="m-4 rounded-xl border border-destructive/40 bg-destructive/5 p-4 text-sm">
        <p class="flex items-start gap-2 font-medium">
          <AlertTriangle class="mt-0.5 size-4 shrink-0 text-destructive" />
          <span v-if="action.kind === 'switch'">
            Сайт начнёт работать на базе «{{ action.db.name }}» ({{ action.db.patients }} пациентов, {{ action.db.examinations }} обследований).
            Текущая рабочая база не удаляется — она будет переименована в «…_broken_…». На несколько секунд сайт будет недоступен.
          </span>
          <span v-else>База «{{ action.db.name }}» будет удалена безвозвратно.</span>
        </p>
        <label class="mt-3 flex flex-wrap items-center gap-3">
          <span>Для подтверждения введите имя базы:</span>
          <input v-model="confirm" class="w-72 rounded-lg border border-border px-3 py-2" :placeholder="action.db.name" />
        </label>
        <div class="mt-3 flex gap-2">
          <button
            class="rounded-lg bg-destructive px-4 py-2 font-medium text-white hover:opacity-90 disabled:opacity-50"
            :disabled="busy || confirm !== action.db.name"
            @click="runAction"
          >
            {{ action.kind === 'switch' ? 'Переключить сайт' : 'Удалить базу' }}
          </button>
          <button class="rounded-lg border border-border px-4 py-2 hover:bg-muted" @click="action = null">Отмена</button>
        </div>
      </div>
      <p v-if="actionMessage" class="px-4 pb-4 text-sm" :class="actionMessage.ok ? 'text-success' : 'text-destructive'">{{ actionMessage.text }}</p>
    </section>

    <p class="text-sm text-muted-foreground">
      В списке доступны рабочая база и базы, созданные или отложенные этим сервисом. Посторонние и служебные базы не предлагаются.
    </p>
    <p class="text-sm text-muted-foreground">
      Каждое действие записывается в журнал и приходит в Telegram. Если копия старее текущей схемы базы (колонка «Схема» красная),
      переключайте через консоль: <code>sudo sh deploy/backup/switch-db.sh &lt;база&gt;</code> — бэкенд перезапустится и догонит миграции.
    </p>
  </div>
</template>
