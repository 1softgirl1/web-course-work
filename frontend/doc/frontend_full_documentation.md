# Документация по фронтенду

## 1. Назначение и стек

Фронтенд реализован как SPA-приложение для медицинского сервиса с ролями пользователя (пациент, врач, расширенный врач/администратор).

Технологии:
- Vue 3 (`<script setup>` + Composition API)
- TypeScript
- Vue Router (hash history)
- Vite
- Tailwind CSS v4 + утилиты для вариативных классов (`class-variance-authority`, `tailwind-merge`)

Ключевые зависимости (из `package.json`):
- `vue`, `vue-router`
- `lucide-vue-next` (иконки)
- `class-variance-authority`, `tailwind-merge`

## 2. Быстрый старт

Рабочая директория фронтенда: `frontend`

Команды:
- Установка зависимостей: `npm install`
- Запуск dev-сервера: `npm run dev`
- Production-сборка: `npm run build`
- Локальный просмотр сборки: `npm run preview`

## 3. Конфигурация окружения

### 3.1 API URL

В `src/api/httpClient.ts` используется:
- `VITE_API_BASE_URL` — базовый URL backend API
- fallback по умолчанию: `http://localhost:8080`

Пример `.env`:
```env
VITE_API_BASE_URL=http://localhost:8080
```

### 3.2 Base path приложения

В `vite.config.ts` настроен `base: '/web-course-work/'`.
Это важно для корректной работы ассетов/роутинга при деплое в подпапку.

### 3.3 Алиасы

В `vite.config.ts`:
- `@` -> `./src`

Использование: `import { useAuthStore } from '@/stores/authStore'`

## 4. Структура проекта

Основная структура (`frontend/src`):
- `main.ts` — bootstrap приложения
- `App.vue` — корневой компонент
- `router/` — маршрутизация и guard-логика
- `views/` — верхнеуровневые страницы (Main/Login/Patient/Doctor)
- `components/` — UI и бизнес-компоненты
- `stores/` — состояние приложения (reactive/computed stores)
- `api/` — слой запросов к backend
- `lib/` — утилиты
- `mocks/` — моки/контракты для локальной разработки

Компоненты разделены по доменам:
- `components/main` — публичная часть (hero, header, faq и т.д.)
- `components/login` — авторизация
- `components/patient` — кабинет пациента
- `components/doctor` — кабинет врача
- `components/profile` — профиль пользователя
- `components/ui` — переиспользуемые UI-элементы

## 5. Точка входа приложения

Файл: `src/main.ts`

Порядок инициализации:
1. Создание Vue-приложения через `createApp(App)`
2. Подключение роутера `app.use(router)`
3. Монтирование в `#app`

## 6. Маршрутизация и доступ

Файл: `src/router/index.ts`

### 6.1 Режим роутинга

Используется `createWebHashHistory(import.meta.env.BASE_URL)`.
Маршруты вида: `#/login`, `#/patient/myCard`, `#/doctor/myPatients`.

### 6.2 Основные маршруты

Публичные:
- `/` — главная
- `/about`
- `/faq`
- `/login` (meta: `guestOnly`)

Личный кабинет пациента:
- `/patient` (meta: `requiresAuth`, `roles: ['PATIENT']`)
- дочерние: `myCard/:code?`, `examinations`

Личный кабинет врача:
- `/doctor` (meta: `requiresAuth`, `roles: ['DOCTOR', 'DOCTOR_EXTENDED']`)
- дочерние:
  - `myPatients`
  - `myPatients/addPatient`
  - `allPatients`
  - `allDoctors`
  - `allDoctors/addDoctor`
  - `allDoctors/:id`
  - `patientCard/:code`
  - `patientCard/:code/addExamination`
  - `doctorCard`

### 6.3 Navigation guards

Глобальный `beforeEach` проверяет:
- выбор роли на экране логина (`selectedLoginRole`) и попытки зайти в чужой кабинет
- наличие авторизации для защищенных маршрутов
- соответствие роли пользователя `meta.roles`
- редирект с корневого `/doctor` на:
  - `/doctor/allPatients` для `DOCTOR_EXTENDED`
  - `/doctor/myPatients` для `DOCTOR`
- запрет захода на `guestOnly`-роуты для уже авторизованных

## 7. Управление состоянием (stores)

В проекте используется не Pinia/Vuex, а собственные composable stores на `reactive/ref/computed`.

### 7.1 `authStore.ts`

Отвечает за:
- access/refresh токены
- профиль пользователя
- текущую роль входа (`patient`/`doctor`) для UX логина
- состояние региона врача
- гидратацию/персист в `localStorage`

Ключи localStorage:
- `authSession`
- `loginRole`

Основные методы:
- `login(payload)`
- `logout()`
- `changePassword(payload)`
- `updateDoctorRegion(regionId, regionName)`
- `setSelectedLoginRole(role)`

Вычисляемые флаги:
- `isAuthenticated`
- `isDoctor`
- `isDoctorExtended`
- `isPatient`

### 7.2 Остальные stores

По файловой структуре:
- `doctorStore.ts`
- `patientStore.ts`
- `examinationStore.ts`
- `regionsStore.ts`

Они инкапсулируют доменные данные, загрузку и преобразование данных для компонентов.

## 8. API-слой и обработка ошибок

Основные файлы:
- `api/httpClient.ts` — общий HTTP-клиент
- `api/authApi.ts`, `api/doctorsApi.ts`, `api/patientsApi.ts` — доменные API
- `api/sessionBridge.ts` — мост между auth store и HTTP-клиентом
- `api/patientApi.contract.ts` — типовые контракты

### 8.1 `apiRequest`

Общий метод `apiRequest<T>(path, options)` поддерживает:
- HTTP-метод
- query-параметры
- JSON body
- флаг `auth` (подставлять ли Bearer токен)
- retry при `401`

### 8.2 Авторизация запросов

Если `auth: true`, в заголовок добавляется:
- `Authorization: Bearer <accessToken>`

### 8.3 Авто-refresh токена

При `401` для непубличных auth endpoint:
1. Выполняется `/auth/refresh` с refresh token
2. При успехе сессия обновляется
3. Исходный запрос повторяется один раз
4. При неуспехе сессия очищается и пользователь переводится на `#/login`

### 8.4 Нормализация ошибок

`ApiClientError` содержит:
- `status`
- `message`
- `details`
- `rawError`

`toHumanErrorMessage(error)` маппит основные HTTP-коды в пользовательские сообщения.

## 9. UI-слой

В `components/ui` собраны переиспользуемые элементы:
- `button`, `input`, `card`, `badge`, `dialog`, `tabs`
- набор `table/*`
- набор `select/*`
- набор `field/*`

Это базовый дизайн-слой, на котором построены доменные экраны пациента и врача.

## 10. Пользовательские роли и сценарии

Роли системы:
- `PATIENT`
- `DOCTOR`
- `DOCTOR_EXTENDED`

Сценарии:
- Пациент: личная карточка + просмотр обследований
- Врач: пациенты, карточки пациентов, загрузка обследований
- Расширенный врач: доп. админ-функции (списки врачей, создание врача, просмотр профиля врача)

## 11. Деплой и эксплуатационные особенности

1. Из-за `base: '/web-course-work/'` прод окружение должно отдавать SPA по этому префиксу.
2. Используется hash-router, поэтому сервер не требует специального fallback для path-based history.
3. Для production backend URL задается через `VITE_API_BASE_URL`.


