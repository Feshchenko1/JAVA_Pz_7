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

ALTER TABLE events
    ADD CONSTRAINT FK_EVENTS_ON_VENUE FOREIGN KEY (venue_id) REFERENCES venues (id);