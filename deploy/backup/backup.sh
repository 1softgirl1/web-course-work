#!/bin/sh
# One 3-2-1-1-0 backup run of the registry (started by the backup service on schedule or by hand):
#   dump -> restore check in a throwaway Postgres ("0 errors") -> tar with project configs
#   -> encrypt with the public age key -> local copies with rotation (copy 2)
#   -> independent S3 and NFS copies -> checksum checks -> status.json.
# The private key never lives on the server: the server can encrypt, not decrypt.
# Exit code != 0 means failure; status.json says at which step. Notifications are the service's job.
set -eu
umask 077

: "${PGHOST:?}" "${PGUSER:?}" "${PGPASSWORD:?}" "${PGDATABASE:?}" "${AGE_RECIPIENT:?}"
BACKUP_DIR=${BACKUP_DIR:-/backups}
CONFIG_DIR=${CONFIG_DIR:-/config}
KEEP_DAILY=${KEEP_DAILY:-7}
KEEP_WEEKLY=${KEEP_WEEKLY:-4}
KEEP_MONTHLY=${KEEP_MONTHLY:-12}
WEEKLY_DAY=${WEEKLY_DAY:-7}    # 1 = Monday ... 7 = Sunday
MONTHLY_DAY=${MONTHLY_DAY:-1}  # day of month
NAME=${BACKUP_NAME:-registry}
case "$NAME" in ''|.*|*[!A-Za-z0-9_.-]*) echo 'invalid backup name' >&2; exit 1 ;; esac

STAMP=$(date +%F_%H%M%S)
FILE="$NAME-$STAMP.tar.age"
WORK=$(mktemp -d)
STEP=start
INCOMING=

status() {
    printf '{"time":"%s","result":"%s","step":"%s","file":"%s","details":"%s"}\n' \
        "$(date -Iseconds)" "$1" "$STEP" "$FILE" "$2" > "$BACKUP_DIR/status.json"
}

finish() {
    code=$?
    [ -n "${PGTEST:-}" ] && su postgres -s /bin/sh -c "pg_ctl -D '$WORK/pg' -m fast stop" >/dev/null 2>&1 || true
    rm -rf "$WORK"
    [ -z "$INCOMING" ] || rm -rf "$INCOMING"
    if [ "$code" -ne 0 ]; then
        echo "$(date -Iseconds) BACKUP FAILED at step: $STEP" >&2
        status failed "exit $code"
    fi
}
trap finish EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

log() { echo "$(date -Iseconds) $*"; }
mkdir -p "$BACKUP_DIR/daily" "$BACKUP_DIR/weekly" "$BACKUP_DIR/monthly"
status running ""

STEP=dump
log "dump $PGDATABASE@$PGHOST"
mkdir "$WORK/archive"
pg_dump -Fc -f "$WORK/archive/db.dump"

STEP=restore-check
log "restore check in a temporary cluster"
chown -R postgres "$WORK"
su postgres -s /bin/sh -c "initdb -D '$WORK/pg' -U postgres --auth=trust --no-locale -E UTF8 >/dev/null"
PGTEST=1
su postgres -s /bin/sh -c "pg_ctl -D '$WORK/pg' -o \"-c listen_addresses='' -k '$WORK'\" -w start >/dev/null"
PGHOST="$WORK" PGUSER=postgres createdb restore_check
PGHOST="$WORK" PGUSER=postgres pg_restore --no-owner --no-acl --exit-on-error -d restore_check "$WORK/archive/db.dump"
CHECK=$(PGHOST="$WORK" PGUSER=postgres psql -d restore_check -Atc \
    "select 'patients=' || (select count(*) from patient_profiles)
         || ' examinations=' || (select count(*) from examinations)
         || ' flyway=' || (select max(version::int) from flyway_schema_history where success and version ~ '^[0-9]+$')")
SOURCE=$(psql -Atc "select 'patients=' || count(*) from patient_profiles")
log "restored: $CHECK (source now: $SOURCE)"
case "$CHECK" in "$SOURCE "*) ;; *) log "WARN: patient count differs from the live database (changed during the night?)";; esac
su postgres -s /bin/sh -c "pg_ctl -D '$WORK/pg' -m fast -w stop" >/dev/null
PGTEST=
chown "$(id -u)" "$WORK"

STEP=configs
# .env, compose and nginx configs: not in git (or edited on the server), needed to rebuild a lost server.
if [ -d "$CONFIG_DIR" ]; then
    cp -R "$CONFIG_DIR" "$WORK/archive/config"
    log "configs: $(find "$WORK/archive/config" -type f | wc -l) files"
else
    log "WARN: $CONFIG_DIR not mounted, configs skipped"
fi

STEP=encrypt
# Separate producer and consumer so /bin/sh cannot hide tar's failure behind age's exit code.
# This needs space for one additional plaintext tar until encryption finishes.
tar -C "$WORK/archive" -cf "$WORK/archive.tar" .
INCOMING=$(mktemp -d "$BACKUP_DIR/.incoming.XXXXXX")
age -r "$AGE_RECIPIENT" -o "$INCOMING/$FILE" "$WORK/archive.tar"
MD5=$(md5sum "$INCOMING/$FILE")
MD5=${MD5%% *}
echo "$MD5" > "$INCOMING/$FILE.md5"
TIERS=
copy_tier() {
    tier=$1
    # Publish the checksum first and the completed archive last (the listing commit marker).
    # Hard links are atomic and refuse to overwrite an existing successful backup.
    ln "$INCOMING/$FILE.md5" "$BACKUP_DIR/$tier/$FILE.md5"
    if ! ln "$INCOMING/$FILE" "$BACKUP_DIR/$tier/$FILE"; then
        rm -f "$BACKUP_DIR/$tier/$FILE.md5"
        return 1
    fi
    TIERS="${TIERS:+$TIERS }$tier"
}
copy_tier daily
[ "$KEEP_WEEKLY" -gt 0 ] && [ "$(date +%u)" = "$WEEKLY_DAY" ] && copy_tier weekly
[ "$KEEP_MONTHLY" -gt 0 ] && [ "$(date +%d | sed 's/^0//')" = "$MONTHLY_DAY" ] && copy_tier monthly
SIZE=$(wc -c < "$BACKUP_DIR/daily/$FILE")
log "encrypted $FILE ($SIZE bytes, md5 $MD5), tiers: $TIERS"

STEP=rotate
# Finish every listing and sort before any deletion; partial output is not a usable inventory.
for tier in daily weekly monthly; do
    find "$BACKUP_DIR/$tier" -maxdepth 1 -type f -name '*.tar.age' > "$WORK/$tier.files"
    sort -r "$WORK/$tier.files" > "$WORK/$tier.sorted"
done
keep() {
    count=0
    while IFS= read -r old; do
        expected=$(cat "$old.md5" 2>/dev/null) || continue
        actual=$(md5sum "$old") || continue
        [ "${actual%% *}" = "$expected" ] || continue
        count=$((count + 1))
        [ "$count" -le "$2" ] || rm -f "$old" "$old.md5"
    done < "$WORK/$1.sorted"
}
keep daily "$KEEP_DAILY"; keep weekly "$KEEP_WEEKLY"; keep monthly "$KEEP_MONTHLY"

cloud_result=skipped
network_result=skipped
# Keep destinations independent: a failed cloud upload must not prevent the NFS copy (and vice versa).
set +e
(
    set -e
    if [ -n "${S3_ENDPOINT:-}" ]; then
    export AWS_ACCESS_KEY_ID="$S3_ACCESS_KEY" AWS_SECRET_ACCESS_KEY="$S3_SECRET_KEY" AWS_DEFAULT_REGION="${S3_REGION:-ru-central1}"
    MD5_B64=$(echo "$MD5" | xxd -r -p | base64)
    for tier in $TIERS; do
        case $tier in
            daily) days=$KEEP_DAILY ;;
            weekly) days=$((KEEP_WEEKLY * 7)) ;;
            monthly) days=$((KEEP_MONTHLY * 31)) ;;
        esac
        until=$(date -u -d "@$(($(date +%s) + days * 86400))" +%Y-%m-%dT%H:%M:%SZ)
        # Content-MD5 makes the storage reject a corrupted upload; the compliance lock means nobody,
        # including the bucket owner, can delete or overwrite this version before $until.
        aws --endpoint-url "$S3_ENDPOINT" s3api put-object --bucket "$S3_BUCKET" --key "$tier/$FILE" \
            --body "$BACKUP_DIR/$tier/$FILE" --content-md5 "$MD5_B64" \
            --object-lock-mode COMPLIANCE --object-lock-retain-until-date "$until" >/dev/null
        echo "upload-verify $tier"
        HEAD=$(aws --endpoint-url "$S3_ENDPOINT" s3api head-object --bucket "$S3_BUCKET" --key "$tier/$FILE" \
            --query '[ETag, ObjectLockMode]' --output text)
        case "$HEAD" in *"$MD5"*COMPLIANCE*) ;; *) log "uploaded object check failed: '$HEAD', expected md5 $MD5"; exit 1;; esac
        log "uploaded $tier/$FILE, locked until $until, checksum ok"
    done
    else
        log "WARN: S3_ENDPOINT not set, offsite copy skipped"
    fi
)
cloud_exit=$?
set -e
if [ -n "${S3_ENDPOINT:-}" ]; then
    if [ "$cloud_exit" -eq 0 ]; then cloud_result=ok; else cloud_result=failed; fi
fi

if [ "${NFS_ENABLED:-false}" = true ]; then
    STEP=network-copy
    if timeout -s KILL "${NFS_TIMEOUT_SECONDS:-120}" network-storage store "$FILE" "$TIERS"; then
        network_result=ok
    else
        network_result=failed
        log "NFS copy failed; local archive and cloud result are preserved"
    fi
fi

STEP=destinations
DETAILS="$CHECK; size=$SIZE; tiers=$TIERS; local=ok cloud=$cloud_result network=$network_result"
if [ "$cloud_result" = failed ] || [ "$network_result" = failed ]; then
    status failed "$DETAILS"
    # Preserve the destination report instead of overwriting it in the EXIT trap.
    trap - EXIT
    finish
    exit 1
fi
STEP=done
status ok "$DETAILS"
log "backup ok"
