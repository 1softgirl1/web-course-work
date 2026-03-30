<script setup>
import { ref } from 'vue'
import { Heart, Menu, X, LogIn } from 'lucide-vue-next'
import Button from '../components/ui/button.vue'


const mobileMenuOpen = ref(false)

const toggleMenu = () => {
  mobileMenuOpen.value = !mobileMenuOpen.value
}
</script>

<template>
  <header class="sticky top-0 z-50 w-full bg-card/95 backdrop-blur-sm border-b border-border">
    <div class="container mx-auto px-4 sm:px-6 lg:px-8">
      <div class="flex h-16 items-center justify-between">

        <!-- Logo -->
        <a href="/" class="flex items-center gap-2">
          <div class="flex items-center justify-center w-10 h-10 rounded-xl bg-red-500">
            <Heart class="w-5 h-5 text-white" />
          </div>
          <div class="flex flex-col">
            <span class="text-lg font-semibold text-foreground leading-tight">
              КардиоРеестр
            </span>
            <span class="text-xs text-muted-foreground hidden sm:block">
              Реестр редких патологий сердца
            </span>
          </div>
        </a>

        <!-- Navigation -->
        <nav class="flex items-center gap-6">
          <a href="/#about" class="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors">
            О проекте
          </a>
          <a href="/#articles" class="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors">
            Полезные статьи
          </a>
          <a href="/#blog" class="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors">
            Блог
          </a>
          <a href="/#faq" class="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors">
            Частые вопросы
          </a>
          <a href="/#contacts" class="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors">
            Контакты
          </a>
        </nav>

        <!-- Auth Button -->
        <div class="flex items-center gap-3">
          <a href="/login">
            <Button size="sm">
              <LogIn class="w-4 h-4 mr-2" />
              Войти
            </Button>
          </a>
        </div>


      </div>

      <!-- Mobile Menu (по желанию можно удалить полностью) -->
      <div v-if="mobileMenuOpen" class="py-4 border-t border-border">
        <nav class="flex flex-col gap-4">
          <a href="/#about" class="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors">
            О проекте
          </a>
          <a href="/#articles" class="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors">
            Полезные статьи
          </a>
          <a href="/#blog" class="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors">
            Блог
          </a>
          <a href="/#faq" class="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors">
            Частые вопросы
          </a>
          <a href="/#contacts" class="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors">
            Контакты
          </a>

          <div class="pt-4 border-t border-border">
            <a href="/login">
              <Button size="sm" class="w-full">
                <LogIn class="w-4 h-4 mr-2" />
                Войти
              </Button>
            </a>
          </div>
        </nav>
      </div>

    </div>
  </header>
</template>