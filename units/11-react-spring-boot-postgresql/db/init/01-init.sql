-- PostgreSQL Official Image が空の Data Directory を初回初期化するときだけ実行する。
-- Unit 11 は 3 Container 間の通信確認が目的のため、Schema / Data は最小限にする。
CREATE TABLE IF NOT EXISTS study_message (
    id INTEGER PRIMARY KEY,
    message VARCHAR(255) NOT NULL
);

INSERT INTO
    study_message (id, message)
VALUES
    (1, 'Hello from PostgreSQL through Spring Boot') ON CONFLICT (id) DO NOTHING;
