# Remaining Infrastructure / Docker Issues

## 1. Vault token for service-registry
The init script (`vault/init.sh`) only issues tokens for `api-gateway`,
`user-service`, and `booking-service`. If the registry itself needs Vault
access (e.g. secure registration), add `service-registry` to the `SERVICES`
list and define a policy for it.

## 2. Eureka client / server dependencies
No service has Eureka on its classpath yet.
- **service-registry** — needs `spring-cloud-starter-netflix-eureka-server`
- **user-service, booking-service, api-gateway** — need
  `spring-cloud-starter-netflix-eureka-client`
- A Spring Cloud BOM version compatible with Spring Boot 4.0.6 must be added
  to the root `pom.xml`.

## 3. Eureka service URL
Services need to know where the registry lives:
`eureka.client.serviceUrl.defaultZone=http://service-registry:8761/eureka/`
This can be added as a Compose environment variable or in each service's
`application.yaml`.

## 4. OpenFeign dependency
Inter-service calls will use OpenFeign.  Add
`spring-cloud-starter-openfeign` to the POMs of any service that calls
another.  Also requires the Spring Cloud BOM from item 2.

## 5. Actuator dependency
The service-registry healthcheck pings `/actuator/health`, but no service
has `spring-boot-starter-actuator` in its POM yet.  Without it:
- The registry healthcheck always fails
- The `depends_on: condition: service_healthy` on every other service
  blocks them indefinitely

## 6. Healthchecks on every service
Only the service-registry defines a `healthcheck` block.
`user-service`, `booking-service`, and `api-gateway` have none, so crashes
go undetected by Docker.  Add healthchecks once actuator is available.

## 7. TLS / mTLS infrastructure
No CA is configured yet.  Would require:
- Enabling the Vault PKI secrets engine in `init.sh`
- Issuing a root CA and intermediate certificates
- Signing a certificate per service
- Mounting the key material into each service container
- Configuring `server.ssl.*` and client-side TLS in each Spring Boot app

## 8. Secure Eureka registration
Currently any service that knows the registry address can register.
Would need:
- Spring Security on the registry
- Credentials stored in Vault
- Each service authenticating with its Vault token (or HTTP Basic creds)
  during registration

## 9. Maven base image
`maven:3-eclipse-temurin-25` may not exist on all registries.
If the build fails, fall back to `maven:3-eclipse-temurin-21` and update
`<java.version>` in `pom.xml` to 21, or find a suitable JDK-25 image with
Maven pre-installed.

## 10. Port config in source
`SERVER_PORT` is passed via Compose env vars, which works.  But when Eureka
is added, each service must tell Eureka which port to advertise.  Without
explicit config, Eureka defaults to `server.port` (already set), so this
should be fine as long as the env var is visible to the app.
