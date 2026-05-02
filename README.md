# HR Vacancy Bot

A Telegram bot that monitors job vacancy sites and notifies about new vacancies based on configurable filters. Runs on AWS Lambda.

## Features

- Monitors multiple job sites simultaneously
- Detects new vacancies and changes in existing ones
- Sends notifications directly to Telegram
- Configurable filters per chat (keywords, location, salary, sites)
- AI-powered relevance scoring via DeepSeek
- Pluggable parser architecture — easy to add new sites

## Supported Sites

| Site | Method |
|------|--------|
| [HH.ru](https://hh.ru) | HTML scraping (Jsoup) |
| [Djinni](https://djinni.co) | HTML scraping (Jsoup) |
| [Getmatch](https://getmatch.ru) | HTML scraping (Jsoup) |
| [LinkedIn](https://linkedin.com) | HTML scraping (Jsoup) |
| [Indeed](https://indeed.com) | HTML scraping (Jsoup) |
| [Wellfound](https://wellfound.com) | Playwright (cookie-refresher Lambda) |
| [Job Bank Canada](https://jobbank.gc.ca) | Atom feed |
| [RemoteOK](https://remoteok.com) | HTML scraping (Jsoup) |
| [We Work Remotely](https://weworkremotely.com) | HTML scraping (Jsoup) |
| [Remotive](https://remotive.com) | HTML scraping (Jsoup) |

## Tech Stack

- **Java 21** + **Spring Boot 3.5** — application core
- **AWS Lambda** — serverless runtime (BotHandler + Scanner functions)
- **AWS SAM** — infrastructure as code and deployment
- **API Gateway (HTTP API)** — Telegram webhook endpoint
- **PostgreSQL (Neon)** — vacancy storage and deduplication
- **Flyway** — database migrations
- **Jsoup** — HTML parsing
- **Playwright (Python Lambda)** — JS-rendered pages (Wellfound)
- **DeepSeek API** — AI vacancy relevance scoring
- **TelegramBots 9.5** — bot API client

## Architecture

```
Telegram Webhook
      │
API Gateway → BotHandlerLambda
                    │
              ScannerLambda (EventBridge, every 2h)
                    ├── HhParser
                    ├── DjinniParser
                    ├── GetmatchParser
                    ├── LinkedInParser
                    ├── IndeedParser
                    ├── WellfoundParser ──→ CookieRefresherLambda (Python/Playwright)
                    ├── JobBankParser
                    ├── RemoteOkParser
                    ├── WeWorkRemotelyParser
                    └── RemotiveParser
                              │
                    VacancyRelevanceService (DeepSeek)
                              │
                    PostgreSQL (Neon) — deduplication
                              │
                    Telegram notification
```

## Local Development

### Prerequisites

- Java 21
- Docker + Docker Compose (or Colima on macOS)
- Telegram bot token from [@BotFather](https://t.me/BotFather)

### Setup

```bash
git clone https://github.com/dbudnikau-personal/hr-vacancy-bot-aws.git
cd hr-vacancy-bot-aws

cp .env.example .env
# Fill in .env with your values
```

### Run locally (Docker Compose)

```bash
docker-compose up -d
```

## Deployment

Deployment to AWS is handled automatically by GitHub Actions on push to `master`.

For manual deploy:

```bash
cp .env.example .env
# Fill in .env

./scripts/deploy.sh
```

## Bot Commands

| Command | Description |
|---------|-------------|
| `/addfilter <name> <keywords> [location] [salaryMin] [sites]` | Add vacancy filter |
| `/filters` | List active filters |
| `/removefilter <id>` | Deactivate filter by ID |
| `/vacancies` | Browse saved vacancies with inline pagination |
| `/vacancies <keyword>` | Search vacancies by keyword |
| `/report <site>` | Export vacancies for a site to CSV |
| `/scan` | Trigger manual scan for all active filters |
| `/scan <filter_id>` | Trigger manual scan for a specific filter |
| `/stopscan` | Disable automatic and manual scanning |
| `/startscan` | Re-enable scanning |
| `/interval` | Show current scan schedule |
| `/interval <Nm\|Nh\|Nd>` | Set scan schedule (e.g. `30m`, `6h`, `1d`) |
| `/status` | Show parser health status |
| `/version` | Show deployed version |
| `/help` | Show available commands |

### Scan Schedule

Default schedule is `rate(3 days)` (defined in `template.yaml` as the `ScanScheduleRule` EventBridge rule).
`/interval Nm|Nh|Nd` updates the rule directly via `events:PutRule`. The change persists until the next
`sam deploy`, which resets the rule back to the template default — re-run `/interval` after deployment if
you've customized the schedule. Minimum interval is 1 minute (EventBridge limit).

### Filter Examples

```
# Search Java vacancies on all sites
/addfilter java-all "Java" "" "" djinni,hh

# Search Senior Java in Georgia on HH
/addfilter senior-tbilisi "Senior Java" 2758 5000 hh

# Search remote Java roles
/addfilter remote-java "Java" "" "" remoteok,weworkremotely,remotive
```

### HH Location IDs

| ID | Location |
|----|----------|
| `1` | Moscow |
| `2` | Saint Petersburg |
| `113` | Russia |
| `2758` | Georgia |

## Adding a New Site Parser

1. Create a class implementing `SiteParser` interface
2. Annotate with `@Component`
3. Implement `getSiteKey()` and `parse(VacancyFilter)`
4. Spring auto-registers it in `ParserRegistry`

```java
@Slf4j
@Component
public class MySiteParser implements SiteParser {

    @Override
    public String getSiteKey() { return "mysite"; }

    @Override
    public List<Vacancy> parse(VacancyFilter filter) {
        // implement scraping logic
    }
}
```

## Project Structure

```
src/main/java/com/hrbot/
├── lambda/
│   ├── BotHandlerLambda.java
│   └── ScannerLambda.java
├── bot/
│   ├── MessageSender.java
│   ├── DeploymentNotifier.java
│   ├── callback/
│   │   ├── CallbackHandler.java
│   │   └── CallbackRouter.java
│   └── command/
│       ├── BotCommand.java (interface)
│       ├── CommandRouter.java
│       ├── AddFilterCommand.java
│       ├── ListFiltersCommand.java
│       ├── RemoveFilterCommand.java
│       ├── VacanciesCommand.java
│       ├── ScanCommand.java
│       ├── ReportCommand.java
│       ├── StatusCommand.java
│       ├── VersionCommand.java
│       └── HelpCommand.java
├── parser/
│   ├── SiteParser.java (interface)
│   ├── ParserRegistry.java
│   ├── ParserStatusRegistry.java
│   └── impl/
│       ├── HhParser.java
│       ├── DjinniParser.java
│       ├── GetmatchParser.java
│       ├── LinkedInParser.java
│       ├── IndeedParser.java
│       ├── WellfoundParser.java
│       ├── JobBankParser.java
│       ├── RemoteOkParser.java
│       ├── WeWorkRemotelyParser.java
│       └── RemotiveParser.java
├── ai/
│   ├── DeepSeekClient.java
│   └── VacancyRelevanceService.java
├── model/
│   ├── Vacancy.java
│   ├── VacancyFilter.java
│   ├── ScanResult.java
│   └── HhArea.java
└── config/
    └── AppConfig.java

cookie-refresher/         — Python Lambda, refreshes Wellfound cookies via Playwright
```

## License

MIT
