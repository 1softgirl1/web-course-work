create table patient_profiles (
    id bigserial primary key,
    user_id bigint not null unique,
    last_name varchar(100) not null,
    first_name varchar(100) not null,
    middle_name varchar(100),
    birth_date date not null,
    phone varchar(50) not null,
    region_id bigint not null,
    constraint fk_patient_profiles_user
        foreign key (user_id) references users(id)
        on delete cascade,
    constraint fk_patient_profiles_region
        foreign key (region_id) references regions(id)
        on delete restrict
);
