# Startup, Secure Communication & Authentication Sequence

## Startup

```
STARTUP PHASE
=============

 Docker         Service Registry     User-Service     Booking-Service    API Gateway
 Compose         (Eureka :8761)        (:8081)           (:8082)          (:8080)
   |                  |                   |                 |                |
   |--- 1] start ---->|                   |                 |                |
   |<-- ready --------|                   |                 |                |
   |                                       |                 |                |
   |--- 2] start ------------------------------------------>|                |
   |                  |<-- 3] register ---|                  |                |
   |                  |--- 200 OK -------->|                 |                |
   |<-- ready --------|                                       |                |
   |                                                                          |
   |--- 4] start -------------------------------------------------------------------->|
   |                  |<-- 5] fetch registry -----------------------------------------|
   |                  |--- [User,Booking] -------------------------------------------->|
   |<-- ready -------------------------------------------------------------------------|
```

## Login & JWT Issuance

```
LOGIN FLOW (Centralized Auth via API Gateway)
==============================================

 Client            API Gateway           User-Service        Booking-Service
   |                   |                      |                    |
   |-- POST /api/auth/login ----------------->|                    |
   |   {username, pwd} |                      |                    |
   |                   |-- validate creds ---->|                    |
   |                   |<-- user ok ----------|                    |
   |                   |                      |                    |
   |                   | (issue JWT with      |                    |
   |                   |  roles/subject/exp)   |                    |
   |<-- 200 OK --------|                      |                    |
   |   {jwt_token}     |                      |                    |
```

## Authenticated Request Flow

```
AUTHENTICATED REQUEST (e.g. GET /api/users)
============================================

 Client            API Gateway           User-Service        Booking-Service
   |                   |                      |                    |
   |-- GET /api/users ----------------------->|                    |
   |   Bearer: JWT     |                      |                    |
   |                   |                      |                    |
   |                   | 1] Validate JWT      |                    |
   |                   |    - signature       |                    |
   |                   |    - expiry          |                    |
   |                   | 2] Extract roles     |                    |
   |                   |                      |                    |
   |                   |-- forward + JWT ---->|                    |
   |                   |                      |                    |
   |                   |                      | 3] Validate JWT    |
   |                   |                      |    (Spring Security)|
   |                   |                      | 4] Check RBAC      |
   |                   |                      |    (e.g. ROLE_ADMIN)|
   |                   |                      |                    |
   |                   |<-- response ---------|                    |
   |<-- response ------|                      |                    |
```

## Inter-Service Call with OpenFeign (JWT Propagation)

```
SERVICE-TO-SERVICE (Booking -> User via OpenFeign)
===================================================

 Client            API Gateway           Booking-Service      User-Service
   |                   |                      |                    |
   |-- POST /api/bookings ------------------->|                    |
   |   Bearer: JWT     |                      |                    |
   |                   |-- forward + JWT ---->|                    |
   |                   |                      |                    |
   |                   |                      | 1] Validate JWT    |
   |                   |                      | 2] Check RBAC      |
   |                   |                      |    (e.g. ROLE_USER) |
   |                   |                      |                    |
   |                   |                      | 3] OpenFeign call   |
   |                   |                      |    GET /internal/   |
   |                   |                      |    users/validate   |
   |                   |                      |    (propagates JWT  |
   |                   |                      |     via interceptor)|
   |                   |                      |-------- JWT ------->|
   |                   |                      |                    |
   |                   |                      |                    | 4] Validate JWT
   |                   |                      |                    | 5] Check internal
   |                   |                      |                    |    role
   |                   |                      |<-- user valid -----|
   |                   |                      |                    |
   |                   |                      | 6] Process booking |
   |                   |<-- response ---------|                    |
   |<-- response ------|                      |                    |
```

## Token Details

```
JWT STRUCTURE
==============

Header:    { "alg": "RS256", "typ": "JWT" }
Payload:   {
             "sub": "user@example.com",
             "roles": ["ROLE_USER"],
             "iss": "api-gateway",
             "exp": 1719000000,
             "iat": 1718913600
           }
```

## Security Summary

| Component       | Responsibility                                              |
|----------------|-------------------------------------------------------------|
| API Gateway    | Login endpoint, issues JWT, validates on incoming requests   |
| User-Service   | Stores credentials, validates login, validates JWT locally   |
| Booking-Service| Validates JWT via Spring Security, enforces RBAC on endpoints |
| OpenFeign      | Propagates JWT between services via request interceptor      |
| Service Registry| Handles discovery only — no security role                    |

## Key Design Decisions

- **Centralized auth**: Login only at API Gateway — single point of credential validation
- **Decentralized validation**: Each service validates the JWT independently (no gateway bottleneck for internal calls)
- **JWT propagation**: OpenFeign `RequestInterceptor` forwards the Bearer token from the incoming request to outgoing service calls
- **RBAC**: Spring Security method-level (`@PreAuthorize`) or filter-level role checks in each service
- **No HTTPS on localhost**: For a learning project on a single machine, HTTP + JWT is sufficient. HTTPS would be added in production with a reverse proxy
