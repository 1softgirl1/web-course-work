<script setup lang="ts">
import { ref, onMounted } from "vue"
import { RouterLink } from "vue-router"
import Button from './ui/button.vue'
import type { Region } from "../data/regions"
import { REGIONS } from "../data/regions"
import { ShieldCheck, User, Phone, Mail, MapPin, LucideHospital } from "lucide-vue-next"
import RegionModal from "@/components/RegionModal.vue"

const TEXTS = {
  badge: "Медицинский регистр пациентов",
  title: "У вашего ребенка редкое заболевание сердца?",
  subtitle: "Получите удобный доступ к хранению медицинских данных и обследований при врожденных пороках сердца — в единой системе для пациентов, представителей и врачей.",
  cta: "Запишитесь в клинику или свяжитесь с нами, чтобы получить доступ к личному кабинету.",
  patientBtn: "Вход для пациентов",
  clinicsTitle: "Клиники в вашем регионе",
  changeRegion: "Изменить регион",
}

const selectedRegion = ref<Region | null>(null)
const mounted = ref(false)
const openModal = ref(false)

// Обработчик выбора региона из модалки
const handleRegionSelect = (region: Region) => {
  selectedRegion.value = region
  localStorage.setItem("selectedRegion", region.id)
  openModal.value = false
}

// Кнопка «Изменить регион»
const handleChangeRegionClick = () => {
  openModal.value = true
}

onMounted(() => {
  mounted.value = true

  const defaultRegion = REGIONS.find(r => r.id === "moscow") || REGIONS[0]

  const savedRegionId = localStorage.getItem("selectedRegion")
  if (savedRegionId) {
    const region = REGIONS.find(r => r.id === savedRegionId)
    selectedRegion.value = region || defaultRegion
  } else {
    selectedRegion.value = defaultRegion
    openModal.value = true // показать модалку при первом посещении
  }
})
</script>

<template>
  <section class="py-16 lg:py-24">
    <div class="container mx-auto px-4 sm:px-6 lg:px-8">
      <div class="max-w-4xl mx-auto text-center mb-12">
        <div class="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-primary/10 text-primary text-sm font-medium mb-8">
          <ShieldCheck class="w-4 h-4" /> {{ TEXTS.badge }}
        </div>

        <h1 class="text-3xl sm:text-4xl lg:text-5xl font-bold text-foreground leading-tight mb-6">{{ TEXTS.title }}</h1>
        <p class="text-lg sm:text-xl text-muted-foreground mb-6 max-w-3xl mx-auto">{{ TEXTS.subtitle }}</p>
        <p class="text-base text-foreground font-medium mb-8">{{ TEXTS.cta }}</p>

        <RouterLink to="/login?role=patient">
          <Button size="lg" class="mb-8"><User class="w-4 h-4 mr-2" /> {{ TEXTS.patientBtn }}</Button>
        </RouterLink>
      </div>

      <div v-if="mounted && selectedRegion" class="max-w-4xl mx-auto">
        <div class="flex items-center justify-between mb-6">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-xl bg-red-500/10 flex items-center justify-center">
              <LucideHospital class="w-5 h-5 text-red-500" />
            </div>
            <div>
              <h2 class="text-lg font-semibold">{{ TEXTS.clinicsTitle }}</h2>
              <p class="text-sm text-muted-foreground">{{ selectedRegion.name }}</p>
            </div>
          </div>

          <button class="border px-3 py-1 rounded-sm flex items-center gap-2" @click="handleChangeRegionClick">
            <MapPin class="w-4 h-4" />
            <span class="text-base font-medium">{{ TEXTS.changeRegion }}</span>
          </button>
        </div>

        <div class="grid gap-4 sm:grid-cols-2">
          <div v-for="(clinic, index) in selectedRegion.clinics" :key="index" class="bg-card border rounded-lg p-5 hover:border-primary/30 transition">
            <h3 class="font-semibold mb-4">{{ clinic.name }}</h3>
            <div class="space-y-3">
              <a :href="`tel:${clinic.phone.replace(/\s/g, '')}`" class="flex items-center gap-3 text-sm hover:text-primary"><Phone class="w-4 h-4 text-primary" />{{ clinic.phone }}</a>
              <a :href="`mailto:${clinic.email}`" class="flex items-center gap-3 text-sm hover:text-primary"><Mail class="w-4 h-4 text-primary" />{{ clinic.email }}</a>
              <div class="flex items-start gap-3 text-sm"><MapPin class="w-4 h-4 text-primary mt-0.5" />{{ clinic.address }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Модалка выбора региона -->
    <RegionModal v-if="mounted" v-model:open="openModal" @select="handleRegionSelect" />
  </section>
</template>