# ✂️ Snip — URL Shortener at Scale

A production-grade URL shortener demonstrating high-throughput system design, multi-layer caching, event-driven analytics, and modern cloud-native architecture.

![Architecture](docs/architecture-badge.svg)

## 🏗️ Architecture

```
┌──────────┐    ┌────────────────┐    ┌──────────────┐
│  React   │───▶│     Nginx      │───▶│   Shortener  │
│ Frontend │    │ (Reverse Proxy │    │   Service    │
│ (Vite)   │    │  + Rate Limit) │    │ (Spring Boot)│
└──────────┘    └────────────────┘    └──────┬───────┘
                                             │
                    ┌────────────────────────┬┴────────────┐
                    │                        │             │
             ┌──────┴──────┐     ┌───────────┴──┐  ┌──────┴───────┐
             │ PostgreSQL  │     │    Redis      │  │   Kafka      │
             │ (URL Store) │     │ (Cache +      │  │ (Click       │
             │             │     │  Rate Limit)  │  │  Events)     │
             └─────────────┘     └──────────────┘  └──────┬───────┘
                                                          │
                                                   ┌──────┴───────┐
                                                   │  Analytics   │
                                                   │  Consumer    │
                                                   └──────────────┘
```

## ✨ Key Features

| Feature | Description |
|:---|:---|
| **Base62 Short Codes** | 7-char codes supporting 3.5 trillion unique URLs |
| **Multi-Layer Caching** | Redis write-through + read-through with TTL |
| **Rate Limiting** | Token bucket via Redis Lua scripts (atomic) |
| **Async Analytics** | Kafka-powered click tracking (decoupled from redirect path) |
| **API Key Auth** | Per-key rate limits, ownership enforcement |
| **Auto-Expiration** | Scheduled cleanup of expired URLs |
| **Device Detection** | User-Agent parsing for analytics breakdown |
| **Dark Mode Dashboard** | React + Recharts with glassmorphism design |

## 🛠️ Tech Stack

| Layer | Technology |
|:---|:---|
| Backend | Java 21 + Spring Boot 3.3 |
| Database | PostgreSQL 16 (Flyway migrations) |
| Cache | Redis 7 (caching + rate limiting) |
| Message Queue | Apache Kafka (KRaft mode) |
| Frontend | React 18 + Vite 6 + Recharts |
| Reverse Proxy | Nginx (rate limiting, security headers) |
| Containerization | Docker + Docker Compose |
| API Docs | OpenAPI 3 / Swagger UI |
| Testing | JUnit 5 + Mockito |

## 🚀 Quick Start

### Prerequisites
- Docker & Docker Compose
- Java 21 (for local dev)
- Node.js 20+ (for frontend dev)

### Run with Docker Compose

```bash
# Clone the repository
git clone <repo-url>
cd url-shortener

# Start all services
docker compose up --build

# The app will be available at:
# - Frontend:    http://localhost:3000
# - API:         http://localhost:8080
# - Swagger UI:  http://localhost:8080/api/swagger-ui
# - Nginx proxy: http://localhost (port 80)
```

### Local Development

```bash
# Start infrastructure only
docker compose up postgres redis kafka -d

# Run the backend
cd shortener-service
mvn spring-boot:run

# Run the analytics consumer
cd analytics-consumer
mvn spring-boot:run

# Run the frontend
cd frontend
npm install
npm run dev
```

## 📡 API Reference

### Authentication
All `/api/*` endpoints require an `X-API-Key` header.

### Endpoints

| Method | Endpoint | Description | Auth |
|:---|:---|:---|:---:|
| `POST` | `/api/v1/auth/keys` | Create API key | ❌ |
| `POST` | `/api/v1/shorten` | Shorten a URL | ✅ |
| `GET` | `/{shortCode}` | Redirect to original URL | ❌ |
| `GET` | `/api/v1/urls` | List your URLs (paginated) | ✅ |
| `GET` | `/api/v1/urls/{shortCode}` | Get URL details | ✅ |
| `DELETE` | `/api/v1/urls/{shortCode}` | Deactivate a URL | ✅ |
| `GET` | `/api/v1/analytics/{shortCode}` | Get click analytics | ✅ |

### Example: Shorten a URL

```bash
# 1. Create an API key
curl -X POST http://localhost:8080/api/v1/auth/keys \
  -H "Content-Type: application/json" \
  -d '{"name": "my-app"}'

# 2. Shorten a URL
curl -X POST http://localhost:8080/api/v1/shorten \
  -H "Content-Type: application/json" \
  -H "X-API-Key: usk_your_api_key_here" \
  -d '{"longUrl": "https://github.com", "ttlDays": 30}'

# 3. Access the short URL
curl -L http://localhost:8080/abc1234
```

## 🧪 Testing

```bash
cd shortener-service
mvn test
```

## 📂 Project Structure

```
url-shortener/
├── docker-compose.yml          # Full stack orchestration
├── shortener-service/          # Core Spring Boot API
│   ├── controller/             # REST endpoints
│   ├── service/                # Business logic
│   ├── model/                  # JPA entities
│   ├── repository/             # Data access
│   ├── config/                 # Redis, Kafka, Security configs
│   ├── util/                   # Base62 encoder
│   └── exception/              # Global error handling
├── analytics-consumer/         # Kafka click event processor
├── frontend/                   # React + Vite dashboard
└── nginx/                      # Reverse proxy config
```

## 🎯 System Design Decisions

### Why Base62 over UUID?
UUIDs are 36 chars. Base62 encoding of a sequential DB ID produces 7-char codes that are URL-friendly and support 3.5 trillion unique values.

### Why Kafka for analytics?
The redirect path is the **hot path** — it must be fast. By publishing click events to Kafka asynchronously, we keep redirect latency <10ms while analytics processing happens out-of-band.

### Why Redis for rate limiting?
Redis Lua scripts provide **atomic** check-and-decrement operations, preventing race conditions in distributed environments. The token bucket algorithm provides smooth rate limiting.

### Caching Strategy
- **Write-through**: URL mappings are cached in Redis on creation
- **Read-through**: On cache miss during redirect, the DB result is cached for next time
- **TTL-based eviction**: Cache entries expire to prevent serving stale data

## 📄 License

MIT
