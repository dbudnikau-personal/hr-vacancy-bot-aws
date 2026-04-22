# Infrastructure

## Services

### Bot — Render
- URL: https://hr-vacancy-bot.onrender.com
- Plan: Free (750h/month)
- Region: Frankfurt (EU Central)
- Deploy: automatically on push to `master` on GitHub
- Healthcheck: `/actuator/health`

### Database — Neon
- Host: `ep-plain-leaf-al28oh21.c-3.eu-central-1.aws.neon.tech`
- Database: `neondb`
- User: `neondb_owner`
- Region: EU Frankfurt
- Plan: Free (3 GB)
- Dashboard: https://console.neon.tech

### Keepalive — UptimeRobot
- Monitor: `https://hr-vacancy-bot.onrender.com/actuator/health`
- Interval: 5 minutes
- Purpose: prevent Render from sleeping (sleeps after 15 min of inactivity)

### Source — GitHub
- Repo: https://github.com/dbudnikau-personal/hr-vacancy-bot
- Main branch: `master`

## Environment Variables (Render)

| Variable | Description |
|----------|-------------|
| `BOT_TOKEN` | Telegram bot token from @BotFather |
| `SPRING_DATASOURCE_URL` | Neon JDBC URL (no credentials in URL) |
| `SPRING_DATASOURCE_USERNAME` | Neon database user |
| `SPRING_DATASOURCE_PASSWORD` | Neon database password |

## Deploy

```bash
git push origin master
```

Render automatically triggers a new build on push.

## Notes

- HF Spaces was rejected — Telegram blocks AWS IP ranges (HF runs on AWS)
- Render uses its own infrastructure — not blocked by Telegram
- First app startup takes ~2.5 min (cold JVM start + Maven image)
