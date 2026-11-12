# User registration and Google sign-in — Backend

## Scope

The User Service now supports password registration after email ownership is
verified, and Google OpenID Connect sign-in. Local registration creates a
temporary pending intent only; it does not create an account or store a password
before the email token is confirmed. The browser receives the raw token only in
the email link fragment. MySQL stores only its SHA-256 hash.

Google identities are persisted by the exact OIDC `(issuer, subject)` pair.
Google sign-in requires `email_verified=true`. A verified Google email may be
linked to an existing ACTIVE local account only when that account's email is
already verified. Locked, disabled and pending accounts cannot sign in.
New Google accounts receive only the seeded `USER` role. Local-password accounts
never receive a Google credential; Google-only accounts receive an unguessable
random local password hash.

## Local registration API

Both endpoints are public POST operations at User Service and Gateway:

```http
POST /api/v1/auth/registrations
Content-Type: application/json
```

```json
{
  "email": "person@example.com",
  "firstName": "First",
  "lastName": "Last",
  "phoneNumber": "+84000000000"
}
```

The endpoint returns `202 Accepted` with the same generic message for an
available address and an address that already has an account. Pending email
requests are throttled by a per-address cooldown. No password is accepted at
this step.

The verification page must read `token` from the email URL fragment, ask the
user to choose a password, then POST:

```http
POST /api/v1/auth/email-verifications/confirm
Content-Type: application/json
```

```json
{
  "token": "raw-url-safe-token-from-email-fragment",
  "password": "at-least-12-characters"
}
```

Success returns `204 No Content`. The server atomically creates an ACTIVE
verified account, its password credential, profile and `USER` role, then deletes
the pending intent. Tokens are single-use, expire using the existing email
verification lifetime, and invalid/expired tokens share the existing generic
invalid-token error. Password length is at least 12 characters and the existing
bcrypt byte-length limit still applies.
Expired pending intents are deleted by an hourly User Service cleanup job.

The FE repository currently has no `/verify-email` page or registration screen;
that UI work is still required. Do not expose the token as an API response or
put it in a URL query parameter.

## Google sign-in

Google sign-in is disabled by default. Enable it in User Service with:

```text
USER_GOOGLE_LOGIN_ENABLED=true
USER_GOOGLE_CLIENT_ID=<OAuth client ID from deployment environment>
USER_GOOGLE_CLIENT_SECRET=<OAuth client secret from deployment environment>
```

Configure the matching Google OAuth consent screen and an exact authorized
redirect URI. For local direct User Service access, the default callback is
`http://localhost:8082/login/oauth2/code/google`; production deployments should
set the callback/base URL to their externally reachable User Service issuer.
Never commit client credentials.

When enabled, `/login` displays “Tiếp tục với Google”. Google authenticates the
user, then the success handler replaces the external principal with the local
`CinemaUserDetails` principal before the saved Authorization Code + PKCE request
continues. Existing User Service JWT claims, client scopes, issuer, session
revocation and OAuth2 grant policy remain in use.

## Email delivery configuration

Configure SMTP through environment properties; credentials are not committed:

```text
USER_MAIL_HOST
USER_MAIL_PORT
USER_MAIL_USERNAME
USER_MAIL_PASSWORD
USER_MAIL_SMTP_AUTH
USER_MAIL_SMTP_STARTTLS
USER_REGISTRATION_EMAIL_FROM
USER_REGISTRATION_VERIFICATION_URL
USER_REGISTRATION_RESEND_COOLDOWN
```

The verification URL defaults to `http://localhost:5173/verify-email`. The FE
page must consume the fragment token and call the confirmation endpoint. With
SMTP unavailable, new registration fails before creating pending state; it does
not return a token to the caller.

## MySQL and deployment

Flyway V3 adds `pending_user_registrations` and `user_external_identities`.
The migration keeps the existing MySQL dialect, UUID `BINARY(16)` conventions,
FK ownership and existing account tables. No existing rows are transformed.
Deploy the User Service artifact/configuration and allow its normal Flyway
migration before enabling Google login or accepting registration. The migration
was added to source but not executed in this development session. Rollback after
V3 has been applied requires a reviewed forward migration; do not drop these
tables if accounts have started using the new flows.

## Verification state

Production source compilation succeeded with Maven `-DskipTests compile` for
User Service and Gateway. No tests were run, no service was restarted, no
Google provider call was made, and no database/API write was performed. Runtime
OAuth callback, SMTP delivery, Flyway schema validation, and FE confirmation UX
remain to be verified after environment configuration.
