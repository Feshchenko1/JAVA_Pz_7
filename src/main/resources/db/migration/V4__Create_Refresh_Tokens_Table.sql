CREATE TABLE refresh_tokens (
                                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                user_id BIGINT NOT NULL UNIQUE,
                                token VARCHAR(255) NOT NULL UNIQUE,
                                expiry_date DATETIME(6) NOT NULL,
                                CONSTRAINT fk_user_refresh_token FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
