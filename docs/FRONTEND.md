# Контракт API для фронтенда

Base URL: `http://localhost:8080/api/v1`  
Swagger UI: `http://localhost:8080/swagger-ui.html`  
Content-Type: `application/json` (кроме upload резюме)

## Авторизация

### Production (Keycloak)

1. Login через Keycloak realm `jobsearcher`, client `jobsearcher-frontend`
2. Получить access token
3. Каждый запрос: `Authorization: Bearer <token>`
4. Роли в JWT `realm_access.roles`: `CANDIDATE` | `EMPLOYER` | `ADMIN`

Issuer: `http://localhost:8081/realms/jobsearcher`

### Local / demo без Keycloak

Профиль `local` или `SECURITY_PERMIT_ALL=true`:

```
X-User-Email: candidate1@example.com
X-User-Role: CANDIDATE
```

## Эндпоинты

### Me / Consents

| Method | Path | Описание |
|--------|------|----------|
| GET | `/me` | текущий user + consents |
| GET | `/consents` | список согласий |
| POST | `/consents` | `{ "type": "PERSONAL_DATA_PROCESSING"\|"PROFILE_PUBLICATION", "granted": true }` |

Публикация профиля без обоих согласий → `403 JS_006`.

### Dictionaries

| Method | Path |
|--------|------|
| GET | `/dictionaries/industries` |
| GET | `/dictionaries/specializations?industryCode=IT` |
| GET | `/dictionaries/grades` |

### Candidate LK

| Method | Path | Body |
|--------|------|------|
| GET | `/candidate/profile` | |
| PUT | `/candidate/profile` | fullName, phone, city, about, stack[], softSkills[], experienceYears, resumeText, claimedGrade, industryCode, specializationCode, fspParticipantId, privacyHideContacts |
| POST | `/candidate/publish` | `{ "published": true }` |
| POST | `/candidate/resume` | multipart `file` (PDF/text) → NLP parse |
| GET/POST | `/candidate/fsp` | stub достижений ФСП |

Категория (`categoryCode`) появляется только после теста.  
Резюме само по себе категорию не задаёт.

### Surveys

| Method | Path | Body/Query |
|--------|------|------------|
| GET | `/surveys?audience=CANDIDATE&industryCode=IT` | |
| GET | `/surveys/{id}/questions` | |
| POST | `/surveys/sessions` | `{ "questionnaireCode": "IT_CANDIDATE" }` |
| POST | `/surveys/sessions/{id}/answers` | map `questionCode -> optionCode` |
| POST | `/surveys/sessions/{id}/complete?employerNeedId=` | считает vector |

Коды анкет:

- `IT_CANDIDATE`, `IT_EMPLOYER` (глубокие)
- `CONSTRUCTION_CANDIDATE`, `CONSTRUCTION_EMPLOYER`
- `MARKETING_CANDIDATE`, `MARKETING_EMPLOYER`

### Testing

| Method | Path | Body |
|--------|------|------|
| POST | `/tests/sessions` | `{ "targetGrade": "JUNIOR" }` |
| GET | `/tests/sessions/{id}` | items без правильных ответов |
| POST | `/tests/sessions/{id}/submit` | map `itemCode -> answer` |

Ответ submit: `score`, `passed`, на профиле обновляются `assignedGrade`, `categoryCode`.  
Смена грейда чаще 1 раза / 90 дней → `409 JS_007`.

### Employer LK

| Method | Path |
|--------|------|
| GET/PUT | `/employer/profile` |
| GET/POST | `/employer/needs` |

Need обязателен: `salaryFrom`, `salaryTo` (руб, from>0, to>=from), industry/specialization/grade.

### Matching

| Method | Path |
|--------|------|
| GET | `/matching/needs/{needId}?stack=Java` | категории + кандидаты + `explain[]` |
| GET | `/matching/candidates?industryCode&specializationCode&grade&categoryCode&stack&requireFsp` |

В выдаче банка **нет** email/phone кандидата.

### Invitations

| Method | Path | Body |
|--------|------|------|
| POST | `/invitations` | candidateId, needId?, message, salaryFrom, salaryTo |
| GET | `/invitations` | свои (по роли) |
| POST | `/invitations/{id}/status` | `{ "status": "VIEWED"\|"ACCEPTED"\|"DECLINED" }` |

Статусы: `SENT`, `VIEWED`, `ACCEPTED`, `DECLINED`.  
`candidateEmail`/`candidatePhone` в ответе работодателя заполняются только при `ACCEPTED`.

### Vacancies (доп. сценарий)

| Method | Path |
|--------|------|
| GET | `/vacancies` | опубликованные |
| GET | `/vacancies/mine` | работодателя |
| POST | `/vacancies` | create (+ publish) |
| POST | `/vacancies/{id}/applications` | `{ "coverLetter" }` |
| GET | `/vacancies/applications/mine` | отклики кандидата |
| GET | `/vacancies/{id}/applications` | отклики на вакансию |

## DTO-ориентиры

### categoryCode

Формат: `{INDUSTRY}:{SPECIALIZATION}:{GRADE}`  
Пример: `IT:BACKEND:MIDDLE`

### InvitationView

```json
{
  "id": "uuid",
  "employerId": "uuid",
  "companyName": "Acme",
  "candidateId": "uuid",
  "candidateName": "Иван",
  "candidateEmail": null,
  "candidatePhone": null,
  "needId": "uuid",
  "message": "...",
  "salaryFrom": 150000,
  "salaryTo": 200000,
  "status": "SENT",
  "createdAt": "2026-10-05T10:00:00Z"
}
```

### MatchResponse

```json
{
  "needId": "uuid",
  "primaryCategory": "IT:BACKEND:JUNIOR",
  "candidates": [
    {
      "candidateId": "uuid",
      "categoryCode": "IT:BACKEND:JUNIOR",
      "score": 0.81,
      "explain": ["Категория: BACKEND / JUNIOR", "Совпал стек: java"]
    }
  ],
  "categories": { "IT:BACKEND:JUNIOR": [] }
}
```

## Что фронту нужно доделать / реализовать

1. Keycloak login/logout (Authorization Code + PKCE), хранение access token, refresh
2. Выбор роли при регистрации / экраны онбординга кандидата и работодателя
3. UI согласий 152-ФЗ до публикации профиля
4. Мастер кандидата: анкета → выбор грейда → тест → результат категории
5. ЛК кандидата: профиль, приватность, FSP ID, входящие приглашения accept/decline
6. ЛК работодателя: компания, потребность, подборка с explain, банк кандидатов, приглашения
7. Загрузка PDF резюме и отображение распарсенных полей (бэк уже отдаёт обновлённый профиль)
8. Автогенерация стандартизированного PDF-профиля из данных ЛК (клиентский рендер)
9. Список вакансий + отклик (доп. сценарий MVP+)
10. Обработка кодов ошибок `JS_*` и статусов приглашений/откликов
11. Не показывать контакты кандидата до `ACCEPTED` (бэк скрывает, UI не должен кэшировать раньше времени)
12. Адаптивная вёрстка — опционально для MVP

## Demo curl (local)

```bash
# кандидат
curl -s -H 'X-User-Email: c@x.ru' -H 'X-User-Role: CANDIDATE' http://localhost:8080/api/v1/me

# справочник
curl -s http://localhost:8080/api/v1/dictionaries/industries
```
