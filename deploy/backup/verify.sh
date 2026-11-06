#!/bin/sh
# Re-check ciphertext checksums; each configured destination must contain a valid copy.
# No private key is needed. Destination failures are independent and always make the run fail.
set -eu
umask 077
BACKUP_DIR=${BACKUP_DIR:-/backups}
bad=0
checked=0
WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

local_valid=0
for f in "$BACKUP_DIR"/daily/*.tar.age "$BACKUP_DIR"/weekly/*.tar.age "$BACKUP_DIR"/monthly/*.tar.age; do
    [ -f "$f" ] || continue
    checked=$((checked + 1))
    expected=$(cat "$f.md5" 2>/dev/null || true)
    if [ -s "$f" ] && actual=$(md5sum "$f") && [ "${actual%% *}" = "$expected" ]; then
        echo "ok local ${f#"$BACKUP_DIR"/}"
        local_valid=$((local_valid + 1))
    else
        echo "BAD local ${f#"$BACKUP_DIR"/}"; bad=$((bad + 1))
    fi
done
for sum in "$BACKUP_DIR"/daily/*.tar.age.md5 "$BACKUP_DIR"/weekly/*.tar.age.md5 "$BACKUP_DIR"/monthly/*.tar.age.md5; do
    [ -f "$sum" ] || continue
    if [ ! -f "${sum%.md5}" ]; then echo "BAD local ${sum#"$BACKUP_DIR"/} (archive missing)"; bad=$((bad + 1)); fi
done
if [ "$local_valid" -eq 0 ]; then echo 'BAD local no valid copies'; bad=$((bad + 1)); fi

if [ -n "${S3_ENDPOINT:-}" ]; then
    export AWS_ACCESS_KEY_ID="${S3_ACCESS_KEY:-}" AWS_SECRET_ACCESS_KEY="${S3_SECRET_KEY:-}" AWS_DEFAULT_REGION="${S3_REGION:-ru-central1}"
    cloud_valid=0
    if keys=$(aws --endpoint-url "$S3_ENDPOINT" s3api list-objects-v2 --bucket "${S3_BUCKET:?}" \
        --query 'Contents[].Key' --output text); then
        for key in $keys; do
            case "$key" in daily/*.tar.age|weekly/*.tar.age|monthly/*.tar.age) ;; *) continue ;; esac
            file=${key#*/}
            case "$file" in ''|.*|*[!A-Za-z0-9_.-]*) echo 'BAD cloud invalid archive key'; bad=$((bad + 1)); continue ;; esac
            checked=$((checked + 1))
            # Never put aws before tr in a pipeline: even a usable-looking result can accompany an error.
            if ! head=$(aws --endpoint-url "$S3_ENDPOINT" s3api head-object --bucket "$S3_BUCKET" --key "$key" \
                --query ETag --output text); then
                echo "BAD cloud $key (head failed)"; bad=$((bad + 1)); continue
            fi
            etag=$(printf '%s' "$head" | tr -d '"')
            if ! aws --endpoint-url "$S3_ENDPOINT" s3 cp --quiet "s3://$S3_BUCKET/$key" "$WORK/cloud"; then
                echo "BAD cloud $key (download failed)"; bad=$((bad + 1)); continue
            fi
            local_md5=$(cat "$BACKUP_DIR/$key.md5" 2>/dev/null || true)
            expected=${local_md5:-$etag}
            if [ -s "$WORK/cloud" ] && actual=$(md5sum "$WORK/cloud") && [ "${actual%% *}" = "$expected" ]; then
                echo "ok cloud $key"; cloud_valid=$((cloud_valid + 1))
            else
                echo "BAD cloud $key (checksum mismatch)"; bad=$((bad + 1))
            fi
        done
    else
        echo 'BAD cloud listing failed'; bad=$((bad + 1))
    fi
    if [ "$cloud_valid" -eq 0 ]; then echo 'BAD cloud no valid copies'; bad=$((bad + 1)); fi
fi

if [ "${NFS_ENABLED:-false}" = true ]; then
    if timeout -s KILL "${NFS_TIMEOUT_SECONDS:-120}" network-storage verify > "$WORK/network" 2>&1; then
        network_failed=0
    else
        network_failed=1
    fi
    cat "$WORK/network"
    network_checked=$(awk '/^checked=[0-9]+ bad=[0-9]+$/ { split($1,a,"="); n=a[2] } END { print n+0 }' "$WORK/network")
    network_bad=$(awk '/^BAD NFS / { n++ } END { print n+0 }' "$WORK/network")
    if [ "$network_failed" -ne 0 ] && [ "$network_bad" -eq 0 ]; then
        echo 'BAD NFS unavailable or check timed out'; network_bad=1
    elif [ "$network_checked" -eq 0 ] && [ "$network_bad" -eq 0 ]; then
        echo 'BAD NFS no valid copies'; network_bad=1
    fi
    checked=$((checked + network_checked))
    bad=$((bad + network_bad))
fi

echo "checked=$checked bad=$bad"
[ "$bad" -eq 0 ]
