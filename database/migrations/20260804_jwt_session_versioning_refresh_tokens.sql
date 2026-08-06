-- Auditoría de seguridad #4: sesiones JWT no revocables y roles obsoletos.
-- token_version permite invalidar access tokens ya emitidos al bloquear,
-- cambiar el rol o la contraseña de un usuario, sin esperar a su caducidad.
ALTER TABLE users
    ADD COLUMN token_version INT NOT NULL DEFAULT 0 AFTER team_id;

-- Solo se guarda el hash SHA-256 del token; el valor en claro vive únicamente
-- en la cookie HttpOnly del cliente. Rotatorio: cada uso revoca el token
-- actual y emite uno nuevo. Reutilizar uno ya revocado indica robo.
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
