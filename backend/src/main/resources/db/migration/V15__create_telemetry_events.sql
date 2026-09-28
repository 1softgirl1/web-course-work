-- Product telemetry, technical observability and audit facts. No clinical values or personal data.
create table telemetry_events (
    id bigserial primary key,
    occurred_at timestamptz not null default now(),
    event_name varchar(100) not null,
    event_group varchar(30) not null,
    source varchar(30) not null,

    actor_user_id bigint,
    actor_role varchar(30),
    actor_region_id bigint,

    request_id varchar(128),
    route_template varchar(255),

    entity_type varchar(50),
    entity_id bigint,
    patient_id bigint,
    patient_region_id bigint,

    result varchar(30),
    duration_ms integer,
    frontend_version varchar(50),
    backend_version varchar(50),

    metadata jsonb not null default '{}'::jsonb,

    constraint chk_telemetry_events_group check (event_group in ('product', 'technical', 'audit')),
    constraint chk_telemetry_events_source check (source in ('frontend', 'backend'))
);

create index idx_telemetry_events_name_time on telemetry_events (event_name, occurred_at desc);
create index idx_telemetry_events_actor_time on telemetry_events (actor_user_id, occurred_at desc);
create index idx_telemetry_events_patient_time on telemetry_events (patient_id, occurred_at desc);
create index idx_telemetry_events_group_time on telemetry_events (event_group, occurred_at desc);
