
INSERT INTO roles (name) VALUES ('ROLE_USER') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO roles (name) VALUES ('ROLE_ADMIN') ON DUPLICATE KEY UPDATE name=name;


INSERT INTO users (username, password, email, enabled, created_date, last_modified_date, created_by, last_modified_by)
VALUES ('admin', '$2a$12$m9SuI5DD4koKcE/Ov/CE5..Uik5LUDcQnqa/9uRVgVFDsWTlqrwuW', 'admin@example.com', TRUE, NOW(), NOW(), 'MIGRATION', 'MIGRATION')
    ON DUPLICATE KEY UPDATE username=username; -- У випадку, якщо користувач вже існує (малоймовірно для міграції)

INSERT INTO users (username, password, email, enabled, created_date, last_modified_date, created_by, last_modified_by)
VALUES ('user', '$2a$12$m9SuI5DD4koKcE/Ov/CE5..Uik5LUDcQnqa/9uRVgVFDsWTlqrwuW', 'user@example.com', TRUE, NOW(), NOW(), 'MIGRATION', 'MIGRATION')
    ON DUPLICATE KEY UPDATE username=username;

INSERT INTO users_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'admin' AND r.name = 'ROLE_ADMIN'
    ON DUPLICATE KEY UPDATE user_id=user_id; -- Не робити нічого, якщо зв'язок вже є

INSERT INTO users_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'admin' AND r.name = 'ROLE_USER' -- Адмін також може бути юзером
    ON DUPLICATE KEY UPDATE user_id=user_id;

INSERT INTO users_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'user' AND r.name = 'ROLE_USER'
    ON DUPLICATE KEY UPDATE user_id=user_id;