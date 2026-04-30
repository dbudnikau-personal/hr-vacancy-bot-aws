# Infrastructure

## Services

### Bot — AWS Lambda
- **BotHandlerLambda** — handles Telegram webhook, invoked via API Gateway (HTTP API)
- **ScannerLambda** — runs vacancy scan, invoked by BotHandler or EventBridge schedule (every 2h)
- **CookieRefresherLambda** — Python/Playwright Lambda, refreshes Wellfound session cookies
- Region: `eu-central-1`
- Runtime: Java 21 with SnapStart enabled
- Deploy: GitHub Actions on push to `master`

### Database — Neon (PostgreSQL)
- Region: EU Frankfurt
- Plan: Free (3 GB)
- Dashboard: https://console.neon.tech
- Connection string stored in GitHub Secrets (`SPRING_DATASOURCE_URL`)

### Source — GitHub
- Repo: https://github.com/dbudnikau-personal/hr-vacancy-bot-aws
- Main branch: `master`
- CI: runs tests on every PR
- CD: deploys to AWS on push to `master`

## GitHub Secrets

| Secret | Description |
|--------|-------------|
| `BOT_TOKEN` | Telegram bot token from @BotFather |
| `SPRING_DATASOURCE_URL` | Neon JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | Neon database user |
| `SPRING_DATASOURCE_PASSWORD` | Neon database password |
| `DEEPSEEK_API_KEY` | DeepSeek API key |
| `AWS_ACCESS_KEY_ID` | AWS IAM key for deploy |
| `AWS_SECRET_ACCESS_KEY` | AWS IAM secret for deploy |

## GitHub Variables

| Variable | Description |
|----------|-------------|
| `ADMIN_CHAT_ID` | Telegram chat ID to notify on deployment |

## Deploy

### Automatic (CI/CD)
Push to `master` — GitHub Actions runs tests then deploys via SAM.

### Manual
```bash
cp .env.example .env
# Fill in .env

./deploy.sh
```

## Notes

- Telegram webhook is registered automatically via CloudFormation Custom Resource on each deploy
- SnapStart is enabled on both Java Lambda functions to reduce cold start latency
- Wellfound requires fresh cookies — CookieRefresherLambda runs Playwright headless Chrome to obtain them and stores in SSM Parameter Store
