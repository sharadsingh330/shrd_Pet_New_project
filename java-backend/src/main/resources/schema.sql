-- =====================================================================
-- Pawlift MySQL 8 schema
-- Run:  mysql -u root -p < src/main/resources/schema.sql
-- IDs are UUID strings (CHAR(36)) so they match the React frontend.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS pawlift CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE pawlift;

-- ---------------------------------------------------------------- users
CREATE TABLE IF NOT EXISTS users (
    id            CHAR(36)     NOT NULL,
    name          VARCHAR(120) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,              -- BCrypt hash, never plain text
    role          ENUM('admin','shelter','adopter') NOT NULL DEFAULT 'adopter',
    phone         VARCHAR(40)  NULL,
    city          VARCHAR(120) NULL,
    bio           VARCHAR(600) NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_email (email),
    KEY idx_users_role (role)
) ENGINE=InnoDB;

-- ------------------------------------------------------------- shelters
CREATE TABLE IF NOT EXISTS shelters (
    id         CHAR(36)     NOT NULL,
    owner_id   CHAR(36)     NOT NULL,
    name       VARCHAR(160) NOT NULL,
    city       VARCHAR(120) NOT NULL DEFAULT '',
    email      VARCHAR(255) NULL,
    verified   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_shelters_owner (owner_id),          -- one shelter per shelter user
    CONSTRAINT fk_shelters_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ----------------------------------------------------------------- pets
CREATE TABLE IF NOT EXISTS pets (
    id              CHAR(36)     NOT NULL,
    shelter_id      CHAR(36)     NOT NULL,
    name            VARCHAR(60)  NOT NULL,
    species         VARCHAR(40)  NOT NULL,
    breed           VARCHAR(80)  NOT NULL DEFAULT '',
    age_label       VARCHAR(30)  NOT NULL DEFAULT '',
    size            VARCHAR(30)  NOT NULL DEFAULT '',
    temperament     VARCHAR(60)  NOT NULL DEFAULT '',
    city            VARCHAR(120) NOT NULL DEFAULT '',
    description     TEXT         NOT NULL,
    image_url       TEXT         NULL,
    image_key       VARCHAR(30)  NOT NULL DEFAULT 'dog',
    match_score     INT          NOT NULL DEFAULT 80,
    adoption_status ENUM('available','pending','adopted')  NOT NULL DEFAULT 'available',
    listing_status  ENUM('pending','approved','rejected')  NOT NULL DEFAULT 'pending',
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_pets_shelter (shelter_id),
    KEY idx_pets_listing (listing_status),
    KEY idx_pets_species (species),
    CONSTRAINT fk_pets_shelter FOREIGN KEY (shelter_id) REFERENCES shelters (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------- adoption_applications
CREATE TABLE IF NOT EXISTS adoption_applications (
    id         CHAR(36)     NOT NULL,
    pet_id     CHAR(36)     NOT NULL,
    adopter_id CHAR(36)     NOT NULL,
    housing    VARCHAR(120) NOT NULL,
    experience VARCHAR(200) NOT NULL DEFAULT '',
    note       VARCHAR(800) NOT NULL DEFAULT '',
    status     ENUM('pending','approved','rejected') NOT NULL DEFAULT 'pending',
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_app_pet_adopter (pet_id, adopter_id), -- one application per pet per adopter
    KEY idx_app_adopter (adopter_id),
    CONSTRAINT fk_app_pet     FOREIGN KEY (pet_id)     REFERENCES pets (id)  ON DELETE CASCADE,
    CONSTRAINT fk_app_adopter FOREIGN KEY (adopter_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------- messages
CREATE TABLE IF NOT EXISTS messages (
    id             CHAR(36)      NOT NULL,
    application_id CHAR(36)      NOT NULL,
    sender_id      CHAR(36)      NOT NULL,
    body           VARCHAR(1000) NOT NULL,
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_messages_app (application_id, created_at),
    CONSTRAINT fk_msg_app    FOREIGN KEY (application_id) REFERENCES adoption_applications (id) ON DELETE CASCADE,
    CONSTRAINT fk_msg_sender FOREIGN KEY (sender_id)      REFERENCES users (id)                 ON DELETE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------ system_settings
CREATE TABLE IF NOT EXISTS system_settings (
    setting_key   VARCHAR(60)  NOT NULL,
    setting_value VARCHAR(200) NOT NULL DEFAULT '',
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (setting_key)
) ENGINE=InnoDB;

INSERT IGNORE INTO system_settings (setting_key, setting_value) VALUES
    ('platform_name',         'Pawlift'),
    ('applications_open',     'true'),
    ('auto_approve_listings', 'true'),
    ('support_email',         'hello@pawlift.app');

-- To create the first admin: register normally, then run
--   UPDATE users SET role = 'admin' WHERE email = 'you@example.com';
