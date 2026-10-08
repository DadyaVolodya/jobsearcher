ALTER TABLE candidate_profile
    ADD COLUMN IF NOT EXISTS fsp_grade VARCHAR(32);

ALTER TABLE fsp_achievement
    ADD COLUMN IF NOT EXISTS grade_hint VARCHAR(32);
