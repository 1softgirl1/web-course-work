<script setup lang="ts">
import { Heart, ShieldUser, Activity, ArrowLeft } from "lucide-vue-next"
import { RouterLink } from "vue-router"
import LoginForm from "@/components/login/loginForm.vue"
</script>

<template>
  <div class="min-h-screen bg-background lg:grid lg:grid-cols-2">
    <div class="relative hidden overflow-hidden bg-linear-to-br from-primary to-primary/80 lg:flex lg:flex-col">
      <div class="absolute inset-0 opacity-40 [background-image:radial-gradient(circle_at_1px_1px,rgba(255,255,255,0.2)_1px,transparent_0)] [background-size:18px_18px]" />

      <div class="flex p-10 items-center gap-3">
        <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-white/20 backdrop-blur-sm">
          <Heart class="h-6 w-6 text-white" />
        </div>
        <div>
          <span class="block text-lg text-white font-semibold">КардиоРеестр</span>
          <span class="text-sm text-white/70">Реестр редких патологий сердца</span>
        </div>
      </div>

      <div class="relative z-10 flex h-full flex-col justify-center p-10 text-white xl:p-12">


        <div class="space-y-8">
          <div>
            <h1 class="mb-4 text-3xl font-semibold leading-tight xl:text-4xl">
              Единая платформа для мониторинга редких заболеваний сердца
            </h1>
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div class="rounded-xl bg-white/10 p-4 backdrop-blur-sm">
              <ShieldUser class="mb-3 h-8 w-8 text-white/90" />
              <h3 class="mb-1 font-semibold text-white">Безопасность данных</h3>
              <p class="text-sm text-white/70">Защищенное хранение медицинской информации</p>
            </div>
            <div class="rounded-xl bg-white/10 p-4 backdrop-blur-sm">
              <Activity class="mb-3 h-8 w-8 text-white/90" />
              <h3 class="mb-1 font-semibold text-white">Мониторинг состояния</h3>
              <p class="text-sm text-white/70">Отслеживание динамики состояния пациента</p>
            </div>
          </div>
        </div>

      </div>
    </div>

    <div class="flex min-h-screen flex-col bg-background">
      <header class="px-4 py-4 sm:px-6 sm:py-6">
        <RouterLink
          to="/"
          class="inline-flex items-center gap-2 text-sm text-muted-foreground transition-colors hover:text-foreground"
        >
          <ArrowLeft class="h-4 w-4" />
          На главную
        </RouterLink>
      </header>

      <main class="flex flex-1 items-center justify-center px-4 pb-8 sm:px-6 lg:px-8">
        <div class="w-full max-w-md">
          <div class="mb-8 text-center lg:hidden">
            <div class="mb-4 inline-flex items-center justify-center rounded-xl bg-red-500 p-3">
              <Heart class="h-6 w-6 text-white" />
            </div>
            <h1 class="text-2xl font-bold text-foreground">КардиоРеестр</h1>
            <p class="mt-1 text-sm text-muted-foreground">Вход в личный кабинет</p>
          </div>

          <LoginForm />
        </div>
      </main>
    </div>
  </div>

</template>

<style scoped>

</style>