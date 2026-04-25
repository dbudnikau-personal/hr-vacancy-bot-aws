---
title: HR Vacancy Bot
emoji: 🤖
colorFrom: blue
colorTo: green
sdk: docker
app_port: 8080
pinned: false
---

# HR Vacancy Bot

A Telegram bot that monitors job vacancy sites and notifies HR about new or updated vacancies based on configurable filters.

## Features

- 🔍 Monitors multiple job sites simultaneously
- 🆕 Detects new vacancies and changes in existing ones
- 📬 Sends notifications directly to Telegram
- ⚙️ Configurable filters per chat (keywords, location, salary, sites)
- 🔌 Pluggable parser architecture — easy to add new sites

## Supported Sites

| Site | Method | Status |
|------|--------|--------|
| [Djinni](https://djinni.co) | HTML scraping (Jsoup) | ✅ Working |
| [HH.ru](https://hh.ru) | HTML scraping (Jsoup) | ✅ Working |
| LinkedIn | Playwright (planned) | 🚧 Not implemented |

## Tech Stack

- **Java 21** + **Spring Boot 3.2**
- **PostgreSQL** — vacancy storage and deduplication
- **Flyway** — database migrations
- **Jsoup** — HTML parsing
- **Playwright** — JS-rendered pages (planned)
- **TelegramBots 9.2** — bot API
- **Docker Compose** — local and production deployment

## Quick Start

### Prerequisites

- Java 21
- Docker + Docker Compose (or Colima on macOS)
- Telegram bot token from [@BotFather](https://t.me/BotFather)

### Setup

```bash
git clone https://github.com/dbudnikau-personal/hr-vacancy-bot.git
cd hr-vacancy-bot

cp .env.example .env
# Edit .env — set BOT_TOKEN
```

### Run locally

```bash
# Start PostgreSQL
docker-compose up postgres -d

# Run the app
export $(cat .env | xargs) && mvn spring-boot:run
```

### Run with Docker

```bash
docker-compose up -d
```

## Configuration

Edit `.env`:

```env
BOT_TOKEN=your_telegram_bot_token

SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/hrbot
SPRING_DATASOURCE_USERNAME=hrbot
SPRING_DATASOURCE_PASSWORD=hrbot

# Cron schedule (default: every 2 hours)
BOT_SCAN_CRON=0 0 */2 * * *
```

## Bot Commands

| Command | Description |
|---------|-------------|
| `/addfilter <name> <keywords> [location] [salaryMin] [sites]` | Add vacancy filter |
| `/filters` | List active filters |
| `/removefilter <id>` | Deactivate filter by ID |
| `/vacancies` | Browse all saved vacancies with inline Prev/Next pagination |
| `/vacancies <keyword>` | Search vacancies by keyword in title or company |
| `/report <site>` | Export all saved vacancies for a site to CSV (e.g. `/report hh`) |
| `/scan` | Trigger manual scan for all active filters |
| `/scan <filter_id>` | Trigger manual scan for a specific filter |
| `/status` | Show parser health status |
| `/help` | Show available commands |

### Filter Examples

```
# Search Java vacancies on all sites
/addfilter java-all "Java" "" "" djinni,hh

# Search Senior Java in Georgia on HH
/addfilter senior-tbilisi "Senior Java" 2758 5000 hh

# Search Java Spring on Djinni
/addfilter spring-djinni "Java Spring" "" "" djinni
```

### HH Location IDs

| ID | Location |
|----|----------|
| `1` | Moscow |
| `2` | Saint Petersburg |
| `113` | Russia |
| `2758` | Georgia |

## Architecture

```
Scheduler
    └── VacancyService
            ├── DjinniParser (Jsoup)
            ├── HhParser (Jsoup)
            └── LinkedInParser (Playwright, planned)
                        │
                DiffDetectorService (PostgreSQL)
                        │
                NotificationService
                        │
                TelegramBot
```

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
├── bot/
│   ├── TelegramBot.java
│   ├── MessageSender.java
│   └── command/
│       ├── BotCommand.java (interface)
│       ├── CommandRouter.java
│       ├── AddFilterCommand.java
│       ├── ListFiltersCommand.java
│       ├── RemoveFilterCommand.java
│       └── HelpCommand.java
├── parser/
│   ├── SiteParser.java (interface)
│   ├── ParserRegistry.java
│   └── impl/
│       ├── DjinniParser.java
│       ├── HhParser.java
│       └── LinkedInParser.java
├── scheduler/
│   └── VacancyScanScheduler.java
├── service/
│   ├── VacancyService.java
│   ├── FilterService.java
│   ├── DiffDetectorService.java
│   └── NotificationService.java
├── model/
│   ├── Vacancy.java
│   ├── VacancyFilter.java
│   └── ScanResult.java
├── repository/
│   ├── VacancyRepository.java
│   └── FilterRepository.java
└── config/
    └── AppConfig.java
```

## License

MIT
