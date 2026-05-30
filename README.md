# Guest Database

A personal guest database populated by my reservation system.

## How data flows in

**Step 1 — Reservation email**

When a guest books online, the reservation system sends an email. An automation parses that email and calls `POST /stays/upsert` with the guest's name, dates, and invoice. The guest field is optional — a sparse call without it creates the stay without a linked guest.

**Step 2 — Scraper enrichment**

Separately, I have a web scraper that calls `GET /stays/without-guest` to find stays not yet linked to a guest. For each stay it pulls full details from the reservation system, then calls `POST /stays/upsert` again with the complete data including the guest. The upsert:
- Updates all stay fields unconditionally
- Upserts the guest by `externalId` (creates if new, updates if returning)
- Accumulates any new phone numbers, emails, or addresses

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

| Method | Path | Description                                      |
|--------|------|--------------------------------------------------|
| `POST` | `/stays/upsert` | Create or update one or more stays               |
| `GET` | `/stays?from=&to=` | Get stays within a date range                    |
| `GET` | `/stays/briefing?from=&to=` | Get a summary of scheduled stays in a date range |
| `GET` | `/stays/without-guest` | Get stays not yet linked to a guest              |
| `POST` | `/stays/{externalId}/cancel` | Cancel a stay                                    |
| `POST` | `/guests` | Create a guest manually                          |
| `GET` | `/guests/{externalId}/history` | Get a guest's stay count and last stay details   |
| `POST` | `/database/backup` | Back up the database                             |

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
- `cancel_stay.sh` — cancel a stay
- `get_guest_history.sh` — get a guest's stay count and last stay details

**Database**
- `backup.sh` — does a hot backup of the database to the same directory as the database
- `clear.sh` — clear all data (requires `test` profile)

## Tech

- Kotlin, Spring Boot, Spring Data JPA
- SQLite (database file stored at `DB_PATH`, defaults to `./data/guest_database.db`)
- Docker image published to GitHub Container Registry via GitHub Actions on push to `main`
