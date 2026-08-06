-- =============================================================
-- AELB 2.0 - Schema MySQL
-- Gestión de Campeonatos de Lucha de Brazos
-- =============================================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- =============================================================
-- EQUIPOS (sin FK a users para evitar ciclo con created_by)
-- =============================================================
CREATE TABLE teams (
    id                    CHAR(36)     NOT NULL,
    name                  VARCHAR(100) NOT NULL,
    responsible_name      VARCHAR(100),
    responsible_last_name VARCHAR(100),
    phone                 VARCHAR(20),
    province              VARCHAR(100),
    locality              VARCHAR(100),
    logo_data             LONGTEXT,
    logo_type             VARCHAR(50),
    created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_teams       PRIMARY KEY (id),
    CONSTRAINT uq_teams_name  UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================
-- USUARIOS
-- El email NO es editable una vez creado.
-- El DNI es único en todo el sistema.
-- El bloqueo impide login sin borrar datos.
-- =============================================================
CREATE TABLE users (
    id            CHAR(36)     NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    dni           VARCHAR(20)  NOT NULL,
    phone         VARCHAR(20),
    city          VARCHAR(100),
    province      VARCHAR(100),
    nationality   VARCHAR(100),
    birth_date    DATE,
    role          ENUM('USER','GESTOR','ADMIN') NOT NULL DEFAULT 'USER',
    is_blocked    TINYINT(1)   NOT NULL DEFAULT 0,
    team_id       CHAR(36),
    -- Se incrementa al bloquear/desbloquear, cambiar el rol o la contraseña.
    -- Un access token cuyo claim "ver" no coincida se trata como revocado.
    token_version INT          NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_users        PRIMARY KEY (id),
    CONSTRAINT uq_users_email  UNIQUE (email),
    CONSTRAINT uq_users_dni    UNIQUE (dni),
    CONSTRAINT fk_users_team   FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Índices de búsqueda frecuente
CREATE INDEX idx_users_role      ON users (role);
CREATE INDEX idx_users_is_blocked ON users (is_blocked);

-- =============================================================
-- REFRESH TOKENS
-- Solo se guarda el hash SHA-256 del token — el valor en claro únicamente
-- vive en la cookie HttpOnly del cliente. Rotatorio: cada uso revoca el
-- token actual y emite uno nuevo (revoked_at). Reutilizar uno ya revocado
-- indica robo y revoca todas las sesiones del usuario.
-- =============================================================
CREATE TABLE refresh_tokens (
    id          CHAR(36)     NOT NULL,
    user_id     CHAR(36)     NOT NULL,
    token_hash  CHAR(64)     NOT NULL,
    expires_at  DATETIME     NOT NULL,
    revoked_at  DATETIME,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_refresh_tokens        PRIMARY KEY (id),
    CONSTRAINT uq_refresh_tokens_hash   UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user   FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);

-- =============================================================
-- CATEGORÍAS (reutilizables entre campeonatos)
-- En lucha de brazos: género, lado del brazo y clase de peso
-- =============================================================
CREATE TABLE categories (
    id           CHAR(36)     NOT NULL,
    name         VARCHAR(100) NOT NULL,
    gender       ENUM('MALE','FEMALE','OPEN')    NOT NULL,
    arm_side     ENUM('RIGHT','LEFT','BOTH')      NOT NULL DEFAULT 'BOTH',
    weight_limit DECIMAL(5,2),
    age_group    VARCHAR(50), -- campo legado compatible con precios y clientes existentes
    age_category ENUM('SUB_JUNIOR','JUNIOR','YOUTH','AMATEUR','SENIOR','MASTER','GRAND_MASTER','SENIOR_GRAND_MASTER','SUPER_SENIOR_GRAND_MASTER'),
    shift        ENUM('MORNING','AFTERNOON')        NOT NULL DEFAULT 'MORNING',

    CONSTRAINT pk_categories      PRIMARY KEY (id),
    CONSTRAINT uq_categories_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================
-- CAMPEONATOS
-- =============================================================
CREATE TABLE championships (
    id                    CHAR(36)       NOT NULL,
    name                  VARCHAR(200)   NOT NULL,
    location              VARCHAR(200)   NOT NULL,
    event_date            DATE           NOT NULL,
    registration_deadline DATE           NOT NULL,
    price                 DECIMAL(8,2)   NOT NULL DEFAULT 0.00,
    is_visible            TINYINT(1)     NOT NULL DEFAULT 0,
    image_url             VARCHAR(500),
    description           TEXT,
    requires_current_quota TINYINT(1) NOT NULL DEFAULT 0,
    created_by            CHAR(36)       NOT NULL,
    created_at            DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_championships          PRIMARY KEY (id),
    CONSTRAINT fk_championships_creator  FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT ck_registration_before_event CHECK (registration_deadline <= event_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_championships_event_date ON championships (event_date);
CREATE INDEX idx_championships_is_visible ON championships (is_visible);

-- =============================================================
-- CAMPEONATO ↔ CATEGORÍA (pivot)
-- Cada campeonato puede tener su propio precio por categoría,
-- o heredar el precio del campeonato (si override_price es NULL)
-- =============================================================
CREATE TABLE championship_categories (
    championship_id CHAR(36)     NOT NULL,
    category_id     CHAR(36)     NOT NULL,
    override_price  DECIMAL(8,2),

    CONSTRAINT pk_champ_cat             PRIMARY KEY (championship_id, category_id),
    CONSTRAINT fk_champ_cat_champ       FOREIGN KEY (championship_id) REFERENCES championships (id) ON DELETE CASCADE,
    CONSTRAINT fk_champ_cat_category    FOREIGN KEY (category_id)     REFERENCES categories (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================
-- INSCRIPCIONES
-- UNIQUE (user_id, championship_id) → previene doble inscripción.
-- La restricción vive tanto en el dominio como en la BD.
-- =============================================================
CREATE TABLE registrations (
    id              CHAR(36)   NOT NULL,
    user_id         CHAR(36)   NOT NULL,
    championship_id CHAR(36)   NOT NULL,
    category_id     CHAR(36)   NOT NULL,
    amount          DECIMAL(8,2) NOT NULL DEFAULT 0.00,
    payment_status  ENUM('PENDING','PAID','CANCELLED') NOT NULL DEFAULT 'PENDING',
    notes           VARCHAR(500),
    registered_by   CHAR(36)   NOT NULL,   -- quien realizó la inscripción
    created_at      DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_registrations           PRIMARY KEY (id),
    CONSTRAINT fk_registrations_user      FOREIGN KEY (user_id)         REFERENCES users (id),
    CONSTRAINT fk_registrations_champ     FOREIGN KEY (championship_id) REFERENCES championships (id),
    CONSTRAINT fk_registrations_category  FOREIGN KEY (category_id)     REFERENCES categories (id),
    CONSTRAINT fk_registrations_by        FOREIGN KEY (registered_by)   REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_registrations_user         ON registrations (user_id);
CREATE INDEX idx_registrations_championship ON registrations (championship_id);
CREATE INDEX idx_registrations_payment      ON registrations (payment_status);
CREATE UNIQUE INDEX uq_registration_user_championship_category ON registrations (user_id, championship_id, category_id);

-- =============================================================
-- CUOTAS ANUALES
-- UNIQUE (user_id, year) → solo 1 cuota por año y por usuario.
-- =============================================================
CREATE TABLE quotas (
    id             CHAR(36)    NOT NULL,
    user_id        CHAR(36)    NOT NULL,
    year           INT         NOT NULL,
    age_category   ENUM('SUB_JUNIOR','JUNIOR','YOUTH','SENIOR','MASTERS','GRAND_MASTERS','SENIOR_GRAND_MASTERS') NOT NULL,
    amount         DECIMAL(8,2) NOT NULL,
    payment_status ENUM('PENDING','PAID') NOT NULL DEFAULT 'PENDING',
    payment_date   DATE,
    paid_at        DATETIME,
    payment_source ENUM('STRIPE','MANUAL','LEGACY'),
    paid_by_user_id CHAR(36),
    payment_reference VARCHAR(255),
    created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_quotas             PRIMARY KEY (id),
    CONSTRAINT uq_quotas_user_year   UNIQUE (user_id, year),
    CONSTRAINT fk_quotas_user        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_quotas_paid_by      FOREIGN KEY (paid_by_user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_quotas_year        CHECK (year >= 2020 AND year <= 2100),
    CONSTRAINT ck_quotas_amount      CHECK (amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_quotas_user ON quotas (user_id);
CREATE INDEX idx_quotas_payment_source ON quotas (payment_source);

-- Precios anuales por tramo de edad. Editar un precio no modifica cuotas ya creadas.
CREATE TABLE annual_quota_prices (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    year         INT          NOT NULL,
    age_category ENUM('SUB_JUNIOR','JUNIOR','YOUTH','SENIOR','MASTERS','GRAND_MASTERS','SENIOR_GRAND_MASTERS') NOT NULL,
    amount       DECIMAL(8,2) NOT NULL,

    CONSTRAINT pk_annual_quota_prices PRIMARY KEY (id),
    CONSTRAINT uq_annual_quota_price  UNIQUE (year, age_category),
    CONSTRAINT ck_annual_quota_year   CHECK (year >= 2020 AND year <= 2100),
    CONSTRAINT ck_annual_quota_amount CHECK (amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================
-- NOTICIAS
-- CRUD por GESTOR/ADMIN. Slug único para URLs amigables.
-- =============================================================
CREATE TABLE news (
    id           CHAR(36)     NOT NULL,
    title        VARCHAR(300) NOT NULL,
    slug         VARCHAR(320) NOT NULL,
    content      LONGTEXT     NOT NULL,
    image_data   LONGTEXT,              -- legacy base64 (kept for backward compat)
    image_type   VARCHAR(50),           -- legacy MIME type
    image_key    VARCHAR(500),          -- S3/MinIO object key (new uploads)
    is_published TINYINT(1)   NOT NULL DEFAULT 0,
    published_at DATETIME,
    author_id    CHAR(36)     NOT NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_news        PRIMARY KEY (id),
    CONSTRAINT uq_news_slug   UNIQUE (slug),
    CONSTRAINT fk_news_author FOREIGN KEY (author_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- Migration for existing DBs: ALTER TABLE news ADD COLUMN image_key VARCHAR(500);

CREATE INDEX idx_news_is_published ON news (is_published, published_at);

-- =============================================================
-- RESULTADOS (PDFs descargables)
-- El PDF se almacena comprimido (GZip) y codificado en base64.
-- =============================================================
CREATE TABLE results (
    id                CHAR(36)     NOT NULL,
    title             VARCHAR(300) NOT NULL,
    description       TEXT,
    pdf_data          LONGTEXT     NOT NULL,
    pdf_original_size BIGINT       NOT NULL DEFAULT 0,
    pdf_file_name     VARCHAR(255),
    is_published      TINYINT(1)   NOT NULL DEFAULT 0,
    published_at      DATETIME,
    author_id         CHAR(36)     NOT NULL,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_results        PRIMARY KEY (id),
    CONSTRAINT fk_results_author FOREIGN KEY (author_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_results_is_published ON results (is_published, published_at);

-- =============================================================
-- DOCUMENTACIÓN (PDFs y archivos descargables)
-- El archivo se almacena comprimido (GZip) y codificado en base64.
-- =============================================================
CREATE TABLE documents (
    id                 CHAR(36)     NOT NULL,
    title              VARCHAR(300) NOT NULL,
    description        TEXT,
    file_data          LONGTEXT     NOT NULL,
    file_original_size BIGINT       NOT NULL DEFAULT 0,
    file_name          VARCHAR(255),
    is_published       TINYINT(1)   NOT NULL DEFAULT 0,
    published_at       DATETIME,
    author_id          CHAR(36)     NOT NULL,
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_documents        PRIMARY KEY (id),
    CONSTRAINT fk_documents_author FOREIGN KEY (author_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_documents_is_published ON documents (is_published, published_at);

-- =============================================================
-- PRICING: Configuración de precios por campeonato
-- =============================================================
CREATE TABLE pricing_configs (
    id                    CHAR(36)     NOT NULL,
    championship_id       CHAR(36)     NOT NULL,
    max_categories_per_arm INT         NOT NULL DEFAULT 2,
    max_total_entries     INT          NOT NULL DEFAULT 4,
    currency              VARCHAR(3)   NOT NULL DEFAULT 'EUR',
    active                TINYINT(1)   NOT NULL DEFAULT 1,
    created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_pricing_configs   PRIMARY KEY (id),
    CONSTRAINT uq_pricing_champ     UNIQUE (championship_id),
    CONSTRAINT fk_pricing_champ     FOREIGN KEY (championship_id) REFERENCES championships (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Reglas de precio configurables (condiciones y efectos almacenados como JSON en TEXT)
CREATE TABLE pricing_rules (
    id                CHAR(36)     NOT NULL,
    pricing_config_id CHAR(36)     NOT NULL,
    name              VARCHAR(200) NOT NULL,
    description       TEXT,
    priority          INT          NOT NULL DEFAULT 100,
    active            TINYINT(1)   NOT NULL DEFAULT 1,
    conditions_json   TEXT         NOT NULL COMMENT '[{"type":"AGE_MAX","value":"15"},...]',
    effect_json       TEXT         NOT NULL COMMENT '{"type":"BUNDLE_FIXED_TOTAL","amount":"10.00","description":"..."}',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_pricing_rules      PRIMARY KEY (id),
    CONSTRAINT fk_pricing_rules_conf FOREIGN KEY (pricing_config_id) REFERENCES pricing_configs (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_pricing_rules_config   ON pricing_rules (pricing_config_id, priority);
CREATE INDEX idx_pricing_rules_active   ON pricing_rules (active);

-- Precios WAF por campeonato. El precio general del campeonato queda solo como legado.
CREATE TABLE championship_age_category_prices (
    id                BIGINT NOT NULL AUTO_INCREMENT,
    championship_id   CHAR(36) NOT NULL,
    age_category      ENUM('SUB_JUNIOR','JUNIOR','YOUTH','AMATEUR','SENIOR','MASTER','GRAND_MASTER','SENIOR_GRAND_MASTER','SUPER_SENIOR_GRAND_MASTER') NOT NULL,
    price_per_arm     DECIMAL(8,2) NOT NULL,
    combination_price DECIMAL(8,2) NOT NULL DEFAULT 10.00,
    CONSTRAINT pk_championship_age_category_prices PRIMARY KEY (id),
    CONSTRAINT uq_championship_age_category_price UNIQUE (championship_id, age_category),
    CONSTRAINT fk_championship_age_category_price_championship FOREIGN KEY (championship_id) REFERENCES championships (id) ON DELETE CASCADE,
    CONSTRAINT ck_championship_age_category_price_per_arm CHECK (price_per_arm >= 0),
    CONSTRAINT ck_championship_age_category_price_combination CHECK (combination_price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Checkouts de pago: ciclo de vida PENDING_PAYMENT → PAID / EXPIRED / CANCELLED
CREATE TABLE registration_checkouts (
    id                CHAR(36)     NOT NULL,
    user_id           CHAR(36)     NOT NULL,
    championship_id   CHAR(36)     NOT NULL,
    line_items_json   TEXT         NOT NULL COMMENT '[{"concept":"...","amount":"20.00","ruleId":"..."}]',
    total             DECIMAL(8,2) NOT NULL,
    currency          VARCHAR(3)   NOT NULL DEFAULT 'EUR',
    metadata_json     TEXT         COMMENT '{"userId":"...","categoryTypes":"SENIOR,MASTER",...}',
    basket_key        CHAR(64)     NULL COMMENT 'Hash de la cesta pendiente; evita Checkout concurrentes',
    status            ENUM('PENDING_PAYMENT','PAID','EXPIRED','CANCELLED') NOT NULL DEFAULT 'PENDING_PAYMENT',
    stripe_session_id VARCHAR(255) NULL COMMENT 'Sesión Checkout Stripe vinculada',
    payment_reference VARCHAR(200) COMMENT 'Referencia del proveedor de pago tras confirmación',
    expires_at        DATETIME     NOT NULL,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_registration_checkouts    PRIMARY KEY (id),
    CONSTRAINT fk_checkout_user             FOREIGN KEY (user_id)         REFERENCES users (id),
    CONSTRAINT fk_checkout_championship     FOREIGN KEY (championship_id) REFERENCES championships (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_checkout_user           ON registration_checkouts (user_id);
CREATE INDEX idx_checkout_championship   ON registration_checkouts (championship_id);
CREATE INDEX idx_checkout_status         ON registration_checkouts (status);
CREATE INDEX idx_checkout_expires        ON registration_checkouts (expires_at, status);
CREATE UNIQUE INDEX uq_checkout_active_basket ON registration_checkouts (basket_key);
CREATE UNIQUE INDEX uq_checkout_stripe_session ON registration_checkouts (stripe_session_id);

-- Restricciones de elegibilidad por tipo de categoría
CREATE TABLE category_restrictions (
    id                    CHAR(36)     NOT NULL,
    pricing_config_id     CHAR(36)     NOT NULL,
    category_type_id      VARCHAR(100) NOT NULL,
    age_min               INT,
    age_max               INT,
    max_experience_years  INT,
    max_gold_medals       INT,
    incompatible_with     TEXT         COMMENT 'IDs de tipos incompatibles separados por coma',
    requires_combination  TEXT         COMMENT 'IDs de tipos requeridos para combinar, separados por coma',

    CONSTRAINT pk_cat_restrictions      PRIMARY KEY (id),
    CONSTRAINT uq_cat_type_per_config   UNIQUE (pricing_config_id, category_type_id),
    CONSTRAINT fk_cat_restrictions_conf FOREIGN KEY (pricing_config_id) REFERENCES pricing_configs (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================
-- DATOS INICIALES: Categorías base de lucha de brazos
-- =============================================================
INSERT INTO categories (id, name, gender, arm_side, weight_limit, age_group, age_category, shift) VALUES
    (UUID(), 'Senior Masculino Derecho -70kg',  'MALE',   'RIGHT', 70.00,  'SENIOR', 'SENIOR', 'AFTERNOON'),
    (UUID(), 'Senior Masculino Derecho -80kg',  'MALE',   'RIGHT', 80.00,  'SENIOR', 'SENIOR', 'AFTERNOON'),
    (UUID(), 'Senior Masculino Derecho -90kg',  'MALE',   'RIGHT', 90.00,  'SENIOR', 'SENIOR', 'AFTERNOON'),
    (UUID(), 'Senior Masculino Derecho +90kg',  'MALE',   'RIGHT', NULL,   'SENIOR', 'SENIOR', 'AFTERNOON'),
    (UUID(), 'Senior Masculino Izquierdo -70kg','MALE',   'LEFT',  70.00,  'SENIOR', 'SENIOR', 'AFTERNOON'),
    (UUID(), 'Senior Masculino Izquierdo -80kg','MALE',   'LEFT',  80.00,  'SENIOR', 'SENIOR', 'AFTERNOON'),
    (UUID(), 'Senior Masculino Izquierdo -90kg','MALE',   'LEFT',  90.00,  'SENIOR', 'SENIOR', 'AFTERNOON'),
    (UUID(), 'Senior Masculino Izquierdo +90kg','MALE',   'LEFT',  NULL,   'SENIOR', 'SENIOR', 'AFTERNOON'),
    (UUID(), 'Senior Femenino Derecho -60kg',   'FEMALE', 'RIGHT', 60.00,  'SENIOR', 'SENIOR', 'AFTERNOON'),
    (UUID(), 'Senior Femenino Derecho +60kg',   'FEMALE', 'RIGHT', NULL,   'SENIOR', 'SENIOR', 'AFTERNOON'),
    (UUID(), 'Senior Femenino Izquierdo -60kg', 'FEMALE', 'LEFT',  60.00,  'SENIOR', 'SENIOR', 'AFTERNOON'),
    (UUID(), 'Senior Femenino Izquierdo +60kg', 'FEMALE', 'LEFT',  NULL,   'SENIOR', 'SENIOR', 'AFTERNOON'),
    (UUID(), 'Junior Masculino Derecho -70kg',  'MALE',   'RIGHT', 70.00,  'JUNIOR', 'JUNIOR', 'MORNING'),
    (UUID(), 'Junior Femenino Derecho -60kg',   'FEMALE', 'RIGHT', 60.00,  'JUNIOR', 'JUNIOR', 'MORNING')
ON DUPLICATE KEY UPDATE
    gender = VALUES(gender), arm_side = VALUES(arm_side), weight_limit = VALUES(weight_limit),
    age_group = VALUES(age_group), age_category = VALUES(age_category), shift = VALUES(shift);
