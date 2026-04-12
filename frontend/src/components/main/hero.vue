<script setup lang="ts">
import { ref, onMounted } from "vue"
import { RouterLink } from "vue-router"
import Button from '../ui/button.vue'
import type { Region } from "../../data/regions.ts"
import { REGIONS, CITY_TO_REGION } from "../../data/regions.ts"
import { ShieldCheck, User, Phone, Mail, MapPin, LucideHospital, Stethoscope } from "lucide-vue-next"
import RegionModal from "@/components/main/regionModal.vue"
import RegionConfirmModal from "@/components/main/regionConfirmModal.vue"

const TEXTS = {
  badge: "Медицинский регистр пациентов",
  title: "У вашего ребенка редкое заболевание сердца?",
  subtitle: "Получите удобный доступ к хранению медицинских данных и обследований при врожденных пороках сердца — в единой системе для пациентов, представителей и врачей.",
  cta: "Запишитесь в клинику или свяжитесь с нами, чтобы получить доступ к личному кабинету.",
  patientBtn: "Вход для пациентов",
  doctorBtn: "Вход для врача",
  clinicsTitle: "Клиники в вашем регионе",
  changeRegion: "Изменить регион",
}

const selectedRegion = ref<Region | null>(null)
const detectedRegion = ref<Region | null>(null)
const mounted = ref(false)
const openModal = ref(false)
const openConfirmModal = ref(false)
const noCardioCenterMessage = ref("")

const normalizeLocationValue = (value: string) => {
  return value
    .toLowerCase()
    .replace(/ё/g, "е")
    .replace(/\b(г|гор|город|область|обл|край|республика|р-н|район)\b\.?/g, " ")
    .replace(/[.,]/g, " ")
    .replace(/\s+/g, " ")
    .trim()
}

const findRegionByLocationValue = (locationValue: string): Region | null => {
  const normalizedValue = normalizeLocationValue(locationValue)
  if (!normalizedValue) {
    return null
  }

  const directRegionId = CITY_TO_REGION[normalizedValue]
  if (directRegionId) {
    return REGIONS.find(region => region.id === directRegionId) || null
  }

  for (const [alias, regionId] of Object.entries(CITY_TO_REGION)) {
    const normalizedAlias = normalizeLocationValue(alias)
    if (normalizedValue.includes(normalizedAlias) || normalizedAlias.includes(normalizedValue)) {
      return REGIONS.find(region => region.id === regionId) || null
    }
  }

  return null
}

const findRegionFromGeoData = (address: Record<string, unknown>, displayName: string): Region | null => {
  const candidates = [
    address.city,
    address.town,
    address.village,
    address.municipality,
    address.county,
    address.state,
    address.region,
    address.province,
    address.city_district,
    displayName,
  ]

  for (const candidate of candidates) {
    if (!candidate) {
      continue
    }
    const region = findRegionByLocationValue(String(candidate))
    if (region) {
      return region
    }
  }

  return null
}

const applySelectedRegion = (region: Region) => {
  selectedRegion.value = region
  localStorage.setItem("selectedRegion", region.id)
  noCardioCenterMessage.value = region.clinics.length
    ? ""
    : `В регионе ${region.name} пока нет кардиоцентра. Пожалуйста, выберите другой регион.`
}

const detectRegionByBrowserGeolocation = async (): Promise<Region | null> => {
  if (!navigator.geolocation) {
    return null
  }

  try {
    const position = await new Promise<GeolocationPosition>((resolve, reject) => {
      navigator.geolocation.getCurrentPosition(resolve, reject, {
        enableHighAccuracy: false,
        timeout: 10000,
        maximumAge: 5 * 60 * 1000,
      })
    })

    const lat = position.coords.latitude
    const lon = position.coords.longitude
    const response = await fetch(`https://nominatim.openstreetmap.org/reverse?lat=${lat}&lon=${lon}&format=json&accept-language=ru`)

    if (!response.ok) {
      return null
    }

    const data = await response.json()
    const address = data?.address || {}
    const displayName = String(data?.display_name || "")
    const region = findRegionFromGeoData(address, displayName)

    if (region) {
      return region
    }

    return null
  } catch {
    return null
  }
}

// Обработчик выбора региона из модалки
const handleRegionSelect = (region: Region) => {
  applySelectedRegion(region)
  openModal.value = false
}

// Кнопка «Изменить регион»
const handleChangeRegionClick = () => {
  openModal.value = true
}

const handleConfirmDetectedRegion = () => {
  if (!detectedRegion.value) {
    openConfirmModal.value = false
    openModal.value = true
    return
  }

  applySelectedRegion(detectedRegion.value)
  openConfirmModal.value = false
}

const handleRejectDetectedRegion = () => {
  openConfirmModal.value = false
  openModal.value = true
}

onMounted(async () => {
  mounted.value = true

  const defaultRegion = REGIONS.find(r => r.id === "moscow") || REGIONS[0]
  const savedRegionId = localStorage.getItem("selectedRegion")
  const savedRegion = savedRegionId
    ? REGIONS.find(r => r.id === savedRegionId)
    : null

  if (savedRegion) {
    applySelectedRegion(savedRegion)
    return
  }

  selectedRegion.value = defaultRegion
  noCardioCenterMessage.value = defaultRegion.clinics.length
    ? ""
    : `В регионе ${defaultRegion.name} пока нет кардиоцентра. Пожалуйста, выберите другой регион.`

  const geoRegion = await detectRegionByBrowserGeolocation()
  if (geoRegion) {
    detectedRegion.value = geoRegion
    openConfirmModal.value = true
    return
  }

  if (!savedRegion) {
    selectedRegion.value = defaultRegion
    openModal.value = true
  }
})
</script>

<template>
  <section class="py-10 sm:py-14 lg:py-24">
    <div class="container mx-auto px-4 sm:px-6 lg:px-8">
      <div class="mx-auto mb-10 max-w-4xl text-center sm:mb-12">
        <div class="mb-6 inline-flex items-center gap-2 rounded-full bg-primary/10 px-3 py-1.5 text-xs font-medium text-primary sm:px-4 sm:py-2 sm:text-sm">
          <ShieldCheck class="h-4 w-4" /> {{ TEXTS.badge }}
        </div>

        <h1 class="mb-5 text-2xl font-bold leading-tight text-foreground sm:text-4xl lg:text-5xl">{{ TEXTS.title }}</h1>
        <p class="mx-auto mb-5 max-w-3xl text-base text-muted-foreground sm:text-xl">{{ TEXTS.subtitle }}</p>
        <p class="mb-7 text-sm font-medium text-foreground sm:mb-8 sm:text-base">{{ TEXTS.cta }}</p>

        <div class="gap-4 sm:flex sm:justify-center">

          <RouterLink to="/login?role=doctor" class="block sm:inline-block">
            <Button variant="outline" size="lg" class=" mb-8 w-full sm:w-auto"><Stethoscope class="mr-2 h-4 w-4" /> {{ TEXTS.doctorBtn }}</Button>
          </RouterLink>

          <RouterLink to="/login?role=patient" class="block sm:inline-block">
            <Button   size="lg" class="mb-8 w-full sm:w-auto"><User class="mr-2 h-4 w-4" /> {{ TEXTS.patientBtn }}</Button>
          </RouterLink>
        </div>

      </div>

      <div v-if="mounted && selectedRegion" class="mx-auto max-w-4xl">
        <div class="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div class="flex items-center gap-3">
            <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-red-500/10">
              <LucideHospital class="h-5 w-5 text-red-500" />
            </div>
            <div>
              <h2 class="text-base font-semibold sm:text-lg">{{ TEXTS.clinicsTitle }}</h2>
              <p class="text-sm text-muted-foreground">{{ selectedRegion.name }}</p>
            </div>
          </div>

          <button
            class="inline-flex w-full items-center justify-center gap-2 rounded-sm border px-3 py-2 sm:w-auto"
            @click="handleChangeRegionClick"
          >
            <MapPin class="h-4 w-4" />
            <span class="text-sm font-medium sm:text-base">{{ TEXTS.changeRegion }}</span>
          </button>
        </div>

        <div v-if="noCardioCenterMessage" class="rounded-lg border border-amber-300 bg-amber-50 p-4 text-amber-900">
          <p class="text-sm">{{ noCardioCenterMessage }}</p>
          <Button variant="outline" class="mt-3" @click="handleChangeRegionClick">Выбрать другой регион</Button>
        </div>

        <div v-else class="grid gap-4 sm:grid-cols-2">
          <div
            v-for="(clinic, index) in selectedRegion.clinics"
            :key="index"
            class="rounded-lg border bg-card p-4 transition hover:border-primary/30 sm:p-5"
          >
            <h3 class="mb-4 text-base font-semibold sm:text-lg">{{ clinic.name }}</h3>
            <div class="space-y-3">
              <a :href="`tel:${clinic.phone.replace(/\s/g, '')}`" class="flex items-center gap-3 text-sm break-all hover:text-primary"><Phone class="h-4 w-4 shrink-0 text-primary" />{{ clinic.phone }}</a>
              <a :href="`mailto:${clinic.email}`" class="flex items-center gap-3 text-sm break-all hover:text-primary"><Mail class="h-4 w-4 shrink-0 text-primary" />{{ clinic.email }}</a>
              <div class="flex items-start gap-3 text-sm"><MapPin class="mt-0.5 h-4 w-4 shrink-0 text-primary" />{{ clinic.address }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <RegionConfirmModal
      v-if="mounted && detectedRegion"
      v-model:open="openConfirmModal"
      :region-name="detectedRegion.name"
      @confirm="handleConfirmDetectedRegion"
      @reject="handleRejectDetectedRegion"
    />
    <RegionModal v-if="mounted" v-model:open="openModal" @select="handleRegionSelect" />
  </section>
</template>