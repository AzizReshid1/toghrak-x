# Security Policy — Toghrak X

Toghrak X is a full-stack publishing platform currently under active development.
We appreciate responsible disclosure of security issues.

---

## Supported Versions

Security fixes are applied to the latest code on the default branch only.

| Target | Supported |
|---|---|
| `main` branch | Yes |
| Tagged releases | No (until first stable release) |
| Forks / custom deployments | No |

---

## Reporting a Vulnerability

Please do **not** open a public GitHub Issue for security vulnerabilities.

### Preferred: GitHub Private Vulnerability Reporting
Repository → **Security** → **Advisories** → **Report a vulnerability**

### Alternative: Email
Email: **\<YOUR_EMAIL\>**
Subject: `[SECURITY] Toghrak X vulnerability report`

### What to include
- Component: Frontend / Backend API / Auth / Admin roles / DB / CI
- Vulnerability type: XSS, SQLi, IDOR, auth bypass, CSRF, SSRF, etc.
- Expected vs actual behavior
- Steps to reproduce (include sanitized request/response examples where possible)
- Proof of concept (safe, minimal — no destructive payloads)
- Suggested fix, if you have one

### Response timeline (best effort)
- Acknowledgement within **72 hours**
- Triage within **7 days**
- Fix scheduling depends on severity and development priorities

---

## Scope

### In scope
- Authentication and authorization (JWT, refresh tokens, roles/permissions)
- Broken access control (IDOR), privilege escalation
- Injection vulnerabilities (SQL, command, template)
- XSS, CSRF, CORS misconfiguration
- Sensitive data exposure (tokens, secrets, PII)
- Dependency vulnerabilities with practical impact
- Rate limiting bypass

### Out of scope
- Denial of service via high traffic (no SLA)
- Social engineering / phishing
- Issues requiring compromised local devices

---

## Safe Harbor

We support good-faith security research. Please avoid:
- Accessing data that is not yours
- Disrupting services or development infrastructure
- Exfiltrating personal or secret data

If you follow this policy, we will treat your report as authorized research.

---

## Security Architecture

This section documents the security mechanisms implemented in the codebase for transparency and for contributors.

### Authentication

**JWT Access Tokens**
- Algorithm: **HS512** (HMAC SHA-512)
- Signing key: configured via `app.jwt.secret` environment variable (minimum 64 bytes required)
- Token lifetime: 30 minutes (`app.jwt.expiration=1800000`)
- Token payload: subject (userId) + role claim + issued-at + expiration
- Filter: `JwtAuthenticationFilter` (extends `OncePerRequestFilter`) — extracts the `Authorization: Bearer <token>` header, validates signature and expiration, builds a `UserPrincipal` from claims (no database call per request)

**Refresh Tokens**
- Stored in the database as **SHA-256 hashes** of UUID values
- Lifetime: 12 hours (`app.refresh.expiration=43200000`)
- Token rotation: on refresh, the old token is **revoked** and a new one is issued
- Scheduled cleanup: expired and revoked tokens are purged nightly at 03:00 UTC
- Delivered and received via **HttpOnly cookie**:
  - `HttpOnly: true`
  - `Secure: true` (HTTPS only in production, configurable)
  - `SameSite: Lax`
  - `Path: /api/auth` (scoped to auth endpoints only)

**Password Storage**
- Algorithm: **BCrypt** (`BCryptPasswordEncoder`)
- Applied on user creation in `UserService`

**Session Policy**
- `SessionCreationPolicy.STATELESS` — no server-side session state
- CSRF protection disabled (appropriate for a stateless JWT API)

---

### Authorization

**Role-Based Access Control**

Roles: `ADMIN`, `EDITOR`, `VIEWER`

| Endpoint | Public GET | Write/Delete |
|---|---|---|
| `/api/auth/**` | Yes | Yes (public) |
| `/api/posts/**` | Yes | EDITOR or ADMIN |
| `/api/categories/**` | Yes | ADMIN |
| `/api/pages/**` | Yes | ADMIN |
| `/api/social-links/**` | Yes | ADMIN |
| `/api/site-settings` | Yes | ADMIN |
| `/api/images/**` | Yes | EDITOR or ADMIN |
| `/api/search` | Yes | — |
| `/api/users/me` | Authenticated | — |
| `/api/users/**` | Authenticated | ADMIN |
| `/api/admin/**` | Authenticated | — |
| Swagger UI | ADMIN only (prod) | — |

- `SecurityConfig` defines HTTP-level rules
- `@PreAuthorize` annotations enforce method-level checks in controllers
- Swagger access is controlled via `app.security.swagger-public` (default: `false` in production)

**Frontend Guards**
- `AuthGuard`: redirects unauthenticated users to `/login`, storing a `returnUrl`
- `AlreadyAuthGuard`: prevents already-authenticated users from accessing login/signup
- `RoleGuard`: checks `route.data['roles']` against the user's role, redirects to `/` if unauthorized

**Frontend Interceptors**
- `AuthInterceptor`: injects `Authorization: Bearer <token>` on every request; on 401 automatically calls the refresh endpoint and retries (uses `shareReplay(1)` to prevent duplicate refresh calls); on 403 logs out the user
- `HttpErrorInterceptor`: maps HTTP errors to user-facing messages

**Token Storage (Frontend)**
- Access token is stored in an **in-memory `BehaviorSubject`** — never in `localStorage` or `sessionStorage`
- Refresh token is stored exclusively in an HttpOnly cookie (not accessible to JavaScript)

---

### Input Validation & Sanitization

**DTO Validation**
- All request bodies use `@Valid` with Jakarta Bean Validation annotations (`@NotBlank`, `@Email`, `@NotNull`, etc.)
- `MethodArgumentNotValidException` returns field-level errors with HTTP 400
- Duplicate username/email checks before user creation

**HTML Sanitization**
- Library: **jsoup 1.18** with `Safelist.relaxed()` (extended)
- Applied to all page/post HTML content before persistence
- Strips unconditionally: `<script>`, `<style>`, `<iframe>`, `<form>`, `<input>`, and all JavaScript event attributes
- Enforces on links: `rel="nofollow noopener noreferrer"`, `target="_blank"`
- Prevents stored XSS

---

### Rate Limiting

Custom `RateLimitFilter` using in-memory counters (`ConcurrentHashMap`), 60-second sliding window.

| Endpoint | Limit |
|---|---|
| `POST /api/auth/login` | 10 requests / minute |
| `GET /api/search` | 30 requests / minute |
| `GET /api/posts/popular` | 30 requests / minute |

- IP resolution: reads `X-Forwarded-For` (first value) for proxied requests, falls back to `getRemoteAddr()`
- Memory management: stale counter entries are swept every 500 requests

---

### Security Headers

Set by `SecureHeadersFilter` on every response:

| Header | Value |
|---|---|
| `X-Content-Type-Options` | `nosniff` |
| `X-Frame-Options` | `DENY` |
| `Referrer-Policy` | `no-referrer-when-downgrade` |
| `X-XSS-Protection` | `0` |
| `Content-Security-Policy` | `default-src 'self'; img-src 'self' data: https:; style-src 'self' 'unsafe-inline'; script-src 'self'; connect-src 'self'` |

---

### CORS

Configured in `SecurityConfig`:
- Allowed origins: `${CORS_ALLOWED_ORIGINS}` (default: `http://localhost:4200`)
- Allowed methods: `GET, POST, PUT, DELETE, OPTIONS`
- Allowed headers: `Authorization, Content-Type, Accept`
- Credentials: allowed (required for cookie-based refresh tokens)
- Applied to all paths (`/**`)

In production, set `CORS_ALLOWED_ORIGINS` to your actual frontend domain.

---

### Error Disclosure

- `server.error.include-message=never` — generic error messages only
- `server.error.whitelabel.enabled=false`
- `spring.jpa.show-sql=false` — SQL not logged in production
- `GlobalExceptionHandler` returns sanitized error objects; internal details are not forwarded to the client

---

### Key Security Configuration Reference

| Property | Default | Notes |
|---|---|---|
| `app.jwt.secret` | `${JWT_SECRET}` | **Required.** Must be ≥ 64 bytes. Use a secrets manager in production. |
| `app.jwt.expiration` | `1800000` | Access token TTL in ms (30 min) |
| `app.refresh.expiration` | `43200000` | Refresh token TTL in ms (12 hours) |
| `app.refresh.cookie.secure` | `true` | Set to `false` only in local dev (HTTP) |
| `app.refresh.cookie.domain` | *(empty)* | Set to your domain in production |
| `app.security.swagger-public` | `false` | `true` opens Swagger to all; keep `false` in production |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4200` | Comma-separated list of trusted frontend origins |
| `spring.jpa.hibernate.ddl-auto` | `validate` | Do not change to `update` or `create` in production |
| `spring.servlet.multipart.max-file-size` | `10MB` | Adjust based on your upload requirements |
