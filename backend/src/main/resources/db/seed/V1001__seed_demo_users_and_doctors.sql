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
