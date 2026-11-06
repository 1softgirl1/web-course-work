#!/bin/bash
# Backup operations menu for the server HOST (like manage.sh). Works even when the backup UI is not available.
#   sudo bash deploy/backup/backup-menu.sh            interactive menu
#   sudo bash deploy/backup/backup-menu.sh backup     one action without the menu (status|backup|list|verify|restore|switch|drop|configs)
# Defaults are the production container names; override with BACKUP_CONTAINER / PG_CONTAINER / APP_CONTAINER.
set -uo pipefail
umask 077

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BK=${BACKUP_CONTAINER:-registry-backup}
PG=${PG_CONTAINER:-registry-postgres}
export PG_CONTAINER=$PG APP_CONTAINER=${APP_CONTAINER:-registry-backend}

in_backup() { docker exec "$BK" "$@"; }
pg() { docker exec -i "$PG" sh -c 'exec psql -X -U "$POSTGRES_USER" -d postgres -v ON_ERROR_STOP=1 -At "$@"' _ "$@"; }
managed_databases() {
    local live=${DB_NAME:-$(docker exec "$PG" printenv POSTGRES_DB)}
    pg -v live="$live" <<'SQL'
SELECT datname FROM pg_database
WHERE NOT datistemplate AND datallowconn AND datname NOT IN ('postgres', :'live')
  AND datname NOT LIKE 'template%'
  AND shobj_description(oid, 'pg_database') IN ('registry-backup:restored', 'registry-backup:displaced')
ORDER BY datname;
SQL
}
pause() { [ -t 0 ] && read -rp "Enter — назад в меню " _ </dev/tty; }

pick() { # pick "prompt" item... -> prints the chosen item
    local prompt=$1; shift
    [ $# -eq 0 ] && { echo "Нечего выбрать" >&2; return 1; }
    local i=1
    for item in "$@"; do echo "  $i) $item" >&2; i=$((i + 1)); done
    read -rp "$prompt: " n </dev/tty
    [[ "$n" =~ ^[0-9]+$ ]] && [ "$n" -ge 1 ] && [ "$n" -le $# ] || { echo "Неверный выбор" >&2; return 1; }
    echo "${!n}"
}

cmd_status() {
    echo "--- последний бэкап"; in_backup cat /backups/status.json; echo
    echo "--- журнал (последние 5)"; in_backup sh -c 'tail -5 /backups/journal.jsonl' | sed 's/","/" · "/g'
    echo "--- контейнеры"; docker ps --format '{{.Names}}\t{{.Status}}' | grep -E "registry" || true
}

cmd_backup() { in_backup backup; }

cmd_list() {
    echo "--- на сервере"; in_backup sh -c 'cd /backups && ls -lt daily weekly monthly 2>/dev/null | grep "\.tar\.age$"'
    echo "--- на NFS (/opt1)"; in_backup sh -c 'if [ "${NFS_ENABLED:-false}" = true ]; then network-storage list; else echo "не настроено"; fi'
    echo "--- в облаке"; in_backup cloud ls daily; in_backup cloud ls weekly 2>/dev/null; in_backup cloud ls monthly 2>/dev/null
}

cmd_verify() { in_backup verify; }

cmd_restore() {
    local src file path target
    src=$(pick "Откуда восстанавливать" "из облака" "с сервера" "с NFS (/opt1)") || return 1
    if [ "$src" = "из облака" ]; then
        mapfile -t files < <(in_backup cloud ls daily | awk '{print $4}' | sort -r)
        file=$(pick "Какую копию (свежие сверху)" "${files[@]}") || return 1
        path=$(in_backup cloud get "$file") || return 1
    elif [ "$src" = "с NFS (/opt1)" ]; then
        mapfile -t files < <(in_backup network-storage list | cut -f1 | sort -r)
        file=$(pick "Какую копию (свежие сверху)" "${files[@]}") || return 1
        path=$(in_backup network-storage get "$file") || return 1
    else
        mapfile -t files < <(in_backup sh -c 'ls -t /backups/daily/*.tar.age /backups/weekly/*.tar.age /backups/monthly/*.tar.age 2>/dev/null')
        path=$(pick "Какую копию (свежие сверху)" "${files[@]}") || return 1
    fi
    read -rp "Имя новой базы [restored]: " target </dev/tty
    target=${target:-restored}
    echo "Сейчас скрипт спросит закрытый ключ (AGE-SECRET-KEY-..., из KeePassXC); ввод скрыт."
    docker exec -it "$BK" restore "$path" "$target"
    local rc=$?
    case "$path" in /tmp/*) in_backup rm -f "$path" ;; esac
    [ $rc -eq 0 ] && echo "Дальше: пункт «Переключить сайт на восстановленную базу» (база $target)."
    return $rc
}

cmd_switch() {
    local target
    mapfile -t dbs < <(managed_databases)
    target=$(pick "Какую управляемую восстановленную/отложенную базу сделать рабочей" "${dbs[@]}") || return 1
    read -rp "Сайт переключится на $target (рабочая база сохранится как _broken_). Введите имя базы для подтверждения: " ok </dev/tty
    [ "$ok" = "$target" ] || { echo "Отменено"; return 1; }
    sh "$SCRIPT_DIR/switch-db.sh" "$target"
}

cmd_drop() {
    local live name
    live=${DB_NAME:-$(docker exec "$PG" printenv POSTGRES_DB)}
    mapfile -t dbs < <(managed_databases)
    name=$(pick "Какую управляемую базу удалить" "${dbs[@]}") || return 1
    read -rp "База $name будет удалена безвозвратно. Введите её имя для подтверждения: " ok </dev/tty
    [ "$ok" = "$name" ] || { echo "Отменено"; return 1; }
    # Recheck eligibility at execution time; names are psql values/identifiers, never SQL fragments.
    pg -v live="$live" -v target="$name" <<'SQL'
SELECT EXISTS (
    SELECT 1 FROM pg_database
    WHERE datname = :'target' AND datname NOT IN ('postgres', :'live')
      AND NOT datistemplate AND datallowconn AND datname NOT LIKE 'template%'
      AND shobj_description(oid, 'pg_database') IN ('registry-backup:restored', 'registry-backup:displaced')
) AS eligible \gset
\if :eligible
SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = :'target' AND pid <> pg_backend_pid();
DROP DATABASE :"target";
\else
DO $$ BEGIN RAISE EXCEPTION 'Удаление запрещено: база не является управляемой нерабочей базой'; END $$;
\endif
SQL
}

cmd_configs() (
    local file out tmp
    mapfile -t files < <(in_backup sh -c 'ls -t /backups/daily/*.tar.age 2>/dev/null')
    file=$(pick "Из какой копии" "${files[@]}") || return 1
    out="$HOME/registry-configs-$(date +%Y%m%d%H%M%S).tar"
    tmp=$(in_backup mktemp /tmp/registry-configs.XXXXXX) || return 1
    trap 'in_backup rm -f "$tmp"' EXIT
    trap 'exit 130' INT
    trap 'exit 143' TERM
    echo "Сейчас скрипт спросит закрытый ключ; конфиги (.env с паролями!) будут сохранены в $out"
    docker exec -it "$BK" sh -c 'umask 077; restore --configs "$1" > "$2"' _ "$file" "$tmp" &&
        docker cp "$BK:$tmp" "$out" && chmod 600 "$out" &&
        echo "Готово: $out (после использования удалите: rm $out)"
)

run() {
    case "$1" in
        status) cmd_status ;; backup) cmd_backup ;; list) cmd_list ;; verify) cmd_verify ;;
        restore) cmd_restore ;; switch) cmd_switch ;; drop) cmd_drop ;; configs) cmd_configs ;;
        *) echo "Неизвестная команда: $1 (status|backup|list|verify|restore|switch|drop|configs)"; return 1 ;;
    esac
}

if [ $# -gt 0 ]; then run "$1"; exit $?; fi

while true; do
    echo ""
    echo "=== Бэкапы registry ==="
    echo "  1) Статус"
    echo "  2) Бэкап сейчас"
    echo "  3) Список копий (сервер / NFS / облако)"
    echo "  4) Проверить копии"
    echo "  5) Восстановить базу из копии (в новую базу)"
    echo "  6) Переключить сайт на восстановленную базу"
    echo "  7) Удалить старую / лишнюю базу"
    echo "  8) Достать конфиги из бэкапа"
    echo "  0) Выход"
    read -rp "Выберите действие: " choice </dev/tty
    case "$choice" in
        1) run status ;; 2) run backup ;; 3) run list ;; 4) run verify ;;
        5) run restore ;; 6) run switch ;; 7) run drop ;; 8) run configs ;;
        0) exit 0 ;; *) echo "Неверный выбор" ;;
    esac
    pause
done
