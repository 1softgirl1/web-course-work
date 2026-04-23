# Мини-гайд (шаблон): деплой Vue/Vite на GitHub Pages

Этот гайд сделан как переиспользуемый шаблон. Сначала подставьте свои значения в переменные ниже, потом выполните шаги.

## 0) Что нужно подставить под свой проект

- `GITHUB_USERNAME` — ваш логин на GitHub.
- `REPO_NAME` — имя репозитория на GitHub.
- `DEPLOY_BRANCH` — ветка, push в которую запускает деплой (из workflow).
- `WORK_BRANCH` — ветка, в которой вы обычно работаете.
- `FRONTEND_DIR` — папка фронтенда (в этом проекте это `frontend`).

Пример для текущего проекта:
- `GITHUB_USERNAME = 1softgirl1`
- `REPO_NAME = web-course-work`
- `DEPLOY_BRANCH = frontend`
- `WORK_BRANCH = frontend`
- `FRONTEND_DIR = frontend`

## 1) Проверьте обязательные настройки в проекте

1. В `FRONTEND_DIR/vite.config.ts` должен быть корректный `base`:

```ts
base: '/REPO_NAME/'
```

2. Для текущей схемы роутинга в `FRONTEND_DIR/src/router/index.ts` должен использоваться hash-router:

```ts
createWebHashHistory(import.meta.env.BASE_URL)
```

3. В `.github/workflows/deploy.yml` должен быть trigger на `DEPLOY_BRANCH`:

```yaml
on:
  push:
    branches: ['DEPLOY_BRANCH']
```

4. В GitHub включите Pages через Actions:
   `Settings -> Pages -> Source = GitHub Actions`.

## 2) Локальная проверка перед push

```powershell
Set-Location "<путь-к-проекту>\FRONTEND_DIR"
npm ci
npm run build
```

Если сборка проходит, можно деплоить.

## 3) Деплой (базовый сценарий)

Если вы работаете сразу в `DEPLOY_BRANCH`:

```powershell
Set-Location "<путь-к-проекту>"
git add .
git commit -m "Deploy update"
git push origin DEPLOY_BRANCH
```

Если вы работаете в `WORK_BRANCH` и она отличается от `DEPLOY_BRANCH`:

```powershell
Set-Location "<путь-к-проекту>"
git checkout WORK_BRANCH
git add .
git commit -m "Prepare deploy"
git push origin WORK_BRANCH
```

Дальше влейте `WORK_BRANCH` в `DEPLOY_BRANCH` (через PR или локально), затем запушьте `DEPLOY_BRANCH`.

## 4) Проверка результата

1. Откройте GitHub -> `Actions`.
2. Дождитесь успешного workflow деплоя.
3. Проверьте URL сайта:

```text
https://GITHUB_USERNAME.github.io/REPO_NAME/
```


