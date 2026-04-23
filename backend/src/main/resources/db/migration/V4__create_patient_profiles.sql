create table patient_profiles (
    id bigserial primary key,
    user_id bigint not null unique,
    region_id bigint not null,
    last_name varchar(100) not null,
    first_name varchar(100) not null,
    middle_name varchar(100),
    birth_date date not null,
    diagnosis text not null,
    valve_name varchar(255) not null,
    valve_size varchar(100) not null,
    valve_material varchar(100) not null,
    operation_anesthesia varchar(255) not null,
    operation_duration_minutes integer not null,
    operation_delivery_system varchar(255) not null,
    medications text not null,
    created_at timestamp without time zone not null,
    constraint fk_patient_profiles_user
        foreign key (user_id) references users(id)
        on delete cascade,
    constraint fk_patient_profiles_region
        foreign key (region_id) references regions(id)
        on delete restrict,
    constraint chk_patient_profiles_operation_duration
        check (operation_duration_minutes > 0)
);
