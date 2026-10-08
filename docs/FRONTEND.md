# Контракт API для фронтенда

Base URL: `http://localhost:8080/api/v1`  
Swagger: `http://localhost:8080/swagger-ui.html`  
Auth: Keycloak email/password (realm `jobsearcher`) → `Authorization: Bearer <token>`  
Local: `X-User-Email` + `X-User-Role`

Брендбук ФСП: https://disk.yandex.ru/d/nxMN6oQTZ25wZQ

---

## Экраны, которые нужны фронту

### Кандидат
1. ЛК профиля (ФИО, контакты, стек, город, FSP ID, согласия, publish)
2. Список вакансий + деталка вакансии + отклик
3. Вкладка тестирования по грейдам (Junior/Middle/Senior): анкета → тест A/B/C
4. Загрузка/парсинг резюме (PDF)
5. Приглашения (accept / decline+причина) и чаты
6. Во время теста: fullscreen, запрет copy/select, `visibilitychange`/`blur` → proctor

### Работодатель
1. ЛК компании
2. Список своих вакансий + создание/редактирование
3. Поиск претендентов (matching + bank) с explain
4. Приглашение: **сразу salaryFrom/salaryTo** (без ЗП писать нельзя)
5. Чаты по вакансии / напрямую (через invitation)
6. После ACCEPT кандидата - контакты; при DECLINE - видна причина отказа

---

## Auth / Me / Consents

| Method | Path | Body |
|--------|------|------|
| GET | `/me` | |
| GET/POST | `/consents` | `{type, granted}` |

Типы: `PERSONAL_DATA_PROCESSING`, `PROFILE_PUBLICATION`

---

## Dictionaries

| GET | `/dictionaries/industries` |
| GET | `/dictionaries/specializations?industryCode=IT` |
| GET | `/dictionaries/grades` |

Грейды для UI теста: **JUNIOR / MIDDLE / SENIOR** (INTERN опционально).

---

## Candidate

| Method | Path | Notes |
|--------|------|-------|
| GET/PUT | `/candidate/profile` | `gradeConfirmed` - официально подтверждён ли грейд |
| POST | `/candidate/publish` | `{published:true}` после согласий + категории |
| POST | `/candidate/resume` | multipart `file` → NLP parse (Ollama/Infereco) |
| GET | `/candidate/fsp` | summary: `fspParticipantId`, `fspGrade`, `totalPoints`, `achievements[]` |
| POST | `/candidate/fsp` | `{fspParticipantId, title?, eventName?, place?, points, fspGrade}` |

`categoryCode` = `IT:BACKEND:JUNIOR` только после теста. Резюме категорию не ставит.

### ФСП в поиске (обязательно для UI)
- Участники с `fspLinked=true` / `fspPoints` / `fspGrade` **выше в выдаче** matching и банка.
- В карточке кандидата показывайте: грейд ФСП, сумму баллов, число достижений.
- В `explain[0]` обычно «Приоритет ФСП…».
- Фильтр банка: `GET /matching/candidates?requireFsp=true`.

---

## Surveys (анкета перед тестом)

| Method | Path |
|--------|------|
| GET | `/surveys?audience=CANDIDATE&industryCode=IT` |
| GET | `/surveys/{id}/questions` |
| POST | `/surveys/sessions` | `{questionnaireCode:"IT_CANDIDATE"}` |
| POST | `/surveys/sessions/{id}/answers` | map code→option |
| POST | `/surveys/sessions/{id}/complete` | |

---

## Testing (A/B/C + антисписывание)

| Method | Path | Body |
|--------|------|------|
| POST | `/tests/sessions` | `{targetGrade:"SENIOR"}` |
| GET | `/tests/sessions/{id}` | items без `expected` |
| POST | `/tests/sessions/{id}/submit` | map itemCode→answer |
| POST | `/tests/sessions/{id}/proctor` | `{event, detail}` |

### Секции в item
- `A` теория
- `B` практика
- `C` олимпиадная практика

Поля item для UI:
```json
{
  "itemId": "...",
  "code": "BE_JUN_SUM_PARAM",
  "section": "A",
  "gradeLevel": "JUNIOR",
  "prompt": "...",
  "antiAiPrompt": "...",
  "captchaStyle": true,
  "copyPasteBlocked": true,
  "options": [{"code":"12","label":"12"}],
  "params": {"a": 5, "b": 7}
}
```

### Proctor (обязательно на фронте)
- `document.addEventListener('visibilitychange')` / `window.blur` →  
  `POST .../proctor` с `event: "TAB_HIDDEN"` или `"BLUR"` / `"LEAVE_WINDOW"`
- Запрет copy/cut/select на контейнере теста (`user-select: none`, block clipboard)
- При `failReason=LEFT_WINDOW` показать:  
  **«Вы ушли со вкладки теста. Попытка засчитана как провал.»**
- Пересдача грейда: **не чаще 1 раза в 30 дней** (`409 JS_007`)

### Логика грейда (бэк)
- Если на SENIOR/MIDDLE сдал менее 50% «джун-пола» (секция A / JUNIOR-задания) → авто **JUNIOR**, `gradeConfirmed=false`
- Если overall ≥ 0.6 → целевой грейд, `gradeConfirmed=true`
- Иначе → грейд ниже целевого, неподтверждённый

Параметрические задачи: числа `{{a}}/{{b}}/{{n}}` разные на каждую попытку (seed).

Идея на плюс (ещё не API): webcam presence (документ/флаг в UI), анализ взгляда.

---

## Employer / Needs / Matching

| Method | Path |
|--------|------|
| GET/PUT | `/employer/profile` |
| GET/POST | `/employer/needs` | salaryFrom/To обязательны |
| GET | `/matching/needs/{needId}?stack=` | candidates + explain; поля `fspPoints`, `fspGrade`, `fspLinked` |
| GET | `/matching/candidates?...&requireFsp=` | банк: `matchScore`, `fspPoints`, `fspGrade`, `fspAchievementsCount`, `fspLinked` |

---

## Invitations (ЗП до чата)

| Method | Path | Body |
|--------|------|------|
| POST | `/invitations` | `candidateId, needId?, vacancyId?, message, salaryFrom, salaryTo` |
| GET | `/invitations` | свои |
| POST | `/invitations/{id}/status` | `{status:"ACCEPTED"\|"DECLINED"\|"VIEWED", declineReason?}` |

- `DECLINED` без `declineReason` → 400
- Ответ содержит `chatThreadId`
- `candidateEmail/Phone` только при `ACCEPTED`

---

## Chats

| Method | Path | Body |
|--------|------|------|
| GET | `/chats` | threads (ЗП, статус, contactsRevealed) |
| GET | `/chats/{threadId}/messages` | |
| POST | `/chats/{threadId}/messages` | `{body}` |

Чат создаётся вместе с приглашением (прямым или по вакансии).  
Контакты в thread: `contactsRevealed=true` только после ACCEPT.

---

## Vacancies

| Method | Path |
|--------|------|
| GET | `/vacancies` | опубликованные |
| GET | `/vacancies/mine` | работодателя |
| POST | `/vacancies` | create |
| POST | `/vacancies/{id}/applications` | `{coverLetter}` |
| GET | `/vacancies/applications/mine` | |
| GET | `/vacancies/{id}/applications` | работодатель |

---

## Ошибки

```json
{ "code": "JS_003", "message": "...", "timestamp": "..." }
```

Важные: `JS_006` согласие, `JS_007` cooldown 30 дней, `JS_008` bad state (LEFT_WINDOW / нет ЗП / нет причины отказа).

---

## Frontend checklist (антисписывание)

1. `user-select: none` + block copy/paste на экране теста
2. На blur/hidden сразу `proctor` и показать fail-сообщение
3. Не рендерить правильные ответы (бэк их не отдаёт)
4. Показывать секции A/B/C и antiAiPrompt под заданием
5. Для captchaStyle - стилизовать «рукописный» вид текста (CSS), без OCR-friendly шрифтов
