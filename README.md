# Digital Membership App
Spring Boot / HTML / Vanilla JS app for managing member access via QR code

## Signing in

Open `/login.html` and use the email and password of an enabled app user. On a fresh dev database, set `DEV_ADMIN_PASSWORD` before starting the app to create the initial account at `josh@example.com`.

Spring Security issues an `HttpOnly`, `SameSite=Lax` session cookie with a 30-minute idle timeout. Set `SERVER_SERVLET_SESSION_COOKIE_SECURE=true` in HTTPS deployments, including when a reverse proxy terminates TLS.

For API clients, keep a cookie jar across requests. Get `/api/auth/csrf` before sending a write request and send the returned `token` in the returned `headerName`. Post form-encoded `username` (the email address) and `password` to `/api/auth/login`. Successful login and logout return `204`. Use `GET /api/auth/me` to read the account's `id`, `displayName`, and `role`. Fetch a fresh CSRF token before posting to `/api/auth/logout`, since login clears the previous token.
