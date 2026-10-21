#!/usr/bin/env bash
set -Eeuo pipefail

AWS_PROFILE="${AWS_PROFILE:-aelb-tf}"
AWS_REGION="${AWS_REGION:-eu-south-2}"
INSTANCE_ID="${INSTANCE_ID:-i-073035077dc228e14}"
MYSQL_CONTAINER="${MYSQL_CONTAINER:-aelb-pre-mysql-1}"
REMOTE_PORT="${REMOTE_PORT:-3306}"
LOCAL_PORT="${LOCAL_PORT:-3308}"

die() {
  echo "ERROR: $*" >&2
  exit 1
}

check_dependencies() {
  command -v aws >/dev/null 2>&1 || die "No se encuentra aws. Instala/configura AWS CLI manualmente."
  command -v session-manager-plugin >/dev/null 2>&1 || die "No se encuentra session-manager-plugin. Instálalo manualmente antes de continuar."
  command -v ss >/dev/null 2>&1 || command -v lsof >/dev/null 2>&1 || \
    die "No se encuentra ss ni lsof; no puedo comprobar si el puerto local está libre."
}

validate_port() {
  local name="$1" value="$2"
  [[ "$value" =~ ^[0-9]+$ ]] || die "$name debe ser un puerto numérico: $value"
  (( value >= 1 && value <= 65535 )) || die "$name debe estar entre 1 y 65535: $value"
}

check_aws_credentials() {
  if ! aws sts get-caller-identity --profile "$AWS_PROFILE" --region "$AWS_REGION" >/dev/null; then
    die "No se pudieron validar las credenciales AWS del perfil '$AWS_PROFILE'. Si han expirado, ejecuta manualmente: aws logout --profile aelb && aws login --profile aelb --region eu-south-2"
  fi
}

check_ssm_instance() {
  local instance
  instance="$(aws ssm describe-instance-information \
    --profile "$AWS_PROFILE" --region "$AWS_REGION" \
    --filters "Key=InstanceIds,Values=$INSTANCE_ID" "Key=PingStatus,Values=Online" \
    --query 'InstanceInformationList[0].InstanceId' --output text 2>/dev/null || true)"
  [[ "$instance" == "$INSTANCE_ID" ]] || die "La instancia $INSTANCE_ID no está gestionada por SSM o no está Online."
}

wait_for_command() {
  local command_id="$1" status=''
  for _ in $(seq 1 60); do
    status="$(aws ssm get-command-invocation \
      --profile "$AWS_PROFILE" --region "$AWS_REGION" \
      --command-id "$command_id" --instance-id "$INSTANCE_ID" \
      --query 'Status' --output text 2>/dev/null || true)"
    case "$status" in
      Success) return 0 ;;
      Failed|Cancelled|TimedOut|Cancelling) return 1 ;;
    esac
    sleep 2
  done
  return 1
}

get_mysql_container_info() {
  local remote_command command_id stdout stderr
  remote_command="docker inspect --format 'IP={{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}} STATUS={{.State.Status}} HEALTH={{if .State.Health}}{{.State.Health.Status}}{{else}}no-healthcheck{{end}}' $MYSQL_CONTAINER"

  command_id="$(aws ssm send-command \
    --profile "$AWS_PROFILE" --region "$AWS_REGION" \
    --instance-ids "$INSTANCE_ID" \
    --document-name AWS-RunShellScript \
    --comment "AELB PRE MySQL tunnel inspection" \
    --parameters "{\"commands\":[\"$remote_command\"]}" \
    --query 'Command.CommandId' --output text)" || die "No se pudo enviar el comando SSM para inspeccionar MySQL."

  if ! wait_for_command "$command_id"; then
    stdout="$(aws ssm get-command-invocation --profile "$AWS_PROFILE" --region "$AWS_REGION" --command-id "$command_id" --instance-id "$INSTANCE_ID" --query 'StandardOutputContent' --output text 2>/dev/null || true)"
    stderr="$(aws ssm get-command-invocation --profile "$AWS_PROFILE" --region "$AWS_REGION" --command-id "$command_id" --instance-id "$INSTANCE_ID" --query 'StandardErrorContent' --output text 2>/dev/null || true)"
    [[ -z "$stdout" ]] || echo "SSM stdout: $stdout" >&2
    [[ -z "$stderr" ]] || echo "SSM stderr: $stderr" >&2
    die "No se pudo inspeccionar el contenedor $MYSQL_CONTAINER mediante SSM."
  fi

  MYSQL_INFO="$(aws ssm get-command-invocation \
    --profile "$AWS_PROFILE" --region "$AWS_REGION" \
    --command-id "$command_id" --instance-id "$INSTANCE_ID" \
    --query 'StandardOutputContent' --output text | tr -d '\r\n' | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')"
  [[ "$MYSQL_INFO" =~ ^IP=([^[:space:]]+)[[:space:]]STATUS=([^[:space:]]+)[[:space:]]HEALTH=([^[:space:]]+)$ ]] || \
    die "Salida inesperada de docker inspect para $MYSQL_CONTAINER: $MYSQL_INFO"

  MYSQL_IP="${BASH_REMATCH[1]}"
  MYSQL_STATUS="${BASH_REMATCH[2]}"
  MYSQL_HEALTH="${BASH_REMATCH[3]}"
}

validate_ipv4() {
  local ip="$1" octet
  local -a octets
  [[ "$ip" =~ ^[0-9]+(\.[0-9]+){3}$ ]] || return 1
  IFS=. read -r -a octets <<< "$ip"
  for octet in "${octets[@]}"; do
    (( 10#$octet <= 255 )) || return 1
  done
}

check_local_port() {
  local listeners=''
  if command -v ss >/dev/null 2>&1; then
    listeners="$(ss -ltnH "sport = :$LOCAL_PORT" 2>/dev/null || true)"
  else
    listeners="$(lsof -nP -iTCP:"$LOCAL_PORT" -sTCP:LISTEN 2>/dev/null || true)"
  fi
  [[ -z "$listeners" ]] || {
    echo "El puerto local $LOCAL_PORT ya está ocupado; probablemente existe otro túnel." >&2
    echo "$listeners" >&2
    die "Elige otro puerto, por ejemplo LOCAL_PORT=3310. No se detendrá ningún proceso."
  }
}

open_tunnel() {
  echo
  echo "AELB PRE MySQL tunnel"
  echo
  echo "AWS profile : $AWS_PROFILE"
  echo "AWS region  : $AWS_REGION"
  echo "Instance    : $INSTANCE_ID"
  echo "Container   : $MYSQL_CONTAINER"
  echo "MySQL IP    : $MYSQL_IP"
  echo "Remote port : $REMOTE_PORT"
  echo "Local port  : $LOCAL_PORT"
  echo
  echo "DBeaver:"
  echo "  Host     : 127.0.0.1"
  echo "  Port     : $LOCAL_PORT"
  echo "  User     : aelb_user"
  echo
  echo "Keep this terminal open while using DBeaver."
  echo "Press Ctrl+C to close the tunnel."
  echo

  exec aws ssm start-session \
    --profile "$AWS_PROFILE" --region "$AWS_REGION" \
    --target "$INSTANCE_ID" \
    --document-name AWS-StartPortForwardingSessionToRemoteHost \
    --parameters "{\"host\":[\"$MYSQL_IP\"],\"portNumber\":[\"$REMOTE_PORT\"],\"localPortNumber\":[\"$LOCAL_PORT\"]}"
}

validate_port REMOTE_PORT "$REMOTE_PORT"
validate_port LOCAL_PORT "$LOCAL_PORT"
[[ "$MYSQL_CONTAINER" =~ ^[a-zA-Z0-9][a-zA-Z0-9_.-]*$ ]] || die "MYSQL_CONTAINER contiene caracteres no permitidos."

check_dependencies
check_aws_credentials
check_ssm_instance
get_mysql_container_info
validate_ipv4 "$MYSQL_IP" || die "La IP obtenida no es una IPv4 válida: $MYSQL_IP"
[[ "$MYSQL_STATUS" == running ]] || die "El contenedor $MYSQL_CONTAINER no está running: $MYSQL_STATUS"
[[ "$MYSQL_HEALTH" == healthy || "$MYSQL_HEALTH" == no-healthcheck ]] || \
  die "El contenedor $MYSQL_CONTAINER no está healthy: $MYSQL_HEALTH"
check_local_port
open_tunnel
