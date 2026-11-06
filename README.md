# Медицинский регистр

Backend — Kotlin/Spring Boot и PostgreSQL 17, frontend — Vue. Ниже — запуск **локального демо** с демо-данными, не инструкция для production.

## 1. Скачать и запустить сайт

1. Установите Git и Docker Desktop с Docker Compose v2. На Windows включите Linux containers и запустите Docker Desktop.
2. На странице этого репозитория на GitHub нажмите **Code → HTTPS**, скопируйте адрес и выполните `git clone <скопированный-адрес>`. Либо скачайте **Code → Download ZIP** и распакуйте.
3. Откройте PowerShell в корне проекта — там, где лежит `docker-compose.yml`. Все дальнейшие команды выполняются здесь.
4. Запустите:

```powershell
docker compose up -d --build
if ($LASTEXITCODE -ne 0) { throw 'Запуск не завершён. Исправьте ошибку Docker выше.' }
```

Java, Node.js и PostgreSQL на компьютере устанавливать не нужно. Первая сборка скачивает Docker-образы, Gradle- и npm-зависимости: нужен интернет, сборка может идти долго. Фиксированного времени готовности нет. При сетевой ошибке исправьте соединение и повторите команду.

Откройте **http://localhost:3000**. Если сайт ещё не отвечает, посмотрите состояние и логи:

```powershell
docker compose ps -a
docker compose logs --tail=100 backend nginx front-builder
```

Завершение `front-builder` с кодом `0` — нормально: он собирает frontend и заканчивает работу.

Демо-входы:

| Кто | Логин | Пароль |
| --- | --- | --- |
| Врач | `doctor.demo@example.com` | `doctor-password` |
| Врач с расширенными правами | `head.doctor@example.com` | `extended-password` |
| Пациент | `PT-DEMO-001` | `patient-password` |

Остальные аккаунты и сценарии: [backend/doc/demo-scenarios.md](backend/doc/demo-scenarios.md). Демо-данные подключаются профилем `local` из корневого Compose.

Дополнительно: pgAdmin — **http://localhost:3000/db/admin/** (`admin@example.com` / `admin-password`, если появится форма входа). PostgreSQL с компьютера — `localhost:5434`, база `web_course_work`, пользователь/пароль `postgres` / `postgres`.

Это открытые **локальные** пароли, не используйте их на сервере. Корневой site-only Compose публикует порты 3000 и 5434 не только на loopback; запускайте демо в доверенной сети, не открывайте его в интернет.

## 2. Добавить локальные бэкапы

Этот шаг необязателен: запуск сайта выше от него не зависит. Нужен только Docker — без Яндекса, NFS, Telegram и чужих ключей. Дополнительный Compose создаёт отдельный локальный том `backups` в том же Compose-проекте. Он не меняет том PostgreSQL.

### Один раз: создать свой ключ на компьютере

Выполните весь блок в PowerShell. Он останавливается при ошибке сборки или генерации и **не перезаписывает существующий ключ**:

```powershell
$ErrorActionPreference = 'Stop'
docker build -t registry-backup:local deploy/backup
if ($LASTEXITCODE -ne 0) { throw 'Образ backup не собран. Дальше не продолжайте.' }

New-Item -ItemType Directory -Force .backup-local | Out-Null
$keys = (Resolve-Path .backup-local).Path
if (-not (Test-Path .backup-local/identity.age)) {
    docker run --rm --mount "type=bind,source=$keys,target=/keys" --entrypoint age-keygen registry-backup:local -o /keys/identity.age
    if ($LASTEXITCODE -ne 0) { throw 'Ключ не создан. Разберите ошибку; существующий файл не удаляйте вслепую.' }
}

$recipient = docker run --rm --mount "type=bind,source=$keys,target=/keys,readonly" --entrypoint age-keygen registry-backup:local -y /keys/identity.age
if ($LASTEXITCODE -ne 0) { throw 'Не удалось прочитать публичный ключ. Дальше не продолжайте.' }
if (-not ($recipient -match '^age1[0-9a-z]+$')) { throw 'Неожиданный формат публичного ключа.' }
Set-Content -Path .backup-local/backup.env -Encoding ascii -Value "BACKUP_AGE_RECIPIENT=$recipient"
```

- `.backup-local/identity.age` — **закрытый ключ**. Не выводите его командами в терминал, не отправляйте в чат или Git. Храните защищённую отдельную копию: потеря ключа означает потерю возможности расшифровать архивы.
- `.backup-local/backup.env` содержит только публичный получатель. Вся папка `.backup-local/` исключена из Git.
- Закрытый ключ создаётся на вашем компьютере и **не монтируется в работающий backup-сервис**. Не удаляйте и не заменяйте его, пока нужны старые копии. При повторном выполнении блок использует прежний ключ.

### Запустить сайт вместе с backup

```powershell
docker compose --env-file .backup-local/backup.env -f docker-compose.yml -f docker-compose.backup.yml up -d --build
if ($LASTEXITCODE -ne 0) { throw 'Запуск не завершён. Посмотрите ошибку Docker выше.' }
```

Откройте **http://127.0.0.1:8090**:

- логин: `admin`;
- пароль: `local-backup-password` (только для этого локального демо).

Порт backup доступен только с этого компьютера. Его настройки берутся из явно указанного `backup.env`; копировать серверные `.env` не нужно. Из конфигурации в архив попадают только два корневых Compose-файла, а не серверные `prod/internal` или ваш закрытый ключ. NFS, S3 и notifier явно отключены; сетевой том не монтируется.

## 3. Создать, проверить и восстановить копию

В интерфейсе backup:

1. **Статус** → запустите бэкап и дождитесь окончания в **Журнале**. Во вкладке **Копии** появится локальный архив.
2. Запустите проверку копий и посмотрите её результат в журнале. Пустая история, `pending` и «ещё не было» **не означают успешный бэкап или проверку**. После первого запуска дождитесь свежих результатов; при ошибке откройте журнал.
3. Во вкладке **Настройки** можно выбрать время ежедневного бэкапа. Планировщик работает, пока запущен контейнер и компьютер не спит; часовой пояс локального backup — UTC.

Ежедневный бэкап проверяет восстановление дампа **до шифрования**. Еженедельная проверка сверяет хеши сохранённых архивов; это не полная расшифровка и не проверка переключения сайта. Полный decrypt/restore выполняется отдельно с вашим ключом.

### Восстановить, не заменяя рабочую базу

1. Откройте **Восстановление**, выберите **с сервера** (в локальном демо это локальный Docker-том), затем нужную копию.
2. Укажите новое имя базы, например `restored_demo_001`, которое ещё не используется.
3. На своём компьютере откройте `.backup-local/identity.age` в текстовом редакторе. Самостоятельно скопируйте строку закрытого ключа `AGE-SECRET-KEY-…` в скрытое поле **Закрытый ключ**. Не вставляйте её в команды, логи или чат. После ввода очистите буфер обмена и закройте файл.
4. Нажмите **Восстановить** и дождитесь результата в журнале. Создаётся **новая база**, рабочий сайт продолжает использовать прежнюю.

Переключение сайта — отдельное действие на шаге 2 интерфейса восстановления, с подтверждением. Перед ним остановите внесение записей и согласуйте переключение со всеми, кто работает со стендом. Обратное переключение не переносит новые записи между базами: можно потерять изменения, внесённые после первого переключения. Список управляемых баз ограничен метками backup-сервиса; произвольные базы не предназначены для этих действий. Для проверки восстановления переключать сайт не обязательно. Не удаляйте пациентов или рабочую базу ради демонстрации.

## 4. Повседневные команды

Без бэкапов: повторный запуск без сборки — `docker compose up -d`, перезапуск — `docker compose restart`, остановка — `docker compose stop`.

Если включали backup, **всегда сохраняйте те же `--env-file` и оба `-f`**, запускайте из того же каталога (не меняйте имя Compose-проекта). Например:

```powershell
# Запустить/поднять остановленный стек без пересборки
docker compose --env-file .backup-local/backup.env -f docker-compose.yml -f docker-compose.backup.yml up -d

# Перезапустить существующие контейнеры без пересборки
docker compose --env-file .backup-local/backup.env -f docker-compose.yml -f docker-compose.backup.yml restart

# Состояние и последние логи backup
docker compose --env-file .backup-local/backup.env -f docker-compose.yml -f docker-compose.backup.yml ps -a
docker compose --env-file .backup-local/backup.env -f docker-compose.yml -f docker-compose.backup.yml logs --tail=100 backup

# Остановить, сохранив базы и копии
docker compose --env-file .backup-local/backup.env -f docker-compose.yml -f docker-compose.backup.yml stop
```

После обновления исходников используйте соответствующую команду `up -d --build` из раздела 1 или 2. Не удаляйте Docker-тома и не используйте `down -v`: в томах находятся база и архивы. Локальные копии в Docker не защищают от потери компьютера или его диска.

## Production и документация

Серверная конфигурация находится в [deploy/compose.yaml](deploy/compose.yaml), примеры параметров — `deploy/prod/.env.example` и `deploy/internal/.env.example`. Она рассчитана на отдельную настройку администратором: локальное хранилище + настоящий NFS `/opt1` + Yandex Object Storage, отдельные пароли/ключи и notifier. Не заменяйте работающую серверную конфигурацию локальным демо.

Исходники [deploy/notifier](deploy/notifier) включены в проект, но локальный запуск выше не использует авторские VPS, Telegram, токены или облачные доступы. Локальное демо **не реализует схему 3-2-1-1-0**: для production нужны независимые площадки, подтверждённая защита хранения, отдельная offline-копия и регулярные проверки восстановления. Наличие S3-настроек само по себе не доказывает неизменяемость и не гарантирует время восстановления.

Описание backend и ссылки на предметную документацию: [backend/doc/README.md](backend/doc/README.md).
