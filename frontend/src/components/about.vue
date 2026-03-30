<script setup>
import { Heart, Shield, Users, Database } from 'lucide-vue-next'

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
    icon: Heart,
    title: 'Мониторинг здоровья',
    description: 'Отслеживание состояния пациентов и хранение результатов обследований',
  },
]
</script>

<template>
  <section id="about" class="py-20 lg:py-28 bg-background">
    <div class="container mx-auto px-4 sm:px-6 lg:px-8">
      <!-- Header -->
      <div class="max-w-3xl mx-auto text-center mb-16">
        <h2 class="text-3xl sm:text-4xl font-bold text-foreground mb-6 text-balance">
          О проекте
        </h2>
        <p class="text-lg text-muted-foreground text-pretty">
          Регистр создан для систематизации данных о пациентах с патологией выводного отдела правого желудочка
          после перенесённых операций по поводу врождённых пороков сердца. Платформа объединяет пациентов,
          их законных представителей и врачей для улучшения качества наблюдения и лечения.
        </p>
      </div>

      <!-- Features grid -->
      <div class="grid sm:grid-cols-2 lg:grid-cols-4 gap-8">
        <div
            v-for="feature in features"
            :key="feature.title"
            class="text-center"
        >
          <div class="w-14 h-14 rounded-2xl bg-primary/10 flex items-center justify-center mx-auto mb-4">
            <component :is="feature.icon" class="w-7 h-7 text-primary" />
          </div>
          <h3 class="text-lg font-semibold text-foreground mb-2">{{ feature.title }}</h3>
          <p class="text-sm text-muted-foreground">{{ feature.description }}</p>
        </div>
      </div>
    </div>
  </section>
</template>