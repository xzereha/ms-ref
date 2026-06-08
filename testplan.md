# Test Plan

## Overview

Microservices project with 5 modules: `common`, `service-registry`, `api-gateway`, `user-service`, `booking-service`. Currently has only 4 context-loading smoke tests. This plan adds unit, web-layer, data-layer, and E2E tests plus a GitHub Actions CI workflow.

---

## CI Workflow (`.github/workflows/ci.yml`)

Two jobs:
1. **`test`** — `./gradlew build` (compiles + runs all Gradle tests), uploads test reports + JARs
2. **`e2e`** — needs `test`, downloads JARs, starts `docker compose up -d`, runs smoke tests (health checks, register, login, create booking, list bookings), collects logs on failure

---

## Test Categories

### A. Unit Tests — no Spring context, `@ExtendWith(MockitoExtension.class)`

| Test class | Module | What to test |
|---|---|---|
| `JwtUtilTest` | `common` | Generate → validate round-trip; expired token; invalid signature; missing keys |
| `UserServiceTest` | `user-service` | Register success; duplicate email rejection; validate creds correct/wrong; getUserById found/missing |
| `BookingServiceTest` | `booking-service` | Create booking; list by user; get by id (own user vs other user) |

### B. Web Layer Tests — `@WebMvcTest(Controller.class)`, `@MockBean` services

| Test class | Module | What to test |
|---|---|---|
| `AuthControllerTest` | `user-service` | `POST /api/auth/register` — success / duplicate email |
| `InternalUserControllerTest` | `user-service` | `POST /internal/users/validate` — valid/invalid creds; `GET /internal/users/{id}` — found/404 |
| `BookingControllerTest` | `booking-service` | `POST /api/bookings` — create; `GET /api/bookings` — list; missing/invalid JWT |
| `AuthControllerTest` | `api-gateway` | `POST /api/auth/login` — success / invalid creds; `POST /api/auth/register` — mock `UserServiceClient` + `JwtUtil` |
| `BookingProxyControllerTest` | `api-gateway` | `POST /api/bookings` — proxy; `GET /api/bookings` — proxy; missing/invalid JWT |

### C. Data Layer Tests — `@DataJpaTest`, H2 auto-config

| Test class | Module | What to test |
|---|---|---|
| `UserRepositoryTest` | `user-service` | `save`, `findByEmail`, `existsByEmail` |
| `BookingRepositoryTest` | `booking-service` | `save`, `findByUserIdOrderByDateDesc` |

### D. Context Smoke Tests — existing, keep as-is

- `ServiceRegistryApplicationTests`
- `ApiGatewayApplicationTests`
- `UserServiceApplicationTests`
- `BookingServiceApplicationTests`

Update gateway test to also add `vault.enabled=false` if missing.

### E. E2E Tests — Docker Compose, separate CI job

- Health check all 4 services via `/actuator/health`
- Register user → login → extract JWT
- Create booking with JWT
- List bookings with JWT
- Assert no errors

---

## Test Configuration

All `@SpringBootTest` / `@WebMvcTest` that touch Eureka or Vault should set:

```
eureka.client.enabled=false
vault.enabled=false
```

For `api-gateway` tests, also mock `UserServiceClient` and `BookingServiceClient` (via `@MockBean`), not WireMock.

---

## Order of Implementation

1. Create `.github/workflows/ci.yml`
2. Write `JwtUtilTest` (common, no Spring needed)
3. Write `UserServiceTest` + `BookingServiceTest` (unit tests)
4. Write `AuthControllerTest` + `InternalUserControllerTest` (user-service web)
5. Write `BookingControllerTest` (booking-service web)
6. Write `AuthControllerTest` + `BookingProxyControllerTest` (api-gateway web)
7. Write `UserRepositoryTest` + `BookingRepositoryTest` (data layer)
8. Update smoke test configs if needed
9. Verify with `./gradlew build` that all tests pass
