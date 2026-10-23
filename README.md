# Toghrak X

A full-stack publishing platform: posts in categories, static pages, a rich-text editor, global search and an admin panel.

**Stack:** Angular 21 · Spring Boot 3.5 (Java 21) · PostgreSQL · JWT authentication

---

## Contents

- [Features](#features)
- [Tech stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Configuration](#configuration)
- [Running locally](#running-locally)
- [Tests](#tests)
- [Deployment](#deployment)
- [Troubleshooting](#troubleshooting)

---

## Features

### Readers
- Browse posts by recency, popularity or category (`/category/<slug>`)
- Read posts with cover image and gallery; static pages (About, Contact, FAQ, …)
- Global search across posts, pages and social links
- Footer with pages and social links; responsive layout

### Editors and admins
There are two roles:

| Role | Can do |
|---|---|
| **EDITOR** | Create posts; edit and delete their own posts |
| **ADMIN** | Everything an editor can, on all posts, plus users, categories, static pages, social links and branding |

- Rich-text editor (Quill) for posts and pages
- Image upload for covers, galleries and the site logo
- Admin panel: Posts, Users, Categories, Pages, Social Links, Branding
- Error and success messages as toasts, with the server's own reason

### Security
- Short-lived JWT access token plus a refresh token in an httpOnly cookie, rotated on every use; reusing an old refresh token revokes all of that user's sessions
- Changing a user's password or role revokes their refresh tokens, so existing sessions end when the current access token expires (30 minutes at most)
- Role checks with `@PreAuthorize` on every protected endpoint
- HTML sanitised with jsoup on the server and again by Angular in the browser
- Uploaded images checked by their file signature, not by name or browser type
- Rate limiting (Bucket4j) on login, search and popular posts
- Errors returned as RFC 9457 `ProblemDetail`

---

## Tech stack

### Frontend (`frontend/`)
- Angular 21, TypeScript 5.9, RxJS 7.8
- Tailwind CSS 3 plus component CSS
- Quill 2 rich-text editor (ngx-quill)
- Vitest (Angular's `unit-test` builder, jsdom) for unit tests

### Backend (`backend/`)
- Spring Boot 3.5.9, Java 21
- Spring Security with JWT (JJWT 0.13)
- Spring Data JPA (Hibernate 6) on PostgreSQL
- Flyway for database migrations (`backend/src/main/resources/db/migration`)
- jsoup 1.23 for HTML sanitising, Bucket4j for rate limiting
- Springdoc 2.8 (OpenAPI / Swagger UI)
- JUnit 5 and Testcontainers for integration tests

---

## Prerequisites

- **Node.js** 20.19+, 22.12+ or 24+ (required by Angular 21) and npm
- **Java** 21 (JDK). Maven is not needed: the project includes the Maven wrapper (`./mvnw`).
- **PostgreSQL** (developed and tested with 17)
- **Docker** (optional): only needed to run the backend integration tests

---

## Configuration

The backend reads its settings from environment variables. For local development, put them in `backend/.env.local` (git-ignored) and load that file in your IDE's run configuration.

| Variable | Required | Purpose |
|---|---|---|
| `SPRING_DATASOURCE_URL` | yes | e.g. `jdbc:postgresql://localhost:5432/blogdb` |
| `SPRING_DATASOURCE_USERNAME` | yes | Database user |
| `SPRING_DATASOURCE_PASSWORD` | yes | Database password |
| `JWT_SECRET` | yes | At least 64 characters; the backend refuses to start with a shorter one |
| `DEFAULT_ADMIN_USERNAME`, `DEFAULT_ADMIN_EMAIL`, `DEFAULT_ADMIN_PASSWORD` | first start | Creates the first admin when none exists. Without a password no admin is created. |
| `IMAGE_UPLOAD_DIR` | production | Folder for uploaded images (default: `uploads` in the project root) |
| `IMAGE_BASE_URL` | production | Public URL of that folder, e.g. `https://example.com/images/` (default: `http://localhost:8081/images/`) |
| `CORS_ALLOWED_ORIGINS` | only if needed | Only when the frontend is served from a different origin than `/api` |
| `JWT_EXPIRATION`, `REFRESH_EXPIRATION` | no | Token lifetimes in ms (defaults: 30 minutes, 12 hours) |
| `REFRESH_COOKIE_SECURE` | no | `true` by default (HTTPS only); the `dev` profile sets it to `false` for localhost |

The **`dev` profile** (`application-dev.properties`) turns on SQL logging and detailed error messages, and supplies `admin` as the first admin's password when `DEFAULT_ADMIN_PASSWORD` is not set. Use it only on your own machine.

The database schema is managed by **Flyway**. Migrations run automatically when the backend starts; Hibernate only validates the schema against the entities. Every schema change is a new file `V<n>__description.sql` (or `.java`) in `db/migration`. Never edit a migration that has already run.

---

## Running locally

1. Create the database once:
   ```bash
   createdb blogdb
   ```
2. Start the backend with the `dev` profile (port 8081), with the variables above set:
   ```bash
   cd backend
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```
   In IntelliJ or VS Code: run `ToghrakXApplication` with the VM option `-Dspring.profiles.active=dev`.
3. Start the frontend (port 4200):
   ```bash
   cd frontend
   npm install
   npx ng serve
   ```
   Open `http://localhost:4200`. The dev server forwards `/api` and `/images` to the backend on port 8081 (`frontend/proxy.conf.json`), so the browser only talks to one origin, as in production.

Swagger UI is at `http://localhost:8081/swagger-ui/index.html`. It is admin-only unless `app.security.swagger-public=true`.

---

## Tests

### Backend
```bash
cd backend
./mvnw test
```
Integration tests start a throwaway PostgreSQL in Docker (Testcontainers). Without Docker they are skipped, not failed.

### Frontend
```bash
cd frontend
npx ng test
```
Runs the unit tests with Vitest in Node (jsdom); no browser is needed. Use `npx ng test --watch=false` for a single run instead of watch mode.

---

## Deployment

1. **Backend:** build the jar with `cd backend && ./mvnw package` and run it with the environment variables above. On first start Flyway creates the schema and the first admin is created.
2. **Frontend:** build with `cd frontend && npx ng build`. The output is in `frontend/dist/toghrak-x/browser`.
3. **Reverse proxy** (Nginx or a cloud load balancer): serve the frontend files, and forward `/api` and `/images` to the backend. The frontend calls the API through the relative URL `/api`, so frontend and API share one origin and no CORS setup is needed.
4. **Uploads:** `IMAGE_UPLOAD_DIR` must be on persistent storage, and `IMAGE_BASE_URL` must be its public address.

---

## Troubleshooting

- **The backend does not start: "Schema-validation" or a Flyway error.** The database does not match the migrations. Read the message: it names the table or column.
- **The backend does not start: JWT secret.** `JWT_SECRET` is missing or shorter than 64 characters.
- **The frontend shows no data.** Check that the backend runs on port 8081, and restart `ng serve` after changes to `proxy.conf.json` or `angular.json`.
- **IDE shows "cannot find symbol" for `builder()` or `log` (Lombok), or "Build failed".** Reload the Maven project after `pom.xml` changes (IntelliJ: Maven → Reload All Maven Projects; VS Code: Java: Reload Projects).
