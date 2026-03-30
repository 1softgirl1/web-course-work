<script setup >
import { Heart, Mail, Phone, MapPin} from "lucide-vue-next"

import {RouterLink} from "vue-router";

</script>

<template>
  <footer id="contacts" class="bg-foreground text-background py-16">
    <div class="container mx-auto px-4 sm:px-6 lg:px-8">
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-8 lg:gap-8">

        <div class="lg:col-span-1">
          <a to="/" class="flex items-center gap-2 mb-4">
            <div class="flex items-center justify-center w-10 h-10 rounded-xl bg-primary">
              <Heart class="w-5 h-5 text-primary-foreground" />
            </div>
            <span class="text-xl font-semibold text-background">КардиоРеестр</span>
          </a>
          <p class="text-background/70 text-sm leading-relaxed">
            Регистр пациентов с патологией выводного отдела правого желудочка после операций по поводу врождённых пороков сердца.
          </p>
        </div>

        <div>
          <h4 class="font-semibold text-background mb-4">Разделы сайта</h4>
          <ul class="space-y-3">
            <li>
              <a href="/#about" class="text-sm text-background/70 hover:text-background transition-colors">
                О проекте
              </a>
            </li>
            <li>
              <a href="/#articles" class="text-sm text-background/70 hover:text-background transition-colors">
                Полезные статьи
              </a>
            </li>
            <li>
              <a href="/#blog" class="text-sm text-background/70 hover:text-background transition-colors">
                Блог
              </a>
            </li>
            <li>
              <a href="/#faq" class="text-sm text-background/70 hover:text-background transition-colors">
                Частые вопросы
              </a>
            </li>
            <li>
              <RouterLink to="/login" class="text-sm text-background/70 hover:text-background transition-colors">
                Вход в систему
              </RouterLink>
            </li>
          </ul>
        </div>

        <div>
          <h4 class="font-semibold text-background mb-4">Полезные ссылки</h4>
          <ul class="space-y-3">
            <li class="flex items-center gap-3 text-sm text-background/70">
              <a
                  href="https://minzdrav.gov.ru/"
                  target="_blank"
                  rel="noopener noreferrer"
                  class="hover:text-background transition-colors"
              >
                Минздрав России
              </a>
            </li>
            <li class="flex items-center gap-3 text-sm text-background/70">
              <a
                  href="https://scardio.ru"
                  target="_blank"
                  rel="noopener noreferrer"
                  class="hover:text-background transition-colors"
              >
                Российское кардиологическое общество
              </a>
            </li>
            <li class="flex items-center gap-3 text-sm text-background/70">
              <a
                  href="https://www.childcardio.org"
                  target="_blank"
                  rel="noopener noreferrer"
                  class="hover:text-background transition-colors"
              >
                Ассоциация детских кардиологов
              </a>
            </li>
          </ul>


        </div>

        <div>
          <h4 class="font-semibold text-background mb-4">Контакты</h4>
          <ul class="space-y-3">
            <li class="flex items-center gap-3 text-sm text-background/70">
              <Mail class="w-4 h-4 shrink-0" />
              <span>restr.ru</span>
            </li>
            <li class="flex items-center gap-3 text-sm text-background/70">
              <Phone class="w-4 h-4 shrink-0" />
              <span>8 (800) 123-45-67</span>
            </li>
            <li class="flex items-start gap-3 text-sm text-background/70">
              <MapPin class="w-4 h-4 shrink-0 mt-0.5" />
              <span>бул. имени Академика Леонида Семёновича Барбараша, 6</span>
            </li>
          </ul>
        </div>
      </div>


      <div class="mt-12 pt-8 border-t border-background/20">
        <div class="flex flex-col sm:flex-row items-center justify-between gap-4 text-center sm:text-left">
          <p class="text-sm text-background/60">
            © 2026 КардиоРеестр. Все права защищены.
          </p>
          <div class="flex flex-col sm:flex-row items-center gap-3 sm:gap-6">
            <RouterLink to="/privacy" class="text-sm text-background/60 hover:text-background transition-colors">
              Политика конфиденциальности
            </RouterLink>
            <RouterLink to="/terms" class="text-sm text-background/60 hover:text-background transition-colors">
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