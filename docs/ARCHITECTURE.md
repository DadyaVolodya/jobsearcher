# Архитектура

## Компоненты

- **Frontend** (отдельный репозиторий/приложение) — ЛК кандидата и работодателя
- **Backend** — Spring Boot монолит с модульным разделением пакетов
- **PostgreSQL** — основное хранилище
- **Keycloak** — аутентификация и роли
- **LLM Gateway** — Ollama Cloud (primary) + Infereco (fallback)
- **Local storage** — файлы резюме (`storage/uploads`), порт готов к S3

## Слои backend

```
api/             REST controllers + DTO
application/     use-cases, security utils, policies
domain/          entities, enums, repositories
survey/scoring   расчёт векторов анкет
testing/engine   сборка уникальных тест-сессий
matching/        ранжирование и explain
consents/        проверки 152-ФЗ
infrastructure/  LLM, storage, security, config
fsp/             stub достижений (через candidate service)
```

## Потоки

1. JWT/dev headers → provisioning AppUser + пустой профиль роли
2. Анкета → SurveyScoringEngine → vector на профиле/потребности
3. Тест → TestSessionAssembler(seed) → score → categoryCode
4. Matching → фильтр по категории + score(vector, stack, test, FSP)
5. Invitation → контакты только после ACCEPTED

## Контейнеры

`docker-compose.yml`: postgres, keycloak, app.
