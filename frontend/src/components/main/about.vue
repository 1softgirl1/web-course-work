<script setup>
import { HeartPlus, Shield, Users, Database } from 'lucide-vue-next'

const features = [
  {
    icon: Database,
    title: 'Единая база данных',
    description:
        'Централизованное хранение информации о пациентах с патологиями из всех регионов России',
  },
  {
    icon: Users,
    title: 'Связь пациент-врач',
    description:
        'Прямая коммуникация между пациентами и специалистами своего региона',
  },
  {
    icon: Shield,
    title: 'Защита данных',
    description:
        'Разграничение доступа: полные данные видны только врачам региона пациента',
  },
  {
    icon: HeartPlus,
    title: 'Мониторинг здоровья',
    description: 'Отслеживание состояния пациентов и хранение результатов обследований',
  },
]
</script>

<template>
  <section id="about" class="scroll-mt-20 bg-background py-12 sm:py-16 lg:py-28">
    <div class="container mx-auto px-4 sm:px-6 lg:px-8">
      <!-- Header -->
      <div class="mx-auto mb-10 max-w-3xl text-center sm:mb-14 lg:mb-16">
        <h2 class="mb-4 text-2xl font-bold text-foreground sm:text-4xl sm:mb-6 text-balance">
          О проекте
        </h2>
        <p class="text-base text-muted-foreground sm:text-lg text-pretty">
          Регистр создан для систематизации данных о пациентах с патологией выводного отдела правого желудочка
          после перенесённых операций по поводу врождённых пороков сердца. Платформа объединяет пациентов,
          их законных представителей и врачей для улучшения качества наблюдения и лечения.
        </p>
      </div>

      <!-- Features grid -->
      <div class="grid gap-4 sm:grid-cols-2 sm:gap-6 lg:grid-cols-4 lg:gap-8">
        <div
            v-for="feature in features"
            :key="feature.title"
            class="rounded-xl border bg-card p-5 text-center sm:p-6"
        >
          <div class="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-xl bg-primary/10 sm:h-14 sm:w-14">
            <component :is="feature.icon" class="h-6 w-6 text-primary sm:h-7 sm:w-7" />
          </div>
          <h3 class="mb-2 text-base font-semibold text-foreground sm:text-lg">{{ feature.title }}</h3>
          <p class="text-sm text-muted-foreground">{{ feature.description }}</p>
        </div>
      </div>
    </div>
  </section>
</template>
