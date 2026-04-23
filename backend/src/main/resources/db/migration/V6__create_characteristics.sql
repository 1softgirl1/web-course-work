create table characteristics (
    id bigserial primary key,
    code varchar(50) not null,
    name varchar(255) not null,
    unit varchar(50) not null,
    constraint uq_characteristics_code
        unique (code),
    constraint uq_characteristics_name
        unique (name)
);
