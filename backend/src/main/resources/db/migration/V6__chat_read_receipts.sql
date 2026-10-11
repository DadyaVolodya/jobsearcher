ALTER TABLE chat_thread
    ADD COLUMN IF NOT EXISTS candidate_last_read_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS employer_last_read_at TIMESTAMPTZ;
