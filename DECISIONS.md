# Design Decisions

## 2026-10-26 — Scope and stack (decided before building)
- MVP: create + redirect only, API-only. Stretch features (analytics, custom
  aliases, expiration, QR codes) wait until after the MVP ships.
- Short codes: base62-encode the database row ID. Collisions are impossible
  by construction, no random generation, no uniqueness checks.
- Reads: cache-aside. Redis is checked first; on a miss, read PostgreSQL
  once and populate the cache. PostgreSQL is the source of truth.
- Rate limiting on the create endpoint via Bucket4j.
- Tests: Testcontainers against real Postgres and Redis, not mocks.
- Deliberately skipped: a separate API gateway and load balancer. Theater
  for a single instance — the principles behind them (stateless app, cache
  tier, rate limit at the edge) still apply.

## 2026-11-04 — Walking skeleton
- Docker Compose runs Postgres 16 + Redis 7. The app runs on the host
  during development; it joins Compose at deploy time.
- Postgres gets a named volume (pgdata) so data survives container
  recreation. Redis gets none since it's a cache; its contents can always be
  rebuilt from Postgres.
- Dev environment note: IntelliJ must use its Bundled Maven for this
  project. Its importer fails with a Guice MissingImplementation error
  against the wrapper's Maven 3.10.0, which works fine in the terminal.

## 2026-11-05 — Write path
- POST /api/links creates a link; the short code is base62 of the Postgres
  row ID, so collisions are impossible by construction.
- Two saves per create: insert to get the ID, then store the encoded code.
  short_code is unique in the table.
- Validation on create: non-blank, parseable, http/https scheme with a
  host; anything else gets a 400. (It proved itself during testing and
  caught a typo'd scheme, htpps, exactly as designed.)
- Package by layer: controller / service / repository / entity / util.
  Spring scans the subpackages automatically; no config needed.
- No Lombok: explicit getters/setters while learning the stack, and
  records already cover the DTOs. Revisit if the model grows.
- Redis is untouched so far — the cache belongs to the redirect path
  (Session 3). No rate limiting yet (Session 4).
- server.error.include-message: always stays on for local development
  so API errors explain themselves; it comes off before deploy.
