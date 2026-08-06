-- Integridad de Checkout Stripe para inscripciones.
-- Aplicar una sola vez en instalaciones ya existentes, antes de desplegar el backend.

ALTER TABLE registration_checkouts
    ADD COLUMN basket_key CHAR(64) NULL COMMENT 'Hash de cesta pendiente; evita intentos simultáneos' AFTER metadata_json,
    ADD COLUMN stripe_session_id VARCHAR(255) NULL COMMENT 'Checkout Session de Stripe vinculada' AFTER status,
    ADD UNIQUE KEY uq_checkout_active_basket (basket_key),
    ADD UNIQUE KEY uq_checkout_stripe_session (stripe_session_id);

-- Una categoría concreta solo puede materializarse una vez por atleta y campeonato.
ALTER TABLE registrations
    ADD UNIQUE KEY uq_registration_user_championship_category (user_id, championship_id, category_id);
