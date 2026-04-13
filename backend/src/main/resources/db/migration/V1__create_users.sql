create table users (
    id bigserial primary key,
    login varchar(255) not null unique,
    password_hash varchar(255) not null,
    role varchar(50) not null,
    status varchar(50) not null
);
