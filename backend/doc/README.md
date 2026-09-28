# Документация проекта

В этой папке лежат рабочие материалы по backend:

- `database.md` — описание структуры базы данных.
- `data/er.mmd` — Mermaid-код ER-диаграммы базы данных.
- `data/er.png` — PNG-экспорт ER-диаграммы базы данных.
- `migration.md` — описание Flyway-миграций и их назначения.
- `demo-seed.sql` — памятка о переносе демо-данных в local-only Flyway migrations.
- `demo-scenarios.md` — краткая памятка по демо-сценариям и запуску.
- `telemetry.md` — телеметрия и аудит: каталог событий, что нельзя писать, Actuator.
- `telemetry-queries.sql` — SQL-запросы для отчёта пилота по `telemetry_events`.

OpenAPI для backend теперь живет только в runtime SpringDoc и Swagger UI:

- `http://localhost:8080/v3/api-docs`
- `http://localhost:8080/swagger-ui/index.html`
