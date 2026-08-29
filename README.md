# Resource Booking System

A secure RESTful API for booking resources (rooms, vehicles, equipment) built with **Spring Boot 3**, **Java 17**, **Spring Security + JWT**, and **JPA/Hibernate** on **PostgreSQL or MySQL**.

- `ADMIN` — full CRUD over resources and reservations.
- `USER` — read-only access to resources; can create reservations and view/manage only their own.
- User identity for a reservation is **always** derived from the JWT principal, never from the request body.

---

## 1. Tech Stack

| Concern            | Technology                                   |
|---------------------|-----------------------------------------------|
| Language / Runtime  | Java 17+                                       |
| Framework           | Spring Boot 3.3.x                              |
| Security            | Spring Security 6, JWT (jjwt 0.12.x)           |
| Persistence         | Spring Data JPA / Hibernate                    |
| Database            | PostgreSQL or MySQL                            |
| API Docs            | springdoc-openapi (Swagger UI) + Postman        |
| Build Tool          | Maven                                          |

---

## 2. Project Structure

```
src/main/java/com/exelynt/booking/
├── BookingApplication.java
├── config/          # Security, OpenAPI, and DB seeding configuration
├── security/        # JWT utilities, filter, UserDetails implementation
├── entity/          # User, Resource, Reservation, enums
├── repository/      # Spring Data JPA repositories
├── dto/             # Request/response payloads
├── service/         # Business logic (Auth, Resource, Reservation)
├── controller/      # REST controllers
└── exception/       # Centralized exception handling
src/main/resources/
└── application.yml  # Externalized configuration (env-var driven)
```

---

## 3. Prerequisites

- JDK 17+
- Maven 3.8+
- PostgreSQL 13+ **or** MySQL 8+
- (Optional) Postman, for exercising the collection in `postman_collection.json`

---

## 4. Database Setup

### Option A — PostgreSQL
```sql
CREATE DATABASE booking_db;
```

### Option B — MySQL
```sql
CREATE DATABASE booking_db CHARACTER SET utf8mb4;
```

Both PostgreSQL and MySQL drivers are already declared in `pom.xml`, so no dependency changes are needed to switch — just point the environment variables at the database you want to use (see below). `spring.jpa.hibernate.ddl-auto` is set to `update` by default, so tables are created/updated automatically on startup — no manual migration scripts required for local development.

---

## 5. Environment Variables

Copy `.env.example` to `.env` (or export the variables directly in your shell / CI / container) before running the app.

| Variable              | Description                                             | Example (PostgreSQL)                                         |
|-----------------------|-----------------------------------------------------------|----------------------------------------------------------------|
| `DB_URL`              | JDBC connection string                                    | `jdbc:postgresql://localhost:5432/booking_db`                  |
| `DB_USERNAME`         | Database username                                          | `postgres`                                                      |
| `DB_PASSWORD`         | Database password                                          | `postgres`                                                      |
| `DB_DRIVER`           | JDBC driver class                                           | `org.postgresql.Driver`                                         |
| `DDL_AUTO`            | Hibernate schema strategy (`update`, `validate`, `none`)    | `update`                                                         |
| `SHOW_SQL`            | Log SQL statements                                          | `false`                                                          |
| `JWT_SECRET`          | HMAC signing secret — **must be ≥ 32 bytes** in production  | a long random string                                             |
| `JWT_EXPIRATION_MS`   | Token lifetime in milliseconds                              | `3600000` (1 hour)                                               |
| `SERVER_PORT`         | HTTP port                                                   | `8080`                                                           |

For MySQL, use:
```
DB_URL=jdbc:mysql://localhost:3306/booking_db?useSSL=false&serverTimezone=UTC
DB_USERNAME=root
DB_PASSWORD=root
DB_DRIVER=com.mysql.cj.jdbc.Driver
```

---

## 6. Running the Application

```bash
# 1. Set environment variables (edit values as needed)
export DB_URL=jdbc:postgresql://localhost:5432/booking_db
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export DB_DRIVER=org.postgresql.Driver
export JWT_SECRET=change-this-to-a-long-random-secret-at-least-32-bytes-long

# 2. Build
mvn clean package -DskipTests

# 3. Run
java -jar target/resource-booking-system-1.0.0.jar

# ...or run directly with Maven during development
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`.

---

## 7. Seed Users

On first startup, `DataSeeder` creates two accounts (and three sample resources) if they don't already exist:

| Username | Password    | Role  |
|----------|-------------|-------|
| `admin`  | `Admin@123` | ADMIN |
| `user`   | `User@123`  | USER  |

> Change or remove these credentials before deploying to any shared/production environment.

---

## 8. Authentication

**POST** `/auth/login`
```json
{
  "username": "admin",
  "password": "Admin@123"
}
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "username": "admin",
  "role": "ADMIN",
  "expiresInMs": 3600000
}
```

Send the token on every subsequent request:
```
Authorization: Bearer <token>
```

---

## 9. API Reference

### Auth
| Method | Endpoint       | Access | Description        |
|--------|----------------|--------|---------------------|
| POST   | `/auth/login`  | Public | Authenticate, get JWT |

### Resources
| Method | Endpoint                | Access        | Description                  |
|--------|--------------------------|---------------|--------------------------------|
| GET    | `/api/resources`         | ADMIN, USER   | List (paginated, sortable)     |
| GET    | `/api/resources/{id}`    | ADMIN, USER   | Get by id                       |
| POST   | `/api/resources`         | ADMIN only    | Create                          |
| PUT    | `/api/resources/{id}`    | ADMIN only    | Update                          |
| DELETE | `/api/resources/{id}`    | ADMIN only    | Delete                          |

Query params for `GET /api/resources`: `page`, `size`, `sortBy`, `sortDir`.

### Reservations
| Method | Endpoint                          | Access      | Description                                                   |
|--------|------------------------------------|-------------|-----------------------------------------------------------------|
| GET    | `/api/reservations`                | ADMIN, USER | ADMIN sees all; USER sees only their own. Supports filters below |
| GET    | `/api/reservations/{id}`           | Owner/ADMIN | Get by id                                                         |
| POST   | `/api/reservations`                | ADMIN, USER | Create — owner is taken from the JWT, not the request body       |
| PUT    | `/api/reservations/{id}`           | Owner/ADMIN | Update (owner may only edit while `PENDING`)                     |
| PATCH  | `/api/reservations/{id}/status`    | Owner/ADMIN | Change status (USER may only cancel their own pending booking)   |
| DELETE | `/api/reservations/{id}`           | Owner/ADMIN | Delete                                                            |

Query params for `GET /api/reservations`:
- `status` — `PENDING` \| `CONFIRMED` \| `CANCELLED`
- `minPrice`, `maxPrice` — decimal bounds
- `page`, `size` — pagination
- `sortBy` — one of `id`, `startTime`, `endTime`, `price`, `status`, `createdAt`
- `sortDir` — `asc` \| `desc`

Example:
```
GET /api/reservations?status=PENDING&minPrice=10&maxPrice=100&page=0&size=5&sortBy=price&sortDir=asc
```

### Sample paginated response
```json
{
  "content": [ { "id": 1, "resourceId": 1, "resourceName": "Conference Room A", "userId": 2, "username": "user", "startTime": "2026-10-01T10:00:00", "endTime": "2026-10-01T12:00:00", "status": "PENDING", "price": 50.00, "createdAt": "2026-08-30T10:00:00Z" } ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

### Sample error response
```json
{
  "timestamp": "2026-08-30T10:00:00Z",
  "status": 403,
  "error": "Forbidden",
  "message": "You do not have permission to access this reservation",
  "details": null
}
```

---

## 10. Authorization Rules Enforced

- Every endpoint under `/api/**` requires a valid `Bearer` JWT; `/auth/**` and Swagger routes are public.
- Method-level `@PreAuthorize` checks enforce role requirements on top of the URL-level filter chain.
- Resource mutation (`POST` / `PUT` / `DELETE`) is `ADMIN`-only; reads are open to both roles.
- Reservation ownership is enforced in `ReservationService`: a `USER` can only view/update/cancel/delete reservations where `reservation.user.id == principal.id`; an `ADMIN` bypasses this check.
- The reservation owner is resolved from the `CustomUserDetails` JWT principal on the server side — the `resourceId`/`startTime`/`endTime` are the only client-supplied identifiers; there is no `userId` field in `ReservationRequest`, so it can't be spoofed.

---

## 11. API Documentation

- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8080/v3/api-docs`
- **Postman:** import `postman_collection.json` from the project root. Run "Login as Admin" / "Login as User" first, then copy the returned token into the `adminToken` / `userToken` collection variables.

---

## 12. Validation & Error Handling

- Bean Validation (`jakarta.validation`) annotations on all request DTOs (e.g. required fields, non-negative price, future start/end times).
- A centralized `GlobalExceptionHandler` maps exceptions to consistent JSON error responses:
  - `400` — validation failures, malformed business rules (e.g. end time before start time)
  - `401` — missing/invalid/expired JWT, bad credentials
  - `403` — authenticated but not authorized (wrong role or not the resource owner)
  - `404` — resource/reservation not found
  - `500` — unexpected server errors

---

## 13. Testing the Flow End-to-End

1. `POST /auth/login` with `admin`/`Admin@123` → copy `token`.
2. `POST /api/resources` (as admin) to add a resource, or use the 3 seeded ones.
3. `POST /auth/login` with `user`/`User@123` → copy `token`.
4. `POST /api/reservations` (as user) with a `resourceId`, `startTime`, `endTime`.
5. `GET /api/reservations` (as user) → only that user's bookings are returned.
6. `GET /api/reservations` (as admin) → all bookings across all users are returned.
7. `PATCH /api/reservations/{id}/status` (as user) with `{"status": "CANCELLED"}` → succeeds only while `PENDING`.
8. `PATCH /api/reservations/{id}/status` (as admin) with `{"status": "CONFIRMED"}` → succeeds regardless of owner.

---

## 14. Notes / Design Decisions

- Passwords are hashed with BCrypt (`BCryptPasswordEncoder`).
- JWT is stateless (`SessionCreationPolicy.STATELESS`); no server-side session state.
- Filtering/sorting/pagination on reservations is implemented with Spring Data JPA `Specification` + `Pageable` to keep it composable and avoid N+1 query sprawl.
- Both PostgreSQL and MySQL drivers ship in the jar so grading/review environments can point at whichever database is available without a rebuild.
