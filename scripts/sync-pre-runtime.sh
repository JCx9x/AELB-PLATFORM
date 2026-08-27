#!/usr/bin/env bash
set -Eeuo pipefail

: "${INSTANCE_ID:?INSTANCE_ID is required}"
: "${AWS_REGION:?AWS_REGION is required}"
: "${FILES_BUCKET:?FILES_BUCKET is required}"
: "${BACKUPS_BUCKET:?BACKUPS_BUCKET is required}"
: "${BACKEND_IMAGE:?BACKEND_IMAGE is required}"
: "${FRONTEND_IMAGE:?FRONTEND_IMAGE is required}"
: "${CORS_ALLOWED_ORIGINS:?CORS_ALLOWED_ORIGINS is required}"
: "${FRONTEND_PUBLIC_URL:?FRONTEND_PUBLIC_URL is required}"
: "${STRIPE_SUCCESS_URL:?STRIPE_SUCCESS_URL is required}"
: "${STRIPE_CANCEL_URL:?STRIPE_CANCEL_URL is required}"
: "${STRIPE_WEBHOOK_ENABLED:?STRIPE_WEBHOOK_ENABLED is required}"

command -v aws >/dev/null || { echo "Falta AWS CLI" >&2; exit 1; }
command -v jq >/dev/null || { echo "Falta jq" >&2; exit 1; }
command -v base64 >/dev/null || { echo "Falta base64" >&2; exit 1; }

for value in "$FILES_BUCKET" "$BACKUPS_BUCKET" "$BACKEND_IMAGE" "$FRONTEND_IMAGE" \
  "$CORS_ALLOWED_ORIGINS" "$FRONTEND_PUBLIC_URL" "$STRIPE_SUCCESS_URL" \
  "$STRIPE_CANCEL_URL" "$STRIPE_WEBHOOK_ENABLED"; do
  [[ "$value" != *$'\n'* && "$value" != *$'\r'* ]] || {
    echo "Los valores de runtime no pueden contener saltos de línea" >&2
    exit 1
  }
done

encode_file() { base64 --wrap=0 "$1"; }
write_file_command() {
  local source="$1" target="$2" mode="$3"
  printf 'printf %%s %q | base64 --decode > %q && chmod %s %q' \
    "$(encode_file "$source")" "$target" "$mode" "$target"
}

runtime_env="$(mktemp)"
trap 'rm -f "$runtime_env"' EXIT
umask 077
{
  printf 'AWS_REGION=%q\n' "$AWS_REGION"
  printf 'FILES_BUCKET=%q\n' "$FILES_BUCKET"
  printf 'BACKUPS_BUCKET=%q\n' "$BACKUPS_BUCKET"
  printf 'BACKEND_IMAGE=%q\n' "$BACKEND_IMAGE"
  printf 'FRONTEND_IMAGE=%q\n' "$FRONTEND_IMAGE"
  printf 'AELB_PARAMETER_PREFIX=%q\n' "${AELB_PARAMETER_PREFIX:-/aelb/pre/}"
  printf 'CORS_ALLOWED_ORIGINS=%q\n' "$CORS_ALLOWED_ORIGINS"
  printf 'FRONTEND_PUBLIC_URL=%q\n' "$FRONTEND_PUBLIC_URL"
  printf 'STRIPE_SUCCESS_URL=%q\n' "$STRIPE_SUCCESS_URL"
  printf 'STRIPE_CANCEL_URL=%q\n' "$STRIPE_CANCEL_URL"
  printf 'STRIPE_WEBHOOK_ENABLED=%q\n' "$STRIPE_WEBHOOK_ENABLED"
  printf 'DOMAIN_NAME=%q\n' "${DOMAIN_NAME:-:80}"
  printf 'APP_VERSION=%q\n' "${APP_VERSION:-pre}"
  printf 'GIT_COMMIT=%q\n' "${GIT_COMMIT:-unknown}"
} > "$runtime_env"

commands=(
  "install -d -m 0750 /opt/aelb /opt/aelb/database /opt/aelb/config"
  "$(write_file_command docker-compose.pre.yml /opt/aelb/docker-compose.pre.yml 0640)"
  "$(write_file_command Caddyfile.pre /opt/aelb/Caddyfile.pre 0640)"
  "$(write_file_command database/schema.sql /opt/aelb/database/schema.sql 0640)"
  "$(write_file_command scripts/backup-mysql-pre.sh /opt/aelb/backup-mysql-pre.sh 0750)"
  "$(write_file_command scripts/aelb-pre-backup.service /etc/systemd/system/aelb-pre-backup.service 0644)"
  "$(write_file_command scripts/aelb-pre-backup.timer /etc/systemd/system/aelb-pre-backup.timer 0644)"
  "$(write_file_command scripts/deploy-pre.sh /opt/aelb/deploy-pre.sh 0750)"
  "$(write_file_command "$runtime_env" /opt/aelb/config/deploy.env 0640)"
  "chown -R root:root /opt/aelb && systemctl daemon-reload"
)

commands_json="$(printf '%s\n' "${commands[@]}" | jq -R . | jq -s .)"
aws ssm send-command --region "$AWS_REGION" --instance-ids "$INSTANCE_ID" \
  --document-name AWS-RunShellScript --comment "AELB PRE runtime sync" \
  --parameters "{\"commands\":${commands_json}}" --output text --query 'Command.CommandId'
