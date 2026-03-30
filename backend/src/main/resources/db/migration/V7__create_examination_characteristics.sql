create table examination_characteristics (
    examination_id bigint not null,
    characteristic_id bigint not null,
    value numeric(12, 2) not null,
    unit varchar(50),
    comment text,
    primary key (examination_id, characteristic_id),
    constraint fk_exam_characteristics_exam
        foreign key (examination_id) references examinations(id)
        on delete cascade,
    constraint fk_exam_characteristics_characteristic
        foreign key (characteristic_id) references characteristics(id)
        on delete restrict
);
