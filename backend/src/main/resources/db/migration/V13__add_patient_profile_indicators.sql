alter table patient_profiles
    add column sex varchar(1) not null default 'M',
    add column operation_place varchar(255) not null default '',
    add column observation_place varchar(255) not null default '',
    add column coronary_anatomy text not null default '';

alter table patient_profiles
    alter column sex drop default,
    alter column operation_place drop default,
    alter column observation_place drop default,
    alter column coronary_anatomy drop default;

alter table patient_profiles
    add constraint chk_patient_profiles_sex
        check (sex in ('M', 'F'));
