-- PostgreSQL Official Image が空の Data Directory を初回初期化するときだけ実行する。
-- Unit 10 は Docker / Compose 学習が目的のため、Schema / Data は最小限にする。
CREATE TABLE IF NOT EXISTS study_message (
    id INTEGER PRIMARY KEY,
    message VARCHAR(255) NOT NULL
);

INSERT INTO
    study_message (id, message)
VALUES
    (1, 'Hello from PostgreSQL') ON CONFLICT (id) DO NOTHING;
