-- Demo catalog for local UI + consent document version labels.
-- Replaces auto-created demo accounts (candidate1/employer1@example.com) with rich seed data.

DO $$
DECLARE
  emails TEXT[] := ARRAY[
    'candidate1@example.com','candidate2@example.com','candidate3@example.com',
    'employer1@example.com','employer2@example.com'
  ];
BEGIN
  DELETE FROM chat_message WHERE thread_id IN (
    SELECT ct.id FROM chat_thread ct
    JOIN invitation i ON i.id = ct.invitation_id
    JOIN candidate_profile cp ON cp.id = i.candidate_id
    JOIN app_user u ON u.id = cp.user_id
    WHERE u.email = ANY (emails)
  );
  DELETE FROM chat_thread WHERE invitation_id IN (
    SELECT i.id FROM invitation i
    JOIN candidate_profile cp ON cp.id = i.candidate_id
    JOIN app_user u ON u.id = cp.user_id
    WHERE u.email = ANY (emails)
  );
  DELETE FROM invitation WHERE candidate_id IN (
    SELECT cp.id FROM candidate_profile cp JOIN app_user u ON u.id = cp.user_id WHERE u.email = ANY (emails)
  ) OR employer_id IN (
    SELECT ep.id FROM employer_profile ep JOIN app_user u ON u.id = ep.user_id WHERE u.email = ANY (emails)
  );
  DELETE FROM vacancy_application WHERE candidate_id IN (
    SELECT cp.id FROM candidate_profile cp JOIN app_user u ON u.id = cp.user_id WHERE u.email = ANY (emails)
  ) OR vacancy_id IN (
    SELECT v.id FROM vacancy v
    JOIN employer_profile ep ON ep.id = v.employer_id
    JOIN app_user u ON u.id = ep.user_id
    WHERE u.email = ANY (emails)
  );
  DELETE FROM vacancy WHERE employer_id IN (
    SELECT ep.id FROM employer_profile ep JOIN app_user u ON u.id = ep.user_id WHERE u.email = ANY (emails)
  );
  DELETE FROM employer_need WHERE employer_id IN (
    SELECT ep.id FROM employer_profile ep JOIN app_user u ON u.id = ep.user_id WHERE u.email = ANY (emails)
  );
  DELETE FROM fsp_achievement WHERE candidate_id IN (
    SELECT cp.id FROM candidate_profile cp JOIN app_user u ON u.id = cp.user_id WHERE u.email = ANY (emails)
  );
  DELETE FROM grade_change_log WHERE candidate_id IN (
    SELECT cp.id FROM candidate_profile cp JOIN app_user u ON u.id = cp.user_id WHERE u.email = ANY (emails)
  );
  DELETE FROM test_session WHERE candidate_id IN (
    SELECT cp.id FROM candidate_profile cp JOIN app_user u ON u.id = cp.user_id WHERE u.email = ANY (emails)
  );
  DELETE FROM survey_session WHERE user_id IN (SELECT id FROM app_user WHERE email = ANY (emails));
  DELETE FROM user_consent WHERE user_id IN (SELECT id FROM app_user WHERE email = ANY (emails));
  DELETE FROM candidate_profile WHERE user_id IN (SELECT id FROM app_user WHERE email = ANY (emails));
  DELETE FROM employer_profile WHERE user_id IN (SELECT id FROM app_user WHERE email = ANY (emails));
  DELETE FROM app_user WHERE email = ANY (emails);
END $$;

INSERT INTO app_user (id, keycloak_sub, email, role, email_verified, created_at, updated_at) VALUES
('a1000001-0000-4000-8000-000000000001', 'dev-candidate1@example.com', 'candidate1@example.com', 'CANDIDATE', TRUE, NOW(), NOW()),
('a1000001-0000-4000-8000-000000000002', 'dev-candidate2@example.com', 'candidate2@example.com', 'CANDIDATE', TRUE, NOW(), NOW()),
('a1000001-0000-4000-8000-000000000003', 'dev-candidate3@example.com', 'candidate3@example.com', 'CANDIDATE', TRUE, NOW(), NOW()),
('a1000001-0000-4000-8000-000000000011', 'dev-employer1@example.com', 'employer1@example.com', 'EMPLOYER', TRUE, NOW(), NOW()),
('a1000001-0000-4000-8000-000000000012', 'dev-employer2@example.com', 'employer2@example.com', 'EMPLOYER', TRUE, NOW(), NOW());

INSERT INTO user_consent (id, user_id, consent_type, granted, granted_at, version_label) VALUES
('c1000001-0000-4000-8000-000000000001', 'a1000001-0000-4000-8000-000000000001', 'PERSONAL_DATA_PROCESSING', TRUE, NOW(), 'v1'),
('c1000001-0000-4000-8000-000000000002', 'a1000001-0000-4000-8000-000000000001', 'PROFILE_PUBLICATION', TRUE, NOW(), 'v1'),
('c1000001-0000-4000-8000-000000000003', 'a1000001-0000-4000-8000-000000000002', 'PERSONAL_DATA_PROCESSING', TRUE, NOW(), 'v1'),
('c1000001-0000-4000-8000-000000000004', 'a1000001-0000-4000-8000-000000000002', 'PROFILE_PUBLICATION', TRUE, NOW(), 'v1'),
('c1000001-0000-4000-8000-000000000005', 'a1000001-0000-4000-8000-000000000003', 'PERSONAL_DATA_PROCESSING', TRUE, NOW(), 'v1'),
('c1000001-0000-4000-8000-000000000006', 'a1000001-0000-4000-8000-000000000003', 'PROFILE_PUBLICATION', TRUE, NOW(), 'v1');

INSERT INTO candidate_profile (
  id, user_id, full_name, phone, city, about, stack_json, experience_years, soft_skills_json, resume_text,
  industry_code, specialization_code, claimed_grade, assigned_grade, category_code,
  profile_score, test_score, survey_vector_json, fsp_participant_id, fsp_grade,
  privacy_hide_contacts, published, grade_confirmed, created_at, updated_at
) VALUES
('b1000001-0000-4000-8000-000000000001', 'a1000001-0000-4000-8000-000000000001',
 'Анна Ковалёва', '+79991112201', 'Москва',
 'Backend-разработчик, участник ФСП. Люблю чистые API и PostgreSQL.',
 '["Java","Spring","PostgreSQL","Kafka"]'::jsonb, 2.5,
 '["Коммуникация","Ответственность"]'::jsonb,
 'Java/Spring, REST, SQL. Хакатоны ФСП.',
 'IT', 'BACKEND', 'JUNIOR', 'JUNIOR', 'IT:BACKEND:JUNIOR',
 0.78, 0.82,
 '{"architecture":0.7,"databases":0.85,"stack_affinity":1.0,"tests":0.7}'::jsonb,
 'FSP-1001', 'MIDDLE', TRUE, TRUE, TRUE, NOW(), NOW()),
('b1000001-0000-4000-8000-000000000002', 'a1000001-0000-4000-8000-000000000002',
 'Борис Орлов', '+79991112202', 'Санкт-Петербург',
 'Frontend-инженер, React и дизайн-системы.',
 '["TypeScript","React","Vite","CSS"]'::jsonb, 3.0,
 '["Внимание к деталям"]'::jsonb,
 'SPA, a11y, performance.',
 'IT', 'FRONTEND', 'MIDDLE', 'MIDDLE', 'IT:FRONTEND:MIDDLE',
 0.74, 0.76,
 '{"architecture":0.55,"databases":0.3,"stack_affinity":0.9,"tests":0.6}'::jsonb,
 NULL, NULL, TRUE, TRUE, TRUE, NOW(), NOW()),
('b1000001-0000-4000-8000-000000000003', 'a1000001-0000-4000-8000-000000000003',
 'Кира Семёнова', '+79991112203', 'Казань',
 'Senior backend, олимпиадный бэкграунд ФСП — выше в выдаче.',
 '["Java","Go","PostgreSQL","Kubernetes"]'::jsonb, 6.0,
 '["Лидерство","Менторство"]'::jsonb,
 'Highload, design docs, mentoring.',
 'IT', 'BACKEND', 'SENIOR', 'SENIOR', 'IT:BACKEND:SENIOR',
 0.91, 0.88,
 '{"architecture":0.95,"databases":0.9,"stack_affinity":1.0,"tests":0.85}'::jsonb,
 'FSP-9001', 'SENIOR', TRUE, TRUE, TRUE, NOW(), NOW());

INSERT INTO fsp_achievement (id, candidate_id, fsp_participant_id, title, event_name, place, points, grade_hint, achieved_at, verified) VALUES
('d1000001-0000-4000-8000-000000000001', 'b1000001-0000-4000-8000-000000000001', 'FSP-1001', 'Призёр Code Cup', 'ФСП Code Cup', 2, 180, 'MIDDLE', CURRENT_DATE - 90, TRUE),
('d1000001-0000-4000-8000-000000000002', 'b1000001-0000-4000-8000-000000000003', 'FSP-9001', 'Чемпион алгоритмов', 'ФСП Algorithms', 1, 320, 'SENIOR', CURRENT_DATE - 60, TRUE),
('d1000001-0000-4000-8000-000000000003', 'b1000001-0000-4000-8000-000000000003', 'FSP-9001', 'Финал командного турнира', 'ФСП Team Final', 3, 140, 'SENIOR', CURRENT_DATE - 30, TRUE);

INSERT INTO employer_profile (id, user_id, company_name, description, industry_focus, contact_email, contact_phone, website, created_at, updated_at) VALUES
('e1000001-0000-4000-8000-000000000011', 'a1000001-0000-4000-8000-000000000011',
 'TechNova', 'Продуктовая ИТ-компания. Ищем инженеров с подтверждённым грейдом.',
 'IT', 'hr@technova.example', '+74951110011', 'https://technova.example', NOW(), NOW()),
('e1000001-0000-4000-8000-000000000012', 'a1000001-0000-4000-8000-000000000012',
 'CloudPeak', 'Облачная платформа, удалённые команды.',
 'IT', 'jobs@cloudpeak.example', '+74951110022', 'https://cloudpeak.example', NOW(), NOW());

INSERT INTO employer_need (
  id, employer_id, title, description, industry_code, specialization_code, grade,
  stack_json, salary_from, salary_to, need_vector_json, active, created_at, updated_at
) VALUES
('f1000001-0000-4000-8000-000000000001', 'e1000001-0000-4000-8000-000000000011',
 'Backend Java в платформу подбора',
 'Нужен разработчик API, PostgreSQL, тесты. Важны подтверждённый грейд и достижения ФСП.',
 'IT', 'BACKEND', 'JUNIOR',
 '["Java","Spring","PostgreSQL"]'::jsonb, 140000, 200000,
 '{"architecture":0.75,"databases":0.85,"stack_affinity":1.0,"tests":0.7}'::jsonb,
 TRUE, NOW(), NOW()),
('f1000001-0000-4000-8000-000000000002', 'e1000001-0000-4000-8000-000000000011',
 'Frontend Middle в кабинет работодателя',
 'React/TypeScript, аккуратная вёрстка, работа с дизайн-системой ФСП.',
 'IT', 'FRONTEND', 'MIDDLE',
 '["TypeScript","React"]'::jsonb, 180000, 250000,
 '{"architecture":0.5,"databases":0.2,"stack_affinity":0.95,"tests":0.55}'::jsonb,
 TRUE, NOW(), NOW());

INSERT INTO vacancy (
  id, employer_id, title, description, industry_code, specialization_code, grade,
  stack_json, salary_from, salary_to, status, created_at, updated_at
) VALUES
('aa000001-0000-4000-8000-000000000001', 'e1000001-0000-4000-8000-000000000011',
 'Junior Java Developer',
 'Развитие API платформы ФСП Карьера. Зарплата указана диапазоном от–до.',
 'IT', 'BACKEND', 'JUNIOR', '["Java","Spring","SQL"]'::jsonb,
 130000, 180000, 'PUBLISHED', NOW(), NOW()),
('aa000001-0000-4000-8000-000000000002', 'e1000001-0000-4000-8000-000000000011',
 'Middle React Engineer',
 'Кабинеты кандидата и работодателя, фирменный стиль ФСП.',
 'IT', 'FRONTEND', 'MIDDLE', '["TypeScript","React","Vite"]'::jsonb,
 180000, 240000, 'PUBLISHED', NOW(), NOW()),
('aa000001-0000-4000-8000-000000000003', 'e1000001-0000-4000-8000-000000000012',
 'Senior Backend (highload)',
 'Проектирование сервисов, менторство, олимпиадный бэкграунд приветствуется.',
 'IT', 'BACKEND', 'SENIOR', '["Java","Go","PostgreSQL","K8s"]'::jsonb,
 280000, 400000, 'PUBLISHED', NOW(), NOW()),
('aa000001-0000-4000-8000-000000000004', 'e1000001-0000-4000-8000-000000000012',
 'QA Automation Junior',
 'Автотесты API и UI, работа в продуктовой команде.',
 'IT', 'QA', 'JUNIOR', '["Java","Selenium","Postman"]'::jsonb,
 110000, 160000, 'PUBLISHED', NOW(), NOW());
