# Guest Database

A personal guest database populated by my reservation system.

## How data flows in

**Step 1 — Confirmation email**

When a guest books online, the reservation system sends a confirmation email. An automation parses that email and calls `POST /stays/upsert-by-confirmation` with the confirmation code, guest name, dates, and invoice. This creates a lightweight stay record with no external ID or linked guest profile.

**Step 2 — Cancellation email**

If a guest cancels, the reservation system sends a cancellation email. An automation parses it and calls `POST /stays/cancel` with the room names and check-in date to match the stay and mark it canceled.

**Step 3 — Scraper enrichment**

A web scraper calls `GET /stays/without-guest` to find stays not yet linked to a guest profile. For each one it pulls full details from the reservation system, then calls `POST /stays/upsert` with the complete data including the guest. The upsert:
- Sets the external ID and updates all stay fields unconditionally
- Upserts the guest by `externalId` (creates if new, updates if returning)
- Accumulates any new phone numbers, emails, or addresses without replacing existing ones

**Step 4 — Invoice updates**

When a stay is modified after booking (added nights, add-ons, etc.), an automation calls `POST /stays/update-invoice` with the updated invoice. Check-in and check-out dates are derived from the invoice item dates.

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

Note: _you do not need to build the image before running the container._ It is publicly available from GitHub's registry.

**With Docker:**
```bash
./scripts/run_container.sh
```

The `test` Spring profile enables the `DELETE /database/clear` endpoint for resetting the database during development.

## Authentication

All endpoints require HTTP Basic or form login. There are two roles:

- **ADMIN** — full access (all GET and POST endpoints)
- **READER** — read-only (GET endpoints only)

Users are configured via environment variables as comma-separated `username:password` pairs:

| Variable | Default | Description |
|---|---|---|
| `GUEST_DB_ADMINS` | `admin:changeme` | Admin users |
| `GUEST_DB_READERS` | `reader:changeme` | Read-only users |
| `GUEST_DB_REMEMBER_ME_KEY` | `changeme-key` | Key for remember-me cookies (14-day validity) |

Multiple users per role: `GUEST_DB_ADMINS=alice:pass1,bob:pass2`

## API

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/stays/upsert` | Create or update one or more stays by external ID |
| `POST` | `/stays/upsert-by-confirmation` | Create or update a stay by confirmation code |
| `POST` | `/stays/update-invoice` | Replace a stay's invoice by confirmation code |
| `POST` | `/stays/cancel` | Cancel a stay by matching room names and check-in date |
| `GET` | `/stays?from=&to=` | Get stays within a date range |
| `GET` | `/stays/briefing?from=&to=` | Get a summary of scheduled stays in a date range |
| `GET` | `/stays/without-guest` | Get stays not yet linked to a guest profile |
| `POST` | `/guests` | Create a guest manually |
| `GET` | `/guests/{externalId}/history` | Get a guest's stay count and last stay details |
| `GET` | `/guests/search?name=&email=&phone=&city=&state=&street=&zip=` | Search guests |
| `POST` | `/database/backup` | Back up the database |

## UI

Two browser UIs are served as static pages:

- `/briefing.html` — daily guest briefing view, showing check-ins, check-outs, and in-house guests for a date range
- `/search.html` — guest search page

## Configuration

Room combo mappings expand a combo room name in the invoice into its individual component rooms in the briefing view. Configured in `application.yaml` under `guest-database.room-combos`:

```yaml
guest-database:
  room-combos:
    "[dogwood-maple suite]":
      - Dogwood Suite
      - Maple Suite
    "[whole main house]":
      - Dogwood Suite
      - Jade Vine Suite
      - Gum Tree Suite
      - Maple Suite
```

## Scripts

The `scripts/` directory contains scripts for building, running, and exercising the API:

**Build**
- `build_jar.sh` — build the jar via Gradle
- `build_image.sh` — build the Docker image

**Run**
- `run_jar.sh` — start the app via Gradle
- `run_container.sh` — start the app via Docker
- `run_tests.sh` — run the test suite

**API**
- `create_stay.sh` — create a stay stub then enrich it with full guest data
- `create_stay_without_guest.sh` — create a stay stub with no guest (for testing the scraper queue)
- `upsert_by_confirmation.sh` — create or update a stay by confirmation code
- `update_invoice.sh` — replace a stay's invoice
- `get_stays.sh` — get all stays within a date range
- `get_stays_briefing.sh` — get the guest briefing for a date range
- `get_stays_without_guest.sh` — get all unenriched stays
- `cancel_stay.sh` — cancel a stay
- `get_guest_history.sh` — get a guest's stay count and last stay details

**Database**
- `backup.sh` — hot backup of the database to the same directory as the database file
- `clear.sh` — clear all data (requires `test` profile)

## Tech

- Kotlin, Spring Boot, Spring Data JPA, Spring Security
- SQLite (database file stored at `DB_PATH`, defaults to `./data/guest_database.db`)
- Docker image published to GitHub Container Registry via GitHub Actions on push to `main`
