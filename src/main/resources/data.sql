-- Seed data for local development / evaluation.
-- Passwords below are BCrypt hashes of the plaintext values documented in the README:
--   admin  / admin123  (ROLE_ADMIN)
--   user1  / user123   (ROLE_USER)
--   user2  / user123   (ROLE_USER)
--
-- Uses INSERT ... WHERE NOT EXISTS style guards so this file is safe to run
-- repeatedly (e.g. on every application restart with ddl-auto=update).

INSERT INTO users (username, email, password, role, created_at)
SELECT 'admin', 'admin@booking.local', '$2b$10$gkxwl2JGU/L0rBMrq8z.kOJPHXgNXxCi4/tKJPq8qy.91RH.jYKHe', 'ADMIN', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');

INSERT INTO users (username, email, password, role, created_at)
SELECT 'user1', 'user1@booking.local', '$2b$10$58n98K2jxgY7C5x8Hm5VkubwelfqNXrEEZaKDsqF.KVQHRgPEvOS2', 'USER', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'user1');

INSERT INTO users (username, email, password, role, created_at)
SELECT 'user2', 'user2@booking.local', '$2b$10$5VIo/A8/j1oWkABFjBOI0u5ag0cHQescC/4PqWtPD4T08C56Ctd86', 'USER', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'user2');

INSERT INTO resources (name, description, type, location, available, created_at, updated_at)
SELECT 'Conference Room A', 'Large conference room with projector and whiteboard, seats 12', 'ROOM', 'Building 1, Floor 2', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM resources WHERE name = 'Conference Room A');

INSERT INTO resources (name, description, type, location, available, created_at, updated_at)
SELECT 'Conference Room B', 'Small meeting room, seats 4, video conferencing setup', 'ROOM', 'Building 1, Floor 2', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM resources WHERE name = 'Conference Room B');

INSERT INTO resources (name, description, type, location, available, created_at, updated_at)
SELECT 'Company Sedan #1', 'Toyota Camry, 5 seats, GPS included', 'VEHICLE', 'Parking Garage, Level 1', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM resources WHERE name = 'Company Sedan #1');

INSERT INTO resources (name, description, type, location, available, created_at, updated_at)
SELECT 'Delivery Van', 'Ford Transit cargo van', 'VEHICLE', 'Parking Garage, Level 1', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM resources WHERE name = 'Delivery Van');

INSERT INTO resources (name, description, type, location, available, created_at, updated_at)
SELECT '4K Projector', 'Portable Epson 4K projector with HDMI/USB-C', 'EQUIPMENT', 'Storage Room 3', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM resources WHERE name = '4K Projector');

INSERT INTO resources (name, description, type, location, available, created_at, updated_at)
SELECT 'DSLR Camera Kit', 'Canon EOS R6 with two lenses and tripod', 'EQUIPMENT', 'Storage Room 1', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM resources WHERE name = 'DSLR Camera Kit');
