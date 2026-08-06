-- Añade ciudad, provincia (opcional) y nacionalidad al registro de usuarios.
-- Nullable para no romper usuarios existentes; ciudad y nacionalidad se exigen
-- a nivel de aplicación (CreateUserRequest) para las altas nuevas.
ALTER TABLE users
    ADD COLUMN city        VARCHAR(100) NULL AFTER phone,
    ADD COLUMN province    VARCHAR(100) NULL AFTER city,
    ADD COLUMN nationality VARCHAR(100) NULL AFTER province;
