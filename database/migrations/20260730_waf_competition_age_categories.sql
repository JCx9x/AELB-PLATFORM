-- Categorías WAF de competición. Es seguro volver a ejecutar las sentencias de datos.
SET @age_category_column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'categories' AND column_name = 'age_category'
);
SET @add_age_category_sql = IF(
    @age_category_column_exists = 0,
    'ALTER TABLE categories ADD COLUMN age_category ENUM(''SUB_JUNIOR'',''JUNIOR'',''YOUTH'',''AMATEUR'',''SENIOR'',''MASTER'',''GRAND_MASTER'',''SENIOR_GRAND_MASTER'',''SUPER_SENIOR_GRAND_MASTER'') NULL AFTER age_group',
    'SELECT 1'
);
PREPARE add_age_category_statement FROM @add_age_category_sql;
EXECUTE add_age_category_statement;
DEALLOCATE PREPARE add_age_category_statement;

-- Permite la conversión sin perder los valores de turno previos.
ALTER TABLE categories
    MODIFY COLUMN shift ENUM('TURNO_1','TURNO_2','MORNING','AFTERNOON') NOT NULL DEFAULT 'MORNING';

UPDATE categories
SET age_category = CASE UPPER(age_group)
    WHEN 'SUB_JUNIOR' THEN 'SUB_JUNIOR'
    WHEN 'JUNIOR' THEN 'JUNIOR'
    WHEN 'YOUTH' THEN 'YOUTH'
    WHEN 'AMATEUR' THEN 'AMATEUR'
    WHEN 'SENIOR' THEN 'SENIOR'
    WHEN 'MASTER' THEN 'MASTER'
    WHEN 'MASTERS' THEN 'MASTER'
    WHEN 'GRAND_MASTER' THEN 'GRAND_MASTER'
    WHEN 'GRAND_MASTERS' THEN 'GRAND_MASTER'
    WHEN 'SENIOR_GRAND_MASTER' THEN 'SENIOR_GRAND_MASTER'
    WHEN 'SENIOR_GRAND_MASTERS' THEN 'SENIOR_GRAND_MASTER'
    WHEN 'SUPER_SENIOR_GRAND_MASTER' THEN 'SUPER_SENIOR_GRAND_MASTER'
    ELSE NULL
END
WHERE age_category IS NULL;

-- El turno queda determinado por la categoría oficial, en un único punto.
UPDATE categories
SET shift = CASE WHEN age_category = 'SENIOR' THEN 'AFTERNOON' ELSE 'MORNING' END
WHERE age_category IS NOT NULL;

UPDATE categories
SET shift = CASE shift WHEN 'TURNO_2' THEN 'AFTERNOON' ELSE 'MORNING' END
WHERE age_category IS NULL;

ALTER TABLE categories
    MODIFY COLUMN shift ENUM('MORNING','AFTERNOON') NOT NULL DEFAULT 'MORNING';
