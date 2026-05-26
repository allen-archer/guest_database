# Guest Database

A personal guest database populated by my reservation system.

## How data flows in

**Step 1 — Reservation email**

When a guest books online, the reservation system sends an email. An automation parses that email and calls `POST /stays` to create a stub stay with the guest's name, dates, and invoice.

**Step 2 — Scraper enrichment**

Separately, I have a web scraper that gets extra data and calls `GET /stays/without-guest` to find stays that haven't been enriched yet. For each stay it uses the `externalId` to pull full details from the reservation system, then calls `POST /stays/enrich` to:
- Upsert the guest by `externalId` (creates if new, updates if returning)
- Accumulate any new phone numbers, emails, or addresses
- Populate stay details like arrival time, dietary restrictions, and special accommodations

## Building

**With Gradle:**
```bash
./scripts/build_jar.sh
```

**With Docker:**
```bash
./scripts/build_image.sh
```

## Running locally

**With Gradle:**
```bash
./scripts/run_jar.sh
```

Note: _you do not need to build the image before running the container._ It is publicly available from Github's registry.

**With Docker:**
```bash
./scripts/run_container.sh
```

The `test` Spring profile enables the `DELETE /database/clear` endpoint for resetting the database during development.

## API

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/stays` | Create a stub stay from a reservation email |
| `GET` | `/stays?from=&to=` | Get stays within a date range |
| `GET` | `/stays/without-guest` | Get stays not yet enriched by the scraper |
| `POST` | `/stays/enrich` | Enrich one or more stays with guest and stay details |
| `POST` | `/guests` | Create a guest manually |

## Scripts

The `scripts/` directory contains scripts for building, running, and exercising the API:

**Build**
- `build_jar.sh` — build the jar via Gradle
- `build_image.sh` — build the Docker image

**Run**
- `run_jar.sh` — start the app via Gradle
- `run_container.sh` — start the app via Docker

**API**
- `create_stay.sh` — create a stay stub then enrich it
- `create_stay_without_guest.sh` — create a stay stub with no guest (for testing the scraper queue)
- `get_stays.sh` — get all stays within a date range
- `get_stays_without_guest.sh` — get all unenriched stays

**Database**
- `backup.sh` — does a hot backup of the database to the same directory as the database
- `clear.sh` — clear all data (requires `test` profile)

## Tech

- Kotlin, Spring Boot, Spring Data JPA
- SQLite (database file stored at `DB_PATH`, defaults to `./data/guest_database.db`)
- Docker image published to GitHub Container Registry via GitHub Actions on push to `main`
