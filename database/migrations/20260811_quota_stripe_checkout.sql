-- Persiste la Checkout Session de Stripe para hacer idempotente el checkout de cuotas.
ALTER TABLE quotas
    ADD COLUMN stripe_session_id VARCHAR(255) NULL AFTER payment_reference,
    ADD COLUMN stripe_session_url VARCHAR(2048) NULL AFTER stripe_session_id,
    ADD UNIQUE KEY uq_quotas_stripe_session (stripe_session_id);
