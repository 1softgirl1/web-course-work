<script setup>
import Button from '@/components/ui/button.vue'
import { ShieldCheck, User, Stethoscope, LogIn } from 'lucide-vue-next'
import { useRouter } from 'vue-router'

const TEXTS = {
  badge: 'Медицинский регистр пациентов',
  title: 'Регистр пациентов с редкими патологиями сердца',
  subtitle:
      'Единая платформа для хранения и управления информацией о пациентах после операций по поводу врожденных пороков сердца. Для пациентов, законных представителей и врачей.',
  patientBtn: 'Вход для пациентов',
  doctorBtn: 'Вход для врачей',
  info: 'Регистрация осуществляется администраторами. Обратитесь в клинику для получения доступа.',
}

const router = useRouter()

function goToLogin(role) {
  router.push(`/login?role=${role}`)
}
</script>

<template>
  <section class="relative overflow-hidden bg-gradient-to-b from-background to-secondary/30 py-20 lg:py-28">
    <!-- Background decoration -->
    <div class="absolute inset-0 overflow-hidden">
      <div class="absolute -top-40 -right-40 w-80 h-80 rounded-full bg-primary/5 blur-3xl"></div>
      <div class="absolute -bottom-40 -left-40 w-80 h-80 rounded-full bg-accent/5 blur-3xl"></div>
    </div>

    <div class="container relative mx-auto px-4 sm:px-6 lg:px-8">
      <div class="max-w-4xl mx-auto text-center">
        <!-- Badge -->
        <div class="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-primary/10 text-primary text-sm font-medium mb-8">
          <ShieldCheck class="w-4 h-4" />
          {{ TEXTS.badge }}
        </div>

        <!-- Main heading -->
        <h1 class="text-3xl sm:text-4xl lg:text-5xl font-bold text-foreground leading-tight text-balance mb-6">
          {{ TEXTS.title }}
        </h1>

        <!-- Subheading -->
        <p class="text-lg sm:text-xl text-muted-foreground text-pretty mb-10 max-w-3xl mx-auto">
          {{ TEXTS.subtitle }}
        </p>

        <!-- CTA Buttons -->
        <div class="flex flex-col sm:flex-row items-center justify-center gap-4 mb-12">
          <Button size="lg" class="w-full sm:w-auto" @click="goToLogin('patient')">
            <User class="w-4 h-4 mr-2" />
            {{ TEXTS.patientBtn }}
          </Button>
          <Button variant="outline" size="lg" class="w-full sm:w-auto" @click="goToLogin('doctor')">
            <Stethoscope class="w-4 h-4 mr-2" />
            {{ TEXTS.doctorBtn }}
          </Button>
        </div>

        <!-- Info note -->
        <div class="inline-flex items-center gap-2 px-3 sm:px-4 py-3 rounded-xl bg-card border border-border text-xs sm:text-sm text-muted-foreground text-left">
          <LogIn class="w-4 h-4 flex-shrink-0" />
          <span>{{ TEXTS.info }}</span>
        </div>
      </div>
    </div>
  </section>
</template>