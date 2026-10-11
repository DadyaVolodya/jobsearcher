# Перенос второго хакатон-проекта на Selectel

Сервер: `188.124.47.163`  
ФСП Карьера занимает порт **80** (`/opt/apps/jobsearcher`).  
Heat / трассировка занимает порт **8080** (`/opt/heat-network`).

| Проект | URL |
|--------|-----|
| ФСП кандидат | http://188.124.47.163/candidate/ |
| ФСП работодатель | http://188.124.47.163/employer/ |
| Heat UI | http://188.124.47.163:8080/ |

Копия инструкции на сервере: `/opt/apps/README.md`, `/opt/apps/SECOND_PROJECT.md`

## 1. Подключение

```bash
ssh root@188.124.47.163
# предпочтительно ключом:
ssh -i ~/.ssh/selectel_ed25519 root@188.124.47.163
```

## 2. Клонировать репозиторий

```bash
mkdir -p /opt/apps
cd /opt/apps
git clone <URL_ВТОРОГО_РЕПО> other-project
cd other-project
```

## 3. Порты (чтобы не конфликтовать с ФСП)

| Сервис ФСП | Порт на хосте |
|---|---|
| UI + API proxy | **80** |
| Postgres ФСП | только внутри docker-сети |

Для второго проекта:

- UI: **8081** (или 81)
- API: **8082**
- Postgres: без publish на хост (только internal) или **5433**

Пример в `docker-compose.yml`:

```yaml
services:
  web:
    ports: ["8081:80"]
  app:
    ports: ["8082:8080"]
  # postgres без ports
```

Firewall (8081/8082 уже открыты на сервере):

```bash
ufw allow 8081/tcp
ufw allow 8082/tcp
ufw reload
```

## 4. Запуск отдельным compose-проектом

```bash
cd /opt/apps/other-project
docker compose -p heat up -d --build
docker compose -p heat ps
curl -I http://127.0.0.1:8081/
```

Снаружи: `http://188.124.47.163:8081/`

Флаг `-p heat` даёт отдельные сети и volumes, без пересечения с `jobsearcher`.

## 5. Обновление

```bash
cd /opt/apps/other-project
git pull
docker compose -p heat up -d --build
```

## 6. Логи

```bash
docker compose -f /opt/apps/jobsearcher/docker-compose.demo.yml logs -f --tail=100
docker compose -p heat -f /opt/apps/other-project/docker-compose.yml logs -f --tail=100
```

Когда будет URL второго репо - можно так же выкатить одной командой.
