INSERT INTO venues (name, address, capacity, created_date, last_modified_date, created_by, last_modified_by)
VALUES ('Test Venue', '123 Test St', 100, NOW(), NOW(), 'TEST_MIGRATION', 'TEST_MIGRATION')
    ON DUPLICATE KEY UPDATE name=name;
