# Материалы к сдаче ЛЦТ 2026 / спец. трек ФСП

## 1. Ссылка на репозиторий

https://github.com/DadyaVolodya/jobsearcher  

Рабочая ветка: **`vorobev`**

## 2. Презентация

Файл: [presentation/FSP_Jobsearcher_presentation.pptx](presentation/FSP_Jobsearcher_presentation.pptx)

Собрана из официального шаблона ФСП. Слайды **7–11** сохранены в дизайне шаблона и заполнены.  
Пересборка: `python3 scripts/build-presentation.py`

На слайде 9 при необходимости подставьте ФИО/телефоны остальных участников команды.

## 3. Развёрнутое демо

Инструкция: [DEMO_CLOUD.md](DEMO_CLOUD.md)

Локально / на VPS (Timeweb, Selectel, DigitalOcean и т.п.):

```bash
git clone -b vorobev https://github.com/DadyaVolodya/jobsearcher.git
cd jobsearcher
docker compose -f docker-compose.demo.yml up -d --build
```

- Кандидат: `http://<host>/candidate/`
- Работодатель: `http://<host>/employer/`
- API/Swagger: `http://<host>/swagger-ui.html`

Демо-пользователи: `candidate1@example.com`, `employer1@example.com`.

Альтернатива без своего VPS: **Render Blueprint** (`render.yaml`) или **Railway** — шаги в DEMO_CLOUD.md.
Яндекс Облако для этого стенда не используется.
