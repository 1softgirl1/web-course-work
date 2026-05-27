-- Demo seed snapshot.
--
-- This file is a single SQL version of local-only Flyway seed migrations from:
--   backend/src/main/resources/db/seed/V1000__reset_demo_runtime_users.sql
--   backend/src/main/resources/db/seed/V1001__seed_demo_users_and_doctors.sql
--   backend/src/main/resources/db/seed/V1002__seed_demo_patients.sql
--   backend/src/main/resources/db/seed/V1003__seed_demo_examinations.sql
--   backend/src/main/resources/db/seed/V1004__seed_demo_examination_measurements.sql
--
-- Preferred local startup still uses Spring profile local and Flyway:
--   classpath:db/migration,classpath:db/seed
--
-- This script is kept for manual/demo database setup and must stay in sync with db/seed.
-- It does not add patient_profiles.patient_code or patient full names; patient code lives only in users.username.

-- Local/dev only. Keeps repeated local runs predictable by removing demo and Bruno runtime users.
delete from users
where username like 'bruno.%'
   or username like 'PT-%'
   or username in (
    'doctor.demo@example.com',
    'head.doctor@example.com',
    'outsider.doctor@example.com',
    'kemerovo.doctor@example.com',
    'novosibirsk.doctor@example.com',
    'tomsk.doctor@example.com',
    'perm.doctor@example.com',
    'moscow.doctor@example.com'
);
-- Local/dev only demo users.
with inserted_users as (
    insert into users (username, password_hash, role, status)
    values
        ('doctor.demo@example.com', '$2a$10$Vvh4BhrIYEtRO0BJ99ZyouqsvijULT5r4KBhMzFZs58sQo4lZW342', 'DOCTOR', 'ACTIVE'),
        ('head.doctor@example.com', '$2a$10$rtNF4r7xy9nbnWSEnWDETubjduV.qCedTCtwVNEEHfTu7iJP2.c0i', 'DOCTOR_EXTENDED', 'ACTIVE'),
        ('outsider.doctor@example.com', '$2a$10$Vvh4BhrIYEtRO0BJ99ZyouqsvijULT5r4KBhMzFZs58sQo4lZW342', 'DOCTOR', 'ACTIVE'),
        ('kemerovo.doctor@example.com', '$2a$10$Vvh4BhrIYEtRO0BJ99ZyouqsvijULT5r4KBhMzFZs58sQo4lZW342', 'DOCTOR', 'ACTIVE'),
        ('novosibirsk.doctor@example.com', '$2a$10$Vvh4BhrIYEtRO0BJ99ZyouqsvijULT5r4KBhMzFZs58sQo4lZW342', 'DOCTOR', 'ACTIVE'),
        ('tomsk.doctor@example.com', '$2a$10$Vvh4BhrIYEtRO0BJ99ZyouqsvijULT5r4KBhMzFZs58sQo4lZW342', 'DOCTOR', 'ACTIVE'),
        ('perm.doctor@example.com', '$2a$10$Vvh4BhrIYEtRO0BJ99ZyouqsvijULT5r4KBhMzFZs58sQo4lZW342', 'DOCTOR', 'ACTIVE'),
        ('moscow.doctor@example.com', '$2a$10$Vvh4BhrIYEtRO0BJ99ZyouqsvijULT5r4KBhMzFZs58sQo4lZW342', 'DOCTOR', 'ACTIVE')
    returning id, username
)
insert into doctor_profiles (user_id, specialization, workplace, last_name, first_name, middle_name, region_id)
select id, 'Cardiac surgeon', 'Regional Cardiology Center', 'Ivanov', 'Ivan', 'Sergeevich', 1
from inserted_users
where username = 'doctor.demo@example.com'
union all
select id, 'Chief cardiac surgeon', 'Regional Cardiology Center', 'Sidorov', 'Alexey', 'Petrovich', 1
from inserted_users
where username = 'head.doctor@example.com'
union all
select id, 'Cardiologist', 'Federal Cardiology Center', 'Kuznetsov', 'Oleg', 'Ivanovich', 2
from inserted_users
where username = 'outsider.doctor@example.com'
union all
select id, 'Cardiologist', 'Kuzbass Cardiology Center', 'Belov', 'Dmitry', 'Andreevich', 22
from inserted_users
where username = 'kemerovo.doctor@example.com'
union all
select id, 'Interventional cardiologist', 'Novosibirsk Regional Cardiology Center', 'Morozov', 'Pavel', 'Igorevich', 39
from inserted_users
where username = 'novosibirsk.doctor@example.com'
union all
select id, 'Cardiologist', 'Tomsk Cardiology Dispensary', 'Orlova', 'Anna', 'Viktorovna', 76
from inserted_users
where username = 'tomsk.doctor@example.com'
union all
select id, 'Cardiac surgeon', 'Perm Heart Center', 'Fedorov', 'Mikhail', 'Romanovich', 44
from inserted_users
where username = 'perm.doctor@example.com'
union all
select id, 'Chief cardiologist', 'Moscow Clinical Cardiology Center', 'Volkova', 'Elena', 'Pavlovna', 33
from inserted_users
where username = 'moscow.doctor@example.com';
-- Local/dev only demo patients. Patient code lives only in users.username.
-- Pediatric cohort (0-18 years on 2026-05-26) consistent with pokazateli.md.
with inserted_users as (
    insert into users (username, password_hash, role, status)
    values
        ('PT-DEMO-001', '$2a$10$fg9VwpbVioZ3/D6BCqMEiOw1ii02FmSKOOZt52xb.m1pjFcyblURi', 'PATIENT', 'ACTIVE'),
        ('PT-DEMO-002', '$2a$10$fg9VwpbVioZ3/D6BCqMEiOw1ii02FmSKOOZt52xb.m1pjFcyblURi', 'PATIENT', 'ACTIVE'),
        ('PT-DEMO-003', '$2a$10$fg9VwpbVioZ3/D6BCqMEiOw1ii02FmSKOOZt52xb.m1pjFcyblURi', 'PATIENT', 'ACTIVE'),
        ('PT-DEMO-004', '$2a$10$fg9VwpbVioZ3/D6BCqMEiOw1ii02FmSKOOZt52xb.m1pjFcyblURi', 'PATIENT', 'ACTIVE'),
        ('PT-DEMO-005', '$2a$10$fg9VwpbVioZ3/D6BCqMEiOw1ii02FmSKOOZt52xb.m1pjFcyblURi', 'PATIENT', 'ACTIVE'),
        ('PT-DEMO-006', '$2a$10$fg9VwpbVioZ3/D6BCqMEiOw1ii02FmSKOOZt52xb.m1pjFcyblURi', 'PATIENT', 'ACTIVE'),
        ('PT-DEMO-007', '$2a$10$fg9VwpbVioZ3/D6BCqMEiOw1ii02FmSKOOZt52xb.m1pjFcyblURi', 'PATIENT', 'ACTIVE'),
        ('PT-DEMO-008', '$2a$10$fg9VwpbVioZ3/D6BCqMEiOw1ii02FmSKOOZt52xb.m1pjFcyblURi', 'PATIENT', 'ACTIVE'),
        ('PT-DEMO-009', '$2a$10$fg9VwpbVioZ3/D6BCqMEiOw1ii02FmSKOOZt52xb.m1pjFcyblURi', 'PATIENT', 'ACTIVE'),
        ('PT-DEMO-010', '$2a$10$fg9VwpbVioZ3/D6BCqMEiOw1ii02FmSKOOZt52xb.m1pjFcyblURi', 'PATIENT', 'ACTIVE'),
        ('PT-DEMO-011', '$2a$10$fg9VwpbVioZ3/D6BCqMEiOw1ii02FmSKOOZt52xb.m1pjFcyblURi', 'PATIENT', 'ACTIVE'),
        ('PT-DEMO-012', '$2a$10$fg9VwpbVioZ3/D6BCqMEiOw1ii02FmSKOOZt52xb.m1pjFcyblURi', 'PATIENT', 'ACTIVE')
    returning id, username
)
insert into patient_profiles (
    user_id,
    region_id,
    birth_date,
    sex,
    diagnosis,
    operation_place,
    observation_place,
    coronary_anatomy,
    valve_name,
    valve_size,
    valve_material,
    operation_anesthesia,
    operation_duration_minutes,
    operation_delivery_system,
    medications,
    created_at
)
select id, 1, date '2014-03-12', 'M',
       'Тетрада Фалло, состояние после радикальной коррекции',
       'НМИЦ им. Е.Н. Мешалкина',
       'Городская детская поликлиника №1, Новосибирск',
       'Правый тип, без аномалий',
       'Contegra', '16 mm', 'Кондуит из яремной вены быка',
       'Общая анестезия', 240, 'Стернотомия',
       'Аспирин 50 мг через день, Эналаприл 2.5 мг утром',
       timestamp '2026-03-01 09:30:00'
from inserted_users where username = 'PT-DEMO-001'
union all
select id, 1, date '2018-07-22', 'F',
       'Атрезия лёгочной артерии с ДМЖП',
       'НМИЦ им. Е.Н. Мешалкина',
       'Городская детская поликлиника №2, Новосибирск',
       'Левый тип, аномальное отхождение ПКА от левого синуса',
       'Hancock', '14 mm', 'Биопротез свиньи',
       'Общая анестезия', 285, 'Стернотомия',
       'Варфарин 1.25 мг (МНО 2.5), Каптоприл 3.125 мг 2 р/сут',
       timestamp '2026-02-10 11:15:00'
from inserted_users where username = 'PT-DEMO-002'
union all
select id, 1, date '2009-11-02', 'M',
       'Общий артериальный ствол, тип I, состояние после коррекции',
       'НМИЦ им. Е.Н. Мешалкина',
       'Детская краевая клиническая больница, Новосибирск',
       'Сбалансированный тип',
       'Contegra', '22 mm', 'Кондуит из яремной вены быка',
       'Общая анестезия', 310, 'Стернотомия',
       'Дигоксин 0.125 мг, Фуросемид 20 мг, Спиронолактон 25 мг',
       timestamp '2026-01-20 14:45:00'
from inserted_users where username = 'PT-DEMO-003'
union all
select id, 2, date '2020-06-17', 'F',
       'Стеноз клапана лёгочной артерии, состояние после баллонной вальвулопластики',
       'ФГБУ НМИЦ ССХ им. А.Н. Бакулева',
       'Детская поликлиника №4, Омск',
       'Правый тип, без аномалий',
       'Sapien 3 Ultra', '20 mm', 'Биопротез на каркасе',
       'Сочетанная анестезия', 165, 'Трансвенозный доступ',
       'Аспирин 25 мг, Пропранолол 5 мг 2 р/сут',
       timestamp '2026-04-01 10:00:00'
from inserted_users where username = 'PT-DEMO-004'
union all
select id, 3, date '2012-12-04', 'M',
       'Двойное отхождение сосудов от правого желудочка с ДМЖП',
       'ФГБУ НМИЦ ССХ им. А.Н. Бакулева',
       'Детская республиканская клиническая больница, Уфа',
       'Сбалансированный тип, без аномалий',
       'Contegra', '18 mm', 'Кондуит из яремной вены быка',
       'Общая анестезия', 295, 'Стернотомия',
       'Эналаприл 5 мг, Бисопролол 1.25 мг утром',
       timestamp '2026-03-15 16:20:00'
from inserted_users where username = 'PT-DEMO-005'
union all
select id, 22, date '2016-04-19', 'F',
       'Стеноз ствола лёгочной артерии после операции Росса',
       'НМИЦ им. В.А. Алмазова',
       'Детская поликлиника при НМИЦ им. В.А. Алмазова, СПб',
       'Левый тип, без аномалий',
       'Melody', '20 mm', 'Биопротез на каркасе',
       'Общая анестезия', 220, 'Транскатетерно, трансвенозный доступ',
       'Аспирин 50 мг через день',
       timestamp '2026-03-18 09:10:00'
from inserted_users where username = 'PT-DEMO-006'
union all
select id, 39, date '2011-09-07', 'M',
       'Аномалия Эбштейна, состояние после реконструкции',
       'НМИЦ им. В.А. Алмазова',
       'Детская краевая больница, Краснодар',
       'Правый тип, без аномалий',
       'Contour', '23 mm', 'Биопротез на каркасе',
       'Сочетанная анестезия', 260, 'Стернотомия',
       'Аспирин 50 мг, Сотолекс 40 мг 2 р/сут',
       timestamp '2026-02-28 12:35:00'
from inserted_users where username = 'PT-DEMO-007'
union all
select id, 76, date '2008-05-30', 'F',
       'Стеноз протеза в позиции лёгочной артерии',
       'НМИЦ им. Е.Н. Мешалкина',
       'Детская больница №2, Екатеринбург',
       'Правый тип, гипоплазия задней нисходящей ветви',
       'Sapien 3 Ultra', '23 mm', 'Биопротез на каркасе',
       'Общая анестезия', 245, 'Трансвенозный доступ',
       'Аспирин 75 мг, Эналаприл 5 мг',
       timestamp '2026-01-25 15:40:00'
from inserted_users where username = 'PT-DEMO-008'
union all
select id, 44, date '2022-02-11', 'M',
       'Атриовентрикулярная коммуникация, полная форма',
       'ФГБУ НМИЦ ССХ им. А.Н. Бакулева',
       'Детская поликлиника №5, Челябинск',
       'Сбалансированный тип',
       'Hancock', '16 mm', 'Биопротез свиньи',
       'Общая анестезия', 270, 'Стернотомия',
       'Каптоприл 6.25 мг 3 р/сут, Фуросемид 10 мг 2 р/сут',
       timestamp '2026-04-04 08:50:00'
from inserted_users where username = 'PT-DEMO-009'
union all
select id, 33, date '2009-10-24', 'F',
       'Транспозиция магистральных артерий, состояние после операции Растелли',
       'НМИЦ им. Е.Н. Мешалкина',
       'Детская поликлиника, Тюмень',
       'Правый тип, без аномалий',
       'Contegra', '22 mm', 'Кондуит из яремной вены быка',
       'Общая анестезия', 320, 'Стернотомия',
       'Аспирин 75 мг, Эналаприл 5 мг, Спиронолактон 25 мг',
       timestamp '2026-03-29 13:05:00'
from inserted_users where username = 'PT-DEMO-010'
union all
select id, 67, date '2019-07-03', 'M',
       'Стеноз лёгочной артерии, состояние после пластики',
       'НМИЦ им. В.А. Алмазова',
       'Детская поликлиника №3, Самара',
       'Левый тип, без аномалий',
       'Sapien 3 Ultra', '18 mm', 'Биопротез на каркасе',
       'Сочетанная анестезия', 195, 'Трансвенозный доступ',
       'Аспирин 50 мг через день',
       timestamp '2026-02-16 10:25:00'
from inserted_users where username = 'PT-DEMO-011'
union all
select id, 26, date '2013-03-21', 'F',
       'Тетрада Фалло с агенезией клапана ЛА',
       'НМИЦ им. Е.Н. Мешалкина',
       'Детская поликлиника, Иркутск',
       'Сбалансированный тип',
       'Contegra', '20 mm', 'Кондуит из яремной вены быка',
       'Общая анестезия', 305, 'Стернотомия',
       'Аспирин 50 мг, Эналаприл 2.5 мг, Дигоксин 0.0625 мг',
       timestamp '2026-01-12 11:55:00'
from inserted_users where username = 'PT-DEMO-012';
-- Local/dev only demo examinations.
insert into examinations (patient_id, title, exam_date, comment)
select p.id, 'Initial check', date '2026-02-15', 'Baseline values'
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-001'
union all
select p.id, 'Quarterly follow-up', date '2026-04-10', 'Stable postoperative condition'
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-001'
union all
select p.id, 'Initial check', date '2025-10-01', 'Baseline values'
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-002'
union all
select p.id, 'Control visit', date '2025-12-10', 'Moderate symptoms remain'
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-002'
union all
select p.id, 'Initial check', date '2025-07-01', 'Baseline values'
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-003'
union all
select p.id, 'Control visit', date '2025-09-01', 'Long interval without updates'
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-003'
union all
select p.id, 'Initial check', date '2026-01-15', 'Baseline values'
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-004'
union all
select p.id, 'Quarterly follow-up', date '2026-03-25', 'Condition improving'
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-004'
union all
select p.id, 'Initial check', date '2025-11-20', 'Baseline values'
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-005'
union all
select p.id, 'Control visit', date '2026-01-05', 'Requires continued monitoring'
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-005'
union all
select p.id, 'Initial check', date '2026-02-05', 'Baseline values'
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-006'
union all
select p.id, 'Quarterly follow-up', date '2026-04-15', 'Good exercise tolerance'
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-006'
union all
select p.id, 'Initial check', date '2025-12-18', 'Baseline values'
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-007'
union all
select p.id, 'Control visit', date '2026-02-20', 'Requires pressure monitoring'
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-007'
union all
select p.id, 'Initial check', date '2025-09-12', 'Baseline values'
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-008'
union all
select p.id, 'Control visit', date '2025-11-30', 'Stable with mild symptoms'
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-008'
union all
select p.id, 'Initial check', date '2026-03-03', 'Baseline values'
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-009'
union all
select p.id, 'Control visit', date '2026-04-20', 'Rhythm stabilized'
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-009'
union all
select p.id, 'Initial check', date '2025-08-05', 'Baseline values'
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-010'
union all
select p.id, 'Control visit', date '2026-01-18', 'Needs closer follow-up'
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-010'
union all
select p.id, 'Initial check', date '2026-02-02', 'Baseline values'
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-011'
union all
select p.id, 'Control visit', date '2026-03-12', 'Stable measurements'
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-011'
union all
select p.id, 'Initial check', date '2025-06-22', 'Baseline values'
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-012'
union all
select p.id, 'Control visit', date '2025-10-15', 'Long interval without updates'
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-012';
-- Local/dev only demo measurements aligned with the indicator catalog
-- from pokazateli.md. Each examination carries a compact set of core
-- pediatric cardiology indicators with realistic values.
insert into examination_characteristics (examination_id, characteristic_id, value, comment)
select e.id, c.id, v.measurement_value, v.measurement_comment
from (
    values
        -- PT-DEMO-001 (age 12, post-Fallot)
        ('PT-DEMO-001', 'Initial check',        date '2026-02-15', 'weight_kg',          38.0, 'Базовое взвешивание'),
        ('PT-DEMO-001', 'Initial check',        date '2026-02-15', 'height_cm',         148.0, null),
        ('PT-DEMO-001', 'Initial check',        date '2026-02-15', 'ef_pct',             58.0, 'Норма'),
        ('PT-DEMO-001', 'Initial check',        date '2026-02-15', 'rv_size_mm',         28.0, 'В пределах нормы'),
        ('PT-DEMO-001', 'Initial check',        date '2026-02-15', 'rvsp_mmhg',          26.0, null),
        ('PT-DEMO-001', 'Initial check',        date '2026-02-15', 'exercise_tolerance', 1.0,  'NYHA I'),
        ('PT-DEMO-001', 'Initial check',        date '2026-02-15', 'tc_insufficiency',   1.0,  'I степень'),
        ('PT-DEMO-001', 'Initial check',        date '2026-02-15', 'rvot_gradient',       8.0, 'В норме'),
        ('PT-DEMO-001', 'Quarterly follow-up',  date '2026-04-10', 'weight_kg',          39.0, null),
        ('PT-DEMO-001', 'Quarterly follow-up',  date '2026-04-10', 'height_cm',         149.0, null),
        ('PT-DEMO-001', 'Quarterly follow-up',  date '2026-04-10', 'ef_pct',             60.0, 'Стабильно'),
        ('PT-DEMO-001', 'Quarterly follow-up',  date '2026-04-10', 'rv_size_mm',         28.0, null),
        ('PT-DEMO-001', 'Quarterly follow-up',  date '2026-04-10', 'rvsp_mmhg',          25.0, null),
        ('PT-DEMO-001', 'Quarterly follow-up',  date '2026-04-10', 'exercise_tolerance', 1.0,  'NYHA I'),
        ('PT-DEMO-001', 'Quarterly follow-up',  date '2026-04-10', 'tc_insufficiency',   1.0,  null),
        ('PT-DEMO-001', 'Quarterly follow-up',  date '2026-04-10', 'rvot_gradient',       7.0, null),

        -- PT-DEMO-002 (age 7, PA atresia + VSD)
        ('PT-DEMO-002', 'Initial check', date '2025-10-01', 'weight_kg',          24.0, null),
        ('PT-DEMO-002', 'Initial check', date '2025-10-01', 'height_cm',         122.0, null),
        ('PT-DEMO-002', 'Initial check', date '2025-10-01', 'ef_pct',             48.0, 'Сниженная'),
        ('PT-DEMO-002', 'Initial check', date '2025-10-01', 'rv_size_mm',         36.0, 'Расширен'),
        ('PT-DEMO-002', 'Initial check', date '2025-10-01', 'rvsp_mmhg',          42.0, 'Лёгочная гипертензия'),
        ('PT-DEMO-002', 'Initial check', date '2025-10-01', 'exercise_tolerance', 2.0,  'NYHA II'),
        ('PT-DEMO-002', 'Initial check', date '2025-10-01', 'tc_insufficiency',   2.0,  'II степень'),
        ('PT-DEMO-002', 'Initial check', date '2025-10-01', 'rvot_gradient',      18.0, 'Умеренный градиент'),
        ('PT-DEMO-002', 'Control visit', date '2025-12-10', 'weight_kg',          24.5, null),
        ('PT-DEMO-002', 'Control visit', date '2025-12-10', 'height_cm',         123.0, null),
        ('PT-DEMO-002', 'Control visit', date '2025-12-10', 'ef_pct',             49.0, null),
        ('PT-DEMO-002', 'Control visit', date '2025-12-10', 'rv_size_mm',         36.0, null),
        ('PT-DEMO-002', 'Control visit', date '2025-12-10', 'rvsp_mmhg',          40.0, null),
        ('PT-DEMO-002', 'Control visit', date '2025-12-10', 'exercise_tolerance', 2.0,  null),
        ('PT-DEMO-002', 'Control visit', date '2025-12-10', 'tc_insufficiency',   2.0,  null),
        ('PT-DEMO-002', 'Control visit', date '2025-12-10', 'rvot_gradient',      17.0, null),

        -- PT-DEMO-003 (age 16, truncus arteriosus repair)
        ('PT-DEMO-003', 'Initial check', date '2025-07-01', 'weight_kg',          54.0, null),
        ('PT-DEMO-003', 'Initial check', date '2025-07-01', 'height_cm',         168.0, null),
        ('PT-DEMO-003', 'Initial check', date '2025-07-01', 'ef_pct',             52.0, null),
        ('PT-DEMO-003', 'Initial check', date '2025-07-01', 'rv_size_mm',         34.0, null),
        ('PT-DEMO-003', 'Initial check', date '2025-07-01', 'rvsp_mmhg',          38.0, null),
        ('PT-DEMO-003', 'Initial check', date '2025-07-01', 'exercise_tolerance', 2.0,  null),
        ('PT-DEMO-003', 'Initial check', date '2025-07-01', 'tc_insufficiency',   2.0,  null),
        ('PT-DEMO-003', 'Initial check', date '2025-07-01', 'rvot_gradient',      22.0, 'Умеренный стеноз кондуита'),
        ('PT-DEMO-003', 'Control visit', date '2025-09-01', 'weight_kg',          54.5, null),
        ('PT-DEMO-003', 'Control visit', date '2025-09-01', 'height_cm',         168.0, null),
        ('PT-DEMO-003', 'Control visit', date '2025-09-01', 'ef_pct',             50.0, null),
        ('PT-DEMO-003', 'Control visit', date '2025-09-01', 'rv_size_mm',         35.0, null),
        ('PT-DEMO-003', 'Control visit', date '2025-09-01', 'rvsp_mmhg',          41.0, 'Лёгочная гипертензия'),
        ('PT-DEMO-003', 'Control visit', date '2025-09-01', 'exercise_tolerance', 3.0,  'NYHA III'),
        ('PT-DEMO-003', 'Control visit', date '2025-09-01', 'tc_insufficiency',   3.0,  'III степень'),
        ('PT-DEMO-003', 'Control visit', date '2025-09-01', 'rvot_gradient',      25.0, 'Прогрессирование стеноза'),

        -- PT-DEMO-004 (age 5, PV stenosis post valvuloplasty)
        ('PT-DEMO-004', 'Initial check',       date '2026-01-15', 'weight_kg',          19.0, null),
        ('PT-DEMO-004', 'Initial check',       date '2026-01-15', 'height_cm',         110.0, null),
        ('PT-DEMO-004', 'Initial check',       date '2026-01-15', 'ef_pct',             62.0, null),
        ('PT-DEMO-004', 'Initial check',       date '2026-01-15', 'rv_size_mm',         24.0, null),
        ('PT-DEMO-004', 'Initial check',       date '2026-01-15', 'rvsp_mmhg',          28.0, null),
        ('PT-DEMO-004', 'Initial check',       date '2026-01-15', 'exercise_tolerance', 1.0,  null),
        ('PT-DEMO-004', 'Initial check',       date '2026-01-15', 'tc_insufficiency',   1.0,  null),
        ('PT-DEMO-004', 'Initial check',       date '2026-01-15', 'rvot_gradient',      12.0, null),
        ('PT-DEMO-004', 'Quarterly follow-up', date '2026-03-25', 'weight_kg',          19.5, null),
        ('PT-DEMO-004', 'Quarterly follow-up', date '2026-03-25', 'height_cm',         111.0, null),
        ('PT-DEMO-004', 'Quarterly follow-up', date '2026-03-25', 'ef_pct',             63.0, null),
        ('PT-DEMO-004', 'Quarterly follow-up', date '2026-03-25', 'rv_size_mm',         24.0, null),
        ('PT-DEMO-004', 'Quarterly follow-up', date '2026-03-25', 'rvsp_mmhg',          26.0, null),
        ('PT-DEMO-004', 'Quarterly follow-up', date '2026-03-25', 'exercise_tolerance', 1.0,  null),
        ('PT-DEMO-004', 'Quarterly follow-up', date '2026-03-25', 'tc_insufficiency',   1.0,  null),
        ('PT-DEMO-004', 'Quarterly follow-up', date '2026-03-25', 'rvot_gradient',      10.0, null),

        -- PT-DEMO-005 (age 13, DORV+VSD)
        ('PT-DEMO-005', 'Initial check', date '2025-11-20', 'weight_kg',          42.0, null),
        ('PT-DEMO-005', 'Initial check', date '2025-11-20', 'height_cm',         152.0, null),
        ('PT-DEMO-005', 'Initial check', date '2025-11-20', 'ef_pct',             54.0, null),
        ('PT-DEMO-005', 'Initial check', date '2025-11-20', 'rv_size_mm',         32.0, null),
        ('PT-DEMO-005', 'Initial check', date '2025-11-20', 'rvsp_mmhg',          34.0, null),
        ('PT-DEMO-005', 'Initial check', date '2025-11-20', 'exercise_tolerance', 2.0,  null),
        ('PT-DEMO-005', 'Initial check', date '2025-11-20', 'tc_insufficiency',   2.0,  null),
        ('PT-DEMO-005', 'Initial check', date '2025-11-20', 'rvot_gradient',      16.0, null),
        ('PT-DEMO-005', 'Control visit', date '2026-01-05', 'weight_kg',          43.0, null),
        ('PT-DEMO-005', 'Control visit', date '2026-01-05', 'height_cm',         153.0, null),
        ('PT-DEMO-005', 'Control visit', date '2026-01-05', 'ef_pct',             55.0, null),
        ('PT-DEMO-005', 'Control visit', date '2026-01-05', 'rv_size_mm',         32.0, null),
        ('PT-DEMO-005', 'Control visit', date '2026-01-05', 'rvsp_mmhg',          33.0, null),
        ('PT-DEMO-005', 'Control visit', date '2026-01-05', 'exercise_tolerance', 2.0,  null),
        ('PT-DEMO-005', 'Control visit', date '2026-01-05', 'tc_insufficiency',   2.0,  null),
        ('PT-DEMO-005', 'Control visit', date '2026-01-05', 'rvot_gradient',      15.0, null),

        -- PT-DEMO-006 (age 10, post-Ross PA stenosis)
        ('PT-DEMO-006', 'Initial check',       date '2026-02-05', 'weight_kg',          32.0, null),
        ('PT-DEMO-006', 'Initial check',       date '2026-02-05', 'height_cm',         136.0, null),
        ('PT-DEMO-006', 'Initial check',       date '2026-02-05', 'ef_pct',             60.0, null),
        ('PT-DEMO-006', 'Initial check',       date '2026-02-05', 'rv_size_mm',         26.0, null),
        ('PT-DEMO-006', 'Initial check',       date '2026-02-05', 'rvsp_mmhg',          24.0, null),
        ('PT-DEMO-006', 'Initial check',       date '2026-02-05', 'exercise_tolerance', 1.0,  null),
        ('PT-DEMO-006', 'Initial check',       date '2026-02-05', 'tc_insufficiency',   1.0,  null),
        ('PT-DEMO-006', 'Initial check',       date '2026-02-05', 'rvot_gradient',       9.0, null),
        ('PT-DEMO-006', 'Quarterly follow-up', date '2026-04-15', 'weight_kg',          33.0, null),
        ('PT-DEMO-006', 'Quarterly follow-up', date '2026-04-15', 'height_cm',         137.0, null),
        ('PT-DEMO-006', 'Quarterly follow-up', date '2026-04-15', 'ef_pct',             62.0, null),
        ('PT-DEMO-006', 'Quarterly follow-up', date '2026-04-15', 'rv_size_mm',         25.0, null),
        ('PT-DEMO-006', 'Quarterly follow-up', date '2026-04-15', 'rvsp_mmhg',          23.0, null),
        ('PT-DEMO-006', 'Quarterly follow-up', date '2026-04-15', 'exercise_tolerance', 1.0,  null),
        ('PT-DEMO-006', 'Quarterly follow-up', date '2026-04-15', 'tc_insufficiency',   1.0,  null),
        ('PT-DEMO-006', 'Quarterly follow-up', date '2026-04-15', 'rvot_gradient',       8.0, null),

        -- PT-DEMO-007 (age 14, Ebstein anomaly post-repair)
        ('PT-DEMO-007', 'Initial check', date '2025-12-18', 'weight_kg',          46.0, null),
        ('PT-DEMO-007', 'Initial check', date '2025-12-18', 'height_cm',         160.0, null),
        ('PT-DEMO-007', 'Initial check', date '2025-12-18', 'ef_pct',             50.0, null),
        ('PT-DEMO-007', 'Initial check', date '2025-12-18', 'rv_size_mm',         38.0, 'Расширен'),
        ('PT-DEMO-007', 'Initial check', date '2025-12-18', 'rvsp_mmhg',          30.0, null),
        ('PT-DEMO-007', 'Initial check', date '2025-12-18', 'exercise_tolerance', 2.0,  null),
        ('PT-DEMO-007', 'Initial check', date '2025-12-18', 'tc_insufficiency',   3.0,  'III степень'),
        ('PT-DEMO-007', 'Initial check', date '2025-12-18', 'rvot_gradient',      11.0, null),
        ('PT-DEMO-007', 'Control visit', date '2026-02-20', 'weight_kg',          46.5, null),
        ('PT-DEMO-007', 'Control visit', date '2026-02-20', 'height_cm',         161.0, null),
        ('PT-DEMO-007', 'Control visit', date '2026-02-20', 'ef_pct',             51.0, null),
        ('PT-DEMO-007', 'Control visit', date '2026-02-20', 'rv_size_mm',         38.0, null),
        ('PT-DEMO-007', 'Control visit', date '2026-02-20', 'rvsp_mmhg',          31.0, null),
        ('PT-DEMO-007', 'Control visit', date '2026-02-20', 'exercise_tolerance', 2.0,  null),
        ('PT-DEMO-007', 'Control visit', date '2026-02-20', 'tc_insufficiency',   3.0,  null),
        ('PT-DEMO-007', 'Control visit', date '2026-02-20', 'rvot_gradient',      12.0, null),

        -- PT-DEMO-008 (age 17, prosthetic PA stenosis)
        ('PT-DEMO-008', 'Initial check', date '2025-09-12', 'weight_kg',          62.0, null),
        ('PT-DEMO-008', 'Initial check', date '2025-09-12', 'height_cm',         170.0, null),
        ('PT-DEMO-008', 'Initial check', date '2025-09-12', 'ef_pct',             48.0, null),
        ('PT-DEMO-008', 'Initial check', date '2025-09-12', 'rv_size_mm',         36.0, null),
        ('PT-DEMO-008', 'Initial check', date '2025-09-12', 'rvsp_mmhg',          45.0, 'Тяжёлая гипертензия'),
        ('PT-DEMO-008', 'Initial check', date '2025-09-12', 'exercise_tolerance', 3.0,  null),
        ('PT-DEMO-008', 'Initial check', date '2025-09-12', 'tc_insufficiency',   3.0,  null),
        ('PT-DEMO-008', 'Initial check', date '2025-09-12', 'rvot_gradient',      32.0, 'Выраженный стеноз протеза'),
        ('PT-DEMO-008', 'Control visit', date '2025-11-30', 'weight_kg',          62.5, null),
        ('PT-DEMO-008', 'Control visit', date '2025-11-30', 'height_cm',         170.0, null),
        ('PT-DEMO-008', 'Control visit', date '2025-11-30', 'ef_pct',             49.0, null),
        ('PT-DEMO-008', 'Control visit', date '2025-11-30', 'rv_size_mm',         37.0, null),
        ('PT-DEMO-008', 'Control visit', date '2025-11-30', 'rvsp_mmhg',          44.0, null),
        ('PT-DEMO-008', 'Control visit', date '2025-11-30', 'exercise_tolerance', 3.0,  null),
        ('PT-DEMO-008', 'Control visit', date '2025-11-30', 'tc_insufficiency',   3.0,  null),
        ('PT-DEMO-008', 'Control visit', date '2025-11-30', 'rvot_gradient',      30.0, null),

        -- PT-DEMO-009 (age 4, complete AVSD)
        ('PT-DEMO-009', 'Initial check', date '2026-03-03', 'weight_kg',          15.0, null),
        ('PT-DEMO-009', 'Initial check', date '2026-03-03', 'height_cm',         101.0, null),
        ('PT-DEMO-009', 'Initial check', date '2026-03-03', 'ef_pct',             52.0, null),
        ('PT-DEMO-009', 'Initial check', date '2026-03-03', 'rv_size_mm',         22.0, null),
        ('PT-DEMO-009', 'Initial check', date '2026-03-03', 'rvsp_mmhg',          30.0, null),
        ('PT-DEMO-009', 'Initial check', date '2026-03-03', 'exercise_tolerance', 2.0,  null),
        ('PT-DEMO-009', 'Initial check', date '2026-03-03', 'tc_insufficiency',   2.0,  null),
        ('PT-DEMO-009', 'Initial check', date '2026-03-03', 'rvot_gradient',      10.0, null),
        ('PT-DEMO-009', 'Control visit', date '2026-04-20', 'weight_kg',          15.2, null),
        ('PT-DEMO-009', 'Control visit', date '2026-04-20', 'height_cm',         102.0, null),
        ('PT-DEMO-009', 'Control visit', date '2026-04-20', 'ef_pct',             54.0, null),
        ('PT-DEMO-009', 'Control visit', date '2026-04-20', 'rv_size_mm',         22.0, null),
        ('PT-DEMO-009', 'Control visit', date '2026-04-20', 'rvsp_mmhg',          28.0, null),
        ('PT-DEMO-009', 'Control visit', date '2026-04-20', 'exercise_tolerance', 1.0,  null),
        ('PT-DEMO-009', 'Control visit', date '2026-04-20', 'tc_insufficiency',   2.0,  null),
        ('PT-DEMO-009', 'Control visit', date '2026-04-20', 'rvot_gradient',       9.0, null),

        -- PT-DEMO-010 (age 16, TGA post-Rastelli)
        ('PT-DEMO-010', 'Initial check', date '2025-08-05', 'weight_kg',          58.0, null),
        ('PT-DEMO-010', 'Initial check', date '2025-08-05', 'height_cm',         172.0, null),
        ('PT-DEMO-010', 'Initial check', date '2025-08-05', 'ef_pct',             50.0, null),
        ('PT-DEMO-010', 'Initial check', date '2025-08-05', 'rv_size_mm',         34.0, null),
        ('PT-DEMO-010', 'Initial check', date '2025-08-05', 'rvsp_mmhg',          36.0, null),
        ('PT-DEMO-010', 'Initial check', date '2025-08-05', 'exercise_tolerance', 2.0,  null),
        ('PT-DEMO-010', 'Initial check', date '2025-08-05', 'tc_insufficiency',   2.0,  null),
        ('PT-DEMO-010', 'Initial check', date '2025-08-05', 'rvot_gradient',      20.0, null),
        ('PT-DEMO-010', 'Control visit', date '2026-01-18', 'weight_kg',          59.0, null),
        ('PT-DEMO-010', 'Control visit', date '2026-01-18', 'height_cm',         172.5, null),
        ('PT-DEMO-010', 'Control visit', date '2026-01-18', 'ef_pct',             49.0, null),
        ('PT-DEMO-010', 'Control visit', date '2026-01-18', 'rv_size_mm',         35.0, null),
        ('PT-DEMO-010', 'Control visit', date '2026-01-18', 'rvsp_mmhg',          38.0, null),
        ('PT-DEMO-010', 'Control visit', date '2026-01-18', 'exercise_tolerance', 2.0,  null),
        ('PT-DEMO-010', 'Control visit', date '2026-01-18', 'tc_insufficiency',   2.0,  null),
        ('PT-DEMO-010', 'Control visit', date '2026-01-18', 'rvot_gradient',      22.0, null),

        -- PT-DEMO-011 (age 6, post-PA repair)
        ('PT-DEMO-011', 'Initial check', date '2026-02-02', 'weight_kg',          21.0, null),
        ('PT-DEMO-011', 'Initial check', date '2026-02-02', 'height_cm',         116.0, null),
        ('PT-DEMO-011', 'Initial check', date '2026-02-02', 'ef_pct',             61.0, null),
        ('PT-DEMO-011', 'Initial check', date '2026-02-02', 'rv_size_mm',         24.0, null),
        ('PT-DEMO-011', 'Initial check', date '2026-02-02', 'rvsp_mmhg',          26.0, null),
        ('PT-DEMO-011', 'Initial check', date '2026-02-02', 'exercise_tolerance', 1.0,  null),
        ('PT-DEMO-011', 'Initial check', date '2026-02-02', 'tc_insufficiency',   1.0,  null),
        ('PT-DEMO-011', 'Initial check', date '2026-02-02', 'rvot_gradient',       9.0, null),
        ('PT-DEMO-011', 'Control visit', date '2026-03-12', 'weight_kg',          21.5, null),
        ('PT-DEMO-011', 'Control visit', date '2026-03-12', 'height_cm',         117.0, null),
        ('PT-DEMO-011', 'Control visit', date '2026-03-12', 'ef_pct',             62.0, null),
        ('PT-DEMO-011', 'Control visit', date '2026-03-12', 'rv_size_mm',         24.0, null),
        ('PT-DEMO-011', 'Control visit', date '2026-03-12', 'rvsp_mmhg',          25.0, null),
        ('PT-DEMO-011', 'Control visit', date '2026-03-12', 'exercise_tolerance', 1.0,  null),
        ('PT-DEMO-011', 'Control visit', date '2026-03-12', 'tc_insufficiency',   1.0,  null),
        ('PT-DEMO-011', 'Control visit', date '2026-03-12', 'rvot_gradient',       8.0, null),

        -- PT-DEMO-012 (age 13, ToF with absent PV)
        ('PT-DEMO-012', 'Initial check', date '2025-06-22', 'weight_kg',          40.0, null),
        ('PT-DEMO-012', 'Initial check', date '2025-06-22', 'height_cm',         150.0, null),
        ('PT-DEMO-012', 'Initial check', date '2025-06-22', 'ef_pct',             46.0, 'Сниженная'),
        ('PT-DEMO-012', 'Initial check', date '2025-06-22', 'rv_size_mm',         39.0, 'Расширен'),
        ('PT-DEMO-012', 'Initial check', date '2025-06-22', 'rvsp_mmhg',          48.0, 'Высокая гипертензия'),
        ('PT-DEMO-012', 'Initial check', date '2025-06-22', 'exercise_tolerance', 3.0,  null),
        ('PT-DEMO-012', 'Initial check', date '2025-06-22', 'tc_insufficiency',   3.0,  null),
        ('PT-DEMO-012', 'Initial check', date '2025-06-22', 'rvot_gradient',      28.0, null),
        ('PT-DEMO-012', 'Control visit', date '2025-10-15', 'weight_kg',          40.5, null),
        ('PT-DEMO-012', 'Control visit', date '2025-10-15', 'height_cm',         151.0, null),
        ('PT-DEMO-012', 'Control visit', date '2025-10-15', 'ef_pct',             45.0, null),
        ('PT-DEMO-012', 'Control visit', date '2025-10-15', 'rv_size_mm',         40.0, null),
        ('PT-DEMO-012', 'Control visit', date '2025-10-15', 'rvsp_mmhg',          50.0, null),
        ('PT-DEMO-012', 'Control visit', date '2025-10-15', 'exercise_tolerance', 3.0,  null),
        ('PT-DEMO-012', 'Control visit', date '2025-10-15', 'tc_insufficiency',   3.0,  null),
        ('PT-DEMO-012', 'Control visit', date '2025-10-15', 'rvot_gradient',      30.0, null)
) as v(username, title, exam_date, characteristic_code, measurement_value, measurement_comment)
join users u on u.username = v.username
join patient_profiles p on p.user_id = u.id
join examinations e on e.patient_id = p.id and e.title = v.title and e.exam_date = v.exam_date
join characteristics c on c.code = v.characteristic_code;
