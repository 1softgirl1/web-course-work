<script setup>
import { ref } from 'vue'
import { Heart, Menu, X, LogIn } from 'lucide-vue-next'
import Button from '../ui/button.vue'

const mobileMenuOpen = ref(false)

const navLinks = [
  { href: '/#about', label: 'О проекте' },
  { href: '/#faq', label: 'Частые вопросы' },
  { href: '/#contacts', label: 'Контакты' },
]

const toggleMenu = () => {
  mobileMenuOpen.value = !mobileMenuOpen.value
}

const closeMenu = () => {
  mobileMenuOpen.value = false
}
</script>

<template>
  <header class="sticky top-0 z-50 w-full border-b border-border bg-card/95 backdrop-blur-sm">
    <div class=" container mx-auto px-4 sm:px-6 lg:px-8 ">
      <div class="flex h-16 items-center justify-between">
        <router-link to="/" class="flex items-center gap-2" @click="closeMenu">
          <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-red-500">
            <Heart class="h-5 w-5 text-white" />
          </div>
          <div class="flex flex-col">
            <span class="text-base font-semibold leading-tight text-foreground sm:text-lg">
              КардиоРеестр
            </span>
            <span class="hidden text-xs text-muted-foreground sm:block">
              Реестр редких патологий сердца
            </span>
          </div>
        </router-link>

        <nav class="hidden items-center gap-6 lg:flex">
          <a
            v-for="link in navLinks"
            :key="link.href"
            :href="link.href"
            class="text-sm font-medium text-muted-foreground transition-colors hover:text-foreground"
          >
            {{ link.label }}
          </a>
        </nav>

        <div class="hidden items-center gap-3 lg:flex">
          <router-link to="/login">
            <Button size="sm">
              <LogIn class="mr-2 h-4 w-4" />
              Войти
            </Button>
          </router-link>
        </div>

        <button
          class="inline-flex items-center justify-center rounded-md p-2 text-muted-foreground transition-colors hover:text-foreground lg:hidden"
          aria-label="Открыть меню"
          @click="toggleMenu"
        >
          <X v-if="mobileMenuOpen" class="h-6 w-6" />
          <Menu v-else class="h-6 w-6" />
        </button>
      </div>

      <div
        v-if="mobileMenuOpen"
        class="border-t border-border py-4 lg:hidden"
      >
        <nav class="flex flex-col gap-4">
          <a
            v-for="link in navLinks"
            :key="`mobile-${link.href}`"
            :href="link.href"
            class="text-sm font-medium text-muted-foreground transition-colors hover:text-foreground"
            @click="closeMenu"
          >
            {{ link.label }}
          </a>

          <div class="border-t border-border pt-4">
            <router-link to="/login" @click="closeMenu">
              <Button size="sm" class="w-full">
                <LogIn class="mr-2 h-4 w-4" />
                Войти
              </Button>
            </router-link>
          </div>
        </nav>
      </div>

    </div>
  </header>
</template>
