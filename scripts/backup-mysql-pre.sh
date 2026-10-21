#!/usr/bin/env bash
set -Eeuo pipefail

: "${MYSQL_DATABASE:?MYSQL_DATABASE is required}"
: "${MYSQL_USER:?MYSQL_USER is required}"
: "${MYSQL_PASSWORD:?MYSQL_PASSWORD is required}"
: "${BACKUPS_BUCKET:?BACKUPS_BUCKET is required}"
: "${AWS_REGION:?AWS_REGION is required}"

stamp="$(date -u +%Y%m%dT%H%M%SZ)"
out="/tmp/aelb-${MYSQL_DATABASE}-${stamp}.sql.gz"
trap 'rm -f "$out"' EXIT
docker compose -f /opt/aelb/docker-compose.pre.yml exec -T -e MYSQL_PWD="$MYSQL_PASSWORD" mysql \
  mysqldump --single-transaction --quick --routines --triggers \
  -u"$MYSQL_USER" "$MYSQL_DATABASE" | gzip -9 > "$out"
aws s3 cp "$out" "s3://${BACKUPS_BUCKET}/mysql/${stamp}.sql.gz" --region "$AWS_REGION" --only-show-errors
echo "MySQL backup uploaded: $stamp"
