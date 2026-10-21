#!/usr/bin/env bash
set -Eeuo pipefail

region="${AWS_REGION:-eu-south-2}"
prefix="${AELB_PARAMETER_PREFIX:-/aelb/pre}"
prefix="${prefix%/}"
command -v aws >/dev/null || { echo "Falta AWS CLI" >&2; exit 1; }

put_secret() {
  local name="$1" description="$2" value
  read -r -s -p "$description: " value
  echo >&2
  [ -n "$value" ] || { echo "El valor no puede estar vacío" >&2; exit 1; }
  aws ssm put-parameter --region "$region" --name "$prefix/$name" --type SecureString --value "$value" --overwrite >/dev/null
  unset value
  echo "Creado/actualizado $prefix/$name" >&2
}

put_jwt_secret() {
  local value
  read -r -s -p "JWT secret (mínimo 32 bytes): " value
  echo >&2
  [ "${#value}" -ge 32 ] || { echo "JWT secret demasiado corto" >&2; exit 1; }
  aws ssm put-parameter --region "$region" --name "$prefix/jwt/secret" --type SecureString --value "$value" --overwrite >/dev/null
  unset value
  echo "Creado/actualizado $prefix/jwt/secret" >&2
}

put_stripe_secret() {
  local value
  read -r -s -p "Stripe TEST secret key (sk_test_...): " value
  echo >&2
  [[ "$value" == sk_test_* ]] || { echo "PRE requiere una clave Stripe TEST (sk_test_...)" >&2; exit 1; }
  aws ssm put-parameter --region "$region" --name "$prefix/stripe/secret-key" --type SecureString --value "$value" --overwrite >/dev/null
  unset value
  echo "Creado/actualizado $prefix/stripe/secret-key" >&2
}

put_stripe_webhook_secret() {
  local value
  read -r -s -p "Stripe PRE webhook secret (whsec_...): " value
  echo >&2
  [[ "$value" == whsec_* ]] || { echo "El webhook secret debe empezar por whsec_" >&2; exit 1; }
  aws ssm put-parameter --region "$region" --name "$prefix/stripe/webhook-secret" --type SecureString --value "$value" --overwrite >/dev/null
  unset value
  echo "Creado/actualizado $prefix/stripe/webhook-secret" >&2
}

put_secret "mysql/root-password" "MySQL root password"
put_secret "mysql/password" "MySQL application password"
put_jwt_secret
put_stripe_secret
if [[ "${STRIPE_WEBHOOK_ENABLED:-false}" == true ]]; then
  put_stripe_webhook_secret
else
  echo "Webhook Stripe PRE deshabilitado; no se solicita webhook secret" >&2
fi
echo "Secrets PRE almacenados en Parameter Store bajo $prefix/" >&2
