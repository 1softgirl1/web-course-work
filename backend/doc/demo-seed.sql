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

-- ============================================================================
-- V1000__reset_demo_runtime_users.sql
-- ============================================================================
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

-- ============================================================================
-- V1001__seed_demo_users_and_doctors.sql
-- ============================================================================
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

-- ============================================================================
-- V1002__seed_demo_patients.sql
-- ============================================================================
-- Local/dev only demo patients. Patient code lives only in users.username.
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
    diagnosis,
    valve_name,
    valve_size,
    valve_material,
    operation_anesthesia,
    operation_duration_minutes,
    operation_delivery_system,
    medications,
    created_at
)
select id, 1, date '1971-01-15',
       'Aortic valve stenosis', 'Medtronic Evolut', '26 mm', 'Bioprosthetic',
       'General anesthesia', 120, 'Transfemoral', 'Bisoprolol 5 mg daily',
       timestamp '2026-03-01 09:30:00'
from inserted_users where username = 'PT-DEMO-001'
union all
select id, 1, date '1965-08-22',
       'Mitral regurgitation', 'Edwards Sapien', '23 mm', 'Bioprosthetic',
       'General anesthesia', 145, 'Transapical', 'Aspirin 75 mg daily',
       timestamp '2026-02-10 11:15:00'
from inserted_users where username = 'PT-DEMO-002'
union all
select id, 1, date '1959-11-02',
       'Postoperative observation', 'Abbott Portico', '27 mm', 'Bioprosthetic',
       'General anesthesia', 135, 'Transfemoral', 'Warfarin 2.5 mg daily',
       timestamp '2026-01-20 14:45:00'
from inserted_users where username = 'PT-DEMO-003'
union all
select id, 2, date '1980-06-17',
       'Chronic heart failure monitoring', 'Boston Acurate', '25 mm', 'Bioprosthetic',
       'Sedation', 110, 'Transfemoral', 'Furosemide 40 mg daily',
       timestamp '2026-04-01 10:00:00'
from inserted_users where username = 'PT-DEMO-004'
union all
select id, 3, date '1976-12-04',
       'Valve replacement follow-up', 'CoreValve', '29 mm', 'Bioprosthetic',
       'General anesthesia', 160, 'Transaortic', 'Clopidogrel 75 mg daily',
       timestamp '2026-03-15 16:20:00'
from inserted_users where username = 'PT-DEMO-005'
union all
select id, 22, date '1968-04-19',
       'Post-TAVI observation', 'Medtronic Evolut', '29 mm', 'Bioprosthetic',
       'General anesthesia', 125, 'Transfemoral', 'Apixaban 5 mg twice daily',
       timestamp '2026-03-18 09:10:00'
from inserted_users where username = 'PT-DEMO-006'
union all
select id, 39, date '1973-09-07',
       'Aortic valve regurgitation', 'Edwards Sapien', '26 mm', 'Bioprosthetic',
       'Sedation', 118, 'Transfemoral', 'Atorvastatin 20 mg daily',
       timestamp '2026-02-28 12:35:00'
from inserted_users where username = 'PT-DEMO-007'
union all
select id, 76, date '1956-05-30',
       'Mitral valve follow-up', 'Abbott Portico', '25 mm', 'Bioprosthetic',
       'General anesthesia', 150, 'Transapical', 'Bisoprolol 2.5 mg daily',
       timestamp '2026-01-25 15:40:00'
from inserted_users where username = 'PT-DEMO-008'
union all
select id, 44, date '1982-02-11',
       'Postoperative rhythm monitoring', 'Boston Acurate', '27 mm', 'Bioprosthetic',
       'Sedation', 105, 'Transfemoral', 'Amiodarone 200 mg daily',
       timestamp '2026-04-04 08:50:00'
from inserted_users where username = 'PT-DEMO-009'
union all
select id, 33, date '1962-10-24',
       'Complex valve replacement follow-up', 'CoreValve', '31 mm', 'Bioprosthetic',
       'General anesthesia', 175, 'Transaortic', 'Clopidogrel 75 mg daily',
       timestamp '2026-03-29 13:05:00'
from inserted_users where username = 'PT-DEMO-010'
union all
select id, 67, date '1979-07-03',
       'Long-term valve function control', 'Medtronic Evolut', '23 mm', 'Bioprosthetic',
       'Sedation', 115, 'Transfemoral', 'Aspirin 100 mg daily',
       timestamp '2026-02-16 10:25:00'
from inserted_users where username = 'PT-DEMO-011'
union all
select id, 26, date '1954-03-21',
       'Chronic heart failure and valve monitoring', 'Edwards Sapien', '29 mm', 'Bioprosthetic',
       'General anesthesia', 155, 'Transapical', 'Torsemide 10 mg daily',
       timestamp '2026-01-12 11:55:00'
from inserted_users where username = 'PT-DEMO-012';

-- ============================================================================
-- V1003__seed_demo_examinations.sql
-- ============================================================================
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

-- ============================================================================
-- V1004__seed_demo_examination_measurements.sql
-- ============================================================================
-- Local/dev only demo measurements.
insert into examination_characteristics (examination_id, characteristic_id, value, comment)
select e.id, c.id, v.measurement_value, v.measurement_comment
from (
    values
        ('PT-DEMO-001', 'Initial check', date '2026-02-15', 'metric_01', 72, 'Measured at rest'),
        ('PT-DEMO-001', 'Initial check', date '2026-02-15', 'metric_02', 120, 'Systolic pressure'),
        ('PT-DEMO-001', 'Quarterly follow-up', date '2026-04-10', 'metric_01', 68, 'Measured at rest'),
        ('PT-DEMO-001', 'Quarterly follow-up', date '2026-04-10', 'metric_02', 118, 'Systolic pressure'),
        ('PT-DEMO-002', 'Initial check', date '2025-10-01', 'metric_01', 80, 'Measured at rest'),
        ('PT-DEMO-002', 'Initial check', date '2025-10-01', 'metric_02', 135, 'Systolic pressure'),
        ('PT-DEMO-002', 'Control visit', date '2025-12-10', 'metric_01', 82, 'Measured at rest'),
        ('PT-DEMO-002', 'Control visit', date '2025-12-10', 'metric_02', 132, 'Systolic pressure'),
        ('PT-DEMO-003', 'Initial check', date '2025-07-01', 'metric_01', 78, 'Measured at rest'),
        ('PT-DEMO-003', 'Initial check', date '2025-07-01', 'metric_02', 140, 'Systolic pressure'),
        ('PT-DEMO-003', 'Control visit', date '2025-09-01', 'metric_01', 85, 'Measured at rest'),
        ('PT-DEMO-003', 'Control visit', date '2025-09-01', 'metric_02', 145, 'Systolic pressure'),
        ('PT-DEMO-004', 'Initial check', date '2026-01-15', 'metric_01', 76, 'Measured at rest'),
        ('PT-DEMO-004', 'Initial check', date '2026-01-15', 'metric_02', 128, 'Systolic pressure'),
        ('PT-DEMO-004', 'Quarterly follow-up', date '2026-03-25', 'metric_01', 70, 'Measured at rest'),
        ('PT-DEMO-004', 'Quarterly follow-up', date '2026-03-25', 'metric_02', 122, 'Systolic pressure'),
        ('PT-DEMO-005', 'Initial check', date '2025-11-20', 'metric_01', 88, 'Measured at rest'),
        ('PT-DEMO-005', 'Initial check', date '2025-11-20', 'metric_02', 138, 'Systolic pressure'),
        ('PT-DEMO-005', 'Control visit', date '2026-01-05', 'metric_01', 84, 'Measured at rest'),
        ('PT-DEMO-005', 'Control visit', date '2026-01-05', 'metric_02', 134, 'Systolic pressure'),
        ('PT-DEMO-006', 'Initial check', date '2026-02-05', 'metric_01', 73, 'Measured at rest'),
        ('PT-DEMO-006', 'Initial check', date '2026-02-05', 'metric_02', 124, 'Systolic pressure'),
        ('PT-DEMO-006', 'Quarterly follow-up', date '2026-04-15', 'metric_01', 69, 'Measured at rest'),
        ('PT-DEMO-006', 'Quarterly follow-up', date '2026-04-15', 'metric_02', 119, 'Systolic pressure'),
        ('PT-DEMO-007', 'Initial check', date '2025-12-18', 'metric_01', 77, 'Measured at rest'),
        ('PT-DEMO-007', 'Initial check', date '2025-12-18', 'metric_02', 130, 'Systolic pressure'),
        ('PT-DEMO-007', 'Control visit', date '2026-02-20', 'metric_01', 79, 'Measured at rest'),
        ('PT-DEMO-007', 'Control visit', date '2026-02-20', 'metric_02', 128, 'Systolic pressure'),
        ('PT-DEMO-008', 'Initial check', date '2025-09-12', 'metric_01', 83, 'Measured at rest'),
        ('PT-DEMO-008', 'Initial check', date '2025-09-12', 'metric_02', 142, 'Systolic pressure'),
        ('PT-DEMO-008', 'Control visit', date '2025-11-30', 'metric_01', 81, 'Measured at rest'),
        ('PT-DEMO-008', 'Control visit', date '2025-11-30', 'metric_02', 136, 'Systolic pressure'),
        ('PT-DEMO-009', 'Initial check', date '2026-03-03', 'metric_01', 74, 'Measured at rest'),
        ('PT-DEMO-009', 'Initial check', date '2026-03-03', 'metric_02', 126, 'Systolic pressure'),
        ('PT-DEMO-009', 'Control visit', date '2026-04-20', 'metric_01', 71, 'Measured at rest'),
        ('PT-DEMO-009', 'Control visit', date '2026-04-20', 'metric_02', 121, 'Systolic pressure'),
        ('PT-DEMO-010', 'Initial check', date '2025-08-05', 'metric_01', 86, 'Measured at rest'),
        ('PT-DEMO-010', 'Initial check', date '2025-08-05', 'metric_02', 146, 'Systolic pressure'),
        ('PT-DEMO-010', 'Control visit', date '2026-01-18', 'metric_01', 84, 'Measured at rest'),
        ('PT-DEMO-010', 'Control visit', date '2026-01-18', 'metric_02', 139, 'Systolic pressure'),
        ('PT-DEMO-011', 'Initial check', date '2026-02-02', 'metric_01', 75, 'Measured at rest'),
        ('PT-DEMO-011', 'Initial check', date '2026-02-02', 'metric_02', 122, 'Systolic pressure'),
        ('PT-DEMO-011', 'Control visit', date '2026-03-12', 'metric_01', 72, 'Measured at rest'),
        ('PT-DEMO-011', 'Control visit', date '2026-03-12', 'metric_02', 120, 'Systolic pressure'),
        ('PT-DEMO-012', 'Initial check', date '2025-06-22', 'metric_01', 90, 'Measured at rest'),
        ('PT-DEMO-012', 'Initial check', date '2025-06-22', 'metric_02', 150, 'Systolic pressure'),
        ('PT-DEMO-012', 'Control visit', date '2025-10-15', 'metric_01', 87, 'Measured at rest'),
        ('PT-DEMO-012', 'Control visit', date '2025-10-15', 'metric_02', 144, 'Systolic pressure')
) as v(username, title, exam_date, characteristic_code, measurement_value, measurement_comment)
join users u on u.username = v.username
join patient_profiles p on p.user_id = u.id
join examinations e on e.patient_id = p.id and e.title = v.title and e.exam_date = v.exam_date
join characteristics c on c.code = v.characteristic_code;
