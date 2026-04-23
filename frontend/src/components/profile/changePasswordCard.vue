<script setup lang="ts">
import { ref } from 'vue'
import Input from '@/components/ui/input.vue'
import Button from '@/components/ui/button.vue'
import { useAuthStore } from '@/stores/authStore'
import { Eye, EyeOff } from 'lucide-vue-next'

const emit = defineEmits<{
  (e: 'success'): void
}>()

const authStore = useAuthStore()

const currentPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const isCurrentPasswordVisible = ref(false)
const isNewPasswordVisible = ref(false)
const isConfirmPasswordVisible = ref(false)

const errorMessage = ref('')
const successMessage = ref('')
const isSubmitting = ref(false)

const clearMessages = () => {
  errorMessage.value = ''
  successMessage.value = ''
}

const resetForm = () => {
  currentPassword.value = ''
  newPassword.value = ''
  confirmPassword.value = ''
}

const submitChangePassword = async () => {
  clearMessages()
  const current = currentPassword.value.trim()
  const next = newPassword.value.trim()
  const confirm = confirmPassword.value.trim()

  if (!current || !next || !confirm) {
    errorMessage.value = 'Заполните все поля'
    return
  }

  if (next !== confirm) {
    errorMessage.value = 'Новый пароль и подтверждение не совпадают'
    return
  }

  isSubmitting.value = true

  try {
    await authStore.changePassword({
      currentPassword: current,
      newPassword: next,
    })

    successMessage.value = 'Пароль успешно изменен'
    resetForm()
    emit('success')
  } catch (error) {
    const code = error instanceof Error ? error.message : 'UNKNOWN_ERROR'

    if (code === 'INVALID_CURRENT_PASSWORD') {
      errorMessage.value = 'Текущий пароль введен неверно'
    } else if (code === 'WEAK_PASSWORD') {
      errorMessage.value = 'Новый пароль должен содержать минимум 6 символов'
    } else if (code === 'VALIDATION_FAILED') {
      errorMessage.value = 'Проверьте корректность введенных данных'
    } else if (code === 'SESSION_EXPIRED') {
      errorMessage.value = 'Сессия истекла. Войдите в систему снова.'
    } else if (code === 'REFRESH_TOKEN_MISSING') {
      errorMessage.value = 'Сессия истекла. Войдите в систему снова.'
    } else {
      errorMessage.value = 'Не удалось изменить пароль'
    }
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <form class="space-y-4" @submit.prevent="submitChangePassword">
    <div class="rounded-md border border-border/70 bg-muted/40 p-3 text-sm text-muted-foreground">
      Требования к паролю: минимум 6 символов.
    </div>

    <div class="space-y-2">
      <label class="text-sm text-muted-foreground" for="current-password">Текущий пароль</label>
      <div class="relative">
        <Input
          id="current-password"
          v-model="currentPassword"
          :type="isCurrentPasswordVisible ? 'text' : 'password'"
          autocomplete="current-password"
          class="pr-10"
        />
        <button
          type="button"
          class="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground transition-colors hover:text-foreground"
          @click="isCurrentPasswordVisible = !isCurrentPasswordVisible"
        >
          <EyeOff v-if="isCurrentPasswordVisible" class="h-4 w-4" />
          <Eye v-else class="h-4 w-4" />
        </button>
      </div>
    </div>

    <div class="space-y-2">
      <label class="text-sm text-muted-foreground" for="new-password">Новый пароль</label>
      <div class="relative">
        <Input
          id="new-password"
          v-model="newPassword"
          :type="isNewPasswordVisible ? 'text' : 'password'"
          autocomplete="new-password"
          class="pr-10"
        />
        <button
          type="button"
          class="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground transition-colors hover:text-foreground"
          @click="isNewPasswordVisible = !isNewPasswordVisible"
        >
          <EyeOff v-if="isNewPasswordVisible" class="h-4 w-4" />
          <Eye v-else class="h-4 w-4" />
        </button>
      </div>
    </div>

    <div class="space-y-2">
      <label class="text-sm text-muted-foreground" for="confirm-password">Подтверждение нового пароля</label>
      <div class="relative">
        <Input
          id="confirm-password"
          v-model="confirmPassword"
          :type="isConfirmPasswordVisible ? 'text' : 'password'"
          autocomplete="new-password"
          class="pr-10"
        />
        <button
          type="button"
          class="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground transition-colors hover:text-foreground"
          @click="isConfirmPasswordVisible = !isConfirmPasswordVisible"
        >
          <EyeOff v-if="isConfirmPasswordVisible" class="h-4 w-4" />
          <Eye v-else class="h-4 w-4" />
        </button>
      </div>
    </div>

    <p v-if="errorMessage" class="text-sm text-destructive">{{ errorMessage }}</p>
    <p v-if="successMessage" class="text-sm text-green-600">{{ successMessage }}</p>

    <Button type="submit" :disabled="isSubmitting">
      {{ isSubmitting ? 'Сохранение...' : 'Изменить пароль' }}
    </Button>
  </form>
</template>
