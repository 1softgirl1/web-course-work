<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import {
  Heart,
  Users,
  User,
  Stethoscope,
  Globe,
  LogOut,
  Menu,
  X,
} from 'lucide-vue-next'
import Button from '@/components/ui/button.vue'
import { cn } from '@/lib/utils'
import { DEFAULT_REGION_ID, resolveSelectedRegionName } from '@/stores/regionsStore'
import { useAuthStore } from '@/stores/authStore'

const route = useRoute()
const authStore = useAuthStore()
const sidebarOpen = ref(false)

const doctorNavigation = [
  { name: 'Мои пациенты', href: '/doctor/myPatients', icon: Users },
  { name: 'Все пациенты', href: '/doctor/allPatients', icon: Globe },
  { name: 'Моя карточка', href: '/doctor/doctorCard', icon: User },
]

const doctorExtendedNavigation = [
  { name: 'Все пациенты', href: '/doctor/allPatients', icon: Globe },
  { name: 'Все врачи', href: '/doctor/allDoctors', icon: Stethoscope },
  { name: 'Моя карточка', href: '/doctor/doctorCard', icon: User },
]

const navigation = computed(() => (
  authStore.isDoctorExtended.value ? doctorExtendedNavigation : doctorNavigation
))

const DOCTOR_MENU_ACTIVE_KEY = 'doctorMenuActive'

const saveActiveHref = (href) => {
  if (typeof window === 'undefined') return
  localStorage.setItem(DOCTOR_MENU_ACTIVE_KEY, href)
}

const getSavedActiveHref = () => {
  if (typeof window === 'undefined') return ''
  const savedHref = localStorage.getItem(DOCTOR_MENU_ACTIVE_KEY)
  const isKnownHref = navigation.value.some(item => item.href === savedHref)
  return isKnownHref ? savedHref : ''
}

const resolveActiveHref = () => {
  if (route.path.startsWith('/doctor/allPatients')) return '/doctor/allPatients'
  if (route.path.startsWith('/doctor/myPatients')) return '/doctor/myPatients'
  if (route.path.startsWith('/doctor/allDoctors')) return '/doctor/allDoctors'
  if (route.path.startsWith('/doctor/doctorCard')) return '/doctor/doctorCard'

  if (route.path.startsWith('/doctor/patientCard/')) {
    if (route.query.from === 'allPatients') return '/doctor/allPatients'
    if (route.query.from === 'myPatients') return '/doctor/myPatients'
  }

  if (authStore.isDoctorExtended.value) {
    return getSavedActiveHref() || '/doctor/allPatients'
  }

  return getSavedActiveHref() || '/doctor/myPatients'
}

const activeHref = ref(resolveActiveHref())
const selectedRegionName = computed(() => resolveSelectedRegionName())
const doctorFullName = ref('Борискова Д.В.')
const isDoctorExtended = computed(() => authStore.isDoctorExtended.value)

const toInitialsName = (value) => {
  const normalized = (value || '').trim().replace(/\s+/g, ' ')
  if (!normalized) return 'Врач'

  const parts = normalized.split(' ')
  if (parts.length === 1) return parts[0]

  const lastName = parts[0]
  const firstInitial = parts[1]?.charAt(0)?.toUpperCase() || ''
  const middleInitial = parts[2]?.charAt(0)?.toUpperCase() || ''

  if (!firstInitial && !middleInitial) return lastName
  if (firstInitial && middleInitial) return `${lastName} ${firstInitial}.${middleInitial}.`
  return `${lastName} ${firstInitial}.`
}

onMounted(() => {
  if (typeof window === 'undefined') return

  if (!localStorage.getItem('selectedRegion')) {
    localStorage.setItem('selectedRegion', DEFAULT_REGION_ID)
  }

  const savedDoctorName = localStorage.getItem('doctorFullName')
  if (authStore.user.value?.displayName) {
    doctorFullName.value = toInitialsName(authStore.user.value.displayName)
  } else if (savedDoctorName && savedDoctorName.trim()) {
    doctorFullName.value = toInitialsName(savedDoctorName.trim())
  } else {
    localStorage.setItem('doctorFullName', doctorFullName.value)
  }
})

watch(
  () => route.fullPath,
  () => {
    activeHref.value = resolveActiveHref()
    saveActiveHref(activeHref.value)
  },
  { immediate: true }
)

const setActivePage = (href) => {
  activeHref.value = href
  saveActiveHref(href)
}

const isActive = (href) => activeHref.value === href

const handleLogout = () => {
  authStore.logout()
}
</script>

<template>
  <div class="min-h-screen bg-background">
    <header class="sticky top-0 z-50 border-b border-border bg-card px-4 py-3 lg:hidden">
      <div class="flex items-center justify-between">
        <router-link to="/doctor" class="flex items-center gap-2">
          <div class="flex h-8 w-8 items-center justify-center rounded-lg bg-red-500">
            <Heart class="h-4 w-4 text-white" />
          </div>
          <span class="font-semibold text-foreground">КардиоРеестр</span>
        </router-link>

        <button class="p-2" @click="sidebarOpen = !sidebarOpen">
          <X v-if="sidebarOpen" class="h-5 w-5" />
          <Menu v-else class="h-5 w-5" />
        </button>
      </div>
    </header>

    <div
      v-if="sidebarOpen"
      class="fixed inset-0 z-40 bg-foreground/20 backdrop-blur-sm lg:hidden"
      @click="sidebarOpen = false"
    />

    <div class="flex">
      <aside
        :class="
          cn(
            'fixed left-0 top-0 z-50 flex h-screen w-64 flex-col border-r border-border bg-card transition-transform lg:sticky lg:z-0 lg:translate-x-0',
            sidebarOpen ? 'translate-x-0' : '-translate-x-full'
          )
        "
      >
        <div class="hidden border-b border-border p-6 lg:block">
          <router-link to="/doctor" class="flex items-center gap-2">
            <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-red-500">
              <Heart class="h-5 w-5 text-white" />
            </div>
            <div>
              <span class="block font-semibold text-foreground">КардиоРеестр</span>
              <span class="text-sm font-medium text-muted-foreground">Кабинет врача</span>
            </div>
          </router-link>
        </div>

        <div class="border-b border-border p-4">
          <div class="flex items-center gap-3">
            <div v-if="!isDoctorExtended" class="flex h-10 w-10 items-center justify-center rounded-full bg-primary/10">
              <Stethoscope class="h-5 w-5 text-primary" />
            </div>
            <div v-if="isDoctorExtended" class="flex h-10 w-10 items-center justify-center rounded-full bg-red-600/10">
              <Stethoscope class="h-5 w-5 text-red-600" />
            </div>
            <div>
              <p class="text-sm font-medium text-foreground">{{ doctorFullName }}</p>
              <p class="text-xs font-medium text-muted-foreground">
                {{ selectedRegionName }}<span v-if="isDoctorExtended"></span>
              </p>
            </div>
          </div>
        </div>

        <nav class="flex-1 space-y-2 p-4">
          <router-link
            v-for="item in navigation"
            :key="item.name"
            :to="item.href"
            :class="
              cn(
                'flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-colors',
                isActive(item.href)
                  ? 'bg-primary text-primary-foreground'
                  : 'bg-secondary text-primary hover:bg-primary hover:text-primary-foreground'
              )
            "
            @click="setActivePage(item.href); sidebarOpen = false"
          >
            <component :is="item.icon" class="h-5 w-5" />
            {{ item.name }}
          </router-link>
        </nav>

        <div class="border-t border-border p-4">
          <router-link to="/login" @click="handleLogout">
            <Button variant="ghost" class="w-full justify-start gap-3 text-muted-foreground">
              <LogOut class="h-5 w-5" />
              Выйти
            </Button>
          </router-link>
        </div>
      </aside>

      <main class="min-h-screen flex-1 lg:min-h-[calc(100vh)]">
        <router-view />
      </main>
    </div>
  </div>
</template>


