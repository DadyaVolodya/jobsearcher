# Процедура валидации решения

## Датасет

Синтетические кандидаты и потребности (seed Flyway + интеграционные тесты):

- 2+ кандидата (BACKEND/FRONTEND)
- 2+ работодателя с потребностями
- Пары need–candidate с известной релевантностью

## Метрики

1. **Категоризация**: после опроса+теста `categoryCode` соответствует выбранной специализации и результату грейда.
2. **Matching top-N**: доля релевантных (та же specialization+grade) в топе выдачи.
3. **Visibility**: до ACCEPTED email/phone = null; после — заполнены.
4. **Isolation**: работодатель A не читает need B; кандидат не меняет чужое приглашение.
5. **Анти-leak**: разные attemptId дают разные seed/порядок items.

## Автопроверки

```bash
cd backend && ./gradlew test
```

Unit: scoring, matching, assembler, grade policy, consents, LLM failover.  
Integration: `MultiActorFlowIntegrationTest`.
