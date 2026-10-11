# Публичное демо (не Яндекс Облако)

Рекомендуемый хостинг для сдачи: **Render** (бесплатный tier) или любой VPS с Docker Compose
(Timeweb Cloud / Selectel / DigitalOcean). Keycloak на демо не нужен: `SECURITY_PERMIT_ALL=true`
и вход по email в UI.

## Ссылки для жюри (заполнить после выката)

| Что | URL |
|-----|-----|
| Репозиторий | https://github.com/DadyaVolodya/jobsearcher (ветка `vorobev`) |
| Кандидат | `https://<host>/candidate/` |
| Работодатель | `https://<host>/employer/` |
| Swagger | `https://<host>/swagger-ui.html` |
| Health | `https://<host>/actuator/health` |

Демо-аккаунты (seed V5):

- кандидат: `candidate1@example.com` (роль CANDIDATE)
- работодатель: `employer1@example.com` (роль EMPLOYER)

## Вариант A. Один VPS + Docker Compose (предпочтительно)

На машине с Docker:

```bash
git clone -b vorobev https://github.com/DadyaVolodya/jobsearcher.git
cd jobsearcher
cp .env.example .env
# опционально: OLLAMA_API_KEY / INFERECO_API_KEY
docker compose -f docker-compose.demo.yml up -d --build
```

Открыть `http://<IP>/candidate/` и `http://<IP>/employer/`.

Порты: `80` (UI+proxy API), внутри `8080` API. Postgres только во внутренней сети.

## Вариант B. Render Blueprint

1. Аккаунт на [render.com](https://render.com)
2. **New → Blueprint** → репозиторий `DadyaVolodya/jobsearcher`, ветка `vorobev`, файл `render.yaml`
3. После создания Postgres задать у сервиса `jobsearcher-api`:

```text
DB_URL=jdbc:postgresql://<host>:5432/jobsearcher
DB_USER=<user>
DB_PASSWORD=<password>
SECURITY_PERMIT_ALL=true
CORS_ORIGINS=*
```

Render отдаёт `postgres://…` URL; Spring ждёт JDBC-строку `jdbc:postgresql://…`.

4. У `jobsearcher-web` на этапе сборки указать абсолютный API:

```text
VITE_API_URL=https://jobsearcher-api.onrender.com/api/v1
```

и поправить nginx/proxy либо открывать API напрямую из UI.

Free-tier засыпает после простоя: перед показом жюри «разбудить» health-запросом.

## Вариант C. Railway

1. New Project → Deploy from GitHub → ветка `vorobev`
2. Добавить PostgreSQL plugin
3. Variables:

```text
DB_URL=jdbc:postgresql://${PGHOST}:${PGPORT}/${PGDATABASE}
DB_USER=${PGUSER}
DB_PASSWORD=${PGPASSWORD}
SECURITY_PERMIT_ALL=true
CORS_ORIGINS=*
```

4. Dockerfile path: `backend/Dockerfile`
5. Отдельно сервис из `frontend/Dockerfile` или статика после `npm run build`

## Локальная проверка демо-стека

```bash
docker compose -f docker-compose.demo.yml up -d --build
curl -sf http://localhost/actuator/health
curl -sf -H 'X-User-Email: employer1@example.com' -H 'X-User-Role: EMPLOYER' \
  -H 'X-User-Sub: dev-employer1@example.com' http://localhost/api/v1/me
```
