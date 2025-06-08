INSERT INTO roles (name) VALUES ('ROLE_USER');
INSERT INTO roles (name) VALUES ('ROLE_ADMIN');


INSERT INTO users (username, password, email, enabled, created_date, last_modified_date, created_by, last_modified_by)
VALUES ('admin', '$2a$12$m9SuI5DD4koKcE/Ov/CE5..Uik5LUDcQnqa/9uRVgVFDsWTlqrwuW', 'admin@example.com', TRUE, NOW(), NOW(), 'MIGRATION', 'MIGRATION');

INSERT INTO users (username, password, email, enabled, created_date, last_modified_date, created_by, last_modified_by)
VALUES ('user', '$2a$12$m9SuI5DD4koKcE/Ov/CE5..Uik5LUDcQnqa/9uRVgVFDsWTlqrwuW', 'user@example.com', TRUE, NOW(), NOW(), 'MIGRATION', 'MIGRATION');

INSERT INTO users_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'admin' AND r.name = 'ROLE_ADMIN'
  AND NOT EXISTS (
    SELECT 1
    FROM users_roles ur
    WHERE ur.user_id = u.id AND ur.role_id = r.id
);

INSERT INTO users_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'admin' AND r.name = 'ROLE_USER'
  AND NOT EXISTS (
    SELECT 1
    FROM users_roles ur
    WHERE ur.user_id = u.id AND ur.role_id = r.id
);

INSERT INTO users_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'user' AND r.name = 'ROLE_USER'
  AND NOT EXISTS (
    SELECT 1
    FROM users_roles ur
    WHERE ur.user_id = u.id AND ur.role_id = r.id
);