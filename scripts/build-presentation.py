#!/usr/bin/env python3
"""Заполняет обязательные слайды 7–11 шаблона ФСП, сохраняя дизайн."""
from __future__ import annotations

from copy import deepcopy
from pathlib import Path

from pptx import Presentation
from pptx.enum.shapes import MSO_SHAPE_TYPE

SRC = Path("/Users/sparrow/Downloads/ФСП 2/ЛЦТ_2026_Шаблон презентации ФСП.pptx")
OUT = Path(__file__).resolve().parents[1] / "docs" / "presentation" / "FSP_Jobsearcher_presentation.pptx"


def set_text(shape, text: str) -> None:
    if not shape.has_text_frame:
        return
    tf = shape.text_frame
    paragraphs = text.split("\n")
    # keep first paragraph formatting; clear extras
    for i, para in enumerate(tf.paragraphs):
        if i == 0:
            if para.runs:
                para.runs[0].text = paragraphs[0] if paragraphs else ""
                for run in para.runs[1:]:
                    run.text = ""
            else:
                para.text = paragraphs[0] if paragraphs else ""
        else:
            for run in para.runs:
                run.text = ""
            para.text = ""
    # add remaining paragraphs by cloning first paragraph style via text_frame
    first = tf.paragraphs[0]
    for extra in paragraphs[1:]:
        p = tf.add_paragraph()
        p.alignment = first.alignment
        if first.runs:
            # copy font basics
            run = p.add_run()
            run.text = extra
            src = first.runs[0].font
            if src.size:
                run.font.size = src.size
            if src.bold is not None:
                run.font.bold = src.bold
            if src.name:
                run.font.name = src.name
            try:
                if src.color and src.color.rgb:
                    run.font.color.rgb = src.color.rgb
            except Exception:
                pass
        else:
            p.text = extra


def replace_placeholder(shape, mapping: dict[str, str]) -> None:
    if not shape.has_text_frame:
        return
    full = "\n".join(p.text for p in shape.text_frame.paragraphs)
    for old, new in mapping.items():
        if old in full:
            set_text(shape, full.replace(old, new))
            return
    # exact full replace keys
    if full.strip() in mapping:
        set_text(shape, mapping[full.strip()])


def main() -> None:
    prs = Presentation(str(SRC))
    # --- slide 7 (index 6): title ---
    s7 = prs.slides[6]
    for sh in s7.shapes:
        if sh.has_text_frame and "Цифровая платформа" in sh.text_frame.text:
            set_text(
                sh,
                "ФСП Карьера — платформа подбора ИТ-специалистов\n"
                "с подтверждённым грейдом и приоритетом достижений ФСП",
            )
        # title placeholder often empty named Заголовок
        if sh.name.startswith("Заголовок") and (not sh.has_text_frame or not sh.text_frame.text.strip()):
            set_text(sh, "ФСП Карьера")

    # --- slide 8 ---
    s8 = prs.slides[7]
    for sh in s8.shapes:
        if not sh.has_text_frame:
            continue
        t = sh.text_frame.text
        if t.startswith("Капитан:"):
            set_text(
                sh,
                "Капитан: Александр Воробьёв, backend / архитектура\n"
                "Кол-во участников: 2+ (backend + frontend)\n"
                "Краткое описание: команда вокруг продукта «ФСП Карьера»\n"
                "как образовалась команда: хакатон ЛЦТ 2026, спецный трек ФСП\n"
                "место работы/учебы участников: ИТ-разработка\n"
                "Город и регион: Россия",
            )
        elif t.strip() == "В чем суть вашего решения":
            set_text(
                sh,
                "Кандидат проходит анкету и тест, получает категорию и грейд. "
                "Работодатель ищет по подтверждённым навыкам и сам направляет приглашение с зарплатой до начала чата.",
            )
        elif t.strip() == "Что делает ваше решение уникальным или инновационным?":
            set_text(
                sh,
                "Обратная механика подбора: не отклик «вслепую», а адресное приглашение. "
                "Грейд только тестом; достижения ФСП поднимают в выдаче. Контакты скрыты до ACCEPT.",
            )
        # labels stay; content boxes are the two prompts above
        elif sh.name.startswith("Заголовок"):
            set_text(sh, "ФСП Карьера")

    # --- slide 9: team cards ---
    s9 = prs.slides[8]
    members = [
        ("Александр Воробьёв", "Капитан, backend\nGitHub: DadyaVolodya\nvorobev@topcase.ru\nИТ-разработка"),
        ("Frontend-партнёр", "Frontend / UX\nЛК кандидата и работодателя\nбрендбук ФСП\nИТ-разработка"),
        ("QA / демо", "Тесты и стенд\nсценарии жюри\nseed-данные\nИТ"),
        ("Документация", "ТЗ 3.4, презентация\nконтракт API\nDEPLOY / DEMO\nИТ"),
        ("Поддержка демо", "Стенд и доступы\nдемо-аккаунты\nсопровождение показа\nИТ"),
    ]
    name_boxes = []
    role_boxes = []
    for sh in s9.shapes:
        if not sh.has_text_frame:
            continue
        txt = sh.text_frame.text.strip()
        if txt == "Имя Фамилия":
            name_boxes.append(sh)
        elif txt.startswith("Роль в команде"):
            role_boxes.append(sh)
        elif sh.name.startswith("Заголовок"):
            set_text(sh, "Команда «ФСП Карьера»")
    name_boxes.sort(key=lambda s: s.left)
    role_boxes.sort(key=lambda s: s.left)
    for i, (name, role) in enumerate(members):
        if i < len(name_boxes):
            set_text(name_boxes[i], name)
        if i < len(role_boxes):
            set_text(role_boxes[i], role)

    # --- slide 10 ---
    s10 = prs.slides[9]
    for sh in s10.shapes:
        if not sh.has_text_frame:
            continue
        t = sh.text_frame.text
        if "как вы собрались" in t:
            set_text(
                sh,
                "Собрались под спец. трек ФСП на ЛЦТ 2026: backend на Spring, фронт кабинетов по брендбуку. "
                "Цель — рабочий стенд с объяснимым подбором и зарплатой до чата.",
            )
        elif "Почему вы выбрали именно эту задачу" in t:
            set_text(
                sh,
                "Близкая предметная область: тестирование, категории, Fair matching. "
                "Можно показать измеримый эффект достижений ФСП в выдаче, а не только «ещё один job board».",
            )
        elif "Что вас вдохновило" in t:
            set_text(
                sh,
                "Проблема недоверия к самооценке в резюме и непрозрачных откликов. "
                "Хочется, чтобы работодатель видел подтверждённый грейд и спортивную историю ФСП.",
            )
        elif "самых интересных или сложных моментах" in t:
            set_text(
                sh,
                "Антисписывание в тестах, обязательная зарплата в invite без падений API, "
                "152-ФЗ согласия с вложениями, непрочитанные сообщения в сайдбаре. "
                "Решили лимитами зарплаты, read-receipts и seed для демо жюри.",
            )
        elif sh.name.startswith("Заголовок"):
            set_text(sh, "История и вызовы команды")

    # --- slide 11 ---
    s11 = prs.slides[10]
    for sh in s11.shapes:
        if not sh.has_text_frame:
            continue
        t = sh.text_frame.text.strip()
        if t.startswith("Опишите в чем техническая"):
            set_text(
                sh,
                "Java 21 / Spring Boot, PostgreSQL, Flyway, OpenAPI. "
                "Опрос + параметризованный тест A/B/C → categoryCode. "
                "Matching с explain и бустом ФСП. "
                "Приглашения с salary first, чаты, unread badge. "
                "Dev-auth для демо, Keycloak для prod. Docker Compose / Render.",
            )
        elif t.startswith("Опишите ваши идеи по дальнейшему"):
            set_text(
                sh,
                "Боевой реестр ФСП и Keycloak IdP, расширенный банк заданий, "
                "SMTP-уведомления, PDF-профиль, публичный HTTPS-стенд для работодателей-партнёров ФСП, "
                "метрики качества подбора на закрытом датасете жюри.",
            )
        elif t == "КОРОТКО О РЕШЕНИИ":
            pass  # keep

    # Optional content slides 12–14: fill empty numbered slides with key messages if they have placeholders
    content_extra = {
        11: (  # slide 12
            "Архитектура",
            "Монолит со слоями: api / application / domain / infrastructure.\n"
            "PostgreSQL + Flyway V1–V6. LLM: Ollama primary, Infereco fallback.\n"
            "Кабинеты: candidate + employer (Vite/React).",
        ),
        12: (
            "Пользовательский сценарий",
            "1) Кандидат: согласия → профиль → анкета → тест → publish\n"
            "2) Работодатель: потребность → matching → invite с ЗП\n"
            "3) Кандидат: accept → контакты и чат; бейдж непрочитанных",
        ),
        13: (
            "Демо и репозиторий",
            "GitHub: https://github.com/DadyaVolodya/jobsearcher (ветка vorobev)\n"
            "Демо: docker compose -f docker-compose.demo.yml up -d --build\n"
            "Аккаунты: candidate1@ / employer1@ example.com\n"
            "Документация: docs/* + docs/DEMO_CLOUD.md",
        ),
    }
    for idx, (title, body) in content_extra.items():
        if idx >= len(prs.slides):
            break
        slide = prs.slides[idx]
        # try set first empty-ish text boxes
        texts = [sh for sh in slide.shapes if sh.has_text_frame]
        # Prefer large placeholders
        for sh in texts:
            if sh.name.startswith("Заголовок") or (sh.has_text_frame and sh.text_frame.text.strip() in {"", str(idx + 1)}):
                if sh.text_frame.text.strip() == str(idx + 1):
                    continue
                set_text(sh, title)
                break
        # find a large text body placeholder
        for sh in texts:
            if sh == texts[0]:
                continue
            if sh.has_text_frame and len(sh.text_frame.text.strip()) <= 3:
                set_text(sh, body)
                break

    OUT.parent.mkdir(parents=True, exist_ok=True)
    prs.save(str(OUT))
    print(f"Wrote {OUT}")


if __name__ == "__main__":
    main()
