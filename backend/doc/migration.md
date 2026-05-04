# Документация по миграциям

## Назначение

Изменения схемы базы данных управляются через Flyway.

Основные миграции лежат в директории:

- [src/main/resources/db/migration](../src/main/resources/db/migration)

Локальные demo-миграции лежат отдельно и подключаются только Spring-профилем `local`:

- [src/main/resources/db/local-migration](../src/main/resources/db/local-migration)

Default/prod профиль использует только `classpath:db/migration`. Профиль `local` добавляет `classpath:db/local-migration`.

## Список миграций

### `V1__create_users.sql`

Создает таблицу `users` для аутентификации и авторизации сразу в актуальной форме с полем `username`.

### `V2__create_regions.sql`

Создает таблицу `regions` со справочником регионов.

### `V3__create_doctor_profiles.sql`

Создает таблицу `doctor_profiles`, связанную с:

- `users`;
- `regions`.

Профиль врача хранит ФИО, специализацию и регион. Поле `workplace` добавляется отдельной миграцией `V12`.

### `V4__create_patient_profiles.sql`

Создает таблицу `patient_profiles` в актуальной форме карточки пациента:

- `birth_date`;
- диагноз;
- характеристики клапана;
- параметры операции;
- медикаменты;
- `created_at`.

ФИО пациента в этой таблице не хранится. Обезличенный код пациента хранится в `users.username` и возвращается в API как `patientCode`.

### `V5__create_examinations.sql`

Создает таблицу `examinations`, связанную с `patient_profiles`.

### `V6__create_characteristics.sql`

Создает таблицу `characteristics` в актуальной форме с полями `code`, `name`, `unit`.

### `V7__create_examination_characteristics.sql`

Создает таблицу `examination_characteristics` в актуальной форме без дублирования `unit`.

### `V8__seed_regions.sql`

Заполняет таблицу `regions` справочником регионов.

### `V9__finalize_region_based_patient_model.sql`

Фиксирует служебные детали региональной модели доступа:

- добавляет индекс `idx_patient_profiles_region_id` для выборки пациентов по региону;
- документирует правило, по которому для врача `users.username` хранит email, а для пациента — обезличенный код пациента;
- фиксирует правило, по которому врач получает список пациентов региона через свой регион, а не через прямое владение карточкой.

### `V10__seed_characteristics_catalog.sql`

Заполняет каталог характеристик обследований 50 placeholder-значениями с кодами `metric_01` ... `metric_50`.

### `V11__create_refresh_tokens.sql`

Создает таблицу `refresh_tokens` для refresh-сессий Auth 2.0.

В таблице хранится hash refresh token, а не сырой token.

### `V12__add_doctor_workplace.sql`

Добавляет обязательное поле `workplace` в `doctor_profiles`.

## Local-only demo migrations

Эти миграции применяются только при активном Spring-профиле `local`.

### `V1000__reset_demo_runtime_users.sql`

Удаляет demo-пользователей и runtime-пользователей Bruno по префиксам, чтобы локальный seed был предсказуемым на свежей local-БД.

### `V1001__seed_demo_users_and_doctors.sql`

Создает demo-врачей и их профили. ФИО врачей сохраняется в `doctor_profiles`.

### `V1002__seed_demo_patients.sql`

Создает demo-пациентов и карточки пациентов без ФИО и без `patient_code` в `patient_profiles`. Код пациента хранится только в `users.username`.

### `V1003__seed_demo_examinations.sql`

Добавляет demo-обследования.

### `V1004__seed_demo_examination_measurements.sql`

Добавляет значения demo-характеристик обследований.

## Почему миграции разделены именно так

- Базовые таблицы создаются раньше зависимых сущностей.
- Справочник регионов отделен от структуры таблиц.
- Финальная схема пациента закладывается сразу в базовых миграциях, без обязательных transitional replace-steps.
- Логика обследований отделена от карточки пациента.
- Каталог характеристик наполняется отдельной сидирующей миграцией `V10`.
- Auth 2.0 refresh sessions добавлены отдельной миграцией `V11` без изменения базовой таблицы `users`.
- Поле места работы врача добавлено отдельной миграцией `V12`, потому что управление врачами появилось позже базового профиля врача.
- Demo seed вынесен в отдельную Flyway location для local-профиля, чтобы default/prod миграции не создавали demo-пользователей и обследования.
- Исторические transitional/no-op миграции удалены: текущий набор рассчитан на чистую новую БД.

## Текущее состояние

- Все 12 миграций применяются успешно на чистой БД.
- В профиле `local` дополнительно применяются 5 local-only demo migrations.
- Тесты поднимают PostgreSQL через Testcontainers и проверяют применение миграций.
- Миграции синхронизированы с текущей backend-реализацией создания пациента, региональной модели доступа, каталога характеристик, refresh-сессий и управления врачами.
