# Демонстрационные Данные Для Swagger

Демо-данные применяются через Flyway только в Spring-профиле `local`.

## Как Заполнить Базу

Из папки `backend`:

```powershell
.\gradlew.bat bootRun --args='--spring.profiles.active=local'
```

Профиль `local` подключает дополнительные миграции из `classpath:db/seed`. В default/prod профиле эти демо-данные не применяются.

## Демо-Логины

- `doctor.demo@example.com` / `doctor-password`
- `head.doctor@example.com` / `extended-password`
- `outsider.doctor@example.com` / `doctor-password`
- `kemerovo.doctor@example.com` / `doctor-password`
- `novosibirsk.doctor@example.com` / `doctor-password`
- `tomsk.doctor@example.com` / `doctor-password`
- `perm.doctor@example.com` / `doctor-password`
- `moscow.doctor@example.com` / `doctor-password`
- `PT-DEMO-001` / `patient-password`
- `PT-DEMO-002` / `patient-password`
- `PT-DEMO-003` / `patient-password`
- `PT-DEMO-004` / `patient-password`
- `PT-DEMO-005` / `patient-password`
- `PT-DEMO-006` / `patient-password`
- `PT-DEMO-007` / `patient-password`
- `PT-DEMO-008` / `patient-password`
- `PT-DEMO-009` / `patient-password`
- `PT-DEMO-010` / `patient-password`
- `PT-DEMO-011` / `patient-password`
- `PT-DEMO-012` / `patient-password`

## Что Лежит В Базе

- 8 врачей:
- `doctor.demo@example.com` — обычный врач, регион `1`.
- `head.doctor@example.com` — врач с расширенными правами, регион `1`.
- `outsider.doctor@example.com` — обычный врач, регион `2`.
- `kemerovo.doctor@example.com` — обычный врач, регион `22`.
- `novosibirsk.doctor@example.com` — обычный врач, регион `39`.
- `tomsk.doctor@example.com` — обычный врач, регион `76`.
- `perm.doctor@example.com` — обычный врач, регион `44`.
- `moscow.doctor@example.com` — обычный врач, регион `33`.
- 12 пациентов без ФИО:
- `PT-DEMO-001`, `PT-DEMO-002`, `PT-DEMO-003` — регион `1`.
- `PT-DEMO-004` — регион `2`.
- `PT-DEMO-005` — регион `3`.
- `PT-DEMO-006` — регион `22`, Кемеровская область.
- `PT-DEMO-007` — регион `39`, Новосибирская область.
- `PT-DEMO-008` — регион `76`, Томская область.
- `PT-DEMO-009` — регион `44`, Пермский край.
- `PT-DEMO-010` — регион `33`, Москва.
- `PT-DEMO-011` — регион `67`, Санкт-Петербург.
- `PT-DEMO-012` — регион `26`, Красноярский край.
- У каждого пациента уже есть по 2 обследования.
- У обследований есть значения для реальных характеристик из `pokazateli.md` (`weight_kg`, `height_cm`, `ef_pct`, `rv_size_mm`, `rvsp_mmhg`, `exercise_tolerance`, `tc_insufficiency`, `rvot_gradient`).

## Лучший Порядок Показа

1. `POST /api/auth/login` — войти как `doctor.demo@example.com`.
2. `GET /api/doctor/patients?scope=own` — показать пациентов региона врача.
3. `GET /api/doctor/patients?scope=all` — показать пациентов всех регионов.
4. `GET /api/doctor/patients?scope=all&regionId=2` — показать фильтр по региону.
5. `GET /api/doctor/patients?scope=all&regionId=39` — показать фильтр по Новосибирской области.
6. `GET /api/doctor/patients?scope=all&regionId=33` — показать фильтр по Москве.
7. `GET /api/doctor/patients?scope=all&diagnosis=stenosis` — показать фильтр по диагнозу.
8. `GET /api/patients/{id}` — открыть карточку пациента по числовому `id` из списка.
9. `GET /api/patients/{id}/examinations` — показать журнал обследований.
10. `POST /api/patients/{id}/examinations` — добавить новое обследование.
11. `PATCH /api/patients/{id}/examinations/{examId}` — исправить или дозаполнить показатели обследования.
12. `PATCH /api/patients/{id}` — изменить медицинские данные карточки пациента.
13. `POST /api/doctor/patients` — создать нового пациента прямо на встрече.
14. `POST /api/auth/login` — войти как `PT-DEMO-001`.
15. `GET /api/patients/{id}` — показать self-access пациента.

## Важно

- `scope=own` доступен любому врачу и показывает только пациентов региона врача.
- `scope=all` доступен любому врачу и показывает пациентов всех регионов.
- В обоих режимах пациент отображается только по `patientCode`; ФИО пациента не хранится в БД и не возвращается API.
- `DOCTOR_EXTENDED` не получает отдельной привилегии именно для списка пациентов, потому что `scope=all` доступен всем врачам.
- Для демонстрации прав доступа удобно использовать:
- `doctor.demo@example.com` для обычного врача;
- `head.doctor@example.com` для расширенных прав;
- `outsider.doctor@example.com` для врача другого региона;
- `PT-DEMO-001` для пациента.
- Если меняешь пароли через Bruno, после этого лучше заново применить seed или перелогиниться с новыми данными.
