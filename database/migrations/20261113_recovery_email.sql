-- Correo secundario de recuperación de contraseña.
-- Solo se usa para recuperar la contraseña cuando está verificado (recovery_email_verified = 1).
-- No es único: varias cuentas (p. ej. hijos de una misma familia) pueden compartirlo; al
-- solicitar la recuperación se envía un enlace por cada cuenta asociada.
ALTER TABLE users
    ADD COLUMN recovery_email VARCHAR(255) NULL AFTER email_verified,
    ADD COLUMN recovery_email_verified TINYINT(1) NOT NULL DEFAULT 0 AFTER recovery_email,
    ADD KEY idx_users_recovery_email (recovery_email);

-- recovery_email guarda la dirección que se verifica: si el usuario la cambia antes de
-- pulsar el enlace, el token antiguo deja de servir.
CREATE TABLE recovery_email_verification_tokens (
    id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    recovery_email VARCHAR(255) NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expires_at DATETIME NOT NULL,
    used TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id), UNIQUE KEY uq_recovery_email_verification_hash (token_hash),
    KEY idx_recovery_email_verification_user_created (user_id, created_at),
    CONSTRAINT fk_recovery_email_verification_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
