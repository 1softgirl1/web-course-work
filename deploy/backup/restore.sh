#!/bin/sh
# Restore into a NEW database, or export configs on stdout with --configs.
# The private key reaches age over stdin only; it is never saved or exported.
set -eu
umask 077

MODE=db
[ "${1:-}" = "--configs" ] && { MODE=configs; shift; }
SRC=${1:?usage: restore [--configs] <file.tar.age> [target_db]}
[ -f "$SRC" ] || { echo "no such file: $SRC" >&2; exit 1; }
TARGET=
CREATED=false
TTY_HIDDEN=false
WORK=$(mktemp -d)
finish() {
    code=$?
    KEY=
    if [ "$TTY_HIDDEN" = true ]; then stty echo || true; fi
    if [ "$CREATED" = true ]; then
        # Only the database successfully created by this invocation can be removed here.
        dropdb --if-exists --force -- "$TARGET" || { echo "не удалось удалить незавершённую базу $TARGET" >&2; code=1; }
    fi
    rm -rf "$WORK"
    trap - EXIT
    exit "$code"
}
trap finish EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

if [ "$MODE" = db ]; then
    TARGET=${2:-restored_$(date +%Y%m%d%H%M%S)}
    case "$TARGET" in ''|[0-9]*|*[!A-Za-z0-9_]*|postgres|template*) echo 'недопустимое имя новой базы' >&2; exit 1 ;; esac
    [ "${#TARGET}" -le 63 ] && [ "$TARGET" != "${PGDATABASE:-}" ] || { echo 'недопустимое имя новой базы' >&2; exit 1; }
fi

if [ -t 0 ]; then
    printf 'Вставьте закрытый ключ (AGE-SECRET-KEY-..., ввод скрыт): ' >&2
    TTY_HIDDEN=true
    stty -echo
    read -r KEY
    stty echo
    TTY_HIDDEN=false
    echo >&2
else
    KEY=$(grep -m1 'AGE-SECRET-KEY-' || true)
fi
# PowerShell may prepend a BOM or append CR; the key itself has no whitespace.
KEY=$(printf '%s' "$KEY" | tr -d '[:space:]' | sed 's/^.*AGE-SECRET-KEY-/AGE-SECRET-KEY-/')
case "$KEY" in AGE-SECRET-KEY-*) ;; *) echo 'это не закрытый ключ age' >&2; exit 1 ;; esac

# Do not extract or create a database until age has authenticated the ENTIRE archive and exited 0.
printf '%s\n' "$KEY" | age -d -i /dev/stdin -o "$WORK/archive.tar" "$SRC"
KEY=
mkdir "$WORK/archive"
tar -C "$WORK/archive" -xf "$WORK/archive.tar"
if [ "$MODE" = configs ]; then
    [ -d "$WORK/archive/config" ] || { echo 'в этом бэкапе нет конфигов' >&2; exit 1; }
    tar -C "$WORK/archive/config" -cf - .
    exit 0
fi

createdb --template=template0 -- "$TARGET"
CREATED=true
pg_restore --no-owner --no-acl --exit-on-error --single-transaction -d "$TARGET" "$WORK/archive/db.dump"
CHECK=$(psql -X -v ON_ERROR_STOP=1 -d "$TARGET" -Atc "select 'patients=' || (select count(*) from patient_profiles)
    || ' examinations=' || (select count(*) from examinations)
    || ' last exam=' || coalesce((select max(exam_date)::text from examinations), '-')")
psql -X -v ON_ERROR_STOP=1 -v target="$TARGET" -d postgres <<'SQL'
COMMENT ON DATABASE :"target" IS 'registry-backup:restored';
SQL
CREATED=false
printf 'restored into %s:\n%s\n' "$TARGET" "$CHECK"
