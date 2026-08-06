-- Por compatibilidad, los campeonatos existentes no exigen cuota hasta que un gestor lo active.
ALTER TABLE championships
    ADD COLUMN requires_current_quota TINYINT(1) NOT NULL DEFAULT 0 AFTER description;
