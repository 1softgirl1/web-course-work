<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Activity, Archive, ArchiveRestore, DatabaseBackup, History, LogOut, Settings as SettingsIcon } from '@lucide/vue'
import { api, ApiError, setUnauthorizedHandler } from './api'
import StatusPanel from './components/StatusPanel.vue'
import CopiesPanel from './components/CopiesPanel.vue'
import SettingsPanel from './components/SettingsPanel.vue'
import JournalPanel from './components/JournalPanel.vue'
import RestorePanel from './components/RestorePanel.vue'

const tabs = [
  { id: 'status', label: 'Статус', icon: Activity },
  { id: 'copies', label: 'Копии', icon: Archive },
  { id: 'restore', label: 'Восстановление', icon: ArchiveRestore },
  { id: 'settings', label: 'Настройки', icon: SettingsIcon },
  { id: 'journal', label: 'Журнал', icon: History },
] as const
type Tab = (typeof tabs)[number]['id']

const user = ref<string | null>(null)
const checking = ref(true)
const tab = ref<Tab>((location.hash.slice(1) as Tab) || 'status')
const username = ref('admin')
const password = ref('')
const loginError = ref('')

setUnauthorizedHandler(() => (user.value = null))
window.addEventListener('hashchange', () => {
  const id = location.hash.slice(1) as Tab
  if (tabs.some((t) => t.id === id)) tab.value = id
})

const select = (id: Tab) => {
  tab.value = id
  location.hash = id
}

async function login() {
  loginError.value = ''
  try {
    await api.login(username.value, password.value)
    password.value = ''
    user.value = (await api.me()).user
  } catch (e) {
    loginError.value = e instanceof ApiError && e.status === 401 ? 'Неверный логин или пароль' : 'Сервис недоступен'
  }
}

async function logout() {
  await api.logout().catch(() => {})
  user.value = null
}

onMounted(async () => {
  try {
    user.value = (await api.me()).user
  } catch {
    user.value = null
  } finally {
    checking.value = false
  }
})
</script>

<template>
  <div v-if="checking" />

  <main v-else-if="!user" class="flex min-h-screen items-center justify-center p-4">
    <form class="w-full max-w-sm space-y-4 rounded-xl border border-border bg-card p-6 shadow-sm" @submit.prevent="login">
      <div class="flex items-center gap-3">
        <DatabaseBackup class="size-8 text-primary" />
        <div>
          <h1 class="text-lg font-semibold">Бэкапы регистра</h1>
          <p class="text-sm text-muted-foreground">Служебный доступ</p>
        </div>
      </div>
      <label class="block space-y-1 text-sm">
        <span>Логин</span>
        <input v-model="username" autocomplete="username" required class="w-full rounded-lg border border-border px-3 py-2 outline-none focus:border-primary" />
      </label>
      <label class="block space-y-1 text-sm">
        <span>Пароль</span>
        <input v-model="password" type="password" autocomplete="current-password" required class="w-full rounded-lg border border-border px-3 py-2 outline-none focus:border-primary" />
      </label>
      <p v-if="loginError" class="text-sm text-destructive">{{ loginError }}</p>
      <button class="w-full rounded-lg bg-primary px-4 py-2 font-medium text-primary-foreground hover:opacity-90">Войти</button>
    </form>
  </main>

  <div v-else class="min-h-screen">
    <header class="sticky top-0 z-10 border-b border-border bg-card/95 backdrop-blur">
      <div class="mx-auto flex max-w-6xl flex-wrap items-center gap-3 px-4 py-3">
        <div class="flex items-center gap-2 font-semibold">
          <DatabaseBackup class="size-6 text-primary" />
          Бэкапы регистра
        </div>
        <nav class="flex flex-1 flex-wrap gap-1 sm:justify-center">
          <button
            v-for="t in tabs"
            :key="t.id"
            class="flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-sm"
            :class="tab === t.id ? 'bg-secondary font-medium text-primary' : 'text-muted-foreground hover:bg-muted'"
            @click="select(t.id)"
          >
            <component :is="t.icon" class="size-4" />
            {{ t.label }}
          </button>
        </nav>
        <button class="flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground" @click="logout">
          {{ user }} <LogOut class="size-4" />
        </button>
      </div>
    </header>
    <main class="mx-auto max-w-6xl p-4">
      <StatusPanel v-if="tab === 'status'" />
      <CopiesPanel v-else-if="tab === 'copies'" />
      <RestorePanel v-else-if="tab === 'restore'" />
      <SettingsPanel v-else-if="tab === 'settings'" />
      <JournalPanel v-else />
    </main>
  </div>
</template>
