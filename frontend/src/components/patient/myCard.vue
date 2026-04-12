<script setup >
import { reactive } from "vue"
import Card from "@/components/ui/card.vue";

import { User, MapPin, Calendar, FileText, ScanHeartIcon, KeyRound, Heart} from "lucide-vue-next"
import Badge from "@/components/ui/badge.vue";

const patientData = reactive({
  code: "PT-7GZVL7PT",
  lastName: "Иванов",
  firstName: "Иван",
  middleName: "Петрович",
  birthDate: "15.03.2010",
  age: 16,
  region: "Санкт-Петербург",
  diagnosis: "Тетрада Фалло, состояние после радикальной коррекции",
  operations: [
    { name: "Радикальная коррекция тетрады Фалло", date: "20.05.2012", clinic: "НМИЦ им. В.А. Алмазова" },
    { name: "Замена кондуита ВОПЖ", date: "15.09.2020", clinic: "НМИЦ им. В.А. Алмазова" },
  ],
  lastExamination: "10.03.2026",
  attendingDoctor: "Петрова А.В.",

})
</script>

<template>
  <div class="p-4 sm:p-6 lg:p-8">
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-foreground">Моя карточка</h1>
      <p class="text-muted-foreground">Персональные данные и медицинская информация</p>
    </div>

    <Card
        class="lg:col-span-2 lg:w-3xl"
        title="Персональные данные"
        description="Основная информация о пациенте"
    >
      <div class="grid gap-6 lg:grid-cols-2">

        <!-- Левая колонка -->
        <div class="space-y-4">
          <div class="flex items-start gap-3">
            <div class="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center flex-shrink-0">
              <User class="w-5 h-5 text-primary" />
            </div>
            <div>
              <p class="text-sm text-muted-foreground">ФИО</p>
              <p class="font-medium text-foreground">
                {{patientData.lastName}} {{patientData.firstName}} {{patientData.middleName}}
              </p>
            </div>
          </div>

          <div class="flex items-start gap-3">
            <div class="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center flex-shrink-0">
              <Calendar class="w-5 h-5 text-primary" />
            </div>
            <div>
              <p class="text-sm text-muted-foreground">Дата рождения</p>
              <div class="flex items-baseline gap-2">
                <p class="font-medium text-foreground">{{patientData.birthDate}}</p>
                <p class="text-sm text-muted-foreground">{{patientData.age}} лет</p>
              </div>
            </div>
          </div>

          <div class="flex items-start gap-3">
            <div class="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center flex-shrink-0">
              <MapPin class="w-5 h-5 text-primary" />
            </div>
            <div>
              <p class="text-sm text-muted-foreground">Регион</p>
              <p class="font-medium text-foreground">{{patientData.region}}</p>
            </div>
          </div>
          <div class="flex items-start gap-3">
            <div class="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center flex-shrink-0">
              <FileText class="w-5 h-5 text-primary" />
            </div>
            <div>
              <p class="text-sm text-muted-foreground">Последнее обследование</p>
              <p class="font-medium text-foreground">{{patientData.lastExamination}}</p>
            </div>
          </div>
          <div class="flex items-start gap-3">
            <div class="w-10 h-10 rounded-xl bg-red-500/10 flex items-center justify-center flex-shrink-0">
              <Heart class="w-5 h-5 text-red-500 " />
            </div>
            <div>
              <p class="text-sm text-muted-foreground">Диагноз</p>
              <p class="font-medium text-foreground">{{patientData.diagnosis}}</p>
            </div>
          </div>


        </div>

        <!-- Правая колонка -->
        <div class="space-y-4">
          <div class="flex items-start gap-3">
            <div class="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center flex-shrink-0">
              <ScanHeartIcon class="w-5 h-5 text-primary" />
            </div>
            <div>
              <p class="text-sm text-muted-foreground">Код пациента</p>
              <Badge variant="outline" class=" text-xs">
                {{patientData.code}}
              </Badge>
            </div>
          </div>


        </div>


      </div>
    </Card>
  </div>


</template>

<style scoped>

</style>