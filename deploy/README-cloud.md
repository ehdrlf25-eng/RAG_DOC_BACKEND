# AWS EC2 Cloud 배포 가이드

## 아키텍처

- **EC2** (`ec2-user`, Amazon Linux): Docker Compose — backend, frontend, Redis, Kafka
- **RDS** PostgreSQL `ragdoc` (pgvector: Flyway V2)
- **S3** PDF 저장 (`cloud` 프로필)
- **ECR** `011122072035.dkr.ecr.ap-northeast-2.amazonaws.com/edu-was`
  - `backend-latest`, `frontend-latest`

## 선행 작업 (AWS 콘솔)

1. RDS `postgres` DB에 접속 후 `scripts/create-ragdoc-database.sql` 실행
2. EC2 IAM Role 생성 후 인스턴스에 연결
   - ECR pull 권한
   - S3 버킷 `amzn-s3-gaon-bucket-011122072035-ap-northeast-2-an` Put/Get/Delete
3. Security Group
   - EC2: 22(내 IP), 80(0.0.0.0/0)
   - RDS: 5432 ← EC2 SG만
4. EC2에 Docker, Docker Compose, AWS CLI 설치

Amazon Linux에는 `docker-compose-plugin` 패키지가 없을 수 있습니다. 아래 순서로 설치하세요.

```bash
sudo dnf install -y docker git
sudo systemctl enable --now docker
sudo usermod -aG docker ec2-user
# 로그아웃 후 재접속

# Compose V2 plugin (dnf 실패 시 수동 설치)
ARCH=$(uname -m)
sudo mkdir -p /usr/local/lib/docker/cli-plugins
sudo curl -fsSL "https://github.com/docker/compose/releases/download/v2.27.1/docker-compose-linux-${ARCH}" \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

docker compose version
```

`docker compose version`이 나오면 준비 완료입니다.

## EC2 초기 설정

```bash
sudo mkdir -p /opt/ragdoc
sudo chown ec2-user:ec2-user /opt/ragdoc
cd /opt/ragdoc
git clone <backend-repo-url> .
cd deploy
cp .env.cloud.example .env
# .env 편집 (DB_PASSWORD, JWT_SECRET, OPENAI_API_KEY, CORS_ALLOWED_ORIGINS)
chmod +x scripts/deploy-ec2.sh

# Redis, Kafka (최초 1회)
docker compose -f docker-compose.redis.yml up -d
docker compose -f docker-compose.kafka.yml up -d
```

## 배포

```bash
cd /opt/ragdoc/deploy
./scripts/deploy-ec2.sh
```

## IP 변경 시 (학습용 EC2 중지/시작)

1. EC2 퍼블릭 IP 확인
2. `deploy/.env` → `CORS_ALLOWED_ORIGINS=http://<새 IP>`
3. GitHub Secret `EC2_HOST` 갱신
4. `./scripts/deploy-ec2.sh` 재실행

## GitHub Secrets

| Secret | 설명 |
|--------|------|
| `AWS_ACCESS_KEY_ID` | Actions → ECR push |
| `AWS_SECRET_ACCESS_KEY` | Actions → ECR push |
| `EC2_HOST` | EC2 퍼블릭 IP |
| `EC2_SSH_KEY` | PEM private key |

## Spring Profile

| 환경 | Profile |
|------|---------|
| 로컬 개발 | `local` (로컬 디스크) |
| EC2 | `cloud` (S3 + RDS) |

## CI/CD

`main` push → test → ECR push → SSH `deploy-ec2.sh`
