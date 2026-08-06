CREATE TABLE championship_age_category_prices (
    id                BIGINT NOT NULL AUTO_INCREMENT,
    championship_id   CHAR(36) NOT NULL,
    age_category      ENUM('SUB_JUNIOR','JUNIOR','YOUTH','AMATEUR','SENIOR','MASTER','GRAND_MASTER','SENIOR_GRAND_MASTER','SUPER_SENIOR_GRAND_MASTER') NOT NULL,
    price_per_arm     DECIMAL(8,2) NOT NULL,
    combination_price DECIMAL(8,2) NOT NULL DEFAULT 10.00,
    PRIMARY KEY (id),
    UNIQUE KEY uq_championship_age_category_price (championship_id, age_category),
    CONSTRAINT fk_championship_age_category_price_championship
        FOREIGN KEY (championship_id) REFERENCES championships (id) ON DELETE CASCADE,
    CONSTRAINT ck_championship_age_category_price_per_arm CHECK (price_per_arm >= 0),
    CONSTRAINT ck_championship_age_category_price_combination CHECK (combination_price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
