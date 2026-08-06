-- Amplía las categorías de competición con Amateur. No afecta a las cuotas anuales.
ALTER TABLE categories
    MODIFY COLUMN age_category ENUM(
        'SUB_JUNIOR','JUNIOR','YOUTH','AMATEUR','SENIOR','MASTER',
        'GRAND_MASTER','SENIOR_GRAND_MASTER','SUPER_SENIOR_GRAND_MASTER'
    ) NULL;

ALTER TABLE championship_age_category_prices
    MODIFY COLUMN age_category ENUM(
        'SUB_JUNIOR','JUNIOR','YOUTH','AMATEUR','SENIOR','MASTER',
        'GRAND_MASTER','SENIOR_GRAND_MASTER','SUPER_SENIOR_GRAND_MASTER'
    ) NOT NULL;
