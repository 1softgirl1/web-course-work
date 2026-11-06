#!/bin/sh
# HOST only: switch to a managed, usable restored/displaced database and keep the old live database.
# Both renames and provenance change commit together; a SQL error leaves the old names unchanged.
set -eu

NEW=${1:?usage: switch-db.sh <restored_db>}
PG=${PG_CONTAINER:-registry-postgres}
APP=${APP_CONTAINER:-registry-backend}
DB=${DB_NAME:-$(docker exec "$PG" printenv POSTGRES_DB)}
DBUSER=${DB_USER:-$(docker exec "$PG" printenv POSTGRES_USER)}
valid_name() {
    case "$1" in ''|[0-9]*|*[!A-Za-z0-9_]*|postgres|template*) echo 'недопустимое имя базы' >&2; exit 1 ;; esac
    [ "${#1}" -le 63 ] || { echo 'слишком длинное имя базы' >&2; exit 1; }
}
# The configured live name may contain '-' (production uses registry-db); psql quotes it.
case "$DB" in ''|*[!A-Za-z0-9_-]*|postgres|template*) echo 'недопустимое имя рабочей базы' >&2; exit 1 ;; esac
[ "${#DB}" -le 63 ] || { echo 'слишком длинное имя рабочей базы' >&2; exit 1; }
valid_name "$NEW"
[ "$NEW" != "$DB" ] || { echo 'эта база уже рабочая' >&2; exit 1; }
PREFIX=$(printf '%.24s' "$DB" | tr -c 'A-Za-z0-9_' '_')
OLD=$(printf '%s_broken_%s_%s' "$PREFIX" "$(date +%Y%m%d%H%M%S)" "$$")
sql() {
    docker exec -i "$PG" psql -X -U "$DBUSER" -d postgres -v ON_ERROR_STOP=1 \
        -v live="$DB" -v candidate="$NEW" -v old="$OLD" -At
}
ELIGIBLE=$(sql <<'SQL'
SELECT count(*) FROM pg_database
WHERE datname = :'candidate' AND NOT datistemplate AND datallowconn
  AND shobj_description(oid, 'pg_database') IN ('registry-backup:restored', 'registry-backup:displaced');
SQL
)
[ "$ELIGIBLE" = 1 ] || { echo 'целевая база не является управляемой восстановленной/отложенной базой' >&2; exit 1; }
# Assign separately: an error in this SQL must abort BEFORE docker stop, not be hidden by echo.
CHECK=$(docker exec "$PG" psql -X -U "$DBUSER" -d "$NEW" -v ON_ERROR_STOP=1 -Atc \
    "select 'patients=' || (select count(*) from patient_profiles)
        || ' examinations=' || (select count(*) from examinations)
        || ' flyway=' || (select max(version::int) from flyway_schema_history where success and version ~ '^[0-9]+$')")
case "$CHECK" in *' flyway='[0-9]*) ;; *) echo 'в целевой базе нет пригодной схемы регистра' >&2; exit 1 ;; esac
printf 'в %s: %s\n' "$NEW" "$CHECK"

STOPPED=false
finish() {
    code=$?
    trap - EXIT
    if [ "$STOPPED" = true ]; then docker start "$APP" >/dev/null || code=1; fi
    exit "$code"
}
trap finish EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
echo "останавливаю $APP"
docker stop "$APP" >/dev/null
STOPPED=true
sql <<'SQL'
BEGIN;
SELECT pg_terminate_backend(pid) FROM pg_stat_activity
WHERE datname IN (:'live', :'candidate') AND pid <> pg_backend_pid();
COMMENT ON DATABASE :"live" IS 'registry-backup:displaced';
ALTER DATABASE :"live" RENAME TO :"old";
ALTER DATABASE :"candidate" RENAME TO :"live";
COMMIT;
SQL
docker start "$APP" >/dev/null
STOPPED=false
printf 'готово: %s теперь из %s; старая база сохранена как %s\n' "$DB" "$NEW" "$OLD"
echo 'Для возврата выберите сохранённую базу тем же switch-db.sh; для удаления используйте меню.'
