-- La fecha de nacimiento pasa a ser obligatoria: es imprescindible para
-- calcular el tramo de edad de cuotas anuales y categorías de campeonato,
-- y solo GESTOR/ADMIN pueden modificarla tras el registro (ver
-- User.updateManagedProfile). El propio usuario ya no puede editarla desde
-- su perfil.
--
-- Antes de aplicar esta migración, backfillea manualmente cualquier fila con
-- birth_date NULL (la sentencia siguiente falla intencionadamente si queda
-- alguna, para no perder de vista usuarios sin fecha de nacimiento real):
--   SELECT id, email FROM users WHERE birth_date IS NULL;
ALTER TABLE users
    MODIFY COLUMN birth_date DATE NOT NULL;
