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
