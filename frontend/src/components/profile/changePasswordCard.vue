<script setup lang="ts">
import { ref } from 'vue'
import Input from '@/components/ui/input.vue'
import Button from '@/components/ui/button.vue'
import { useAuthStore } from '@/stores/authStore'

const emit = defineEmits<{
  (e: 'success'): void
}>()

const authStore = useAuthStore()

const currentPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')

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

const submitChangePassword = () => {
  clearMessages()

  if (!currentPassword.value || !newPassword.value || !confirmPassword.value) {
    errorMessage.value = 'Заполните все поля'
    return
  }

  if (newPassword.value !== confirmPassword.value) {
    errorMessage.value = 'Новый пароль и подтверждение не совпадают'
    return
  }

  isSubmitting.value = true

  try {
    authStore.changePassword({
      currentPassword: currentPassword.value,
      newPassword: newPassword.value,
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
    <div class="space-y-2">
      <label class="text-sm text-muted-foreground" for="current-password">Текущий пароль</label>
      <Input id="current-password" v-model="currentPassword" type="password" autocomplete="current-password" />
    </div>

    <div class="space-y-2">
      <label class="text-sm text-muted-foreground" for="new-password">Новый пароль</label>
      <Input id="new-password" v-model="newPassword" type="password" autocomplete="new-password" />
    </div>

    <div class="space-y-2">
      <label class="text-sm text-muted-foreground" for="confirm-password">Подтверждение нового пароля</label>
      <Input id="confirm-password" v-model="confirmPassword" type="password" autocomplete="new-password" />
    </div>

    <p v-if="errorMessage" class="text-sm text-destructive">{{ errorMessage }}</p>
    <p v-if="successMessage" class="text-sm text-green-600">{{ successMessage }}</p>

    <Button type="submit" :disabled="isSubmitting">
      {{ isSubmitting ? 'Сохранение...' : 'Изменить пароль' }}
    </Button>
  </form>
</template>
