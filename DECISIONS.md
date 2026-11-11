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

## 2026-11-07 — Hot path
- GET /{code} returns 302 (FOUND) with a Location header; 301
  deliberately not used, so browsers re-ask the shortener every visit.
- Cache-aside on the redirect: Redis key link:{code} is checked first;
  on a miss, Postgres is read via findByShortCode and the result is
  stored in Redis before returning. Unknown codes return 404, and
  nothing is cached for them.
- No TTL on cached links: originalUrl never changes after creation,
  so a cached value cannot go stale. Revisit if links ever become
  editable or expirable.
- RedirectController lives at the root; LinkController keeps
  /api/links. No route clash, and /actuator/health is unaffected.

## 2026-11-09 — Rate limiting
- POST /api/links is limited to 10 creates per minute per client IP,
  enforced with Bucket4j (token bucket, greedy refill) before
  validation or any database write; over the limit returns 429 with
  the wait time in the message.
- The Bucket4j dependency is bucket4j_jdk17-core 8.15.0 — the
  artifacts were renamed at 8.11 (the bucket4j-core name ends at
  8.10.1), so the version and the old artifact name together do
  not exist.
- Spring Boot 4 renamed server.error.* to spring.web.error.* and
  silently ignores the old keys; error messages in response bodies
  are enabled via spring.web.error.include-message — present since
  Session 2 under the old name, effective only from today, which is
  also why the Session 2 400s and Session 3 404s never showed
  their messages.
- Buckets are in-memory per IP (ConcurrentHashMap), reset on
  restart, not shared across instances — right for a single-instance
  MVP; a Redis-backed bucket is the step if the app ever runs
  multi-instance.
- GET /{code} is deliberately not rate-limited: the redirect hot
  path stays fast, and the Session 6 load test hammers it.

## 2026-11-10 — Tests
- Integration tests run against real Postgres 16 and Redis 7 in
  Testcontainers, wired with @ServiceConnection; no test touches
  the local docker compose database.
- Spring Boot 4.1.2 manages Testcontainers 2.x: artifacts are
  testcontainers-postgresql / testcontainers-junit-jupiter and the
  Postgres container lives in org.testcontainers.postgresql — the
  1.x names do not resolve. No versions pinned in pom.xml; the
  parent manages them. (Same rename lesson as Bucket4j, Session 4.)
- Error responses are asserted via the sendError reason
  (getErrorMessage), not the JSON body: MockMvc never performs the
  servlet error dispatch that renders the body, so $.message does
  not exist under MockMvc. Body rendering stays verified by hand
  in Postman.
- Rate limiting is tested as a unit test on RateLimitService, not
  over HTTP: MockMvc requests share one client IP and one
  in-memory bucket, so an HTTP rate-limit test would interfere
  with the integration class's own creates.
- Redis in tests is a GenericContainer with
  @ServiceConnection(name = "redis"); Boot cannot infer the
  service from a generic container, so the name is the hint.
- CI: GitHub Actions runs ./mvnw test on every push and pull
  request to main (JDK 21, temurin). Testcontainers works
  unchanged because GitHub-hosted runners ship Docker.
- UrlShortenerApplicationTests (the Initializr context test) was
  deleted after CI run #2: it booted against the local compose
  database, so it passed locally only because compose was up and
  failed in CI where nothing listens on localhost:5432. It had no
  assertions, and LinkApiIntegrationTest boots the same context
  against real containers anyway — its coverage was a strict
  subset. The suite is now self-contained: ./mvnw test needs only
  the Docker daemon, no compose stack.
