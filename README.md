# Resource Booking System

A RESTful API for booking shared resources (rooms, vehicles, equipment) built with **Spring Boot 3**, **Java 17**, **Spring Security + JWT**, and **PostgreSQL/MySQL**.

Users can browse resources and manage their own reservations. Administrators have full CRUD control over resources and all reservations.

---

## Tech Stack

| Layer Technology  |                                           |
| ----------------- | ----------------------------------------- |
| Language          | Java 17                                   |
| Framework         | Spring Boot 3.3.4                         |
| Security          | Spring Security 6, JWT (jjwt 0.12.6)      |
| Persistence       | Spring Data JPA / Hibernate               |
| Database          | PostgreSQL (default) or MySQL             |
| Validation        | Jakarta Bean Validation                   |
| API Docs          | springdoc-openapi (Swagger UI)            |
| Build             | Maven                                     |
| Testing           | JUnit 5, Mockito, MockMvc, H2 (in-memory) |

---

## Project Structure

```
src/main/java/com/booking/system/
├── config/            # SecurityConfig, JwtProperties, OpenApiConfig
├── controller/        # REST controllers (Auth, Resource, Reservation)
├── dto/
│   ├── request/        # Request DTOs (with Bean Validation annotations)
│   └── response/       # Response DTOs
├── entity/            # JPA entities (User, Resource, Reservation)
├── enums/             # Role, ResourceType, ReservationStatus
├── exception/         # Custom exceptions + GlobalExceptionHandler
├── repository/        # Spring Data repositories + JPA Specifications
├── security/          # JwtService, JwtAuthenticationFilter, UserDetailsService, entry points
└── service/           # Business logic (AuthService, ResourceService, ReservationService)

src/main/resources/
├── application.yml           # Main config (env-var driven, PostgreSQL default)
├── application-mysql.yml     # MySQL profile overrides
└── data.sql                  # Seed users + sample resources

src/test/java/com/booking/system/
├── security/           # JwtService unit tests
├── service/             # ReservationService unit tests (ownership/RBAC logic)
└── integration/         # Full-stack MockMvc tests (auth, resources, reservations)

```

---

## Prerequisites

- **JDK 17+** (verify with `java -version`)
- **Apache Maven 3.8+** (verify with `mvn -version`)
- **PostgreSQL 13+** or **MySQL 8+** — or use the provided `docker-compose.yml`

> This project does not vendor a Maven Wrapper (`mvnw`) binary. Use a locally installed Maven, or open the project in an IDE (IntelliJ IDEA, Eclipse, VS Code) that runs Maven for you.

---

## Quick Start (PostgreSQL — default)

### 1. Start a database

**Option A — Docker (recommended, fastest):**

```bash
docker compose up -d

```

This starts PostgreSQL on `localhost:5432` with database `booking_system`, user `booking_user`, password `booking_pass` — matching the defaults already baked into `application.yml`, so no further config is needed.

**Option B — Existing local PostgreSQL install:**

```sql
CREATE DATABASE booking_system;
CREATE USER booking_user WITH PASSWORD 'booking_pass';
GRANT ALL PRIVILEGES ON DATABASE booking_system TO booking_user;

-- PostgreSQL 15+ also requires this explicitly (see Troubleshooting below):
\c booking_system
GRANT ALL ON SCHEMA public TO booking_user;

```

### 2. (Optional) Configure environment variables

Copy `.env.example` to `.env`, or export the variables in your shell, and adjust as needed. Every variable has a working local-dev default already in `application.yml`, so **for the Docker Postgres setup above, nothing needs to be set** — just run the app.

See the [Environment Variables](#environment-variables) section below for the full list.

### 3. Build and run

```bash
mvn clean install
mvn spring-boot:run

```

Or run the packaged jar directly:

```bash
mvn clean package
java -jar target/resource-booking-system.jar

```

The API is now available at `http://localhost:8080`.

---

## Switching to MySQL

The project defaults to PostgreSQL. To run against MySQL instead, do all three of the following:

**1. Start a MySQL database:**

```bash
docker compose --profile mysql up -d mysql

```

This starts MySQL on `localhost:3306` with database `booking_system`, user `booking_user`, password `booking_pass`.

Or, if using an existing local MySQL install:

```sql
CREATE DATABASE booking_system CHARACTER SET utf8mb4;
CREATE USER 'booking_user'@'%' IDENTIFIED BY 'booking_pass';
GRANT ALL PRIVILEGES ON booking_system.* TO 'booking_user'@'%';
FLUSH PRIVILEGES;

```

**2. Activate the** **`mysql`** **Spring profile** — this switches the JDBC driver and Hibernate dialect via `application-mysql.yml`:

```bash
SPRING_PROFILES_ACTIVE=mysql mvn spring-boot:run

```

On Windows (PowerShell):

```powershell
$env:SPRING_PROFILES_ACTIVE="mysql"; mvn spring-boot:run

```

On Windows (cmd):

```cmd
set SPRING_PROFILES_ACTIVE=mysql && mvn spring-boot:run

```

**3. (Only if your MySQL connection details differ from the defaults)** override the connection variables:

```bash
DB_URL=jdbc:mysql://localhost:3306/booking_system?useSSL=false&serverTimezone=UTC \
DB_USERNAME=booking_user \
DB_PASSWORD=booking_pass \
SPRING_PROFILES_ACTIVE=mysql \
mvn spring-boot:run

```

That's it — no code changes are needed to switch databases. `application-mysql.yml` already sets `DB_DRIVER=com.mysql.cj.jdbc.Driver` and `JPA_DIALECT=org.hibernate.dialect.MySQLDialect` as its own defaults, so you only need to override `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` if they differ from the defaults above.

To run the packaged jar against MySQL instead of `spring-boot:run`:

```bash
java -jar target/resource-booking-system.jar --spring.profiles.active=mysql

```

---


## Docker — Application + PostgreSQL

For a complete Docker-based setup, the project also includes
`docker-compose.full.yml`. It starts both the Spring Boot application
and PostgreSQL together.

### Start the application and database

```bash
docker compose -f docker-compose.full.yml up --build
```

The application will be available at:

`http://localhost:8080`

PostgreSQL is available on:

`localhost:5432`

### Stop the containers

```bash
docker compose -f docker-compose.full.yml down
```

PostgreSQL data is stored in the Docker named volume `postgres-data`,
so the database data persists when the containers are stopped and
recreated.

### Remove the containers and database data

```bash
docker compose -f docker-compose.full.yml down -v
```

> The `-v` option removes the PostgreSQL volume and permanently deletes
> the local Docker database data.

> The Docker configuration uses development-only database credentials.
> Use secure secrets for production deployments.


## Explore the API

- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8080/v3/api-docs`

### Postman

Import both files into Postman:

- `postman/Resource-Booking-System.postman_collection.json`
- `postman/Resource-Booking-System.postman_environment.json`

Select the **Resource Booking System - Local** environment.

The collection includes separate Admin and User login requests. After a successful login,
the collection automatically stores the returned JWT as `adminToken` or `userToken` for
subsequent requests.

Use the JWT in the `Authorization` header:

```text
Authorization: Bearer <token>
```

---

## Seed Users

On first startup, `data.sql` creates these accounts automatically (safe to restart repeatedly — it won't duplicate rows):

| Username Password Role  |            |       |
| ----------------------- | ---------- | ----- |
| `admin`                 | `admin123` | ADMIN |
| `user1`                 | `user123`  | USER  |
| `user2`                 | `user123`  | USER  |

A handful of sample resources (rooms, vehicles, equipment) are seeded too, so you can start creating reservations immediately.

---

## Authentication

### Login

```
POST /auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}

```

Response:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "username": "admin",
  "role": "ADMIN",
  "expiresInMs": 86400000
}

```

Use the token on every subsequent request:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...

```

### Register (optional self-service)

```
POST /auth/register
Content-Type: application/json

{
  "username": "newuser",
  "email": "newuser@example.com",
  "password": "password123"
}

```

Public self-registration always creates a `USER` account. There is deliberately no way to self-register as `ADMIN` — admin accounts are seeded or created directly in the database.

---

## API Overview

### Resources — `/api/resources`

| Method Path Access Description  |                       |                        |                            |
| ------------------------------- | --------------------- | ---------------------- | -------------------------- |
| GET                             | `/api/resources`      | Any authenticated user | List resources (paginated) |
| GET                             | `/api/resources/{id}` | Any authenticated user | Get one resource           |
| POST                            | `/api/resources`      | ADMIN only             | Create a resource          |
| PUT                             | `/api/resources/{id}` | ADMIN only             | Update a resource          |
| DELETE                          | `/api/resources/{id}` | ADMIN only             | Delete a resource          |

### Reservations — `/api/reservations`

| Method Path Access Description  |                          |                                     |                                               |
| ------------------------------- | ------------------------ | ----------------------------------- | --------------------------------------------- |
| POST                            | `/api/reservations`      | Any authenticated user              | Create a reservation (owner = caller, always) |
| GET                             | `/api/reservations`      | Any authenticated user              | ADMIN sees all; USER sees only their own      |
| GET                             | `/api/reservations/{id}` | Owner or ADMIN                      | Get one reservation                           |
| PUT                             | `/api/reservations/{id}` | Owner (cancel only) or ADMIN (full) | Update a reservation                          |
| DELETE                          | `/api/reservations/{id}` | Owner or ADMIN                      | Delete a reservation                          |

**Ownership is always derived from the JWT** — the reservation request body has no `userId` field, so there is no way for a client to create a booking "as" someone else. Row-level ownership is re-checked on every read/update/delete, so even guessing another user's reservation ID returns `403 Forbidden`, not their data.

**Filtering & pagination** (all query params optional, combine freely):

```
GET /api/reservations?status=CONFIRMED&minPrice=10&maxPrice=200&page=0&size=20&sort=price,desc

```

- `status` — `PENDING`, `CONFIRMED`, or `CANCELLED`
- `minPrice` / `maxPrice` — inclusive decimal bounds
- `page` / `size` — standard Spring pagination (0-indexed)
- `sort` — e.g. `sort=price,desc` or `sort=startTime,asc` (repeatable for multi-field sort)

---

## Authorization Rules Summary

| Action ADMIN USER (own) USER (someone else's)  |   |         |         |
| ---------------------------------------------- | - | ------- | ------- |
| View resources                                 | ✅ | ✅       | n/a     |
| Create/update/delete resources                 | ✅ | ❌ (403) | n/a     |
| Create a reservation                           | ✅ | ✅       | n/a     |
| View a reservation                             | ✅ | ✅       | ❌ (403) |
| Cancel a reservation                           | ✅ | ✅       | ❌ (403) |
| Fully update / confirm a reservation           | ✅ | ❌ (403) | ❌ (403) |
| Delete a reservation                           | ✅ | ✅       | ❌ (403) |

---

## Environment Variables

All variables have working defaults for local development (matching the Docker Compose Postgres setup), so the app runs out of the box with zero configuration. Override any of these as needed:

| Variable Default Description  |                                                   |                                                          |
| ----------------------------- | ------------------------------------------------- | -------------------------------------------------------- |
| `SERVER_PORT`                 | `8080`                                            | HTTP port                                                |
| `DB_URL`                      | `jdbc:postgresql://localhost:5432/booking_system` | JDBC connection URL                                      |
| `DB_USERNAME`                 | `booking_user`                                    | Database username                                        |
| `DB_PASSWORD`                 | `booking_pass`                                    | Database password                                        |
| `DB_DRIVER`                   | `org.postgresql.Driver`                           | JDBC driver class                                        |
| `JPA_DIALECT`                 | `org.hibernate.dialect.PostgreSQLDialect`         | Hibernate dialect                                        |
| `DDL_AUTO`                    | `update`                                          | Hibernate schema management (`update`/`validate`/`none`) |
| `SHOW_SQL`                    | `false`                                           | Log generated SQL                                        |
| `SQL_INIT_MODE`               | `always`                                          | Whether `data.sql` runs on startup                       |
| `JWT_SECRET`                  | *(dev default — override in production)*          | HMAC signing key for JWTs, **32+ bytes**                 |
| `JWT_EXPIRATION_MS`           | `86400000` (24h)                                  | Token lifetime in milliseconds                           |
| `LOG_LEVEL`                   | `INFO`                                            | App log level                                            |
| `SECURITY_LOG_LEVEL`          | `WARN`                                            | Spring Security log level                                |

**⚠️ Important:** Always override `JWT_SECRET` with a strong random value before deploying anywhere beyond your own machine. Generate one with:

```bash
openssl rand -base64 48

```

---

## Running Tests

```bash
mvn test

```

Tests run against an in-memory H2 database (`application-test.yml`), so no external database is required to run the test suite.

**Coverage includes:**

- `JwtServiceTest` — token generation, validation, expiry, tampering detection
- `ReservationServiceTest` — ownership enforcement, RBAC business rules, validation (unit tests with Mockito)
- `AuthControllerIntegrationTest` — login/register happy paths and failure cases
- `ResourceControllerIntegrationTest` — RBAC on resource CRUD (full Spring context + real security filter chain)
- `ReservationControllerIntegrationTest` — ownership isolation, filtering, pagination, sorting, cross-user access denial

---


The current test suite contains **48 tests**, all passing successfully.


## Troubleshooting

### `ERROR: permission denied for schema public` on startup (PostgreSQL)

**Cause:** PostgreSQL 15+ no longer grants write access to the `public` schema to non-owner roles by default. If your `booking_user` didn't create the database itself, it may lack `CREATE` privilege on `public`, so Hibernate can't create tables and `data.sql` then fails too (since the tables don't exist yet).

**Fix:** connect as a superuser/admin and grant the privilege:

```sql
\c booking_system
GRANT ALL ON SCHEMA public TO booking_user;

```

If you're using the bundled `docker-compose.yml`, this shouldn't happen (the official Postgres image's init process makes `POSTGRES_USER` the schema owner). If you do hit it anyway, the simplest fix is to reset the volume and let it reinitialize cleanly:

```bash
docker compose down -v
docker compose up -d

```

### `data.sql` fails with "relation does not exist"

This is a downstream symptom of the schema-permission issue above (tables were never created, so seeding fails). Fix the underlying permission issue first; `data.sql` will succeed on the next run.

### Port 8080 already in use

Either stop whatever's using it, or override the port: `SERVER_PORT=8081 mvn spring-boot:run`.

---

## Design Notes

- **Ownership is enforced server-side, not client-side.** `ReservationRequest` has no user/owner field at all — the authenticated principal from the JWT is the only source of truth for "who owns this reservation," both on create and on every subsequent read/update/delete.
- **Two authorization layers, defense in depth.** Coarse role checks live in `SecurityConfig` (URL-pattern based); fine-grained per-row ownership checks live in `ReservationService`, since "is this MY reservation" isn't expressible as a URL pattern.
- **JPA Specifications** power the reservation filtering (`ReservationSpecification`), so status/minPrice/maxPrice/ownerId filters compose freely without a combinatorial explosion of repository methods.
- **Stateless JWT auth.** No server-side session; every request is authenticated independently via the `Authorization: Bearer <token>` header, validated in `JwtAuthenticationFilter`.
- **Uniform error format.** Every failure — validation errors, 401s, 403s, 404s, 409s — returns the same `ErrorResponse` JSON shape via `GlobalExceptionHandler` and the two Spring Security handler beans (`JwtAuthenticationEntryPoint`, `JwtAccessDeniedHandler`).

---
