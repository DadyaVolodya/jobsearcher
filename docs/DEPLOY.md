# Сборка и запуск

## Требования

- Docker Compose v2
- Java 21 (для локальной разработки)
- Порты: 8080 (app), 8081 (Keycloak), 5432 (Postgres)

## Docker

```bash
cp .env.example .env
docker compose up -d --build
curl -sf http://localhost:8080/actuator/health
```

## Local app + Docker deps

```bash
docker compose up -d postgres keycloak
cd backend
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
./gradlew bootRun --args='--spring.profiles.active=local'
```

## Тесты

Интеграционные тесты ожидают Postgres на `localhost:55432`:

```bash
docker run -d --name jobsearcher-test-pg \
  -e POSTGRES_DB=jobsearcher -e POSTGRES_USER=jobsearcher -e POSTGRES_PASSWORD=jobsearcher \
  -p 55432:5432 postgres:16-alpine

cd backend
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
./gradlew test
```

## Демо Keycloak

- URL: http://localhost:8081
- admin/admin
- users: candidate1, candidate2, employer1, employer2 / pass
