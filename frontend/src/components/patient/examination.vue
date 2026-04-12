<script setup lang="ts">
import { ref } from 'vue'
import Card from "@/components/ui/card.vue"
import Button from "@/components/ui/button.vue"
import Dialog from '@/components/ui/dialog.vue'
import ExaminationTabe from '@/components/patient/examinationTabe.vue'

import {FileText, Calendar, Eye, Download, Plus} from "lucide-vue-next"
import { useExaminationStore, type Examination } from '@/stores/examinationStore'
import Badge from "@/components/ui/badge.vue";

const { examinations } = useExaminationStore()
const selectedExam = ref<Examination | null>(null)
const isDetailsOpen = ref(false)

const openDetails = (exam: Examination) => {
  selectedExam.value = exam
  isDetailsOpen.value = true
}

</script>

<template>
  <div class="p-4 sm:p-6 lg:p-8">
    <!-- Header -->
    <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
      <div>
        <h1 class="text-2xl font-bold text-foreground">Обследования</h1>
        <p class="text-muted-foreground">История всех пройденных обследований</p>
      </div>


    </div>

    <!-- Список обследований -->
    <div class="space-y-4">
      <Card
          v-for="exam in examinations"
          :key="exam.id"
          class="hover:border-primary/30 transition-colorsx lg:h-25  "
      >
      <div >
          <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-1">
            <!-- Info -->
            <div class="flex items-start gap-4">
              <div class="w-12 h-12 rounded-xl bg-primary/10 flex items-center justify-center ">
                <FileText class="w-6 h-6 text-primary stroke-1" />
              </div>
              <div>

                <div class="flex items-center gap-2 mb-1">
                  <h3 class="font-semibold text-foreground truncate ">
                    Обследование #{{ exam.id }}
                  </h3>
                </div>
                <p class="text-sm text-muted-foreground">Врач: {{ exam.doctor }}</p>
              </div>
            </div>

            <!-- Actions -->
            <div class="flex items-center gap-3 sm:gap-4">
              <div class="flex items-center gap-2 text-sm text-muted-foreground">
                <Calendar class="w-4 h-4" />
                <span>{{ exam.date }}</span>
              </div>
              <Button type="button" variant="ghost" size="icon" @click="openDetails(exam)">
                <Eye class="w-4 h-4" />
              </Button>
            </div>
          </div>
        </div>

      </Card>
    </div>

    <div class="mt-8 w-full">
      <ExaminationTabe />
    </div>


    <Dialog v-model="isDetailsOpen" content-class="sm:max-w-4xl">
      <div v-if="selectedExam" class="space-y-4">

        <div>
          <div class="flex items-center gap-2 mb-1">
            <h3 class="text-lg font-semibold text-foreground ">Обследование #{{ selectedExam.id }}
            </h3>
            <Badge variant="outline">
              {{ selectedExam.date }}
            </Badge>
          </div>

          <p class="text-sm text-muted-foreground">
             Врач: {{ selectedExam.doctor }}</p>
          <p class="text-sm text-muted-foreground mt-1">Заключение: {{ selectedExam.conclusion || 'Нет данных' }}</p>
        </div>

        <div>
          <p class="text-sm text-muted-foreground mb-3">Численные показатели</p>
          <div class="details-scroll max-h-[65vh] overflow-y-auto pr-2">
            <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2">
            <div
              v-for="(value, index) in selectedExam.indicators"
              :key="`modal-${selectedExam.id}-indicator-${index}`"
              class="text-sm rounded-md bg-secondary/40 px-3 py-2"
            >
              <span class="text-muted-foreground">Показатель {{ index + 1 }}:</span>
              <span class="ml-1 font-medium text-foreground">{{ value }}</span>
            </div>
            </div>
          </div>
        </div>
      </div>
    </Dialog>
  </div>
</template>

<style scoped>
.details-scroll {
  scrollbar-width: thin;
  scrollbar-color: rgb(148 163 184) transparent;
}

.details-scroll::-webkit-scrollbar {
  width: 10px;
}

.details-scroll::-webkit-scrollbar-track {
  background: transparent;
}

.details-scroll::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, rgb(148 163 184), rgb(100 116 139));
  border-radius: 9999px;
  border: 2px solid transparent;
  background-clip: padding-box;
}

.details-scroll::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(180deg, rgb(100 116 139), rgb(71 85 105));
  background-clip: padding-box;
}
</style>
