#!/usr/bin/env bash
set -Eeuo pipefail

[[ -z "$(git status --porcelain)" ]] || {
  echo "ABORTADO: el worktree no está limpio; no se construyen imágenes PRE" >&2
  git status --short >&2
  exit 1
}

: "${AWS_REGION:?AWS_REGION is required}"
: "${ECR_REGISTRY:?ECR_REGISTRY is required}"

command -v aws >/dev/null || { echo "Falta AWS CLI" >&2; exit 1; }
command -v docker >/dev/null || { echo "Falta Docker" >&2; exit 1; }

tag="${IMAGE_TAG:-$(date -u +%Y%m%dT%H%M%SZ)-$(git rev-parse --short=12 HEAD)}"
backend_image="${ECR_REGISTRY}/aelb-pre-backend:${tag}"
frontend_image="${ECR_REGISTRY}/aelb-pre-frontend:${tag}"

backend_cache_args=()
frontend_cache_args=()
if [[ -n "${DOCKER_CACHE_FROM:-}" && -n "${DOCKER_CACHE_TO:-}" ]]; then
  backend_cache_args=(--cache-from "type=gha,scope=aelb-pre-backend" --cache-to "type=gha,mode=max,scope=aelb-pre-backend")
  frontend_cache_args=(--cache-from "type=gha,scope=aelb-pre-frontend" --cache-to "type=gha,mode=max,scope=aelb-pre-frontend")
fi

aws ecr get-login-password --region "$AWS_REGION" |
  docker login --username AWS --password-stdin "$ECR_REGISTRY" >/dev/null
docker buildx build --platform linux/arm64 "${backend_cache_args[@]}" --push -t "$backend_image" ./backendweb
docker buildx build --platform linux/arm64 "${frontend_cache_args[@]}" --build-arg NEXT_PUBLIC_API_URL=/api --push -t "$frontend_image" ./aelb-frontend
printf 'TAG=%s\nBACKEND_IMAGE=%s\nFRONTEND_IMAGE=%s\n' "$tag" "$backend_image" "$frontend_image"
