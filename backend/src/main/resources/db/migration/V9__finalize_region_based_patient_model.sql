create index idx_patient_profiles_region_id
    on patient_profiles (region_id);

comment on column users.username is 'Username used for authentication. For doctors this stores email; for patients this stores the anonymized patient code.';
