<script setup>

import { HelpCircle } from "lucide-vue-next"
import Question from "@/components/ui/question.vue";
import {ref} from "vue";



const faqs = ref([
  {
    question: "Что такое выводной отдел правого желудочка (ВОПЖ)?",
    answer: "Выводной отдел правого желудочка — это часть сердца, через которую кровь поступает из правого желудочка в лёгочную артерию. При некоторых врождённых пороках сердца эта область может быть сужена или иметь другие аномалии.",
  },
  {
    question: "Как получить доступ к личному кабинету?",
    answer: "Регистрация пациентов осуществляется администраторами клиники. Если вам ещё не предоставили доступ, обратитесь в клинику, где наблюдаетесь.",
  },
  {
    question: "Кто может зарегистрировать ребёнка в системе?",
    answer: "Если пациенту менее 16 лет, регистрацию осуществляет законный представитель (родитель или опекун). С 16 лет пациент может быть зарегистрирован самостоятельно.",
  },
  {
    question: "Кто видит мои персональные данные?",
    answer: "Врачи не видят вашу персональную информацию, только обезличенную информацию с присвоенным кодом пациента.",
  },
  {
    question: "Что делать, если я переехал в другой регион?",
    answer: "Обратитесь к вашему врачу для изменения региона в вашей карточке. После этого ваши данные станут доступны специалистам нового региона.",
  },
  {
    question: "Безопасно ли хранить медицинские данные в системе?",
    answer: "Да, система соответствует требованиям защиты персональных данных. Все данные шифруются, доступ строго разграничен по ролям и регионам.",
  },
])


</script>

<template>
  <section id="faq" class="scroll-mt-20 bg-secondary/30 py-12 sm:py-16 lg:py-28">
    <div class="container mx-auto px-4 sm:px-6 lg:px-8">
      <div class="mb-10 text-center sm:mb-12">
        <div class="mb-4 inline-flex items-center gap-2 rounded-full bg-primary/10 px-3 py-1 text-sm font-medium text-primary">
          <HelpCircle class="h-4 w-4" />
          Справка
        </div>
        <h2 class="mb-4 text-2xl font-bold text-foreground sm:text-4xl text-balance">
          Часто задаваемые вопросы
        </h2>
        <p class="mx-auto max-w-2xl text-base text-muted-foreground sm:text-lg text-pretty">
          Ответы на наиболее частые вопросы от пациентов и их законных представителей
        </p>
      </div>
      <div class="mx-auto max-w-3xl space-y-3 sm:space-y-4">
        <Question
            v-for="(faq, index) in faqs"
            :key="index"
            :question="faq.question"
            :answer="faq.answer"
        />
      </div>
    </div>
  </section>

</template>

<style scoped>

</style>
