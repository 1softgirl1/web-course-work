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
