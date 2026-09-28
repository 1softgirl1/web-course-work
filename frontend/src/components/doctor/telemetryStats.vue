<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import Card from '@/components/ui/card.vue'
import Button from '@/components/ui/button.vue'
import { ChevronLeft, ChevronRight, RefreshCw } from 'lucide-vue-next'
import { toHumanErrorMessage } from '@/api/httpClient'
import { telemetryStatsApi, type TelemetryEventView, type TelemetrySummary } from '@/api/telemetry'

const PERIODS = [1, 7, 30, 90]
const PAGE_SIZES = [25, 50, 100]
const GROUPS = [
  { value: '', label: 'Все группы' },
  { value: 'product', label: 'Продуктовые' },
  { value: 'technical', label: 'Технические' },
  { value: 'audit', label: 'Аудит' },
]
const ROLES = [
  { value: '', label: 'Все роли' },
  { value: 'DOCTOR', label: 'Врач' },
  { value: 'DOCTOR_EXTENDED', label: 'Супер-врач' },
  { value: 'PATIENT', label: 'Пациент' },
  { value: 'ANONYMOUS', label: 'Без входа' },
]
const GROUP_LABELS: Record<string, string> = { product: 'Продуктовое', technical: 'Техническое', audit: 'Аудит' }
const EVENT_LABELS: Record<string, string> = {
  public_page_opened: 'Открыта публичная страница',
  public_region_detected: 'Регион определён браузером',
  public_region_confirmed: 'Регион подтверждён',
  public_region_selected_manual: 'Регион выбран вручную',
  regional_contacts_shown: 'Показаны контакты кардиоцентров',
  public_login_clicked: 'Клик «Войти» на главной',
  login_page_opened: 'Открыта страница входа',
  help_opened: 'Открыт вопрос справки',
  login_success: 'Успешный вход',
  login_failed: 'Неудачный вход',
  logout: 'Выход',
  password_changed: 'Смена пароля',
  dashboard_opened: 'Открыт список пациентов',
  patient_list_loaded: 'Загружен список пациентов',
  patient_filter_applied: 'Применён фильтр списка',
  search_used: 'Использован поиск',
  patient_card_opened: 'Открыта карточка пациента',
  characteristics_table_opened: 'Показана таблица показателей',
  dynamics_chart_opened: 'Открыт график динамики',
  patient_created: 'Создан пациент',
  patient_credentials_generated: 'Выданы код и пароль пациенту',
  patient_profile_updated: 'Изменена карточка пациента',
  patient_move_clicked: 'Пациент открыл «Как сменить регион»',
  examination_form_opened: 'Открыта форма обследования',
  examination_created: 'Внесено обследование',
  examination_updated: 'Изменено обследование',
  patient_status_updated: 'Изменился статус актуальности',
  region_change_requested: 'Запущен перевод в другой регион',
  region_change_cancelled: 'Перевод региона отменён',
  region_change_committed: 'Пациент переведён в другой регион',
  doctor_created: 'Добавлен врач',
  doctor_profile_updated: 'Изменён профиль врача',
  doctor_region_changed: 'Изменён регион врача',
  access_denied: 'Отказ в доступе',
  api_error: 'Ошибка сервера (5xx)',
  slow_request: 'Медленный запрос',
  validation_error: 'Ошибка заполнения формы',
  page_load_failed: 'Экран не загрузился',
}
const ROLE_LABELS: Record<string, string> = { PATIENT: 'Пациент', DOCTOR: 'Врач', DOCTOR_EXTENDED: 'Супер-врач' }
// Break routes only after '/', never inside a segment.
const breakableRoute = (route: string) => route.replace(/\//g, '/\u200B')
const STATUS_LABELS: Record<string, string> = { GREEN: 'Зелёный', YELLOW: 'Жёлтый', RED: 'Красный' }

const days = ref(7)
const summary = ref<TelemetrySummary | null>(null)
const loading = ref(false)
const error = ref('')
const updatedAt = ref<Date | null>(null)
const hideUnused = ref(false)

// Recent events: filters and paging live on the server, so the table never holds more than one page.
const group = ref('')
const eventName = ref('')
const role = ref('')
const pageSize = ref(25)
const page = ref(0)
const events = ref<TelemetryEventView[]>([])
const eventsLoading = ref(false)
const hasNextPage = computed(() => events.value.length === pageSize.value)

const overviewCards = computed(() => {
  const o = summary.value?.overview
  if (!o) return []
  return [
    { label: 'Активные врачи', value: o.activeDoctors },
    { label: 'Активные пациенты', value: o.activePatients },
    { label: 'Сессии', value: o.sessions },
    { label: 'Визиты публичной страницы', value: o.publicVisits },
    { label: 'Создано пациентов', value: o.patientsCreated },
    { label: 'Внесено обследований', value: o.examinationsCreated },
    { label: 'Неудачные входы', value: o.loginFailures, warn: o.loginFailures > 0 },
    { label: 'Отказы в доступе', value: o.accessDenied, warn: o.accessDenied > 0 },
    { label: 'Ошибки сервера (5xx)', value: o.apiErrors, warn: o.apiErrors > 0 },
  ]
})

const eventTypes = computed(() => (summary.value?.events ?? []).filter(row => !hideUnused.value || row.events > 0))
const unusedCount = computed(() => (summary.value?.events ?? []).filter(row => row.events === 0).length)
const eventOptions = computed(() => Object.entries(EVENT_LABELS).sort((a, b) => a[1].localeCompare(b[1], 'ru')))
const dailyMax = computed(() => Math.max(...(summary.value?.daily ?? []).map(row => row.events), 1))
const funnels = computed(() => summary.value
  ? [
      { title: 'Воронка врача', steps: summary.value.doctorFunnel },
      { title: 'Воронка пациента', steps: summary.value.patientFunnel },
    ]
  : [])

const funnelWidth = (users: number, steps: { users: number }[]) => {
  const max = Math.max(...steps.map(step => step.users), 1)
  return `${Math.round((users / max) * 100)}%`
}

const formatTime = (value: string) => new Date(value).toLocaleString('ru-RU')
const formatMetadata = (metadata: Record<string, unknown>) =>
  Object.entries(metadata).map(([key, value]) => `${key}: ${value}`).join(', ')

const loadEvents = async () => {
  eventsLoading.value = true
  try {
    events.value = await telemetryStatsApi.events({
      limit: pageSize.value,
      offset: page.value * pageSize.value,
      group: group.value || undefined,
      eventName: eventName.value || undefined,
      role: role.value || undefined,
    })
  } catch (e) {
    error.value = toHumanErrorMessage(e)
  } finally {
    eventsLoading.value = false
  }
}

const load = async () => {
  loading.value = true
  error.value = ''
  try {
    summary.value = await telemetryStatsApi.summary(days.value)
    await loadEvents()
    updatedAt.value = new Date()
  } catch (e) {
    error.value = toHumanErrorMessage(e)
  } finally {
    loading.value = false
  }
}

watch(days, () => void load())
watch([group, eventName, role, pageSize], () => {
  if (page.value !== 0) page.value = 0
  else void loadEvents()
})
watch(page, () => void loadEvents())
onMounted(() => void load())
</script>

<template>
  <div class="p-4 sm:p-6 lg:p-8 space-y-6">
    <div>
      <h1 class="text-2xl font-bold text-foreground">Статистика пилота</h1>
      <p class="text-muted-foreground">Телеметрия использования системы. Без персональных и медицинских данных.</p>
    </div>

    <!-- Period and refresh stay reachable while scrolling. -->
    <div class="sticky top-[65px] z-20 lg:top-0 -mx-4 flex flex-wrap items-center gap-2 border-b bg-background/95 px-4 py-2 backdrop-blur sm:-mx-6 sm:px-6 lg:-mx-8 lg:px-8">
      <span class="text-sm text-muted-foreground">Период:</span>
      <Button
        v-for="period in PERIODS"
        :key="period"
        size="sm"
        :variant="days === period ? 'default' : 'outline'"
        @click="days = period"
      >
        {{ period === 1 ? 'Сутки' : `${period} дней` }}
      </Button>
      <Button size="sm" variant="outline" :disabled="loading" @click="load">
        <RefreshCw class="mr-1 h-4 w-4" :class="{ 'animate-spin': loading }" /> Обновить
      </Button>
      <span v-if="updatedAt" class="text-xs text-muted-foreground">обновлено в {{ updatedAt.toLocaleTimeString('ru-RU') }}</span>
    </div>

    <p v-if="error" class="text-sm text-destructive">{{ error }}</p>

    <template v-if="summary">
      <div class="grid grid-cols-2 gap-3 md:grid-cols-3 xl:grid-cols-5">
        <Card v-for="card in overviewCards" :key="card.label" content-class="p-4">
          <p class="text-xs text-muted-foreground">{{ card.label }}</p>
          <p class="mt-1 text-2xl font-semibold" :class="card.warn ? 'text-destructive' : 'text-foreground'">{{ card.value }}</p>
        </Card>
      </div>

      <div class="grid gap-4 lg:grid-cols-2">
        <Card v-for="funnel in funnels" :key="funnel.title" :title="funnel.title" content-class="space-y-2">
          <div v-for="step in funnel.steps" :key="step.step">
            <div class="flex justify-between text-sm">
              <span>{{ step.step }}</span>
              <span class="font-medium">{{ step.users }}<span v-if="step.events !== null" class="ml-1 text-xs font-normal text-muted-foreground">· {{ step.events }} раз</span></span>
            </div>
            <div class="h-2 rounded bg-muted"><div class="h-2 rounded bg-primary" :style="{ width: funnelWidth(step.users, funnel.steps) }" /></div>
          </div>
        </Card>
      </div>

      <div class="grid gap-4 lg:grid-cols-2">
        <Card title="Загрузка списка пациентов" description="Время ответа сервера, мс">
          <table class="[&_th]:px-2 [&_th]:py-1 [&_th]:whitespace-nowrap [&_td]:px-2 w-full text-sm">
            <thead class="text-left text-muted-foreground"><tr><th>Список</th><th>Загрузок</th><th>p50</th><th>p95</th><th>Макс. пациентов</th></tr></thead>
            <tbody>
              <tr v-for="row in summary.patientListLoad" :key="row.listType ?? 'none'" class="border-t">
                <td class="py-1">{{ row.listType === 'own' ? 'Мои' : row.listType === 'all' ? 'Все' : row.listType }}</td>
                <td>{{ row.loads }}</td><td>{{ Math.round(row.p50Ms) }}</td><td>{{ Math.round(row.p95Ms) }}</td><td>{{ row.maxTotal }}</td>
              </tr>
              <tr v-if="summary.patientListLoad.length === 0"><td colspan="5" class="py-2 text-muted-foreground">Нет данных за период</td></tr>
            </tbody>
          </table>
        </Card>
        <Card title="Актуальность пациентов" description="Текущее состояние по дате последнего обследования">
          <div class="flex flex-wrap gap-4">
            <div v-for="row in summary.patientStatuses" :key="row.status" class="text-sm">
              <span class="font-medium">{{ STATUS_LABELS[row.status] ?? row.status }}:</span> {{ row.patients }}
            </div>
          </div>
        </Card>
      </div>

      <Card title="Пациенты по регионам" :description="`Регионов с пациентами: ${summary.regions.filter(r => r.patients > 0).length}. Переводы — за выбранный период`">
        <div class="max-h-80 overflow-y-auto">
          <table class="[&_th]:px-2 [&_th]:py-1 [&_th]:whitespace-nowrap [&_td]:px-2 w-full text-sm">
            <thead class="sticky top-0 bg-card text-left text-muted-foreground"><tr><th>Регион</th><th>Пациентов</th><th>Переведено в регион</th><th>Переведено из региона</th></tr></thead>
            <tbody>
              <tr v-for="row in summary.regions" :key="row.regionId" class="border-t">
                <td class="py-1">{{ row.regionName }}</td><td>{{ row.patients }}</td>
                <td :class="{ 'text-muted-foreground': row.transfersIn === 0 }">{{ row.transfersIn }}</td>
                <td :class="{ 'text-muted-foreground': row.transfersOut === 0 }">{{ row.transfersOut }}</td>
              </tr>
              <tr v-if="summary.regions.length === 0"><td colspan="4" class="py-2 text-muted-foreground">Пациентов пока нет</td></tr>
            </tbody>
          </table>
        </div>
      </Card>

      <div class="grid gap-4 lg:grid-cols-2">
        <Card title="События по типам" :description="`Все ${summary.events.length} типов за период, не использовались: ${unusedCount}`">
          <template #action>
            <label class="flex items-center gap-2 text-sm"><input v-model="hideUnused" type="checkbox"> Скрыть неиспользуемые</label>
          </template>
          <div class="max-h-96 overflow-y-auto">
            <table class="[&_th]:px-2 [&_th]:py-1 [&_th]:whitespace-nowrap [&_td]:px-2 [&_td]:align-top w-full text-sm">
              <thead class="sticky top-0 bg-card text-left text-muted-foreground"><tr><th>Событие</th><th>Группа</th><th>Кол-во</th><th>Польз.</th></tr></thead>
              <tbody>
                <tr v-for="row in eventTypes" :key="row.eventName" class="border-t" :class="{ 'text-muted-foreground': row.events === 0 }">
                  <td class="py-1"><div>{{ EVENT_LABELS[row.eventName] ?? row.eventName }}</div><div class="text-xs text-muted-foreground">{{ row.eventName }}</div></td>
                  <td>{{ GROUP_LABELS[row.eventGroup] ?? row.eventGroup }}</td><td>{{ row.events }}</td><td>{{ row.users }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </Card>
        <Card title="Активность по дням" description="Только дни, когда были события, свежие сверху">
          <div class="max-h-96 space-y-2 overflow-y-auto pr-1">
            <div v-for="row in [...summary.daily].reverse()" :key="row.day" class="text-sm">
              <div class="flex justify-between">
                <span>{{ new Date(row.day).toLocaleDateString('ru-RU', { day: '2-digit', month: '2-digit', weekday: 'short' }) }}</span>
                <span class="text-muted-foreground">{{ row.events }} событий · {{ row.users }} польз.</span>
              </div>
              <div class="h-2 rounded bg-muted"><div class="h-2 rounded bg-primary/70" :style="{ width: `${Math.round((row.events / dailyMax) * 100)}%` }" /></div>
            </div>
            <p v-if="summary.daily.length === 0" class="text-sm text-muted-foreground">Нет данных за период</p>
          </div>
        </Card>
      </div>
    </template>

    <Card title="Последние события">
      <div class="mb-3 flex flex-wrap items-center gap-2 text-sm">
        <select v-model="group" class="rounded-md border bg-background px-2 py-1"><option v-for="o in GROUPS" :key="o.value" :value="o.value">{{ o.label }}</option></select>
        <select v-model="eventName" class="max-w-[16rem] rounded-md border bg-background px-2 py-1">
          <option value="">Все события</option>
          <option v-for="[code, label] in eventOptions" :key="code" :value="code">{{ label }}</option>
        </select>
        <select v-model="role" class="rounded-md border bg-background px-2 py-1"><option v-for="o in ROLES" :key="o.value" :value="o.value">{{ o.label }}</option></select>
        <select v-model.number="pageSize" class="rounded-md border bg-background px-2 py-1"><option v-for="size in PAGE_SIZES" :key="size" :value="size">по {{ size }}</option></select>
        <div class="ml-auto flex items-center gap-2">
          <Button size="sm" variant="outline" :disabled="page === 0 || eventsLoading" @click="page--"><ChevronLeft class="h-4 w-4" /></Button>
          <span class="whitespace-nowrap text-muted-foreground">стр. {{ page + 1 }}</span>
          <Button size="sm" variant="outline" :disabled="!hasNextPage || eventsLoading" @click="page++"><ChevronRight class="h-4 w-4" /></Button>
        </div>
      </div>
      <div class="max-h-[32rem] overflow-auto">
        <table class="[&_th]:px-2 [&_th]:py-1 [&_th]:whitespace-nowrap [&_td]:px-2 [&_td]:align-top w-full min-w-[1100px] text-sm">
          <thead class="sticky top-0 bg-card text-left text-muted-foreground">
            <tr><th>Время</th><th>Событие</th><th>Источник</th><th>Роль</th><th>Регион</th><th>Маршрут</th><th>Результат</th><th>Данные</th></tr>
          </thead>
          <tbody>
            <tr v-for="event in events" :key="event.id" class="border-t">
              <td class="py-1 whitespace-nowrap">{{ formatTime(event.occurredAt) }}</td>
              <td><div>{{ EVENT_LABELS[event.eventName] ?? event.eventName }}</div><div class="text-xs text-muted-foreground">{{ event.eventName }}</div></td>
              <td>{{ event.source }}</td>
              <td class="whitespace-nowrap">{{ event.actorRole ? ROLE_LABELS[event.actorRole] ?? event.actorRole : '—' }}</td>
              <td>{{ event.actorRegionId ?? '—' }}</td>
              <td class="min-w-[11rem] text-xs"><span class="line-clamp-2" :title="event.routeTemplate ?? ''">{{ event.routeTemplate ? breakableRoute(event.routeTemplate) : '—' }}</span></td>
              <td>{{ event.result ?? '—' }}<span v-if="event.durationMs !== null"> · {{ event.durationMs }} мс</span></td>
              <td class="break-words text-xs text-muted-foreground">{{ formatMetadata(event.metadata) }}</td>
            </tr>
            <tr v-if="events.length === 0"><td colspan="8" class="py-2 text-muted-foreground">Событий нет</td></tr>
          </tbody>
        </table>
      </div>
    </Card>
  </div>
</template>
