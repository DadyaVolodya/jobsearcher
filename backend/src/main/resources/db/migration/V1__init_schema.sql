CREATE TABLE app_user (
    id              UUID PRIMARY KEY,
    keycloak_sub    VARCHAR(128) NOT NULL UNIQUE,
    email           VARCHAR(320) NOT NULL UNIQUE,
    role            VARCHAR(32)  NOT NULL,
    email_verified  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE user_consent (
    id              UUID PRIMARY KEY,
    user_id         UUID         NOT NULL REFERENCES app_user(id),
    consent_type    VARCHAR(64)  NOT NULL,
    granted         BOOLEAN      NOT NULL,
    granted_at      TIMESTAMPTZ,
    revoked_at      TIMESTAMPTZ,
    version_label   VARCHAR(64)  NOT NULL DEFAULT 'v1',
    UNIQUE (user_id, consent_type)
);

CREATE TABLE industry (
    code        VARCHAR(64) PRIMARY KEY,
    name        VARCHAR(128) NOT NULL,
    sort_order  INT NOT NULL DEFAULT 0
);

CREATE TABLE specialization (
    code            VARCHAR(64) PRIMARY KEY,
    industry_code   VARCHAR(64) NOT NULL REFERENCES industry(code),
    name            VARCHAR(128) NOT NULL,
    sort_order      INT NOT NULL DEFAULT 0
);

CREATE TABLE candidate_profile (
    id                  UUID PRIMARY KEY,
    user_id             UUID NOT NULL UNIQUE REFERENCES app_user(id),
    full_name           VARCHAR(256),
    phone               VARCHAR(64),
    city                VARCHAR(128),
    about               TEXT,
    stack_json          JSONB NOT NULL DEFAULT '[]'::jsonb,
    experience_years    NUMERIC(4,1),
    soft_skills_json    JSONB NOT NULL DEFAULT '[]'::jsonb,
    resume_text         TEXT,
    industry_code       VARCHAR(64) REFERENCES industry(code),
    specialization_code VARCHAR(64) REFERENCES specialization(code),
    claimed_grade       VARCHAR(32),
    assigned_grade      VARCHAR(32),
    category_code       VARCHAR(128),
    profile_score       DOUBLE PRECISION NOT NULL DEFAULT 0,
    test_score          DOUBLE PRECISION NOT NULL DEFAULT 0,
    survey_vector_json  JSONB NOT NULL DEFAULT '{}'::jsonb,
    fsp_participant_id  VARCHAR(128),
    privacy_hide_contacts BOOLEAN NOT NULL DEFAULT TRUE,
    published           BOOLEAN NOT NULL DEFAULT FALSE,
    last_grade_change_at TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE employer_profile (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL UNIQUE REFERENCES app_user(id),
    company_name    VARCHAR(256) NOT NULL,
    description     TEXT,
    industry_focus  VARCHAR(128),
    contact_email   VARCHAR(320),
    contact_phone   VARCHAR(64),
    website         VARCHAR(512),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE employer_need (
    id                  UUID PRIMARY KEY,
    employer_id         UUID NOT NULL REFERENCES employer_profile(id),
    title               VARCHAR(256) NOT NULL,
    description         TEXT,
    industry_code       VARCHAR(64) NOT NULL REFERENCES industry(code),
    specialization_code VARCHAR(64) NOT NULL REFERENCES specialization(code),
    grade               VARCHAR(32) NOT NULL,
    stack_json          JSONB NOT NULL DEFAULT '[]'::jsonb,
    salary_from         INT NOT NULL,
    salary_to           INT NOT NULL,
    need_vector_json    JSONB NOT NULL DEFAULT '{}'::jsonb,
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_salary CHECK (salary_from > 0 AND salary_to >= salary_from)
);

CREATE TABLE questionnaire (
    id              UUID PRIMARY KEY,
    code            VARCHAR(64) NOT NULL UNIQUE,
    title           VARCHAR(256) NOT NULL,
    audience        VARCHAR(32) NOT NULL,
    industry_code   VARCHAR(64) REFERENCES industry(code),
    active          BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE survey_question (
    id                  UUID PRIMARY KEY,
    questionnaire_id    UUID NOT NULL REFERENCES questionnaire(id),
    code                VARCHAR(64) NOT NULL,
    text                TEXT NOT NULL,
    question_type       VARCHAR(32) NOT NULL,
    weight              DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    scale_key           VARCHAR(64) NOT NULL,
    options_json        JSONB NOT NULL DEFAULT '[]'::jsonb,
    sort_order          INT NOT NULL DEFAULT 0,
    UNIQUE (questionnaire_id, code)
);

CREATE TABLE survey_session (
    id                  UUID PRIMARY KEY,
    user_id             UUID NOT NULL REFERENCES app_user(id),
    questionnaire_id    UUID NOT NULL REFERENCES questionnaire(id),
    status              VARCHAR(32) NOT NULL,
    answers_json        JSONB NOT NULL DEFAULT '{}'::jsonb,
    result_vector_json  JSONB NOT NULL DEFAULT '{}'::jsonb,
    started_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at        TIMESTAMPTZ
);

CREATE TABLE test_item (
    id                  UUID PRIMARY KEY,
    code                VARCHAR(64) NOT NULL UNIQUE,
    industry_code       VARCHAR(64) NOT NULL REFERENCES industry(code),
    specialization_code VARCHAR(64) REFERENCES specialization(code),
    grade               VARCHAR(32) NOT NULL,
    question_type       VARCHAR(32) NOT NULL,
    prompt_template     TEXT NOT NULL,
    variants_json       JSONB NOT NULL DEFAULT '[]'::jsonb,
    options_pool_json   JSONB NOT NULL DEFAULT '[]'::jsonb,
    correct_answer_json JSONB,
    rubric              TEXT,
    weight              DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    active              BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE test_session (
    id                  UUID PRIMARY KEY,
    candidate_id        UUID NOT NULL REFERENCES candidate_profile(id),
    target_grade        VARCHAR(32) NOT NULL,
    industry_code       VARCHAR(64) NOT NULL,
    specialization_code VARCHAR(64) NOT NULL,
    seed                BIGINT NOT NULL,
    status              VARCHAR(32) NOT NULL,
    items_json          JSONB NOT NULL DEFAULT '[]'::jsonb,
    answers_json        JSONB NOT NULL DEFAULT '{}'::jsonb,
    score               DOUBLE PRECISION,
    passed              BOOLEAN,
    started_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at        TIMESTAMPTZ
);

CREATE TABLE grade_change_log (
    id              UUID PRIMARY KEY,
    candidate_id    UUID NOT NULL REFERENCES candidate_profile(id),
    from_grade      VARCHAR(32),
    to_grade        VARCHAR(32) NOT NULL,
    reason          VARCHAR(256),
    changed_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE fsp_achievement (
    id                  UUID PRIMARY KEY,
    candidate_id        UUID NOT NULL REFERENCES candidate_profile(id),
    fsp_participant_id  VARCHAR(128),
    title               VARCHAR(256) NOT NULL,
    event_name          VARCHAR(256),
    place               INT,
    points              INT NOT NULL DEFAULT 0,
    achieved_at         DATE,
    verified            BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE invitation (
    id                  UUID PRIMARY KEY,
    employer_id         UUID NOT NULL REFERENCES employer_profile(id),
    candidate_id        UUID NOT NULL REFERENCES candidate_profile(id),
    need_id             UUID REFERENCES employer_need(id),
    message             TEXT NOT NULL,
    salary_from         INT NOT NULL,
    salary_to           INT NOT NULL,
    status              VARCHAR(32) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_inv_salary CHECK (salary_from > 0 AND salary_to >= salary_from)
);

CREATE TABLE vacancy (
    id                  UUID PRIMARY KEY,
    employer_id         UUID NOT NULL REFERENCES employer_profile(id),
    title               VARCHAR(256) NOT NULL,
    description         TEXT NOT NULL,
    industry_code       VARCHAR(64) NOT NULL REFERENCES industry(code),
    specialization_code VARCHAR(64) NOT NULL REFERENCES specialization(code),
    grade               VARCHAR(32) NOT NULL,
    stack_json          JSONB NOT NULL DEFAULT '[]'::jsonb,
    salary_from         INT NOT NULL,
    salary_to           INT NOT NULL,
    status              VARCHAR(32) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_vac_salary CHECK (salary_from > 0 AND salary_to >= salary_from)
);

CREATE TABLE vacancy_application (
    id              UUID PRIMARY KEY,
    vacancy_id      UUID NOT NULL REFERENCES vacancy(id),
    candidate_id    UUID NOT NULL REFERENCES candidate_profile(id),
    cover_letter    TEXT,
    status          VARCHAR(32) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (vacancy_id, candidate_id)
);

CREATE TABLE stored_file (
    id              UUID PRIMARY KEY,
    owner_user_id   UUID NOT NULL REFERENCES app_user(id),
    original_name   VARCHAR(512) NOT NULL,
    content_type    VARCHAR(128),
    storage_key     VARCHAR(1024) NOT NULL,
    size_bytes      BIGINT NOT NULL,
    purpose         VARCHAR(64) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_candidate_category ON candidate_profile(category_code);
CREATE INDEX idx_candidate_spec_grade ON candidate_profile(specialization_code, assigned_grade);
CREATE INDEX idx_invitation_candidate ON invitation(candidate_id);
CREATE INDEX idx_invitation_employer ON invitation(employer_id);
CREATE INDEX idx_vacancy_status ON vacancy(status);
