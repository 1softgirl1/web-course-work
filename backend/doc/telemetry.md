# Телеметрия и аудит

Телеметрия отвечает на вопросы пилота: пользуются ли врачи регистром, доходят ли до ключевых действий (создание пациента, внесение обследования, просмотр динамики), где возникают ошибки, соблюдаются ли правила доступа и хватает ли производительности.

**Главный принцип:** в телеметрию попадает факт действия, а не медицинское содержание действия.

## Где хранится

Одна таблица `telemetry_events` (миграции `V15`, `V16`), поле `event_group` разделяет слои:

| Группа | Назначение |
|---|---|
| `product` | Использование сценариев (дашборд, карточка, графики, обследования, публичная страница) |
| `technical` | Ошибки и производительность (`api_error`, `slow_request`, `validation_error`, `page_load_failed`) |
| `audit` | Кто и когда менял значимые данные и входил в систему (создание/правка пациента и врача, пароли, перевод региона, вход/выход, отказ в доступе) |

Основные поля: `occurred_at`, `event_name`, `source` (`backend`/`frontend`), `actor_user_id`, `actor_role`, `actor_region_id`, `session_id_hash`, `request_id`, `route_template`, `entity_type`, `entity_id`, `patient_id`, `patient_region_id`, `result`, `duration_ms`, `frontend_version`, `backend_version`, `metadata` (jsonb, только количественные и кодовые значения).

## Что никогда не пишется

ФИО, дата рождения, диагноз, препараты, значения показателей, комментарии врача, email/логин, пароли, код пациента, текст поиска, координаты и IP, полные URL с параметрами. Для ошибок входа хранится только `reason_code` и `login_type`. Для правок карточек — только коды изменённых полей. На публичной странице регион передаётся кодом из справочника страницы (`kemerovo`, `moscow`), без координат и адреса.

## Как пишется

- Серверные события записываются **после commit** транзакции: откатившаяся операция не оставляет события. Исключение — `login_failed`, который пишется сразу.
- Запись асинхронная (`@Async`): ошибка телеметрии не влияет на ответ пользователю.
- Пользователь, роль, регион и сессия берутся из токена, а не от клиента.
- **Сессия** — это один вход в систему: её идентификатор переживает обновление токенов и заканчивается выходом, сменой пароля или истечением refresh-токена. В телеметрию попадает только SHA-256 от идентификатора (`session_id_hash`), по нему `login_success` связывается с последующими событиями и `logout` (в `logout` есть `session_duration_sec`).
- Каждый запрос `/api/**` получает `X-Request-Id` (заголовок ответа и `requestId` в логах).
- Frontend отправляет события через `POST /api/telemetry/events`. Принимаются только события и ключи `metadata` из белого списка, остальное отбрасывается.
- События публичной зоны (главная, страница входа, справка) принимаются без токена, но не больше 60 в минуту с одного адреса (иначе `429`). Адрес используется только как ключ лимита и не сохраняется. Анонимный клиент не может указать `patient_id`.
- Раз в сутки задача `@Scheduled` удаляет события старше `TELEMETRY_RETENTION_DAYS` (по умолчанию 365) и неактуальные refresh-сессии.

## Каталог событий

| Событие | Группа | Источник | Когда | metadata |
|---|---|---|---|---|
| `public_page_opened` | product | frontend, без входа | Открыта главная страница | `page`, `referrer_source` (direct/internal/external), `device_type` |
| `public_region_detected` | product | frontend, без входа | Браузер определил регион | `method`, `result` (found/not_found), `region_code` |
| `public_region_confirmed` | product | frontend, без входа | Пользователь подтвердил регион | `region_code` |
| `public_region_selected_manual` | product | frontend, без входа | Регион выбран вручную | `region_code` |
| `regional_contacts_shown` | product | frontend, без входа | Показаны контакты кардиоцентров региона | `region_code`, `has_center`, `centers_count` |
| `public_login_clicked` | product | frontend, без входа | Клик «Войти» / «Войти как врач/пациент» | `role_hint`, `placement` |
| `login_page_opened` | product | frontend, без входа | Открыта страница входа | `role_hint` |
| `help_opened` | product | frontend, без входа | Раскрыт вопрос справки (FAQ) | `page`, `question_index` |
| `login_success` | audit | backend | Успешный вход | `login_type` (+ `session_id_hash`) |
| `login_failed` | audit | backend | Ошибка входа | `reason_code`, `login_type` |
| `logout` | audit | backend | Выход | `session_duration_sec` |
| `password_changed` | audit | backend | Смена пароля | `changed_by` (self/doctor/admin_reset), `target_role` |
| `dashboard_opened` | product | frontend | Открыт список «мои»/«все» пациенты | `tab` |
| `patient_list_loaded` | product | backend | Список пациентов отдан | `list_type`, `rows_count`, `total`, `page`, `limit`, `filters_count` (+ `duration_ms`) |
| `patient_filter_applied` | product | frontend | Применён фильтр списка | `tab`, `filter_types`, `filters_count`, `result_count` |
| `search_used` | product | frontend | Использован поиск в списке | `tab`, `query_length`, `result_count` |
| `patient_card_opened` | product | backend | Открыта карточка пациента | `access_scope` (`own_region`/`all_anonymized`/`self`), `examinations_count` |
| `characteristics_table_opened` | product | frontend | Показана таблица показателей | `source`, `examinations_count`, `characteristics_count` |
| `dynamics_chart_opened` | product | frontend | Открыт график динамики | `source`, `chart_type`, `characteristics_selected_count` |
| `patient_created` | audit | backend | Создана карточка | `region_id`, `created_by_role` |
| `patient_credentials_generated` | audit | backend | Сгенерированы код и временный пароль | `delivery_method` |
| `patient_profile_updated` | audit | backend | Изменена карточка | `changed_field_codes`, `changed_fields_count` |
| `patient_move_clicked` | product | frontend | Пациент открыл «Как сменить регион» | `source` |
| `examination_form_opened` | product | frontend | Открыта форма обследования | `source`, `mode` (new/edit) |
| `examination_created` | product | backend | Внесено обследование | `characteristics_count`, `has_comment` |
| `examination_updated` | audit | backend | Изменено обследование | `changed_fields_count`, `measurements_count` |
| `patient_status_updated` | product | backend | Изменился статус актуальности | `old_status`, `new_status` |
| `region_change_requested` | audit | frontend | Запущен таймер перевода | `from_region_id`, `to_region_id` |
| `region_change_cancelled` | audit | frontend | Перевод отменён во время таймера | `from_region_id`, `to_region_id`, `seconds_left` |
| `region_change_committed` | audit | backend | Регион пациента изменён | `from_region_id`, `to_region_id` |
| `doctor_created` | audit | backend | Добавлен врач | `doctor_user_id`, `region_id`, `role` |
| `doctor_profile_updated` | audit | backend | Изменён профиль врача | `doctor_user_id`, `changed_field_codes`, `changed_fields_count` |
| `doctor_region_changed` | audit | backend | Изменён регион врача | `doctor_user_id`, `from_region_id`, `to_region_id` |
| `access_denied` | audit | backend | 403 на `/api/**` (`result=forbidden`) или 401 из-за отсутствующего/поддельного/устаревшего токена (`result=unauthorized`, `reason`). Истёкший токен не пишется: это обычное обновление сессии | `method`, `status_code`, `reason` |
| `api_error` | technical | backend | Ответ 5xx | `method`, `status_code` |
| `slow_request` | technical | backend | Запрос дольше `TELEMETRY_SLOW_REQUEST_MS` (1000 мс) | `method`, `status_code` |
| `validation_error` | technical | backend и frontend | Ошибка валидации: 400 от сервера или проверка формы до отправки | `field_codes`, `errors_count`, `form_name` (frontend) |
| `page_load_failed` | technical | frontend | Ключевой экран не загрузился | `screen`, `error_code` |

Роли пишутся как в системе: `DOCTOR`, `DOCTOR_EXTENDED`, `PATIENT`.

События второго этапа, которые зависят от ещё не существующих функций (выгрузки, отчёты по пациенту, импорт, подсказки качества данных, проверка дублей), добавляются вместе с этими функциями.

## Как смотреть

- **Страница «Статистика»** в кабинете врача с расширенными правами (`/doctor/telemetry`): активность, воронки врача и пациента (люди и число срабатываний), время загрузки списка, актуальность пациентов, все типы событий с нулями для неиспользуемых, активность по дням, журнал событий с фильтрами (группа, событие, роль) и постраничным просмотром. Панель периода и обновления закреплена сверху. Данные отдают `GET /api/telemetry/summary?days=N` и `GET /api/telemetry/events` — только для `DOCTOR_EXTENDED`.
- **SQL**: готовые запросы для отчёта пилота — `telemetry-queries.sql`, выполнять в pgAdmin/DBeaver/psql.

## Наблюдаемость (Actuator)

`/actuator/health`, `/actuator/info`, `/actuator/metrics` доступны на отдельном порту `MANAGEMENT_PORT` (по умолчанию `8081`). Этот порт не публикуется наружу и не проксируется nginx — только для внутренней сети контейнеров:

```bash
# в образе backend нет curl/wget, поэтому запрос идёт из контейнера nginx той же сети
docker compose exec nginx wget -qO- http://backend:8081/actuator/health
```

## Производительность

Интеграционный тест `patientListStaysFastWithPilotVolume` поднимает объём пилота (300 пациентов, 20 врачей, 900 обследований) и проверяет, что список пациентов отдаётся быстрее 2 секунд и событие `patient_list_loaded` фиксирует весь объём.
