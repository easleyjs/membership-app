# Digital Membership App
Spring Boot / HTML / Vanilla JS app for managing member access via QR code

## Signing in

Open `/login.html` and use the email and password of an enabled app user. On a fresh dev database, set `DEV_ADMIN_PASSWORD` before starting the app to create the initial account at `josh@example.com`.

Spring Security issues an `HttpOnly`, `SameSite=Lax` session cookie with a 30-minute idle timeout. Set `SERVER_SERVLET_SESSION_COOKIE_SECURE=true` in HTTPS deployments, including when a reverse proxy terminates TLS.

For API clients, keep a cookie jar across requests. Get `/api/auth/csrf` before sending a write request and send the returned `token` in the returned `headerName`. Post form-encoded `username` (the email address) and `password` to `/api/auth/login`. Successful login and logout return `204`. Use `GET /api/auth/me` to read the account's `id`, `displayName`, and `role`. Fetch a fresh CSRF token before posting to `/api/auth/logout`, since login clears the previous token.

## Changing your password

Signed-in users can open **Profile** from the avatar menu to change their password. Enter the current password and confirm a different new password of at least 12 characters and at most 72 UTF-8 bytes. API clients can post `currentPassword` and `newPassword` as JSON to `/api/auth/change-password` with their session cookie and a fresh CSRF token; success returns `204`.

## Inviting app users

Select a member and role in the admin Users tab. The portal creates a pending account and sends **WELCOME TO OUTLAND STAFF** to the member's address through the profile's email delivery service (see below). The invite expires after 24 hours and activates the account when the recipient sets a password of at least 12 characters. To invalidate an outstanding link and issue a fresh one, open the pending user's editor and choose **Resend invite**.

Set `APP_PUBLIC_BASE_URL` to the portal's public HTTPS origin so recipients can reach the password page. The dev and local profiles default to `http://localhost:8080`. The account and invite are saved before sending email, so an admin can resend if delivery fails.

## Email delivery by profile

With `local` active, welcome emails, member QR resends, and staff invitations use Mailpit at `localhost:1025`.
Start the existing container with `docker start mailpit` and open the [Mailpit inbox](http://localhost:8025).
The SMTP settings live only in `application-local.properties`.

Without `local` (including `dev` and production), these emails use Resend and do not require SMTP.
Set `RESEND_API_KEY` and `RESEND_FROM` in the application environment or, for dev/local runs, `dev.env`.
Use a sender on a verified Resend domain to deliver to other recipients; the default `onboarding@resend.dev` sender restricts delivery to your Resend account email.
There is no SMTP fallback if Resend fails.

Use `SPRING_PROFILES_ACTIVE=dev,local` to combine the dev demo tools with Mailpit; `local` takes precedence for delivery.
The explicit diagnostic `POST /api/email/resend/{memberId}` endpoint always uses Resend, even locally.

## Temporary demo member form

With the `dev` Spring profile active, open `/dev/member-test` and sign in as an Admin.
Enter first name, last name, a unique dummy member email, and **Send welcome emails to:**.
The delivery address can belong to an existing member and can be reused for multiple dummy members.
Only the initial welcome email uses this address; the saved member email stays unique, and later QR resends use the saved address.
The form creates real members with free memberships and sends the welcome email after the member is committed.
Delivery uses Resend with `dev` alone, or Mailpit with `dev,local`.
If delivery fails, the member remains created; check the application and mail delivery logs.
The page and `POST /api/dev/members` are unavailable without `dev`, including with `local` alone.
This repository currently defaults to `dev`; explicitly select your deployment profile outside development.

TODO: Remove the temporary demo form, `DevMemberController`, `DevMemberService`, `DevMemberCreateRequest`, and related login/security/API hooks after the demo period.
