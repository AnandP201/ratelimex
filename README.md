# Ratelimex

Ratelimex is a Spring Boot distributed rate limiter with tenant-aware API policies.

It combines:

- PostgreSQL/H2-backed dynamic tenant API configuration
- Caffeine in-memory config caching
- Redis Lua token bucket execution
- Per-tenant, per-API, and per-user quota buckets
- Per-policy fail-open/fail-closed behavior
- Actuator/Micrometer metrics

## Runtime Flow

```text
request
  -> validate tenantId/userId/api/cost
  -> load tenant + API policy from Caffeine cache
  -> cache miss loads from tenant_api_limits table
  -> reject if API is disabled or not configured for tenant
  -> atomically consume Redis buckets with Lua
  -> return allowed / 429 / degraded decision
```

## Admin API

Create or update a tenant API policy:

```http
POST /admin/tenants/acme/apis
Content-Type: application/json

{
  "api": "/payments/charge",
  "enabled": true,
  "failureMode": "FAIL_CLOSED",
  "tenantLimit": {
    "capacity": 10000,
    "refillTokensPerSecond": 1000,
    "ttlSeconds": 120
  },
  "apiLimit": {
    "capacity": 1000,
    "refillTokensPerSecond": 100,
    "ttlSeconds": 120
  },
  "userLimit": {
    "capacity": 60,
    "refillTokensPerSecond": 10,
    "ttlSeconds": 120
  }
}
```

List policies:

```http
GET /admin/tenants/acme/apis
```

Delete one policy:

```http
DELETE /admin/tenants/acme/apis?api=/payments/charge
```

## Rate Limit Check API

```http
POST /api/rate-limit/check
Content-Type: application/json

{
  "tenantId": "acme",
  "userId": "user-123",
  "api": "/payments/charge",
  "cost": 1
}
```

Allowed response:

```json
{
  "allowed": true,
  "reason": "allowed",
  "remainingTokens": 59,
  "retryAfterMillis": 0,
  "degraded": false
}
```

If the API is not configured or is disabled for the tenant, the service returns `403` with reason `api_not_enabled_for_tenant`.

If the limit is exceeded, the service returns `429` with `Retry-After`.

## Redis Key Design

Each decision consumes three buckets in one Lua call:

```text
ratelimex:{namespace:tenantHash}:tenant:all
ratelimex:{namespace:tenantHash}:api:apiHash
ratelimex:{namespace:tenantHash}:user:userHash
```

The shared hash tag keeps the keys in one Redis Cluster slot so the Lua script can atomically check all buckets. The tradeoff is that very large tenants can become hot slots; shard large tenants by namespace if needed.

## Failure Behavior

Failure behavior is configured per tenant API policy:

- `FAIL_CLOSED`: block when Redis is unavailable.
- `FAIL_OPEN`: allow when Redis is unavailable and mark the decision as degraded.

Use `FAIL_CLOSED` for expensive or risky write APIs. Use `FAIL_OPEN` for low-risk read APIs where availability is more important than strict enforcement.

## Metrics

Actuator exposes metrics at `/actuator/metrics`.

Useful meters:

```text
ratelimit.decisions
ratelimit.redis.lua.latency
```

Decision counters are tagged by `outcome`, `degraded`, and `reason`.

## Local Run

Start dependencies:

```bash
docker compose up -d postgres redis
```

Start the app in Docker:

```bash
docker compose --profile app up --build
```

Run the app:

```bash
./mvnw spring-boot:run
```

For local development, the app defaults to H2. To use PostgreSQL from Docker Compose:

```bash
DB_URL=jdbc:postgresql://localhost:5432/ratelimex
DB_USERNAME=ratelimex
DB_PASSWORD=ratelimex
DB_DRIVER=org.postgresql.Driver
```

## CI/CD

The workflow in `.github/workflows/ci-cd.yml`:

- runs `./mvnw verify` for pull requests and pushes
- builds the Docker image after tests pass
- pushes `latest` and `sha-<commit>` images to GitHub Container Registry on pushes to `main` or `master`
- deploys to Kubernetes only when the repository variable `ENABLE_K8S_DEPLOY` is `true`

Image publishing uses the workflow's built-in `GITHUB_TOKEN`; no registry password is required. In the GitHub repository, ensure Actions has read and write package permission under **Settings > Actions > General > Workflow permissions**.

Before enabling deployment, add:

- repository secret `KUBE_CONFIG_BASE64`: base64-encoded kubeconfig for a cluster reachable from GitHub-hosted runners
- repository variable `ENABLE_K8S_DEPLOY`: `true`
- optional repository variable `KUBE_NAMESPACE`: target namespace, otherwise `default`
- a protected GitHub environment named `production` if deployment approval is required

Create the runtime secret in the target namespace:

```bash
kubectl create secret generic ratelimex-secrets \
  --from-literal=DB_URL='jdbc:postgresql://postgres:5432/ratelimex' \
  --from-literal=DB_USERNAME='ratelimex' \
  --from-literal=DB_PASSWORD='change-me' \
  --from-literal=REDIS_URL='redis://redis:6379' \
  --from-literal=ADMIN_API_KEY='change-me' \
  --from-literal=RATELIMEX_NAMESPACE='prod'
```

For a private GHCR package, also create the image pull secret referenced by the deployment:

```bash
kubectl create secret docker-registry ghcr-pull-secret \
  --docker-server=ghcr.io \
  --docker-username='<github-user>' \
  --docker-password='<token-with-read-packages>'
```

The repository currently uses `master`. The workflow also supports `main`, so it will continue working if the default branch is renamed.

## Production Deployment

Build the image:

```bash
docker build -t ratelimex:latest .
```

Run with the production profile:

```bash
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/ratelimex \
  -e DB_USERNAME=ratelimex \
  -e DB_PASSWORD=ratelimex \
  -e REDIS_URL=redis://host.docker.internal:6379 \
  -e RATELIMEX_NAMESPACE=prod \
  -e ADMIN_API_KEY=change-me \
  ratelimex:latest
```

Production profile expectations:

- `/actuator/health/liveness` and `/actuator/health/readiness` are enabled.
- `spring.jpa.hibernate.ddl-auto=validate`; schema must already exist.
- database, Redis, and namespace values come from environment variables.
- `/admin/**`, `/actuator/info`, and `/actuator/metrics/**` require `X-Admin-Api-Key`.
- the Docker image runs as a non-root user.

## Admin API Security

Admin and diagnostic endpoints use stateless API-key authentication:

```http
X-Admin-Api-Key: your-admin-key
```

Access rules:

```text
/api/rate-limit/check      public/internal caller endpoint
/actuator/health/**        public health probes
/admin/**                  requires admin API key
/actuator/info             requires admin API key
/actuator/metrics/**       requires admin API key
```

Configure the key with:

```properties
ratelimex.security.admin-api-key=${ADMIN_API_KEY}
ratelimex.security.admin-api-key-header=${ADMIN_API_KEY_HEADER:X-Admin-Api-Key}
```

## Production Backlog

High-value next additions:

- Testcontainers Redis/PostgreSQL integration tests
- Prometheus registry and Grafana dashboard
- config-change events with Redis Pub/Sub or Kafka
- Redis Sentinel/Cluster profile
- load tests with k6 or Gatling
