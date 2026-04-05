<script setup lang="ts">
import Card from "@/components/ui/card.vue"
import Button from "@/components/ui/button.vue"

import { FileText, Calendar, Eye } from "lucide-vue-next"
import { useRouter } from "vue-router"
import { useExaminationStore } from '@/stores/examinationStore'

const router = useRouter()
const { examinations } = useExaminationStore()

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
          class="hover:border-primary/30 transition-colorsx lg:h-30 "
      >
      <div >
          <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-1">
            <!-- Info -->
            <div class="flex items-start gap-4">
              <div class="w-16 h-16 rounded-xl bg-primary/10 flex items-center justify-center flex-shrink-0">
                <FileText class="w-10 h-10 text-primary stroke-1" />
              </div>
              <div>

                <div class="flex items-center gap-2 mb-1">
                  <h3 class="font-semibold text-foreground truncate max-w-[200px] lg:max-w-[500px] flex-shrink">
                    {{ exam.type }}
                  </h3>
                </div>
                <p class="text-sm text-muted-foreground">{{ exam.clinic }}</p>
                <p class="text-sm text-muted-foreground">Врач: {{ exam.doctor }}</p>
              </div>
            </div>

            <!-- Actions -->
            <div class="flex items-center gap-3 sm:gap-4">
              <div class="flex items-center gap-2 text-sm text-muted-foreground">
                <Calendar class="w-4 h-4" />
                <span>{{ exam.date }}</span>
              </div>
              <div v-if="exam.hasFile" class="flex gap-2">
                <Button variant="ghost" size="icon">
                  <Eye class="w-4 h-4" />
                </Button>
              </div>
            </div>
          </div>
      </div>

      </Card>
    </div>
  </div>
</template>