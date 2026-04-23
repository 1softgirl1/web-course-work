create table doctor_profiles (
    id bigserial primary key,
    user_id bigint not null unique,
    specialization varchar(255) not null,
    last_name varchar(100) not null,
    first_name varchar(100) not null,
    middle_name varchar(100),
    region_id bigint not null,
    constraint fk_doctor_profiles_user
        foreign key (user_id) references users(id)
        on delete cascade,
    constraint fk_doctor_profiles_region
        foreign key (region_id) references regions(id)
        on delete restrict
);
