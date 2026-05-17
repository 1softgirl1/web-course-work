# Deploy

Эта схема повторяет серверный запуск: backend и PostgreSQL живут в Docker Compose, frontend собирается в общий volume, а nginx раздает собранный `dist` и проксирует backend.

## Локальная проверка как на сервере

1. Создать локальный env:

```powershell
Copy-Item deploy/.env.example deploy/.env.local
```

2. Запустить локальный deploy-like стек:

```powershell
docker compose --env-file deploy/.env.local -f deploy/compose.yaml -f deploy/compose.local.yaml up -d --build
```

3. Проверить:

- frontend: `http://localhost:8088`
- Swagger UI: `http://localhost:8088/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8088/v3/api-docs`
- pgAdmin: `http://localhost:8088/db/admin`

Локальный override включает `SPRING_PROFILES_ACTIVE=local`, поэтому применяются обычные Flyway migrations и demo seed из `db/local-migration`.
Локально pgAdmin получает готовый сервер `web_course_work` из `deploy/pgadmin/servers.json`; если pgAdmin попросит пароль к PostgreSQL, используется значение `DB_PASSWORD` из env.

Остановить и удалить локальные volumes:

```powershell
docker compose --env-file deploy/.env.local -f deploy/compose.yaml -f deploy/compose.local.yaml down -v --remove-orphans
```

## Сервер

1. Скопировать `deploy/.env.example` в `deploy/.env` и заменить секреты/домены.
2. Убедиться, что общий nginx на сервере подключен к network `${COMMON_NAME}_nginx-network` и монтирует volume `${COMMON_NAME}_web-data` в `/var/www/html`.
3. Поднять проект:

```bash
docker compose --env-file deploy/.env -f deploy/compose.yaml up -d --build
```

Серверный compose не задает `SPRING_PROFILES_ACTIVE=local`, поэтому demo migrations из `db/local-migration` на сервере не применяются.

## Nginx

`nginx.conf` - серверный конфиг для общего nginx. В нем временно проксируется `/auth`, пока auth endpoints не перенесены под `/api/auth`.

Сертификаты сейчас не настраиваются в этом проекте: когда сертификат выдадут, TLS-блок можно добавить в общий nginx поверх этой схемы.
