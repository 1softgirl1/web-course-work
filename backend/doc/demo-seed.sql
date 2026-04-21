-- Demo data for Swagger / manual backend presentation
-- Safe to re-run on a clean or already seeded database.
-- This script also removes local Bruno runtime users by demo prefixes, so
-- repeated full collection runs start from a predictable state.
--
-- Demo credentials:
--   doctor.demo@example.com / doctor-password
--   head.doctor@example.com / extended-password
--   outsider.doctor@example.com / doctor-password
--   PT-DEMO-001 / patient-password
--   PT-DEMO-002 / patient-password
--   PT-DEMO-003 / patient-password
--   PT-DEMO-004 / patient-password
--   PT-DEMO-005 / patient-password
--
-- Region layout used by this script:
--   region_id = 1  -> doctor's home region
--   region_id = 2  -> foreign region A
--   region_id = 3  -> foreign region B

begin;

delete from users
where username like 'bruno.%'
   or username like 'PT-%'
   or username in (
    'doctor.demo@example.com',
    'head.doctor@example.com',
    'outsider.doctor@example.com'
);

with inserted_users as (
    insert into users (username, password_hash, role, status)
    values
        ('doctor.demo@example.com', '$2a$10$Vvh4BhrIYEtRO0BJ99ZyouqsvijULT5r4KBhMzFZs58sQo4lZW342', 'DOCTOR', 'ACTIVE'),
        ('head.doctor@example.com', '$2a$10$rtNF4r7xy9nbnWSEnWDETubjduV.qCedTCtwVNEEHfTu7iJP2.c0i', 'DOCTOR_EXTENDED', 'ACTIVE'),
        ('outsider.doctor@example.com', '$2a$10$Vvh4BhrIYEtRO0BJ99ZyouqsvijULT5r4KBhMzFZs58sQo4lZW342', 'DOCTOR', 'ACTIVE')
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
where username = 'outsider.doctor@example.com';

with inserted_users as (
    insert into users (username, password_hash, role, status)
    values
        ('PT-DEMO-001', '$2a$10$fg9VwpbVioZ3/D6BCqMEiOw1ii02FmSKOOZt52xb.m1pjFcyblURi', 'PATIENT', 'ACTIVE'),
        ('PT-DEMO-002', '$2a$10$fg9VwpbVioZ3/D6BCqMEiOw1ii02FmSKOOZt52xb.m1pjFcyblURi', 'PATIENT', 'ACTIVE'),
        ('PT-DEMO-003', '$2a$10$fg9VwpbVioZ3/D6BCqMEiOw1ii02FmSKOOZt52xb.m1pjFcyblURi', 'PATIENT', 'ACTIVE'),
        ('PT-DEMO-004', '$2a$10$fg9VwpbVioZ3/D6BCqMEiOw1ii02FmSKOOZt52xb.m1pjFcyblURi', 'PATIENT', 'ACTIVE'),
        ('PT-DEMO-005', '$2a$10$fg9VwpbVioZ3/D6BCqMEiOw1ii02FmSKOOZt52xb.m1pjFcyblURi', 'PATIENT', 'ACTIVE')
    returning id, username
)
insert into patient_profiles (
    user_id,
    region_id,
    last_name,
    first_name,
    middle_name,
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
select id, 1, 'Petrov', 'Petr', 'Petrovich', date '1971-01-15',
       'Aortic valve stenosis', 'Medtronic Evolut', '26 mm', 'Bioprosthetic',
       'General anesthesia', 120, 'Transfemoral', 'Bisoprolol 5 mg daily',
       timestamp '2026-03-01 09:30:00'
from inserted_users where username = 'PT-DEMO-001'
union all
select id, 1, 'Smirnova', 'Elena', 'Igorevna', date '1965-08-22',
       'Mitral regurgitation', 'Edwards Sapien', '23 mm', 'Bioprosthetic',
       'General anesthesia', 145, 'Transapical', 'Aspirin 75 mg daily',
       timestamp '2026-02-10 11:15:00'
from inserted_users where username = 'PT-DEMO-002'
union all
select id, 1, 'Volkov', 'Andrey', 'Olegovich', date '1959-11-02',
       'Postoperative observation', 'Abbott Portico', '27 mm', 'Bioprosthetic',
       'General anesthesia', 135, 'Transfemoral', 'Warfarin 2.5 mg daily',
       timestamp '2026-01-20 14:45:00'
from inserted_users where username = 'PT-DEMO-003'
union all
select id, 2, 'Sokolova', 'Maria', 'Vladimirovna', date '1980-06-17',
       'Chronic heart failure monitoring', 'Boston Acurate', '25 mm', 'Bioprosthetic',
       'Sedation', 110, 'Transfemoral', 'Furosemide 40 mg daily',
       timestamp '2026-04-01 10:00:00'
from inserted_users where username = 'PT-DEMO-004'
union all
select id, 3, 'Nikolaev', 'Sergey', 'Pavlovich', date '1976-12-04',
       'Valve replacement follow-up', 'CoreValve', '29 mm', 'Bioprosthetic',
       'General anesthesia', 160, 'Transaortic', 'Clopidogrel 75 mg daily',
       timestamp '2026-03-15 16:20:00'
from inserted_users where username = 'PT-DEMO-005';

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
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-005';

insert into examination_characteristics (examination_id, characteristic_id, value, comment)
select e.id, c.id, 72, 'Measured at rest'
from examinations e
join patient_profiles p on p.id = e.patient_id
join users u on u.id = p.user_id
join characteristics c on c.code = 'metric_01'
where u.username = 'PT-DEMO-001' and e.title = 'Initial check' and e.exam_date = date '2026-02-15'
union all
select e.id, c.id, 120, 'Systolic pressure'
from examinations e
join patient_profiles p on p.id = e.patient_id
join users u on u.id = p.user_id
join characteristics c on c.code = 'metric_02'
where u.username = 'PT-DEMO-001' and e.title = 'Initial check' and e.exam_date = date '2026-02-15'
union all
select e.id, c.id, 68, 'Measured at rest'
from examinations e
join patient_profiles p on p.id = e.patient_id
join users u on u.id = p.user_id
join characteristics c on c.code = 'metric_01'
where u.username = 'PT-DEMO-001' and e.title = 'Quarterly follow-up' and e.exam_date = date '2026-04-10'
union all
select e.id, c.id, 118, 'Systolic pressure'
from examinations e
join patient_profiles p on p.id = e.patient_id
join users u on u.id = p.user_id
join characteristics c on c.code = 'metric_02'
where u.username = 'PT-DEMO-001' and e.title = 'Quarterly follow-up' and e.exam_date = date '2026-04-10'
union all
select e.id, c.id, 80, 'Measured at rest'
from examinations e
join patient_profiles p on p.id = e.patient_id
join users u on u.id = p.user_id
join characteristics c on c.code = 'metric_01'
where u.username = 'PT-DEMO-002' and e.title = 'Initial check' and e.exam_date = date '2025-10-01'
union all
select e.id, c.id, 135, 'Systolic pressure'
from examinations e
join patient_profiles p on p.id = e.patient_id
join users u on u.id = p.user_id
join characteristics c on c.code = 'metric_02'
where u.username = 'PT-DEMO-002' and e.title = 'Initial check' and e.exam_date = date '2025-10-01'
union all
select e.id, c.id, 82, 'Measured at rest'
from examinations e
join patient_profiles p on p.id = e.patient_id
join users u on u.id = p.user_id
join characteristics c on c.code = 'metric_01'
where u.username = 'PT-DEMO-002' and e.title = 'Control visit' and e.exam_date = date '2025-12-10'
union all
select e.id, c.id, 132, 'Systolic pressure'
from examinations e
join patient_profiles p on p.id = e.patient_id
join users u on u.id = p.user_id
join characteristics c on c.code = 'metric_02'
where u.username = 'PT-DEMO-002' and e.title = 'Control visit' and e.exam_date = date '2025-12-10'
union all
select e.id, c.id, 78, 'Measured at rest'
from examinations e
join patient_profiles p on p.id = e.patient_id
join users u on u.id = p.user_id
join characteristics c on c.code = 'metric_01'
where u.username = 'PT-DEMO-003' and e.title = 'Initial check' and e.exam_date = date '2025-07-01'
union all
select e.id, c.id, 140, 'Systolic pressure'
from examinations e
join patient_profiles p on p.id = e.patient_id
join users u on u.id = p.user_id
join characteristics c on c.code = 'metric_02'
where u.username = 'PT-DEMO-003' and e.title = 'Initial check' and e.exam_date = date '2025-07-01'
union all
select e.id, c.id, 85, 'Measured at rest'
from examinations e
join patient_profiles p on p.id = e.patient_id
join users u on u.id = p.user_id
join characteristics c on c.code = 'metric_01'
where u.username = 'PT-DEMO-003' and e.title = 'Control visit' and e.exam_date = date '2025-09-01'
union all
select e.id, c.id, 145, 'Systolic pressure'
from examinations e
join patient_profiles p on p.id = e.patient_id
join users u on u.id = p.user_id
join characteristics c on c.code = 'metric_02'
where u.username = 'PT-DEMO-003' and e.title = 'Control visit' and e.exam_date = date '2025-09-01'
union all
select e.id, c.id, 76, 'Measured at rest'
from examinations e
join patient_profiles p on p.id = e.patient_id
join users u on u.id = p.user_id
join characteristics c on c.code = 'metric_01'
where u.username = 'PT-DEMO-004' and e.title = 'Initial check' and e.exam_date = date '2026-01-15'
union all
select e.id, c.id, 128, 'Systolic pressure'
from examinations e
join patient_profiles p on p.id = e.patient_id
join users u on u.id = p.user_id
join characteristics c on c.code = 'metric_02'
where u.username = 'PT-DEMO-004' and e.title = 'Initial check' and e.exam_date = date '2026-01-15'
union all
select e.id, c.id, 70, 'Measured at rest'
from examinations e
join patient_profiles p on p.id = e.patient_id
join users u on u.id = p.user_id
join characteristics c on c.code = 'metric_01'
where u.username = 'PT-DEMO-004' and e.title = 'Quarterly follow-up' and e.exam_date = date '2026-03-25'
union all
select e.id, c.id, 122, 'Systolic pressure'
from examinations e
join patient_profiles p on p.id = e.patient_id
join users u on u.id = p.user_id
join characteristics c on c.code = 'metric_02'
where u.username = 'PT-DEMO-004' and e.title = 'Quarterly follow-up' and e.exam_date = date '2026-03-25'
union all
select e.id, c.id, 88, 'Measured at rest'
from examinations e
join patient_profiles p on p.id = e.patient_id
join users u on u.id = p.user_id
join characteristics c on c.code = 'metric_01'
where u.username = 'PT-DEMO-005' and e.title = 'Initial check' and e.exam_date = date '2025-11-20'
union all
select e.id, c.id, 138, 'Systolic pressure'
from examinations e
join patient_profiles p on p.id = e.patient_id
join users u on u.id = p.user_id
join characteristics c on c.code = 'metric_02'
where u.username = 'PT-DEMO-005' and e.title = 'Initial check' and e.exam_date = date '2025-11-20'
union all
select e.id, c.id, 84, 'Measured at rest'
from examinations e
join patient_profiles p on p.id = e.patient_id
join users u on u.id = p.user_id
join characteristics c on c.code = 'metric_01'
where u.username = 'PT-DEMO-005' and e.title = 'Control visit' and e.exam_date = date '2026-01-05'
union all
select e.id, c.id, 134, 'Systolic pressure'
from examinations e
join patient_profiles p on p.id = e.patient_id
join users u on u.id = p.user_id
join characteristics c on c.code = 'metric_02'
where u.username = 'PT-DEMO-005' and e.title = 'Control visit' and e.exam_date = date '2026-01-05';
commit;
