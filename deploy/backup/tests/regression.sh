#!/bin/sh
# Run as root INSIDE the backup image, with this source tree mounted read-only.
# Uses only a new local Unix-socket Postgres cluster; never connects to external databases/storage.
set -eu
umask 077
SOURCE=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
W=$(mktemp -d)
finish() {
    code=$?
    su postgres -s /bin/sh -c "pg_ctl -D '$W/pg' -m immediate stop" >/dev/null 2>&1 || true
    rm -rf "$W"
    exit "$code"
}
trap finish EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
fail() { echo "FAIL: $*" >&2; exit 1; }
[ "$(id -u)" = 0 ] || fail 'run inside the backup image as root'
chown postgres "$W"
su postgres -s /bin/sh -c "initdb -D '$W/pg' -U postgres --auth=trust --no-locale -E UTF8 >/dev/null; pg_ctl -D '$W/pg' -o \"-c listen_addresses='' -k '$W'\" -w start >/dev/null"
export PGHOST="$W" PGUSER=postgres PGPASSWORD=fixture PGDATABASE=live
export BACKUP_DIR="$W/backups" CONFIG_DIR="$W/config" KEEP_DAILY=2 KEEP_WEEKLY=0 KEEP_MONTHLY=0
export S3_ENDPOINT= NFS_ENABLED=false
mkdir "$W/bin" "$W/fault" "$CONFIG_DIR"
printf 'disposable configuration\n' > "$CONFIG_DIR/fixture.txt"
for command in backup restore verify network-storage; do
    printf '#!/bin/sh\nexec sh "%s/%s.sh" "$@"\n' "$SOURCE" "$command" > "$W/bin/$command"
done
chmod +x "$W/bin/"*
export PATH="$W/bin:$PATH"
createdb live
psql -v ON_ERROR_STOP=1 <<'SQL'
CREATE TABLE patient_profiles(id integer PRIMARY KEY);
INSERT INTO patient_profiles VALUES(1),(2);
CREATE TABLE examinations(id integer PRIMARY KEY, exam_date date);
INSERT INTO examinations VALUES(1,current_date);
CREATE TABLE flyway_schema_history(version varchar, success boolean);
INSERT INTO flyway_schema_history VALUES('1',true);
SQL
KEY=$(age-keygen 2>/dev/null)
export AGE_RECIPIENT=$(printf '%s\n' "$KEY" | age-keygen -y)
backup
GOOD=$(find "$BACKUP_DIR/daily" -name '*.tar.age')
[ -s "$GOOD" ] && [ -s "$GOOD.md5" ] || fail 'successful backup not committed'
[ "$(stat -c %a "$GOOD")" = 600 ] || fail 'ciphertext permissions'
verify

cat > "$W/fault/tar" <<'SH'
#!/bin/sh
kill -KILL $$
SH
chmod +x "$W/fault/tar"
if PATH="$W/fault:$PATH" BACKUP_NAME=producer-failure backup; then fail 'tar SIGKILL was ignored'; fi
[ -z "$(find "$BACKUP_DIR/daily" -name 'producer-failure*')" ] || fail 'failed producer published a file'
rm "$W/fault/tar"
cat > "$W/fault/age" <<'SH'
#!/bin/sh
/usr/bin/age "$@" || exit $?
kill -KILL $$
SH
chmod +x "$W/fault/age"
if PATH="$W/fault:$PATH" BACKUP_NAME=encrypt-failure backup; then fail 'age SIGKILL was ignored'; fi
[ -z "$(find "$BACKUP_DIR/daily" -name 'encrypt-failure*')" ] || fail 'failed encryptor published a file'
rm "$W/fault/age"
# A stale partial final archive must not evict a valid pair during later rotation.
printf 'partial ciphertext\n' > "$BACKUP_DIR/daily/zzzz-incomplete.tar.age"
backup
[ -s "$GOOD" ] && [ -s "$GOOD.md5" ] || fail 'incomplete file displaced a valid archive'
rm "$BACKUP_DIR/daily/zzzz-incomplete.tar.age"
# Enumeration/sort can emit apparently usable output AND fail; neither may trigger deletions.
for tool in find sort; do
    printf '#!/bin/sh\n/usr/bin/%s "$@"\nexit 42\n' "$tool" > "$W/fault/$tool"
    chmod +x "$W/fault/$tool"
    if PATH="$W/fault:$PATH" KEEP_DAILY=0 BACKUP_NAME="rotation-$tool" backup; then fail "rotation $tool failure ignored"; fi
    grep -q '"step":"rotate"' "$BACKUP_DIR/status.json" || fail "rotation $tool did not reach the injected failure"
    [ -s "$GOOD" ] && [ -s "$GOOD.md5" ] || fail "rotation $tool failure deleted a valid archive"
    rm "$W/fault/$tool"
done

# Real decryption succeeds, then its process fails: no database may be created.
cat > "$W/fault/age" <<'SH'
#!/bin/sh
/usr/bin/age "$@" || exit $?
exit 43
SH
chmod +x "$W/fault/age"
if printf '%s\n' "$KEY" | PATH="$W/fault:$PATH" restore "$GOOD" late_age; then fail 'late age failure was ignored'; fi
[ "$(psql -d postgres -Atc "select count(*) from pg_database where datname='late_age'")" = 0 ] || fail 'late age failure created a database'
rm "$W/fault/age"
printf '%s\n' "$KEY" | restore "$GOOD" restored_ok
[ "$(psql -d postgres -Atc "select shobj_description(oid,'pg_database') from pg_database where datname='restored_ok'")" = registry-backup:restored ] || fail 'missing restored provenance'
if printf '%s\n' "$KEY" | restore "$GOOD" restored_ok; then fail 'existing database was overwritten'; fi
[ "$(psql -d restored_ok -Atc 'select count(*) from patient_profiles')" = 2 ] || fail 'existing database was deleted/changed'
# Production's live name can contain a hyphen; the generated target must still be a safe identifier.
createdb --template=live -- registry-db
printf '%s\n' "$KEY" | PGDATABASE=registry-db restore "$GOOD"
DEFAULT_TARGET=$(psql -d postgres -Atc "select datname from pg_database where datname ~ '^restored_[0-9]{14}$' and shobj_description(oid,'pg_database')='registry-backup:restored'")
[ -n "$DEFAULT_TARGET" ] || fail 'default restore target inherited an unsafe live name'
[ "$(psql -d "$DEFAULT_TARGET" -Atc 'select count(*) from patient_profiles')" = 2 ] || fail 'default restore incomplete'
if printf '%s\n' "$KEY" | restore "$GOOD" bad-target; then fail 'explicit unsafe target accepted'; fi
printf '%s\n' "$KEY" | restore --configs "$GOOD" > "$W/configs.tar"
tar -tf "$W/configs.tar" | grep -q fixture.txt || fail 'config export missing'

# Genuine pg_restore COPY error AFTER earlier tables: the failed target must disappear.
psql -v ON_ERROR_STOP=1 <<'SQL'
CREATE FUNCTION fixture_source_only() RETURNS boolean LANGUAGE sql AS $$SELECT current_database()='live'$$;
CREATE TABLE zzz_restore_failure(id integer CHECK(fixture_source_only()));
INSERT INTO zzz_restore_failure VALUES(1);
SQL
mkdir "$W/archive"
pg_dump -Fc -f "$W/archive/db.dump"
tar -C "$W/archive" -cf "$W/partial.tar" .
age -r "$AGE_RECIPIENT" -o "$W/partial.tar.age" "$W/partial.tar"
if printf '%s\n' "$KEY" | restore "$W/partial.tar.age" partial_restore; then fail 'pg_restore failure ignored'; fi
[ "$(psql -d postgres -Atc "select count(*) from pg_database where datname='partial_restore'")" = 0 ] || fail 'partial restored database retained'
[ "$(psql -Atc 'select count(*) from patient_profiles')" = 2 ] || fail 'live database changed by restore'

# Exercise real psql and database renames. Only the app lifecycle boundary is recorded locally.
cat > "$W/bin/docker" <<'SH'
#!/bin/sh
case "$1" in
 exec) shift; [ "$1" != -i ] || shift; shift; exec "$@" ;;
 stop|start) printf '%s %s\n' "$1" "$2" >> "$DOCKER_EVENTS" ;;
 *) exit 90 ;;
esac
SH
chmod +x "$W/bin/docker"
export DB_NAME=live DB_USER=postgres PG_CONTAINER=fixture APP_CONTAINER=fixture-app DOCKER_EVENTS="$W/events"
createdb empty_candidate
psql -d postgres -c "comment on database empty_candidate is 'registry-backup:restored'"
if sh "$SOURCE/switch-db.sh" empty_candidate; then fail 'empty database selected'; fi
[ ! -e "$DOCKER_EVENTS" ] || fail 'backend stopped for unusable candidate'
createdb foreign_candidate --template=restored_ok
if sh "$SOURCE/switch-db.sh" foreign_candidate; then fail 'unmanaged database selected'; fi
if sh "$SOURCE/switch-db.sh" postgres; then fail 'system database selected'; fi
if sh "$SOURCE/switch-db.sh" "bad';select 1;--"; then fail 'injected name accepted'; fi
[ ! -e "$DOCKER_EVENTS" ] || fail 'backend stopped for rejected candidate'
sh "$SOURCE/switch-db.sh" restored_ok
OLD=$(psql -d postgres -Atc "select datname from pg_database where datname <> 'live' and shobj_description(oid,'pg_database')='registry-backup:displaced'")
[ -n "$OLD" ] || fail 'old live not retained as displaced'
sh "$SOURCE/switch-db.sh" "$OLD"
[ "$(psql -Atc 'select count(*) from zzz_restore_failure')" = 1 ] || fail 'reverse switch did not return old live'
[ "$(wc -l < "$DOCKER_EVENTS" | tr -d ' ')" = 4 ] || fail 'backend lifecycle not preserved'
DB_NAME=registry-db sh "$SOURCE/switch-db.sh" "$DEFAULT_TARGET"
HYPHEN_OLD=$(psql -d postgres -Atc "select datname from pg_database where datname like 'registry_db_broken_%' and shobj_description(oid,'pg_database')='registry-backup:displaced'")
[ -n "$HYPHEN_OLD" ] || fail 'hyphenated live name was not safely displaced'
DB_NAME=registry-db sh "$SOURCE/switch-db.sh" "$HYPHEN_OLD"
[ "$(psql -d registry-db -Atc 'select count(*) from patient_profiles')" = 2 ] || fail 'hyphenated live reverse switch failed'

# Real files/digests plus an AWS boundary double that can output an ETag and STILL fail.
cat > "$W/bin/aws" <<'SH'
#!/bin/sh
case "$*" in
 *list-objects-v2*)
   case "$AWS_FIXTURE_MODE" in
     list-failure) exit 44 ;;
     empty) echo None ;;
     *) echo "$AWS_FIXTURE_KEY" ;;
   esac ;;
 *head-object*) cat "$AWS_FIXTURE_SOURCE.md5"; [ "$AWS_FIXTURE_MODE" != head-failure ] || exit 45 ;;
 *'s3 cp'*) [ "$AWS_FIXTURE_MODE" != download-failure ] || exit 46; cp "$AWS_FIXTURE_SOURCE" "$7" ;;
 *) exit 90 ;;
esac
SH
chmod +x "$W/bin/aws"
export S3_ENDPOINT=http://fixture.invalid S3_BUCKET=fixture S3_ACCESS_KEY=fixture S3_SECRET_KEY=fixture
export AWS_FIXTURE_SOURCE="$GOOD" AWS_FIXTURE_KEY="daily/${GOOD##*/}"
for AWS_FIXTURE_MODE in empty list-failure head-failure download-failure; do
    export AWS_FIXTURE_MODE
    if verify; then fail "cloud $AWS_FIXTURE_MODE was ignored"; fi
done
export AWS_FIXTURE_MODE=ok
verify
mv "$BACKUP_DIR/daily" "$W/saved-daily"
# Cloud remains valid and downloadable, but an empty local destination must fail independently.
export AWS_FIXTURE_SOURCE="$W/saved-daily/${GOOD##*/}"
if verify; then fail 'missing local destination masked by valid cloud'; fi
mkdir "$BACKUP_DIR/daily"
export S3_ENDPOINT= NFS_ENABLED=true
# NFS's real mount guard must reject a local directory even when a local backup exists.
cp "$AWS_FIXTURE_SOURCE" "$AWS_FIXTURE_SOURCE.md5" "$BACKUP_DIR/daily/"
export NFS_ROOT="$W/not-nfs"
mkdir "$NFS_ROOT"
if verify; then fail 'unmounted NFS destination ignored'; fi
if network-storage store "${GOOD##*/}" daily; then fail 'stored backup on non-NFS'; fi
# Exercise NFS publication/rotation on fixture files; substitute only the mount-detection boundary.
cat > "$W/fault/awk" <<'SH'
#!/bin/sh
case "$*" in */proc/self/mountinfo) exit 0 ;; *) exec /usr/bin/awk "$@" ;; esac
SH
chmod +x "$W/fault/awk"
export NFS_ROOT="$W/mount-fixture"
mkdir -p "$NFS_ROOT/registry-backups/daily"
cp "$AWS_FIXTURE_SOURCE" "$NFS_ROOT/registry-backups/daily/aaa-old.tar.age"
cp "$AWS_FIXTURE_SOURCE.md5" "$NFS_ROOT/registry-backups/daily/aaa-old.tar.age.md5"
for tool in find sort; do
    printf '#!/bin/sh\n/usr/bin/%s "$@"\nexit 42\n' "$tool" > "$W/fault/$tool"
    chmod +x "$W/fault/$tool"
    if PATH="$W/fault:$PATH" KEEP_DAILY=0 network-storage store "${GOOD##*/}" daily; then fail "NFS rotation $tool failure ignored"; fi
    [ -s "$NFS_ROOT/registry-backups/daily/aaa-old.tar.age" ] || fail "NFS rotation $tool failure deleted a valid archive"
    [ -s "$NFS_ROOT/registry-backups/daily/${GOOD##*/}" ] || fail 'NFS lost the newly committed archive'
    rm "$W/fault/$tool"
done
KEY=
echo 'shell regression checks passed'
