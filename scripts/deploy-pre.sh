#!/usr/bin/env bash
set -Eeuo pipefail

runtime_file=/opt/aelb/config/deploy.env
[[ -r "$runtime_file" ]] || { echo "Falta $runtime_file; ejecuta sync-pre-runtime.sh" >&2; exit 1; }
set -a
# shellcheck disable=SC1091
source "$runtime_file"
set +a

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
command -v docker >/dev/null || { echo "Falta Docker" >&2; exit 1; }
command -v findmnt >/dev/null || { echo "Falta findmnt" >&2; exit 1; }
command -v stat >/dev/null || { echo "Falta stat" >&2; exit 1; }

schema_file=/opt/aelb/database/schema.sql
schema_mode="$(stat -c '%a' "$schema_file" 2>/dev/null || true)"
[[ -f "$schema_file" && -r "$schema_file" && "$schema_mode" =~ [0-7]*[4-7]$ ]] || {
  echo "ABORTADO: $schema_file no existe o no es legible globalmente (se requiere lectura para otros; modo actual: ${schema_mode:-desconocido})" >&2
  exit 1
}

mysql_mount=/var/lib/aelb/mysql
mountpoint -q "$mysql_mount" || { echo "ABORTADO: $mysql_mount no está montado" >&2; exit 1; }
mounted_target="$(findmnt -T "$mysql_mount" -no TARGET)"
mysql_source="$(findmnt -T "$mysql_mount" -no SOURCE)"
root_source="$(findmnt -T / -no SOURCE)"
[[ "$mounted_target" == "$mysql_mount" && -n "$mysql_source" && "$mysql_source" != "$root_source" ]] || {
  echo "ABORTADO: $mysql_mount no corresponde a un filesystem separado del root" >&2
  exit 1
}

prefix="${AELB_PARAMETER_PREFIX:-/aelb/pre/}"
prefix="/${prefix#/}"
prefix="${prefix%/}"
get_secret() {
  aws ssm get-parameter --region "$AWS_REGION" --name "$prefix/$1" \
    --with-decryption --query 'Parameter.Value' --output text
}
validate_secret() {
  local name="$1" value="$2"
  [[ -n "$value" && "$value" != *$'\n'* && "$value" != *$'\r'* ]] || {
    echo "El secreto $name está vacío o contiene saltos de línea" >&2
    exit 1
  }
}

mysql_password="$(get_secret mysql/password)"
mysql_root_password="$(get_secret mysql/root-password)"
jwt_secret="$(get_secret jwt/secret)"
stripe_secret_key="$(get_secret stripe/secret-key)"
if [[ "$STRIPE_WEBHOOK_ENABLED" == true ]]; then
  stripe_webhook_secret="$(get_secret stripe/webhook-secret)"
else
  stripe_webhook_secret=""
fi
validate_secret mysql/password "$mysql_password"
validate_secret mysql/root-password "$mysql_root_password"
validate_secret jwt/secret "$jwt_secret"
validate_secret stripe/secret-key "$stripe_secret_key"
if [[ "$STRIPE_WEBHOOK_ENABLED" == true ]]; then
  validate_secret stripe/webhook-secret "$stripe_webhook_secret"
fi

umask 077
env_tmp="$(mktemp /opt/aelb/config/pre.env.XXXXXX)"
trap 'rm -f "$env_tmp"; unset mysql_password mysql_root_password jwt_secret stripe_secret_key stripe_webhook_secret' EXIT
cat > "$env_tmp" <<EOF
MYSQL_DATABASE=aelb_db
MYSQL_USER=aelb_user
MYSQL_PASSWORD=$mysql_password
MYSQL_ROOT_PASSWORD=$mysql_root_password
DB_NAME=aelb_db
DB_USER=aelb_user
DB_PASSWORD=$mysql_password
JWT_SECRET=$jwt_secret
JWT_EXPIRATION_MS=900000
JWT_REFRESH_EXPIRATION_MS=2592000000
AUTH_COOKIE_SECURE=false
AUTH_COOKIE_SAME_SITE=Strict
CORS_ALLOWED_ORIGINS=$CORS_ALLOWED_ORIGINS
STORAGE_BUCKET=$FILES_BUCKET
STORAGE_REGION=$AWS_REGION
STORAGE_USE_IAM=true
STORAGE_ENDPOINT_OVERRIDE=
STORAGE_PUBLIC_ENDPOINT=
STRIPE_SECRET_KEY=$stripe_secret_key
STRIPE_WEBHOOK_ENABLED=$STRIPE_WEBHOOK_ENABLED
STRIPE_WEBHOOK_SECRET=$stripe_webhook_secret
STRIPE_CURRENCY=eur
STRIPE_SUCCESS_URL=$STRIPE_SUCCESS_URL
STRIPE_CANCEL_URL=$STRIPE_CANCEL_URL
EMAIL_ENABLED=false
EMAIL_WORKER_ENABLED=false
EMAIL_FROM=
EMAIL_FROM_NAME=AELB
FRONTEND_PUBLIC_URL=$FRONTEND_PUBLIC_URL
APP_ENV=pre
APP_VERSION=${APP_VERSION:-pre}
GIT_COMMIT=${GIT_COMMIT:-unknown}
BACKUPS_BUCKET=$BACKUPS_BUCKET
AWS_REGION=$AWS_REGION
EOF
chmod 0600 "$env_tmp"
mv -f "$env_tmp" /opt/aelb/config/pre.env

ecr_registry="${BACKEND_IMAGE%%/*}"
aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$ecr_registry" >/dev/null
compose=(docker compose -f /opt/aelb/docker-compose.pre.yml)
"${compose[@]}" config --quiet
"${compose[@]}" pull
"${compose[@]}" up -d --remove-orphans

for service in mysql backend frontend caddy; do
  for attempt in $(seq 1 40); do
    container="$("${compose[@]}" ps -q "$service")"
    status="$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}no-healthcheck{{end}}' "$container" 2>/dev/null || true)"
    [[ "$status" == healthy || "$status" == no-healthcheck ]] && break
    [[ "$status" == unhealthy || "$status" == "" ]] && { echo "Servicio $service no está sano" >&2; "${compose[@]}" ps; exit 1; }
    sleep 5
  done
  [[ "$status" == healthy || "$status" == no-healthcheck ]] || { echo "Timeout esperando $service" >&2; exit 1; }
done

systemctl enable --now aelb-pre-backup.timer
docker image prune -f >/dev/null
echo "PRE deployment completed: ${GIT_COMMIT:-unknown}"
