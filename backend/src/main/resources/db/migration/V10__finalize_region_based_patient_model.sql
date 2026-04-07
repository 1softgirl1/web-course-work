create index idx_patient_profiles_region_id
    on patient_profiles (region_id);

alter table patient_profiles
    alter column created_at drop default;

comment on column users.login is 'User login. For patients this value matches patient_profiles.patient_code by application rule.';

comment on column patient_profiles.patient_code is 'Anonymized patient code. Backend keeps it equal to users.login for patient accounts.';
