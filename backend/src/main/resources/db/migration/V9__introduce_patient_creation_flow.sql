alter table users rename column email to login;

alter table patient_profiles
    drop column last_name,
    drop column first_name,
    drop column middle_name,
    drop column birth_date,
    drop column phone;

alter table patient_profiles
    add column patient_code varchar(50) not null,
    add column age integer not null,
    add column diagnosis text not null,
    add column valve_name varchar(255) not null,
    add column valve_size varchar(100) not null,
    add column valve_material varchar(100) not null,
    add column operation_anesthesia varchar(255) not null,
    add column operation_duration_minutes integer not null,
    add column operation_delivery_system varchar(255) not null,
    add column medications text not null,
    add column created_at timestamp without time zone not null default current_timestamp;

alter table patient_profiles
    add constraint uq_patient_profiles_patient_code unique (patient_code);

alter table patient_profiles
    add constraint chk_patient_profiles_age
        check (age >= 0 and age <= 150);

alter table patient_profiles
    add constraint chk_patient_profiles_operation_duration
        check (operation_duration_minutes > 0);
