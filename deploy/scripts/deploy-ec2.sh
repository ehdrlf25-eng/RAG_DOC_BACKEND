#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "${DEPLOY_DIR}"

AWS_REGION="${AWS_REGION:-ap-northeast-2}"
ECR_REGISTRY="${ECR_REGISTRY:-011122072035.dkr.ecr.ap-northeast-2.amazonaws.com}"

if [[ ! -f .env ]]; then
  echo "Missing .env in ${DEPLOY_DIR}. Copy from .env.cloud.example" >&2
  exit 1
fi

if docker compose version &>/dev/null; then
  DOCKER_COMPOSE=(docker compose)
elif command -v docker-compose &>/dev/null; then
  DOCKER_COMPOSE=(docker-compose)
else
  echo "Docker Compose not found. Install: sudo dnf install -y docker-compose-plugin" >&2
  exit 1
fi

echo "Using: ${DOCKER_COMPOSE[*]}"

echo "Logging in to ECR (${ECR_REGISTRY})..."
aws ecr get-login-password --region "${AWS_REGION}" \
  | docker login --username AWS --password-stdin "${ECR_REGISTRY}"

COMPOSE_FILES=(
  -f docker-compose.cloud.yml
  -f docker-compose.redis.yml
  -f docker-compose.kafka.yml
)

echo "Pulling images..."
"${DOCKER_COMPOSE[@]}" "${COMPOSE_FILES[@]}" pull

echo "Starting services..."
"${DOCKER_COMPOSE[@]}" "${COMPOSE_FILES[@]}" up -d

docker image prune -f

echo "Deploy complete."
