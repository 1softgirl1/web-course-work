alter table doctor_profiles
    add column workplace varchar(255) not null default 'Unknown workplace';

alter table doctor_profiles
    alter column workplace drop default;
