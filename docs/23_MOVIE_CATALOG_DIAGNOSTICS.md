# Inventory Movie catalog failure diagnostics

**Owner-reported outcome:** after reviewing the User Service OAuth client bootstrap
configuration, the owner confirmed successful showtime creation. This is manual
confirmation of that flow, not an independently observed successful POST or
automated verification. The earlier failure investigation is retained below as
history; it no longer represents a currently reported unresolved incident.

`INVENTORY_MOVIE_CATALOG_UNAVAILABLE` is a sanitized 503 response. Its message
does not identify which internal call failed: Inventory verifies movie scheduling
details first and the active screening version second, before persisting a new
showtime. An error at this stage is not evidence of a room/layout/pricing error.

## Evidence from the reported request

Read-only GET diagnostics on 2026-11-10 found:

- Gateway movie detail and the supplied screening-version detail both returned
  200. The movie was NOW_SHOWING with durationMinutes 95; the screening version
  belonged to the movie, was active, and used valid 2D/STANDARD/NO_SUBTITLES data.
- The submitted start/end timestamps span exactly 95 minutes.
- Default local Movie/User/Inventory health endpoints returned UP, and public
  authorization-server metadata advertised the expected token endpoint and
  client_credentials support.

These observations do **not** verify Inventory's service-client credentials,
actual runtime endpoint overrides, cached token validity or the response received
under Inventory's service identity. No token request or showtime POST was issued
by the agent. No database credentials or secret values were read or requested.
At that point, the original cause was unconfirmed pending the diagnostic below.

The subsequently supplied runtime WARN confirmed operation=MOVIE_SCHEDULING,
reason=OAUTH2_AUTHORIZATION_FAILED and oauthError=invalid_token_response. This
locates the failure at token acquisition, before any movie detail request under
Inventory's identity. It does not by itself prove invalid client credentials:
Spring Security wraps token transport/HTTP/conversion errors with this code.
Inspection of the local 6.5.11 client bytecode and core converter sources confirmed
the configured message converters/status handler match the framework's defaults.

## Bounded server-side diagnostic

MovieCatalogClient now emits one WARN on paths translated to this unavailable
error, using only operation/reason enums, an optional upstream numeric HTTP status
and a fixed allowlist of OAuth error codes. Unknown OAuth codes become OTHER.
For OAuth failures, tokenFailure classifies only known cause types within at most
16 cause links; arbitrary messages, classes and stack traces remain excluded.
upstreamStatus can then represent a token endpoint's HTTP error status, rather
than a Movie response, when reason is OAUTH2_AUTHORIZATION_FAILED.

Example shape, not an observed cause for the reported request:

```text
Movie catalog verification failed: operation=MOVIE_SCHEDULING, reason=OAUTH2_AUTHORIZATION_FAILED, upstreamStatus=null, oauthError=invalid_client, tokenFailure=NO_CLASSIFIED_CAUSE
```

Operations are MOVIE_SCHEDULING, MOVIE_RELEASE_DATE and SCREENING_VERSION. Existing
correlation logging can associate this line with a request. Do not enable HTTP
wire logging or include headers, token/credential values, upstream bodies,
exception messages, exception causes or OAuth descriptions in shared logs.

| Reason / status | Boundary to inspect |
| --- | --- |
| NO_AUTHORIZED_CLIENT | Local movie-catalog registration and Client Credentials provider configuration |
| OAUTH2_AUTHORIZATION_FAILED / invalid_client | User Service client registration/authentication and Inventory's existing private configuration |
| OAUTH2_AUTHORIZATION_FAILED / invalid_scope | Registered client scopes versus movie:catalog:read |
| OAUTH2_AUTHORIZATION_FAILED / invalid_token_response or OTHER | Token response/transport boundary; obtain only safe status/code evidence |
| UPSTREAM_HTTP_ERROR / 401 | Movie rejected the service token; cached client eviction remains in place |
| UPSTREAM_HTTP_ERROR / 403 | Downstream authorization policy/accepted service authority |
| UPSTREAM_HTTP_ERROR / other | Movie/proxy HTTP failure; no raw response body is logged |
| UPSTREAM_TRANSPORT_FAILED | Movie connectivity, endpoint selection or configured timeouts |
| UPSTREAM_RESPONSE_UNREADABLE | Movie response content type/JSON conversion |
| INVALID_MOVIE_RESPONSE | Required movie ID, duration, revision or status contract |
| INVALID_SCREENING_VERSION_RESPONSE | Version/movie IDs, active flag or presentation-type response contract |
| INVALID_SCREENING_VERSION_DEFINITION | Revision, format, projection or language/presentation invariant |

For invalid_token_response, use the extra tokenFailure field:

| tokenFailure | Meaning supported by the wrapped exception type |
| --- | --- |
| HTTP_RESPONSE_ERROR | A token HTTP error status was retained; inspect upstreamStatus |
| CONNECT_TIMEOUT / REQUEST_TIMEOUT | Connection/request timed out |
| CONNECTION_FAILED / DNS_FAILURE | Token endpoint connection or address resolution failed |
| TLS_FAILURE | TLS handshake/certificate/SSL boundary failed |
| UNSUPPORTED_CONTENT_TYPE / RESPONSE_CONVERSION_FAILED | Token response media type or conversion failed |
| TRANSPORT_FAILURE / REQUEST_OR_RESPONSE_FAILED | Wrapped request failure without a more specific known cause |
| NO_CLASSIFIED_CAUSE | No known cause was retained; do not invent an HTTP status or root cause |

Source defaults remain movie-catalog registration `inventory-movie-catalog`,
client_credentials with client_secret_basic, scope `movie:catalog:read`, Movie
base URL `http://localhost:8081` and token URI `http://localhost:8082/oauth2/token`.
Runtime values can be overridden; source defaults do not prove live values. Do
not disable the OAuth client or forward a browser/user token to hide this error.
No credentials were rotated, registrations created or database records modified.

## Next diagnostic input and compatibility

The following steps apply if this class of failure recurs; successful creation
has since been reported by the owner as noted above.

After restarting Inventory with the updated class, obtain the single WARN line
when the normal failed operation is next observed. Share only operation, reason,
upstreamStatus, allowlisted oauthError and tokenFailure, optionally with a correlation ID.
Do not share tokens, secrets or request/response headers. Select a fix based on
that evidence instead of assuming the payload or a credential is wrong.

The patch preserves public code/message/status, 404 domain mapping, inactive
version rejection, token-cache eviction on Movie 401, payload validation, OAuth
flow, existing API contracts and MySQL configuration/schema. It adds no retry or
fallback data. Regression sources cover allowlist redaction, cache eviction,
404 preservation, unreadable responses and accepted contract payloads; none was run.

Compile-only verification completed with BUILD SUCCESS:

```powershell
mvn -o "-Dmaven.repo.local=C:/Users/Administrator/.m2/repository" -pl services/inventory-service -am -DskipTests test-compile
```

Production and test sources compile; the diagnostic has not been observed in the
running Inventory process during that compile-only step. Static allowlist/404/
cache-eviction review and `git diff --check` passed. Resolution was subsequently
reported by the owner after the configuration review, not by the logging patch.
