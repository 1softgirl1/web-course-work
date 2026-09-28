-- A login session survives refresh-token rotation: every rotated token keeps the session id and start time.
alter table refresh_tokens
    add column session_id uuid,
    add column session_started_at timestamptz;

update refresh_tokens
set session_id = gen_random_uuid(),
    session_started_at = created_at
where session_id is null;

alter table refresh_tokens
    alter column session_id set not null,
    alter column session_started_at set not null;

create index idx_refresh_tokens_session_id on refresh_tokens (session_id);

-- Only a hash of the session id reaches telemetry.
alter table telemetry_events
    add column session_id_hash varchar(64);

create index idx_telemetry_events_session_time on telemetry_events (session_id_hash, occurred_at desc);
