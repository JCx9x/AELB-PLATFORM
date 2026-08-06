-- Migración para instalaciones existentes (ejecutar una sola vez).
-- En bases nuevas, database/schema.sql ya contiene esta estructura.
ALTER TABLE quotas
    ADD COLUMN age_category ENUM('SUB_JUNIOR','JUNIOR','YOUTH','SENIOR','MASTERS','GRAND_MASTERS','SENIOR_GRAND_MASTERS') NULL AFTER year;

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

-- Conserva las cuotas históricas calculando el tramo a 31 de diciembre de su año.
UPDATE quotas q
LEFT JOIN users u ON u.id = q.user_id
SET q.age_category = CASE
    WHEN u.birth_date IS NULL THEN 'SENIOR'
    WHEN TIMESTAMPDIFF(YEAR, u.birth_date, CONCAT(q.year, '-12-31')) >= 60 THEN 'SENIOR_GRAND_MASTERS'
    WHEN TIMESTAMPDIFF(YEAR, u.birth_date, CONCAT(q.year, '-12-31')) >= 50 THEN 'GRAND_MASTERS'
    WHEN TIMESTAMPDIFF(YEAR, u.birth_date, CONCAT(q.year, '-12-31')) >= 40 THEN 'MASTERS'
    WHEN TIMESTAMPDIFF(YEAR, u.birth_date, CONCAT(q.year, '-12-31')) BETWEEN 19 AND 23 THEN 'YOUTH'
    WHEN TIMESTAMPDIFF(YEAR, u.birth_date, CONCAT(q.year, '-12-31')) BETWEEN 16 AND 18 THEN 'JUNIOR'
    WHEN TIMESTAMPDIFF(YEAR, u.birth_date, CONCAT(q.year, '-12-31')) BETWEEN 14 AND 15 THEN 'SUB_JUNIOR'
    ELSE 'SENIOR'
END;

ALTER TABLE quotas MODIFY age_category ENUM('SUB_JUNIOR','JUNIOR','YOUTH','SENIOR','MASTERS','GRAND_MASTERS','SENIOR_GRAND_MASTERS') NOT NULL;

ALTER TABLE quotas MODIFY year INT NOT NULL;

ALTER TABLE annual_quota_prices MODIFY year INT NOT NULL;
