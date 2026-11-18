# Digital Membership App
Spring Boot / HTML / Vanilla JS app for managing member access via QR code

## Signing in

Open `/login.html` and use the email and password of an enabled app user. On a fresh dev database, set `DEV_ADMIN_PASSWORD` before starting the app to create the initial account at `josh@example.com`.

Spring Security issues an `HttpOnly`, `SameSite=Lax` session cookie with a 30-minute idle timeout. Set `SERVER_SERVLET_SESSION_COOKIE_SECURE=true` in HTTPS deployments, including when a reverse proxy terminates TLS.

For API clients, keep a cookie jar across requests. Get `/api/auth/csrf` before sending a write request and send the returned `token` in the returned `headerName`. Post form-encoded `username` (the email address) and `password` to `/api/auth/login`. Successful login and logout return `204`. Use `GET /api/auth/me` to read the account's `id`, `displayName`, and `role`. Fetch a fresh CSRF token before posting to `/api/auth/logout`, since login clears the previous token.

## Inviting app users

Select a member and role in the admin Users tab. The portal creates a pending account and sends **WELCOME TO OUTLAND STAFF** to the member's address through Resend. Set `RESEND_API_KEY` and use a `RESEND_FROM` address on a domain verified in Resend to send to staff. The invite expires after 24 hours and activates the account when the recipient sets a password of at least 12 characters. To invalidate an outstanding link and issue a fresh one, open the pending user's editor and choose **Resend invite**.

Set `APP_PUBLIC_BASE_URL` to the portal's public HTTPS origin so recipients can reach the password page. The dev and local profiles default to `http://localhost:8080`. The account and invite are saved before sending email, so an admin can resend if delivery fails.

Regular member welcome emails use SMTP. For local testing, start the existing Mailpit container with `docker start mailpit` and open the [Mailpit inbox](http://localhost:8025).
