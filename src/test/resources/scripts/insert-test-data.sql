
-- Insert roles if they don't exist
INSERT INTO roles (id, name) VALUES (1, 'ROLE_USER') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO roles (id, name) VALUES (2, 'ROLE_ADMIN') ON DUPLICATE KEY UPDATE name=name;

-- Insert a test admin user
INSERT INTO users (id, username, password, email, enabled, created_date, last_modified_date, created_by, last_modified_by)
VALUES (1, 'admin', '$2a$12$m9SuI5DD4koKcE/Ov/CE5..Uik5LUDcQnqa/9uRVgVFDsWTlqrwuW', 'admin@test.com', TRUE, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 'TEST_MIGRATION', 'TEST_MIGRATION')
    ON DUPLICATE KEY UPDATE username=username;

-- Insert a test user
INSERT INTO users (id, username, password, email, enabled, created_date, last_modified_date, created_by, last_modified_by)
VALUES (2, 'user', '$2a$12$m9SuI5DD4koKcE/Ov/CE5..Uik5LUDcQnqa/9uRVgVFDsWTlqrwuW', 'user@test.com', TRUE, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 'TEST_MIGRATION', 'TEST_MIGRATION')
    ON DUPLICATE KEY UPDATE username=username;

-- Link roles to users
INSERT INTO users_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'admin' AND r.name = 'ROLE_ADMIN'
    ON DUPLICATE KEY UPDATE user_id=user_id;

INSERT INTO users_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'admin' AND r.name = 'ROLE_USER'
    ON DUPLICATE KEY UPDATE user_id=user_id;

INSERT INTO users_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'user' AND r.name = 'ROLE_USER'
    ON DUPLICATE KEY UPDATE user_id=user_id;

-- Insert a test venue
INSERT INTO venues (id, name, address, capacity, created_date, last_modified_date, created_by, last_modified_by)
VALUES (1, 'Test Venue', '123 Test St', 100, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), 'TEST_MIGRATION', 'TEST_MIGRATION')
    ON DUPLICATE KEY UPDATE name=name;