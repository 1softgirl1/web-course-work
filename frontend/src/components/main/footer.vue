<script setup >
import { Heart, Mail, Phone, MapPin} from "lucide-vue-next"

import {RouterLink} from "vue-router";

</script>

<template>
  <footer id="contacts" class="bg-foreground py-12 text-background sm:py-14 lg:py-16">
    <div class="container mx-auto px-4 sm:px-6 lg:px-8">
      <div class="grid grid-cols-1 gap-8 sm:grid-cols-2 lg:grid-cols-4 lg:gap-8">

        <div class="lg:col-span-1">
          <RouterLink to="/" class="mb-4 flex items-center gap-2">
            <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-primary">
              <Heart class="h-5 w-5 text-primary-foreground" />
            </div>
            <span class="text-lg font-semibold text-background sm:text-xl">КардиоРеестр</span>
          </RouterLink>
          <p class="text-sm leading-relaxed text-background/70">
            Регистр пациентов с патологией выводного отдела правого желудочка после операций по поводу врождённых пороков сердца.
          </p>
        </div>

        <div>
          <h4 class="mb-4 font-semibold text-background">Разделы сайта</h4>
          <ul class="space-y-3">
            <li>
              <a href="/frontend/public#about" class="text-sm text-background/70 transition-colors hover:text-background">
                О проекте
              </a>
            </li>
            <li>
              <a href="/frontend/public#articles" class="text-sm text-background/70 transition-colors hover:text-background">
                Полезные статьи
              </a>
            </li>
            <li>
              <a href="/frontend/public#blog" class="text-sm text-background/70 transition-colors hover:text-background">
                Блог
              </a>
            </li>
            <li>
              <a href="/frontend/public#faq" class="text-sm text-background/70 transition-colors hover:text-background">
                Частые вопросы
              </a>
            </li>
            <li>
              <RouterLink to="/login" class="text-sm text-background/70 transition-colors hover:text-background">
                Вход в систему
              </RouterLink>
            </li>
          </ul>
        </div>

        <div>
          <h4 class="mb-4 font-semibold text-background">Полезные ссылки</h4>
          <ul class="space-y-3">
            <li class="text-sm text-background/70">
              <a
                  href="https://minzdrav.gov.ru/"
                  target="_blank"
                  rel="noopener noreferrer"
                  class="transition-colors hover:text-background"
              >
                Минздрав России
              </a>
            </li>
            <li class="text-sm text-background/70">
              <a
                  href="https://scardio.ru"
                  target="_blank"
                  rel="noopener noreferrer"
                  class="transition-colors hover:text-background"
              >
                Российское кардиологическое общество
              </a>
            </li>
            <li class="text-sm text-background/70">
              <a
                  href="https://www.childcardio.org"
                  target="_blank"
                  rel="noopener noreferrer"
                  class="transition-colors hover:text-background"
              >
                Ассоциация детских кардиологов
              </a>
            </li>
          </ul>
        </div>

        <div>
          <h4 class="mb-4 font-semibold text-background">Контакты</h4>
          <ul class="space-y-3">
            <li class="flex items-center gap-3 text-sm text-background/70">
              <Mail class="h-4 w-4 shrink-0" />
              <span class="break-all">restr.ru</span>
            </li>
            <li class="flex items-center gap-3 text-sm text-background/70">
              <Phone class="h-4 w-4 shrink-0" />
              <span>8 (800) 123-45-67</span>
            </li>
            <li class="flex items-start gap-3 text-sm text-background/70">
              <MapPin class="mt-0.5 h-4 w-4 shrink-0" />
              <span>бул. имени Академика Леонида Семёновича Барбараша, 6</span>
            </li>
          </ul>
        </div>
      </div>

      <div class="mt-10 border-t border-background/20 pt-6 sm:mt-12 sm:pt-8">
        <div class="flex flex-col items-center justify-between gap-4 text-center sm:flex-row sm:text-left">
          <p class="text-sm text-background/60">
            © 2026 КардиоРеестр. Все права защищены.
          </p>
          <div class="flex flex-col items-center gap-3 sm:flex-row sm:gap-6">
            <RouterLink to="/privacy" class="text-sm text-background/60 transition-colors hover:text-background">
              Политика конфиденциальности
            </RouterLink>
            <RouterLink to="/terms" class="text-sm text-background/60 transition-colors hover:text-background">
              Условия использования
            </RouterLink>
          </div>
        </div>
      </div>
    </div>
  </footer>

</template>

<style scoped>

</style>