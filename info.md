# Microservices Project — Architecture & Decisions

## Stack

| Layer | Technology |
|---|---|
| Language | Java 25 (built with `--release 25` on JDK 26) |
| Framework | Spring Boot 4.0.6 |
| Cloud | Spring Cloud 2025.1.1 (Oakwood) |
| Build | Gradle 9.5.1 (multi-module) |
| Container | Docker Compose |
| Databases | H2 (in-memory / file-based) |
| Auth | JWT (RS256) |
| Service Discovery | Netflix Eureka |

## Services

| Service | Port | Responsibility |
|---|---|---|
| **service-registry** | 8761 | Eureka server — service discovery |
| **api-gateway** | 8080 | Single entry point, JWT issuance, routing |
| **user-service** | 8081 | Users, credentials, roles |
| **booking-service** | 8082 | Bookings, Feign calls to user-service |
| **common** | — | Shared DTOs, JWT utilities |

## Project Structure

```
microservices/
├── settings.gradle.kts        # Includes all 5 modules
├── build.gradle.kts           # Root: Java 25, Boot 4.0.6, Cloud 2025.1.1
├── docker-compose.yml         # 4 services + healthchecks
├── .gitignore
├── .dockerignore
├── info.md
│
├── common/                    # Shared library (no main class)
│   └── build.gradle.kts       # JJWT only
│
├── service-registry/          # Eureka Server
│   ├── build.gradle.kts       # eureka-server, actuator
│   ├── Dockerfile
│   └── src/main/resources/application.yml
│
├── api-gateway/               # Spring Cloud Gateway + Security
│   ├── build.gradle.kts       # gateway-server-webmvc, eureka-client, security
│   ├── Dockerfile
│   └── src/main/resources/application.yml
│
├── user-service/              # Users + auth
│   ├── build.gradle.kts       # web, security, jpa, eureka-client, h2
│   ├── Dockerfile
│   └── src/main/resources/application.yml
│
└── booking-service/           # Bookings + Feign
    ├── build.gradle.kts       # web, security, jpa, eureka-client, openfeign, h2
    ├── Dockerfile
    └── src/main/resources/application.yml
```

## Startup Sequence

```
Docker Compose
     │
     ├── 1. Start service-registry (Eureka :8761)
     │     ├── Wait for healthcheck (curl /actuator/health)
     │     └── Ready
     │
     ├── 2. Start user-service (:8081)
     │     ├── Register with Eureka
     │     └── Ready
     │
     ├── 3. Start booking-service (:8082)
     │     ├── Register with Eureka
     │     └── Ready
     │
     └── 4. Start api-gateway (:8080)
           ├── Fetch registry from Eureka
           └── Ready
```

## Communication Flow

### Login
```
Client → API Gateway → User-Service (validate credentials)
                       ↓
                       API Gateway issues JWT (RS256-signed)
                       ↓
Client ← JWT token
```

### Authenticated Request (e.g. Create Booking)
```
Client → API Gateway (with JWT)
         │
         ├── Validate JWT signature (offline, no call needed)
         ├── Forward request + JWT to Booking-Service
         │
Booking-Service
         ├── Spring Security validates JWT (public key, offline)
         ├── Extract user_id from JWT `sub` claim
         ├── (Optional) Feign call to User-Service for user details
         ├── Save booking in booking-db with user_id
         └── Return response
```

### Inter-Service Communication
```
Booking-Service ──OpenFeign──→ User-Service
   │                               │
   │  GET /internal/users/{id}     │
   │  JWT propagated via            │
   │  RequestInterceptor            │
   │                               │
   └──── UserDto ──────────────────┘
```

## Security Decisions

### JWT: RS256 (asymmetric)

| Key | Held by | Purpose |
|---|---|---|
| **Private key** | API Gateway only | Sign tokens |
| **Public key** | All backend services | Verify tokens (offline) |

- No shared secret between services
- Verification is offline — no call back to Gateway
- If a service is compromised, attacker can't forge tokens (only has public key)

### Eureka with Basic Auth (planned)
Prevents unauthorized services from registering.

```
eureka.client.serviceUrl.defaultZone=http://eureka:password@service-registry:8761/eureka/
```

### Docker Network Isolation
Docker Compose creates an isolated network. Services are unreachable from outside Docker.

## Database-per-Service Pattern

| Table | Owned by | Stored in |
|---|---|---|
| `users` (email, password_hash, roles) | User-Service | `user-db` |
| `bookings` (id, user_id*, title, date) | Booking-Service | `booking-db` |

- `user_id` in booking-db is a **logical reference** (UUID), no FK constraint
- Cross-service data is fetched via OpenFeign API calls, never via shared database access

### Patterns for Cross-Service Data

#### API Composition (simpler — recommended for this project)
```
Booking-Service calls User-Service via Feign when it needs user details.
Good for read-only. Simple to implement.
```

#### Event-Driven / CQRS (more advanced)
```
booking-service publishes BookingCreated event.
user-service subscribes and stores denormalized copy.
Faster reads, more decoupled. Requires message broker.
```

## Development Workflow

### Update a single service
```sh
docker compose up -d --build user-service
docker compose logs -f user-service
```

### Restart everything
```sh
docker compose down && docker compose up -d
```

### Rebuild everything
```sh
docker compose build
```

## Production Readiness Checklist

### Tier 1 — High impact, low effort
- [x] Eureka self-preservation disabled (dev)
- [ ] Eureka HTTP Basic Auth
- [ ] RS256 JWT (replace HS256 if currently using)
- [ ] Non-root user in Dockerfiles
- [ ] Actuator health endpoint on all services

### Tier 2 — Moderate effort
- [ ] HTTPS on Gateway (self-signed cert)
- [ ] Rate limiting on API Gateway
- [ ] PostgreSQL instead of H2 (with volumes)

### Tier 3 — Nice to have (skip for now)
- [ ] mTLS between services
- [ ] Centralized logging (ELK)
- [ ] Config server (Spring Cloud Config)
- [ ] Circuit breakers (Resilience4j)

## Vault PKI — mTLS between Services (Option 2)

### Architecture

```
                     Vault (PKI engine)
                    "Internal CA"
                         │
                     issues short-lived
                     TLS certificates
                         │
           ┌─────────────┼─────────────┐
           │             │             │
     api-gateway    user-service   booking-service
     (cert: G)      (cert: U)      (cert: B)
           │             │             │
           │── mTLS ────→│             │
           │   (G↔U)     │             │
           │             │             │
           │── mTLS ────────────────→│
           │   (G↔B)     │             │
           │             │── mTLS ───→│
           │             │   (U↔B)   │
```

Every internal HTTP connection requires both sides to present a TLS certificate signed by the Vault CA. The certificate proves the service identity (CN = service name).

### High-Level Steps

#### 1. Enable the PKI secrets engine in Vault

Generate a Root CA certificate for your internal network. This CA will sign all service certificates.

**Search terms:** `vault pki engine setup`, `vault pki root generate internal`

#### 2. Create a Vault role for service certificates

The role defines which common names are allowed, how long certs are valid, and other constraints.

**Search terms:** `vault pki role allowed domains`, `vault pki issue certificate`

#### 3. Generate a Vault token for each service

Each token is scoped with a policy that only allows `pki/issue/<role>` — nothing else.

**Search terms:** `vault policy pki issue`, `vault token create policy`

#### 4. Each service fetches a cert on startup

On startup, every Spring Boot service calls Vault's `POST /v1/pki/issue/<role>` with its token, receives a cert + private key, and configures its embedded Tomcat / Netty with an SSL bundle.

**Search terms:** `Spring Boot SslBundle`, `ServerSslBundle spring boot`, `spring boot ssl reload vault`

#### 5. Configure Feign + RestTemplate for mTLS

Feign and RestClient need to be configured with the trust store (Vault's CA cert) and key store (the service's own cert). All inter-service calls now require mTLS.

**Search terms:** `Spring Boot SslBundle RestClient`, `Feign mTLS SslBundle`, `spring.cloud.openfeign.ssl`

#### 6. Configure Eureka for mTLS

Eureka clients and the server itself need to communicate over HTTPS with mutual cert validation. Eureka's `serviceUrl` changes from HTTP to HTTPS.

**Search terms:** `eureka server ssl`, `eureka client https configuration`

#### 7. Cert renewal strategy

Vault PKI issues short-lived certs (e.g., 24h). Services need to reload the cert before it expires. Options:

| Approach | Complexity | Tools |
|---|---|---|
| Sidecar (renew + write shared volume) | Medium | Consul Template, Vault Agent |
| Spring cron + `SslBundleRestore` | Low | Built into Boot 4.x |
| Long-lived certs (disable for learning) | None | Set `max_ttl=365d` on the role |

**Search terms:** `vault agent template`, `spring boot SslBundleRegistry reload`

### Key Vault Commands (Reference)

```sh
# Enable PKI
vault secrets enable pki

# Generate Root CA
vault write pki/root/generate/internal \
    common_name=internal.microservices.local \
    ttl=87600h

# Create role (e.g. for services)
vault write pki/roles/service \
    allowed_domains=service.internal \
    allow_subdomains=true \
    max_ttl=72h

# Create a policy
vault policy write svc-user - <<EOF
path "pki/issue/service" {
  capabilities = ["create", "update"]
}
EOF

# Issue a token for user-service
vault token create -policy=svc-user -ttl=720h
```

### Spring Boot Config Reference

Each service needs in `application.yml`:

```yaml
spring:
  ssl:
    bundle:
      jks:
        server:
          keystore:
            type: PEM
          truststore:
            type: PEM
  cloud:
    vault:
      host: host.docker.internal
      port: 8200
```

On startup, the service fetches cert + key from Vault, writes PEM files, and registers an `SslBundle` with the `SslBundleRegistry`.

**Search terms:** `SslBundleRegistration spring boot`, `spring boot reloadable ssl`, `spring boot 4 pem ssl`

### Docker Compose Changes

Each service gets:
- `VAULT_TOKEN` environment variable
- `host.docker.internal` extra host for Vault access

Eureka URL changes from `http://service-registry:8761/eureka/` to `https://service-registry:8761/eureka/` once Eureka is also configured with HTTPS.

### Verification

```sh
# Check Vault issued the cert
curl --cert user-service.pem --key user-service-key.pem \
     --cacert ca.pem \
     https://user-service:8081/actuator/health

# Verify mTLS is enforced
curl --cacert ca.pem \
     https://user-service:8081/actuator/health
# Expected: 403 / empty response (no client cert provided)
```

## Vault Path Structure

### Recommended Secret Layout

All project secrets live under a dedicated KV v2 engine, plus the PKI engine for certs.

```
microservices/                          # KV v2 secrets engine
├── jwt/
│   ├── private_key        ← PEM file   (api-gateway only)
│   └── public_key         ← PEM file   (all backend services)
│
├── database/
│   ├── user-service/
│   │   ├── url                         (jdbc:postgresql://user-db:5432/userdb)
│   │   ├── username
│   │   └── password
│   └── booking-service/
│       ├── url                         (jdbc:postgresql://booking-db:5432/bookingdb)
│       ├── username
│       └── password
│
├── eureka/
│   └── credentials                     (username:password for service discovery)
│
└── vault-tokens/                       (optional — for reference/audit)
    └── ...

pki/                                    # PKI engine
├── ca/
│   └── pem                             (Root CA cert, for trust store)
└── issue/service/                      (issue endpoint for service certs)
```

### Creating the Structure

```sh
# Enable KV v2 engine at microservices/
vault secrets enable -path=microservices kv-v2

# Store JWT keys
vault kv put microservices/jwt \
    private_key=@private.pem \
    public_key=@public.pem

# Store DB credentials
vault kv put microservices/database/user-service \
    url=jdbc:postgresql://user-db:5432/userdb \
    username=user_svc \
    password=$(openssl rand -base64 18)

vault kv put microservices/database/booking-service \
    url=jdbc:postgresql://booking-db:5432/bookingdb \
    username=booking_svc \
    password=$(openssl rand -base64 18)

# Store Eureka credentials
vault kv put microservices/eureka \
    credentials=eureka:$(openssl rand -base64 12)
```

### Create One Policy Per Service

Each policy grants the minimum access needed — nothing more.

```sh
# api-gateway — needs JWT private key + DB credentials + PKI cert
vault policy write api-gateway - <<EOF
path "microservices/data/jwt" {
  capabilities = ["read"]
}
path "microservices/data/database/api-gateway" {
  capabilities = ["read"]
}
path "pki/issue/service" {
  capabilities = ["create", "update"]
}
EOF

# user-service — needs JWT public key + its own DB creds + PKI cert
vault policy write user-service - <<EOF
path "microservices/data/jwt" {
  capabilities = ["read"]
}
path "microservices/data/database/user-service" {
  capabilities = ["read"]
}
path "pki/issue/service" {
  capabilities = ["create", "update"]
}
EOF

# booking-service — same shape as user-service
vault policy write booking-service - <<EOF
path "microservices/data/jwt" {
  capabilities = ["read"]
}
path "microservices/data/database/booking-service" {
  capabilities = ["read"]
}
path "pki/issue/service" {
  capabilities = ["create", "update"]
}
EOF

# service-registry — only needs PKI cert for mTLS
vault policy write service-registry - <<EOF
path "microservices/data/eureka" {
  capabilities = ["read"]
}
path "pki/issue/service" {
  capabilities = ["create", "update"]
}
EOF
```

### Generate Tokens

```sh
mkdir -p .docker/secrets

for svc in api-gateway user-service booking-service service-registry; do
  vault token create \
    -policy=$svc \
    -ttl=720h \
    -format=json | jq -r '.auth.client_token' \
    > ".docker/secrets/vault-token-$svc"
done
```

Result:
```
.docker/
└── secrets/
    ├── vault-token-api-gateway
    ├── vault-token-user-service
    ├── vault-token-booking-service
    └── vault-token-service-registry
```

Each file contains a single long-lived token (30 days). The `.docker/secrets/` directory is added to `.gitignore`.

### Mount Tokens as Docker Secrets

```yaml
# docker-compose.yml
services:
  api-gateway:
    secrets:
      - vault-token-api-gateway

  user-service:
    secrets:
      - vault-token-user-service

  booking-service:
    secrets:
      - vault-token-booking-service

  service-registry:
    secrets:
      - vault-token-service-registry

secrets:
  vault-token-api-gateway:
    file: .docker/secrets/vault-token-api-gateway
  vault-token-user-service:
    file: .docker/secrets/vault-token-user-service
  vault-token-booking-service:
    file: .docker/secrets/vault-token-booking-service
  vault-token-service-registry:
    file: .docker/secrets/vault-token-service-registry
```

Docker mounts these as `/run/secrets/vault-token-api-gateway` inside the container — accessible file, not an environment variable (no accidental leakage in logs or `docker inspect`).

### Service Startup Flow

```
Container starts
      │
      ├── Read token from /run/secrets/vault-token-*
      │
      ├── Connect to Vault at host.docker.internal:8200
      │
      ├── Fetch secrets relevant to this service:
      │     ├── JWT public/private key
      │     ├── Database credentials
      │     └── TLS certificate from PKI
      │
      ├── Configure datasource, SSL, JWT signing
      │
      └── Register with Eureka
            └── Start accepting requests
```

### Outside Docker (local development)

For running services directly on the IDE without Docker:

```sh
# Option A: Export token as env var
export VAULT_TOKEN=$(cat .docker/secrets/vault-token-user-service)

# Option B: Use Vault agent to authenticate via CLI
vault login -method=userpass username=developer
```

The Spring Boot application can read the token from `VAULT_TOKEN` env var (Vault's standard env var) or from the secret file path. A `@Value` or `@ConfigurationProperties` decides the source based on the active profile.

**Search terms:** `spring cloud vault config`, `vault token environment variable`, `spring boot externalized configuration vault`

## Design Decisions Summary

| Decision | Choice | Reason |
|---|---|---|
| Communication protocol | HTTP/REST | Simple, well-understood |
| Service discovery | Eureka | Mature, Spring-native |
| API Gateway | Spring Cloud Gateway | Native integration |
| Auth | JWT RS256 | Stateless, offline validation |
| Inter-service calls | OpenFeign | Declarative, Spring-managed |
| Database per service | Yes | Microservices best practice |
| Containerization | Docker Compose | Single host, learning project |
| Build tool | Gradle multi-module | Single repo, shared deps |
