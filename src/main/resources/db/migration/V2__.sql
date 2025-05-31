CREATE TABLE events
(
    id                 BIGINT AUTO_INCREMENT NOT NULL,
    name               VARCHAR(255) NOT NULL,
    event_date         date         NOT NULL,
    venue_id           BIGINT       NOT NULL,
    created_date       datetime     NOT NULL,
    last_modified_date datetime     NOT NULL,
    created_by         VARCHAR(255) NULL,
    last_modified_by   VARCHAR(255) NULL,
    CONSTRAINT pk_events PRIMARY KEY (id)
);

CREATE TABLE roles
(
    id   BIGINT AUTO_INCREMENT NOT NULL,
    name VARCHAR(50) NOT NULL,
    CONSTRAINT pk_roles PRIMARY KEY (id)
);

CREATE TABLE users
(
    id                 BIGINT AUTO_INCREMENT NOT NULL,
    username           VARCHAR(100) NOT NULL,
    password           VARCHAR(255) NOT NULL,
    email              VARCHAR(150) NOT NULL,
    enabled            BIT(1)       NOT NULL,
    created_date       datetime     NOT NULL,
    last_modified_date datetime     NOT NULL,
    created_by         VARCHAR(255) NULL,
    last_modified_by   VARCHAR(255) NULL,
    CONSTRAINT pk_users PRIMARY KEY (id)
);

CREATE TABLE users_roles
(
    role_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    CONSTRAINT pk_users_roles PRIMARY KEY (role_id, user_id)
);

CREATE TABLE venues
(
    id                 BIGINT AUTO_INCREMENT NOT NULL,
    name               VARCHAR(255) NOT NULL,
    address            VARCHAR(500) NOT NULL,
    capacity           INT          NOT NULL,
    created_date       datetime     NOT NULL,
    last_modified_date datetime     NOT NULL,
    created_by         VARCHAR(255) NULL,
    last_modified_by   VARCHAR(255) NULL,
    CONSTRAINT pk_venues PRIMARY KEY (id)
);

ALTER TABLE roles
    ADD CONSTRAINT uc_roles_name UNIQUE (name);

ALTER TABLE users
    ADD CONSTRAINT uc_users_email UNIQUE (email);

ALTER TABLE users
    ADD CONSTRAINT uc_users_username UNIQUE (username);

ALTER TABLE events
    ADD CONSTRAINT FK_EVENTS_ON_VENUE FOREIGN KEY (venue_id) REFERENCES venues (id);

ALTER TABLE users_roles
    ADD CONSTRAINT fk_userol_on_role FOREIGN KEY (role_id) REFERENCES roles (id);

ALTER TABLE users_roles
    ADD CONSTRAINT fk_userol_on_user FOREIGN KEY (user_id) REFERENCES users (id);