# Jobsearcher (ФСП) — backend

Цифровая платформа подбора ИТ-специалистов с обратной механикой: кандидат проходит опрос и тест, получает категорию; работодатель ищет по категориям и сам направляет приглашение.

## Быстрый старт

```bash
cp .env.example .env
# заполнить OLLAMA_API_KEY и/или INFERECO_API_KEY

docker compose up -d --build
curl -sf http://localhost:8080/actuator/health
```

Swagger: http://localhost:8080/swagger-ui.html

### Локально без Docker-приложения

```bash
docker compose up -d postgres keycloak
cd backend
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
./gradlew bootRun --args='--spring.profiles.active=local'
```

В профиле `local` включён dev-auth: заголовки `X-User-Email`, `X-User-Role` (`CANDIDATE`|`EMPLOYER`|`ADMIN`).

## Контракт для фронтенда

Полный контракт: [docs/FRONTEND.md](docs/FRONTEND.md)  
OpenAPI: `/v3/api-docs`  
Базовый URL API: `/api/v1`

### Auth

- Production: Bearer JWT из Keycloak realm `jobsearcher` (клиент `jobsearcher-frontend`)
- Роли realm: `CANDIDATE`, `EMPLOYER`, `ADMIN`
- Демо-пользователи Keycloak: `candidate1`/`candidate2`/`employer1`/`employer2`, пароль `pass`
- Local/dev: `X-User-Email` + `X-User-Role`

### Главный пользовательский сценарий

1. Кандидат: согласия 152-ФЗ → профиль → анкета `IT_CANDIDATE` → тест → категория → publish
2. Работодатель: профиль компании → потребность (salary from/to обязательны) → matching → invitation
3. Кандидат: accept/decline; контакты кандидата видны работодателю только после `ACCEPTED`

### Ошибки

```json
{ "code": "JS_00X", "message": "...", "timestamp": "..." }
```

## Документация (п. 3.4 ТЗ)

- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
- [docs/TESTING_MECHANICS.md](docs/TESTING_MECHANICS.md)
- [docs/MATCHING.md](docs/MATCHING.md)
- [docs/VALIDATION.md](docs/VALIDATION.md)
- [docs/FSP_INTEGRATION.md](docs/FSP_INTEGRATION.md)
- [docs/API.md](docs/API.md)
- [docs/DEPLOY.md](docs/DEPLOY.md)
- [docs/LIBRARIES.md](docs/LIBRARIES.md)
- [docs/FRONTEND.md](docs/FRONTEND.md)
- [docs/COMPETITOR_BACKLOG.md](docs/COMPETITOR_BACKLOG.md) - идеи с чужих решений (без копирования кода)

## Стек

Java 21, Spring Boot 3.4, PostgreSQL, Flyway, Keycloak, springdoc OpenAPI, Docker Compose, Ollama + Infereco LLM.
