create table characteristics (
    id bigserial primary key,
    name varchar(255) not null,
    constraint uq_characteristics_name unique (name)
);
