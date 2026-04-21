# Демонстрационные данные для Swagger

Используй этот файл вместе с [demo-seed.sql](demo-seed.sql).

## Как заполнить базу

Если база пустая или ее уже очистили, выполни команду из корня проекта:

```powershell
Get-Content .\backend\doc\demo-seed.sql | docker exec -i web-course-work-postgres psql -U postgres -d web_course_work
```

## Демо-логины

- `doctor.demo@example.com` / `doctor-password`
- `head.doctor@example.com` / `extended-password`
- `outsider.doctor@example.com` / `doctor-password`
- `PT-DEMO-001` / `patient-password`
- `PT-DEMO-002` / `patient-password`
- `PT-DEMO-003` / `patient-password`
- `PT-DEMO-004` / `patient-password`
- `PT-DEMO-005` / `patient-password`

## Что лежит в базе

- 3 врача:
  - `doctor.demo@example.com` — роль `DOCTOR`, регион `1`
  - `head.doctor@example.com` — роль `DOCTOR_EXTENDED`, регион `1`
  - `outsider.doctor@example.com` — роль `DOCTOR`, регион `2`
- 5 пациентов:
  - `PT-DEMO-001`, `PT-DEMO-002`, `PT-DEMO-003` — регион `1`
  - `PT-DEMO-004` — регион `2`
  - `PT-DEMO-005` — регион `3`
- у каждого пациента уже есть по 2 обследования
- демо-данные рассчитаны на показ:
  - списка пациентов своего региона;
  - режима `scope=all` с обезличиванием ФИО;
  - просмотра карточки пациента;
  - истории обследований;
  - редактирования карточки и обследований;
  - сценариев для врача с расширенными правами

## Лучший порядок показа

1. `POST /auth/login`
   - войти как `doctor.demo@example.com`
2. `GET /api/doctor/patients`
   - показать режим `scope=own`
   - врач видит только свой регион и полные ФИО
   - показать сортировку `RED -> YELLOW -> GREEN`
3. `GET /api/doctor/patients?scope=all`
   - показать режим `scope=all`
   - пациенты видны со всех регионов
   - ФИО скрыты, остальные поля остаются
4. `GET /api/doctor/patients?scope=all&regionId=2`
   - показать фильтр по региону
5. `GET /api/doctor/patients?scope=all&diagnosis=stenosis`
   - показать фильтр по диагнозу
6. `GET /api/patients/{id}`
   - открыть карточку пациента своего региона
   - в path нужен числовой `id`, а не `patientCode`
   - числовой `id` удобно взять из ответа `GET /api/doctor/patients`
7. `GET /api/patients/{id}/examinations`
   - показать отдельный журнал обследований
8. `POST /api/patients/{id}/examinations`
   - добавить новое обследование пациенту своего региона
9. `PATCH /api/patients/{id}`
   - показать редактирование карточки пациента
10. `POST /api/doctor/patients`
   - создать нового пациента прямо на встрече
11. `POST /auth/login`
   - войти как `PT-DEMO-001`
12. `GET /api/patients/{id}`
   - показать self-access пациента

## Что важно помнить

- `scope=own` доступен любому врачу и показывает только пациентов региона врача.
- `scope=all` тоже доступен любому врачу и показывает пациентов всех регионов без ФИО.
- `DOCTOR_EXTENDED` не получает отдельной привилегии именно для списка пациентов, потому что это не требуется текущим `F-06`.
- Для демонстрации прав доступа удобнее всего использовать:
  - `doctor.demo@example.com` для обычного врача;
  - `head.doctor@example.com` для расширенных прав;
  - `PT-DEMO-001` для пациента;
- Если ты меняешь пароли через Bruno, после этого лучше заново применить seed или перелогиниться с новыми данными.
