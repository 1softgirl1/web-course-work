#!/bin/sh
# Additional encrypted copies on the host's NFS mount. Never substitutes a local directory for NFS.
# All remote I/O runs in this child process, not in the JVM. Callers impose a deadline.
set -eu
umask 077
ROOT=${NFS_ROOT:-/backup-storage}
DEST="$ROOT/registry-backups"
BACKUP_DIR=${BACKUP_DIR:-/backups}
WORK=
trap '[ -z "$WORK" ] || rm -rf "$WORK"' EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

fail() { echo "$*" >&2; exit 1; }
require_mount() {
    [ "${NFS_ENABLED:-false}" = true ] || fail 'NFS is not configured'
    # A bind of /opt1 retains its filesystem type. No remote I/O before this check.
    # Deliberately reject ext4/overlay/tmpfs: /opt1 may exist even when the NFS mount is absent.
    awk -v root="$ROOT" '
        $5 == root { for (i = 6; i <= NF; i++) if ($i == "-" && ($(i+1) == "nfs" || $(i+1) == "nfs4")) found = 1 }
        END { exit !found }
    ' /proc/self/mountinfo || fail 'NFS is not mounted; refusing to use a local directory'
}
valid_file() {
    case "$1" in ''|*[!A-Za-z0-9_.-]*|.*) fail 'Invalid archive name' ;; esac
    case "$1" in *.tar.age) ;; *) fail 'Expected .tar.age archive' ;; esac
}
valid_key() {
    case "$1" in daily/*|weekly/*|monthly/*) valid_file "${1#*/}" ;; *) fail 'Invalid copy tier' ;; esac
}
checksum() {
    [ -s "$1" ] || return 1
    expected=$(cat "$1.md5") || return 1
    [ ${#expected} -eq 32 ] || return 1
    case "$expected" in *[!a-f0-9]*) return 1 ;; esac
    actual=$(md5sum "$1") || return 1
    [ "${actual%% *}" = "$expected" ]
}

require_mount
case "${1:-}" in
    check)
        # Check both write and read access, including NFS root-squash permissions.
        mkdir -p "$DEST"
        WORK=$(mktemp -d "$DEST/.probe.XXXXXX")
        printf 'probe\n' > "$WORK/write"
        [ "$(cat "$WORK/write")" = probe ] || fail 'NFS read/write check failed'
        echo 'NFS available for reading and writing'
        ;;
    store)
        file=${2:?archive name required}
        valid_file "$file"
        for tier in ${3:?tiers required}; do
            case "$tier" in daily|weekly|monthly) ;; *) fail 'Invalid copy tier' ;; esac
            mkdir -p "$DEST/$tier"
            WORK=$(mktemp -d "$DEST/$tier/.incoming.XXXXXX")
            cp "$BACKUP_DIR/$tier/$file" "$BACKUP_DIR/$tier/$file.md5" "$WORK/"
            checksum "$WORK/$file" || fail "Checksum mismatch: $tier/$file"
            # The archive name is the commit marker: listings never include partially written files.
            if [ -e "$DEST/$tier/$file" ]; then
                checksum "$DEST/$tier/$file" && cmp -s "$WORK/$file.md5" "$DEST/$tier/$file.md5" || fail 'Existing copy differs'
            else
                mv "$WORK/$file.md5" "$DEST/$tier/$file.md5"
                mv "$WORK/$file" "$DEST/$tier/$file"
            fi
            rm -rf "$WORK"; WORK=
            echo "stored NFS $tier/$file, checksum ok"
        done
        # Acquire complete inventories for every copied tier before deleting any old archive.
        WORK=$(mktemp -d)
        for tier in $3; do
            find "$DEST/$tier" -maxdepth 1 -type f -name '*.tar.age' > "$WORK/$tier.files"
            sort -r "$WORK/$tier.files" > "$WORK/$tier.sorted"
        done
        for tier in $3; do
            case "$tier" in
                daily) keep=${KEEP_DAILY:-7} ;;
                weekly) keep=${KEEP_WEEKLY:-4} ;;
                monthly) keep=${KEEP_MONTHLY:-12} ;;
            esac
            count=0
            while IFS= read -r old; do
                checksum "$old" || continue
                count=$((count + 1))
                [ "$count" -le "$keep" ] || rm -f "$old" "$old.md5"
            done < "$WORK/$tier.sorted"
        done
        ;;
    list)
        for tier in daily weekly monthly; do
            for f in "$DEST/$tier"/*.tar.age; do
                [ -f "$f" ] && [ -f "$f.md5" ] || continue
                printf '%s\t%s\t%s\n' "$tier/${f##*/}" "$(stat -c %s "$f")" "$(stat -c %Y "$f")"
            done
        done
        ;;
    get)
        key=${2:?copy key required}
        valid_key "$key"
        WORK=$(mktemp -d)
        cp "$DEST/$key" "$WORK/${key##*/}"
        cp "$DEST/$key.md5" "$WORK/${key##*/}.md5"
        checksum "$WORK/${key##*/}" || fail 'NFS copy checksum mismatch'
        # Return a local verified snapshot so restoring does not depend on a later NFS disconnect.
        result=$(mktemp /tmp/nfs-restore.XXXXXX)
        mv "$WORK/${key##*/}" "$result"
        echo "$result"
        ;;
    verify)
        checked=0; bad=0; valid=0
        for tier in daily weekly monthly; do
            for f in "$DEST/$tier"/*.tar.age; do
                [ -f "$f" ] || continue
                checked=$((checked + 1))
                if checksum "$f"; then echo "ok NFS $tier/${f##*/}"; valid=$((valid + 1))
                else echo "BAD NFS $tier/${f##*/}"; bad=$((bad + 1)); fi
            done
            for sum in "$DEST/$tier"/*.tar.age.md5; do
                [ -f "$sum" ] || continue
                if [ ! -f "${sum%.md5}" ]; then echo "BAD NFS $tier/${sum##*/} (archive missing)"; bad=$((bad + 1)); fi
            done
        done
        if [ "$valid" -eq 0 ]; then echo 'BAD NFS no valid copies'; bad=$((bad + 1)); fi
        echo "checked=$checked bad=$bad"
        [ "$bad" -eq 0 ]
        ;;
    *) fail 'Usage: network-storage check|store <file> <tiers>|list|get <tier/file>|verify' ;;
esac
