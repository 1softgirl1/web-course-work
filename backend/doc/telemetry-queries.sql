-- Отчёт пилота по telemetry_events. Период задаётся в каждом запросе (по умолчанию 7 или 30 дней).
-- Роли: DOCTOR, DOCTOR_EXTENDED, PATIENT.

-- 1. Активные врачи за 7 дней
select count(distinct actor_user_id) as active_doctors
from telemetry_events
where occurred_at >= now() - interval '7 days'
  and actor_role in ('DOCTOR', 'DOCTOR_EXTENDED')
  and event_name in ('login_success', 'dashboard_opened', 'patient_card_opened');

-- 2. Воронка врача за 30 дней: сколько врачей дошли до каждого уровня
select level, doctors
from (
    select 1 as ord, 'вошёл' as level, count(distinct actor_user_id) as doctors
    from telemetry_events where event_name = 'login_success'
      and actor_role in ('DOCTOR', 'DOCTOR_EXTENDED') and occurred_at >= now() - interval '30 days'
    union all
    select 2, 'открыл рабочую область', count(distinct actor_user_id)
    from telemetry_events where event_name = 'dashboard_opened' and occurred_at >= now() - interval '30 days'
    union all
    select 3, 'открыл пациента', count(distinct actor_user_id)
    from telemetry_events where event_name = 'patient_card_opened'
      and actor_role in ('DOCTOR', 'DOCTOR_EXTENDED') and occurred_at >= now() - interval '30 days'
    union all
    select 4, 'внёс данные', count(distinct actor_user_id)
    from telemetry_events where event_name in ('examination_created', 'examination_updated')
      and occurred_at >= now() - interval '30 days'
    union all
    select 5, 'смотрит динамику', count(distinct actor_user_id)
    from telemetry_events where event_name = 'dynamics_chart_opened'
      and actor_role in ('DOCTOR', 'DOCTOR_EXTENDED') and occurred_at >= now() - interval '30 days'
) funnel
order by ord;

-- 3. Наполнение регистра: пациенты созданы, обследования внесены, пациенты с обследованиями
select
    count(*) filter (where event_name = 'patient_created') as patients_created,
    count(*) filter (where event_name = 'examination_created') as examinations_created,
    count(distinct patient_id) filter (where event_name = 'examination_created') as patients_with_examinations
from telemetry_events
where occurred_at >= now() - interval '30 days';

-- 4. Использование графиков динамики по дням
select date_trunc('day', occurred_at) as day,
       count(*) as chart_openings,
       count(distinct actor_user_id) as users
from telemetry_events
where event_name = 'dynamics_chart_opened'
  and occurred_at >= now() - interval '30 days'
group by 1
order by 1;

-- 5. Время загрузки списка пациентов (цель — 300 пациентов без проблем)
select metadata ->> 'list_type' as list_type,
       count(*) as loads,
       percentile_cont(0.5) within group (order by duration_ms) as p50_ms,
       percentile_cont(0.95) within group (order by duration_ms) as p95_ms,
       max((metadata ->> 'total')::int) as max_total_patients
from telemetry_events
where event_name = 'patient_list_loaded'
  and occurred_at >= now() - interval '7 days'
group by 1;

-- 6. Медленные маршруты
select route_template,
       count(*) as slow_count,
       percentile_cont(0.95) within group (order by duration_ms) as p95_ms
from telemetry_events
where event_name = 'slow_request'
  and occurred_at >= now() - interval '7 days'
group by route_template
order by p95_ms desc;

-- 7. Ошибки доступа (403) по ролям и маршрутам
select actor_role, route_template, count(*) as denied_count
from telemetry_events
where event_name = 'access_denied'
  and occurred_at >= now() - interval '30 days'
group by actor_role, route_template
order by denied_count desc;

-- 8. Ошибки 5xx по маршрутам
select route_template, count(*) as errors
from telemetry_events
where event_name = 'api_error'
  and occurred_at >= now() - interval '7 days'
group by route_template
order by errors desc;

-- 9. Ошибки валидации по формам и полям
select route_template as form, metadata ->> 'field_codes' as field_codes, count(*) as errors
from telemetry_events
where event_name = 'validation_error'
  and occurred_at >= now() - interval '30 days'
group by 1, 2
order by errors desc;

-- 10. Ошибки входа и сломанные экраны
select event_name,
       coalesce(metadata ->> 'reason_code', metadata ->> 'screen') as reason_or_screen,
       count(*) as cnt
from telemetry_events
where event_name in ('login_failed', 'page_load_failed')
  and occurred_at >= now() - interval '7 days'
group by 1, 2
order by cnt desc;

-- 11. Переводы пациентов между регионами
select event_name, count(*) as cnt
from telemetry_events
where event_name in ('region_change_requested', 'region_change_cancelled', 'region_change_committed')
  and occurred_at >= now() - interval '30 days'
group by event_name;

-- 12. Актуальность: текущее распределение пациентов по статусу (из медицинской модели, не из телеметрии)
select case
           when last_exam is null or current_date - last_exam > 183 then 'RED'
           when current_date - last_exam >= 91 then 'YELLOW'
           else 'GREEN'
       end as status,
       count(*) as patients
from (
    select p.id, max(e.exam_date) as last_exam
    from patient_profiles p
    left join examinations e on e.patient_id = p.id
    group by p.id
) last_exams
group by 1
order by 1;

-- 13. Сессии: сколько длится вход и сколько сессий на врача (session_id_hash связывает события одного входа)
select actor_role,
       count(*) as logouts,
       percentile_cont(0.5) within group (order by (metadata ->> 'session_duration_sec')::int) / 60 as median_minutes
from telemetry_events
where event_name = 'logout'
  and occurred_at >= now() - interval '30 days'
group by actor_role;

-- 14. Воронка пациента за 30 дней
select 'выданы код и пароль' as level, count(*) as patients
from telemetry_events where event_name = 'patient_credentials_generated' and occurred_at >= now() - interval '30 days'
union all
select 'вошёл', count(distinct actor_user_id)
from telemetry_events where event_name = 'login_success' and actor_role = 'PATIENT' and occurred_at >= now() - interval '30 days'
union all
select 'посмотрел карточку', count(distinct actor_user_id)
from telemetry_events where event_name = 'patient_card_opened' and actor_role = 'PATIENT' and occurred_at >= now() - interval '30 days'
union all
select 'открыл «Я переехал»', count(distinct actor_user_id)
from telemetry_events where event_name = 'patient_move_clicked' and occurred_at >= now() - interval '30 days';

-- 15. Публичная страница: визиты, определение региона, регионы без центра, клики «Войти»
select event_name,
       coalesce(metadata ->> 'result', metadata ->> 'region_code', metadata ->> 'role_hint', metadata ->> 'device_type') as detail,
       count(*) as cnt
from telemetry_events
where event_name in ('public_page_opened', 'public_region_detected', 'public_region_confirmed',
                     'public_region_selected_manual', 'regional_contacts_shown', 'public_login_clicked', 'login_page_opened')
  and occurred_at >= now() - interval '30 days'
group by 1, 2
order by 1, cnt desc;

-- 16. Регионы, где посетитель не нашёл кардиоцентр
select metadata ->> 'region_code' as region_code, count(*) as shown_without_center
from telemetry_events
where event_name = 'regional_contacts_shown'
  and (metadata ->> 'has_center')::boolean = false
  and occurred_at >= now() - interval '30 days'
group by 1
order by 2 desc;

-- 17. Поиск и справка: как ищут пациентов и какие вопросы открывают
select event_name, metadata ->> 'tab' as tab, metadata ->> 'question_index' as question_index, count(*) as cnt
from telemetry_events
where event_name in ('search_used', 'help_opened')
  and occurred_at >= now() - interval '30 days'
group by 1, 2, 3
order by cnt desc;

-- 18. Среднее число обследований на пациента (из медицинской модели)
select round(avg(cnt), 2) as avg_examinations_per_patient
from (select count(e.id) as cnt from patient_profiles p left join examinations e on e.patient_id = p.id group by p.id) per_patient;
