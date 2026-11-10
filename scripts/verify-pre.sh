#!/usr/bin/env bash
# Verificación post-despliegue de PRE. Se ejecuta en la EC2 vía SSM; AWS-RunShellScript usa sh,
# así que la lógica vive aquí para disponer de bash (pipefail, [[ ]]).
set -Eeuo pipefail

expected_mysql_uuid="${1:?Uso: verify-pre.sh <UUID del volumen MySQL>}"
mysql_mount=/var/lib/aelb/mysql

for container in aelb-pre-mysql-1 aelb-pre-backend-1 aelb-pre-frontend-1 aelb-pre-caddy-1; do
  state="$(docker inspect -f '{{.State.Status}}' "$container")"
  health="$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}no-healthcheck{{end}}' "$container")"
  echo "$container $state $health"
  [[ "$state" == running && "$health" == healthy ]] || { echo "ERROR: $container no está running/healthy" >&2; exit 1; }
done

mountpoint -q "$mysql_mount" || { echo "ERROR: $mysql_mount no está montado" >&2; exit 1; }
mount_info="$(findmnt -rn -M "$mysql_mount" -o UUID,TARGET)"
echo "$mount_info"
[[ "$mount_info" == "$expected_mysql_uuid $mysql_mount" ]] || { echo "ERROR: volumen MySQL inesperado" >&2; exit 1; }
