import { ref, computed, provide, inject } from 'vue'

const SELECT_KEY = Symbol('select')

export function provideSelect(props: any, emit: any, isOpenControlled = false) {
  const localOpen = ref(false)
  const triggerEl = ref<HTMLElement | null>(null)
  const contentEl = ref<HTMLElement | null>(null)

  const open = computed({
    get: () => (isOpenControlled ? Boolean(props.open) : localOpen.value),
    set: (value: boolean) => {
      localOpen.value = value
      emit('update:open', value)
    },
  })

  const items = ref<Record<string, string>>({})

  const registerItem = (value: string, label: string) => {
    items.value[value] = label
  }

  const selectValue = (value: string) => {
    emit('update:modelValue', value)
    open.value = false
  }

  const selectedLabel = computed(() => {
    return props.modelValue ? items.value[props.modelValue] : ''
  })

  const context = {
    open,
    registerItem,
    selectValue,
    modelValue: computed(() => props.modelValue),
    selectedLabel,
    items,
    triggerEl,
    contentEl,
    setTriggerEl: (el: HTMLElement | null) => {
      triggerEl.value = el
    },
    setContentEl: (el: HTMLElement | null) => {
      contentEl.value = el
    },
  }

  provide(SELECT_KEY, context)

  return context
}

export function useSelect() {
    const ctx = inject<any>(SELECT_KEY)
    if (!ctx) throw new Error('Select components must be used inside Select')
    return ctx
}


