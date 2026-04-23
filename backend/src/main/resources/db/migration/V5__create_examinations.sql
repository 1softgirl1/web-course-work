create table examinations (
    id bigserial primary key,
    patient_id bigint not null,
    title varchar(255) not null,
    exam_date date not null,
    comment text,
    constraint fk_examinations_patient
        foreign key (patient_id) references patient_profiles(id)
        on delete cascade
);
