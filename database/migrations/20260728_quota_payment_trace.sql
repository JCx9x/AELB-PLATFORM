-- Trazabilidad del origen de pagos de cuotas.
ALTER TABLE quotas
    ADD COLUMN paid_at DATETIME NULL AFTER payment_date,
    ADD COLUMN payment_source ENUM('STRIPE','MANUAL','LEGACY') NULL AFTER paid_at,
    ADD COLUMN paid_by_user_id CHAR(36) NULL AFTER payment_source,
    ADD COLUMN payment_reference VARCHAR(255) NULL AFTER paid_by_user_id,
    ADD CONSTRAINT fk_quotas_paid_by FOREIGN KEY (paid_by_user_id) REFERENCES users (id) ON DELETE SET NULL,
    ADD INDEX idx_quotas_payment_source (payment_source);

-- Los pagos anteriores no pueden atribuirse de forma fiable: se conservan como históricos.
UPDATE quotas
SET payment_source = 'LEGACY', paid_at = COALESCE(updated_at, created_at)
WHERE payment_status = 'PAID' AND payment_source IS NULL;
