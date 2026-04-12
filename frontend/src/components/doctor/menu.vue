<script setup>
import { onMounted, ref, watch } from "vue"
import { useRoute } from "vue-router"

import {
  Heart,
  Users,
  Stethoscope,
  Globe,
  LogOut,
  Menu,
  X,
} from "lucide-vue-next"

import Button from '@/components/ui/button.vue'
import { cn } from "@/lib/utils"

// router
const route = useRoute()

// state
const sidebarOpen = ref(false)

// navigation
const navigation = [
  { name: "Мои пациенты", href: "/doctor/myPatients", icon: Users },
  { name: "Все пациенты", href: "/doctor/allPatients", icon: Globe },

]

const DOCTOR_MENU_ACTIVE_KEY = "doctorMenuActive"

const saveActiveHref = (href) => {
  if (typeof window === "undefined") return
  localStorage.setItem(DOCTOR_MENU_ACTIVE_KEY, href)
}

const getSavedActiveHref = () => {
  if (typeof window === "undefined") return ""
  const savedHref = localStorage.getItem(DOCTOR_MENU_ACTIVE_KEY)
  const isKnownHref = navigation.some(item => item.href === savedHref)
  return isKnownHref ? savedHref : ""
}

const resolveActiveHref = () => {
  if (route.path.startsWith("/doctor/allPatients")) return "/doctor/allPatients"
  if (route.path.startsWith("/doctor/myPatients")) return "/doctor/myPatients"

  if (route.path.startsWith("/doctor/patientCard/")) {
    if (route.query.from === "allPatients") return "/doctor/allPatients"
    if (route.query.from === "myPatients") return "/doctor/myPatients"
  }

  return getSavedActiveHref() || "/doctor/myPatients"
}

const activeHref = ref(resolveActiveHref())
const selectedRegionName = 'Кемерово'

onMounted(() => {
  if (typeof window === 'undefined') return
  localStorage.setItem('selectedRegion', 'kemerovo')
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

// methods
const isActive = (href) => activeHref.value === href
</script>

<template>
  <div class="min-h-screen bg-background">
    <!-- Mobile Header -->
    <header class="lg:hidden sticky top-0 z-50 bg-card border-b border-border px-4 py-3">
      <div class="flex items-center justify-between">
        <router-link to="/doctor" class="flex items-center gap-2">
          <div class="w-8 h-8 rounded-lg bg-red-500 flex items-center justify-center">
            <Heart class="w-4 h-4 text-white" />
          </div>
          <span class="font-semibold text-foreground">КардиоРеестр</span>
        </router-link>

        <button @click="sidebarOpen = !sidebarOpen" class="p-2">
          <X v-if="sidebarOpen" class="w-5 h-5" />
          <Menu v-else class="w-5 h-5" />
        </button>
      </div>
    </header>

    <!-- Overlay -->
    <div
        v-if="sidebarOpen"
        class="lg:hidden fixed inset-0 z-40 bg-foreground/20 backdrop-blur-sm"
        @click="sidebarOpen = false"
    />

    <div class="flex">
      <!-- Sidebar -->
      <aside
          :class="
          cn(
            'fixed lg:sticky top-0 left-0 z-50 lg:z-0 h-screen w-64 bg-card border-r border-border flex flex-col transition-transform lg:translate-x-0',
            sidebarOpen ? 'translate-x-0' : '-translate-x-full'
          )
        "
      >
        <!-- Logo -->
        <div class="p-6 border-b border-border hidden lg:block">
          <router-link to="/doctor" class="flex items-center gap-2">
            <div class="w-10 h-10 rounded-xl bg-red-500 flex items-center justify-center">
              <Heart class="w-5 h-5 text-white" />
            </div>
            <div>
              <span class="font-semibold text-foreground block ">КардиоРеестр</span>
              <span class="font-medium text-muted-foreground text-sm">Кабинет врача</span>
            </div>
          </router-link>
        </div>

        <!-- User -->
        <div class="p-4 border-b border-border">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center">
              <Stethoscope class="w-5 h-5 text-primary" />
            </div>
            <div>
              <p class="font-medium text-foreground text-sm">Борискова Д.В.</p>
              <p class="text-xs font-medium text-muted-foreground">{{ selectedRegionName }}</p>
            </div>
          </div>

        </div>

        <!-- Navigation -->
        <nav class="flex-1 p-4 space-y-2">
          <router-link
              v-for="item in navigation"
              :key="item.name"
              :to="item.href"
              @click="setActivePage(item.href); sidebarOpen = false"
              :class="
              cn(
                'flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-colors',
                isActive(item.href)
                  ? 'bg-primary text-primary-foreground'
                  : 'text-primary bg-secondary hover:bg-primary hover:text-primary-foreground'
              )
            "
          >
            <component :is="item.icon" class="w-5 h-5" />
            {{ item.name }}
          </router-link>
        </nav>

        <!-- Logout -->
        <div class="p-4 border-t border-border">
          <router-link to="/">
            <Button
                variant="ghost"
                class="w-full justify-start gap-3 text-muted-foreground"
            >
              <LogOut class="w-5 h-5" />
              Выйти
            </Button>
          </router-link>
        </div>
      </aside>

      <!-- Content -->
      <main class="flex-1 min-h-screen lg:min-h-[calc(100vh)]">
        <router-view/>
      </main>
    </div>
  </div>
</template>