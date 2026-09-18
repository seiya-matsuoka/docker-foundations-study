-- PostgreSQL Data Directory が空の初回初期化時だけ実行する。
-- Unit 12 は Dockerfile / Compose 改善が目的なので Data は最小限にする。
CREATE TABLE IF NOT EXISTS study_message (
    id INTEGER PRIMARY KEY,
    message VARCHAR(255) NOT NULL
);

INSERT INTO
    study_message (id, message)
VALUES
    (1, 'Hello from the improved Unit 12 containers') ON CONFLICT (id) DO NOTHING;
