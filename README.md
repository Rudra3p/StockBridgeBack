# StockBridge Backend (Spring Boot 3.x)

Backend service for **StockBridge** — a B2B surplus inventory marketplace connecting businesses (manufacturers, distributors, wholesalers) with retailers.

---

## Tech Stack
- **Language**: Java 21 LTS
- **Framework**: Spring Boot 3.4.3
- **Security**:
  - API Key Filter (`X-API-KEY` checked via constant-time `MessageDigest.isEqual`)
  - Stateless JWT Authentication with `@EnableMethodSecurity`
  - Strict CORS policy (no wildcard origins, allows `X-API-KEY` and `Authorization`)
  - Rate Limiting via Bucket4j (10 requests/min per IP on `/api/auth/login` and `/api/auth/register`)
- **Persistence**: Spring Data JPA & PostgreSQL 16 (Flyway database migrations)
- **Monitoring**: Spring Actuator (`/actuator/health` for cloud health checks)
- **API Documentation**: SpringDoc OpenAPI 3.0 / Swagger UI (enabled in `dev` profile only)
- **Build Tool**: Maven Wrapper (`mvnw` / `mvnw.cmd`)

---

## Security Layers

Every request to `/api/**` passes through two sequential security filters:

```
Request to /api/**
       │
       ▼
[OPTIONS Preflight?] ─── YES ───► Allow immediately (CORS)
       │ NO
       ▼
[/actuator/health?]  ─── YES ───► Allow immediately (Health check)
       │ NO
       ▼
[ApiKeyFilter]       ─── Missing/Invalid "X-API-KEY" ───► 401 {"message": "Invalid API key"}
       │ Valid Key
       ▼
[RateLimitingFilter] ─── Exceeded 10 req/min/IP ───────► 429 {"status": 429, "message": "Rate limit exceeded..."}
       │ Within Limit
       ▼
[JwtFilter]          ─── Protected endpoint missing/invalid JWT ───► 401 Unauthorized
       │ Valid JWT / Public Auth Endpoint
       ▼
[Controller Handler] ─── Executes business logic ───► 200 / 201 Response
```

1. **API Key Filter**: Requires `X-API-KEY` header matching `BACKEND_API_KEY`.
2. **Rate Limiting**: Limits login and registration attempts to 10 requests/minute per client IP (respects `X-Forwarded-For`).
3. **JWT Filter**: Applies to all non-public endpoints. Public endpoints (`/api/auth/login`, `/api/auth/register`) require only the API key.

---

## Profiles & Configuration

| Feature | `dev` Profile (Default) | `prod` Profile |
| :--- | :--- | :--- |
| **Active Profile** | `SPRING_PROFILES_ACTIVE=dev` | `SPRING_PROFILES_ACTIVE=prod` |
| **Database** | Local PostgreSQL (`localhost:5432/stockbridge`) | Cloud PostgreSQL via `${DATABASE_URL}`, `${DB_USER}`, `${DB_PASSWORD}` |
| **API Key** | Default: `dev-stockbridge-api-key-2026` | Strictly required via `${BACKEND_API_KEY}` (fails to start if missing) |
| **JWT Secret** | Safe local fallback default | Strictly required via `${JWT_SECRET}` |
| **CORS Origins** | `http://localhost:3000` | Strictly required via `${ALLOWED_ORIGINS}` (no wildcard) |
| **Swagger UI** | **Enabled** at `/swagger-ui.html` | **Disabled** |

---

## Running Locally

### 1. Start PostgreSQL (Docker)
```bash
docker compose up -d
```

### 2. Run with Dev Profile
```powershell
cd StockBridgeBack
.\mvnw.cmd spring-boot:run
```

- Backend runs on `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health check: `http://localhost:8080/actuator/health`
- Default Dev API Key: `dev-stockbridge-api-key-2026`

---

## Deploying to Render

### 1. Create a PostgreSQL Database on Render
1. Go to your Render Dashboard -> **New** -> **PostgreSQL**.
2. Name: `stockbridge-db`.
3. Note the internal connection string or connection parameters (`DATABASE_URL`, `DB_USER`, `DB_PASSWORD`).

### 2. Deploy Web Service using Docker
1. Go to Render Dashboard -> **New** -> **Web Service**.
2. Connect your Git repository (root or `StockBridgeBack` directory).
3. Choose **Docker** as the runtime (pointing to `Dockerfile`).
4. Set the **Health Check Path** to `/actuator/health`.
5. Configure the following **Environment Variables**:

| Variable | Description | Example Value |
| :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | Activates production profile | `prod` |
| `DATABASE_URL` | PostgreSQL connection URL | `jdbc:postgresql://<host>:5432/<dbname>` or Render `postgresql://...` (auto-formatted) |
| `DB_USER` | PostgreSQL username | `stockbridge` |
| `DB_PASSWORD` | PostgreSQL password | `your_secure_db_password` |
| `BACKEND_API_KEY` | Secret API key shared with Next.js | `your_generated_random_64char_key` |
| `JWT_SECRET` | 256-bit secret for signing tokens | `your_secure_256bit_hex_or_base64_secret` |
| `ALLOWED_ORIGINS` | Frontend URL(s) for CORS | `https://stockbridge.vercel.app,http://localhost:3000` |

> **Note**: If `BACKEND_API_KEY` is omitted, the application will deliberately fail startup to prevent deploying an unsecured instance.

---

## Testing the API Key Layer

```bash
# 1. Health check (Excluded from API key check -> 200 OK)
curl http://localhost:8080/actuator/health

# 2. Missing API Key on /api/** (Returns 401 Invalid API key)
curl -i http://localhost:8080/api/auth/login

# 3. Wrong API Key on /api/** (Returns 401 Invalid API key)
curl -i -H "X-API-KEY: wrong-key" http://localhost:8080/api/auth/login

# 4. Valid API Key on /api/auth/login (Proceeds to authentication)
curl -i -X POST http://localhost:8080/api/auth/login \
  -H "X-API-KEY: dev-stockbridge-api-key-2026" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@stockbridge.local","password":"admin123"}'
```
