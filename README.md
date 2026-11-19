# Digital Membership App
Spring Boot / HTML / Vanilla JS app for managing member access via QR code

## Database setup

The app defaults to `jdbc:postgresql://localhost:5432/memberships` with the `membership` role and `POSTGRES_PASSWORD`. Override these with `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` for deployment.

Create an empty database owned by the app role before starting the app. Flyway applies `src/main/resources/db/migration/V1__init.sql` on startup, then Hibernate validates the schema. This consolidated migration is for fresh databases; databases that previously applied V1–V4 retain the old migration history and cannot use it unchanged. Add future schema changes as V2 and later migrations.

## Running locally with Docker

Start the existing PostgreSQL container, build the image, and pass the runtime environment file explicitly:

```powershell
docker start membership-postgres
docker build -f dockerfile -t membership-app:demo .
docker run -d --name membership-app --env-file .env.docker -e SPRING_PROFILES_ACTIVE=dev -p 127.0.0.1:8080:8080 membership-app:demo
```

Open `http://localhost:8080/login.html`. View startup logs with `docker logs membership-app`.
For an existing container created with these settings, use `docker start membership-app`.

Create `.env.docker` using `docker.env.example` and fill in your credentials. Its database settings must be:

```properties
SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/memberships
SPRING_DATASOURCE_USERNAME=membership
SPRING_DATASOURCE_PASSWORD=<your local PostgreSQL password>
```

Also set `DEV_ADMIN_PASSWORD`, `RESEND_API_KEY`, `RESEND_FROM`, and `APP_PUBLIC_BASE_URL` in `.env.docker`. Use `KEY=value` without spaces around `=`; Docker cannot directly read Spring properties entries such as `KEY = value` from `dev.env`.

On Docker Desktop, `host.docker.internal` reaches the Windows host's published PostgreSQL port. Inside the app container, `localhost` refers to the app container itself. Environment files must be passed at runtime; they are excluded from the build context. Publishing port 8080 is also required; `EXPOSE` alone does not publish it.

For deployment, supply your deployment database and Resend environment variables directly, set `APP_PUBLIC_BASE_URL` to your public HTTPS origin, and select `SPRING_PROFILES_ACTIVE=prod`.

## Signing in

Open `/login.html` and use the email and password of an enabled app user. On a fresh dev database, set `DEV_ADMIN_PASSWORD` before starting the app to create the initial account at `josh@example.com`.

Spring Security issues an `HttpOnly`, `SameSite=Lax` session cookie with a 30-minute idle timeout. Set `SERVER_SERVLET_SESSION_COOKIE_SECURE=true` in HTTPS deployments, including when a reverse proxy terminates TLS.

For API clients, keep a cookie jar across requests. Get `/api/auth/csrf` before sending a write request and send the returned `token` in the returned `headerName`. Post form-encoded `username` (the email address) and `password` to `/api/auth/login`. Successful login and logout return `204`. Use `GET /api/auth/me` to read the account's `id`, `displayName`, and `role`. Fetch a fresh CSRF token before posting to `/api/auth/logout`, since login clears the previous token.

## Changing your password

Signed-in users can open **Profile** from the avatar menu to change their password. Enter the current password and confirm a different new password of at least 12 characters and at most 72 UTF-8 bytes. API clients can post `currentPassword` and `newPassword` as JSON to `/api/auth/change-password` with their session cookie and a fresh CSRF token; success returns `204`.

## Inviting app users

Select a member and role in the admin Users tab. The portal creates a pending account and sends **WELCOME TO OUTLAND STAFF** to the member's address through Resend. The invite expires after 24 hours and activates the account when the recipient sets a password of at least 12 characters. To invalidate an outstanding link and issue a fresh one, open the pending user's editor and choose **Resend invite**.

Set `APP_PUBLIC_BASE_URL` to the portal's public HTTPS origin so recipients can reach the password page. The dev profile defaults to `http://localhost:8080`. The account and invite are saved before sending email, so an admin can resend if delivery fails.

## Email delivery

Welcome emails, member QR resends, and staff invitations use Resend in every profile.
Set `RESEND_API_KEY` and `RESEND_FROM` in the application environment or, for dev runs, `dev.env`.
Use a sender on a verified Resend domain to deliver to other recipients; the default `onboarding@resend.dev` sender restricts delivery to your Resend account email.
The diagnostic `POST /api/email/resend/{memberId}` endpoint is available with the dev profile.

## Temporary demo member form

With the `dev` Spring profile active, open `/dev/member-test` and sign in as an Admin.
Enter first name, last name, a unique dummy member email, and **Send welcome emails to:**.
The delivery address can belong to an existing member and can be reused for multiple dummy members.
Only the initial welcome email uses this address; the saved member email stays unique, and later QR resends use the saved address.
The form creates real members with free memberships and sends the welcome email after the member is committed.
Delivery uses Resend.
If delivery fails, the member remains created; check the application and mail delivery logs.
The page and `POST /api/dev/members` are unavailable without `dev`.
This repository currently defaults to `dev`; explicitly select your deployment profile outside development.

TODO: Remove the temporary demo form, `DevMemberController`, `DevMemberService`, `DevMemberCreateRequest`, and related login/security/API hooks after the demo period.
