# Design

## Context

See `proposal.md` for motivation and `specs/auth-token/spec.md` for the normative behavior.

This repository is a Java 21 / Quarkus 3.27.4 authentication backend, documented in `README.md` as an intentionally vulnerable, isolated workshop. `POST /auth/login` currently returns HTTP 200 with `token`, `expiresIn` set to 3600, and `user`. `AuthResource` passes the username and role to `LegacyTokenUtil`, but does not use the request's `client`. `LegacyTokenUtil` returns Base64-encoded data containing a hardcoded secret; it is neither a signed token nor a verifiable expiry. `AuthResourceTest` only checks the successful status, username, and token presence.

The frontend contract in `docs/ENDPOINTS.md` sends `username`, `password`, and `client` and consumes `token` and `user`; it does not require an exact response shape. The same document describes `/api/tasks` as a separate service that consumes a bearer token. This repository issues the credential but does not implement that tasks endpoint or its token validator.

## Goals / Non-Goals

**Goals:**

- Replace the reversible token representation with a verifiably signed, expiring token, with the claims and lifetime required by the spec.
- Fail closed when the signing secret is unavailable in a non-laboratory deployment; do not introduce an implicit secret fallback.
- Keep the login contract used by the frontend stable and make the response lifetime derive from the token's actual expiration.
- Keep this change limited to login input validation, token issuance/configuration, and focused verification.

**Non-Goals:**

- Redesigning credential storage, credential verification, persistence, session management, CORS, or the wider application layering.
- Implementing the `/api/tasks` service or its bearer-token validator in this repository.
- Adding refresh tokens, revocation, or a new login flow.
- Changing the frontend contract to require a particular token serialization or to consume additional response fields.

## Protected Contracts

- `POST /auth/login` remains the endpoint and continues accepting the frontend's `username`, `password`, and `client` request fields.
- A valid login continues returning HTTP 200 and the `token` and `user` fields. The `user.username` and optional `user.displayName` remain usable as described by `docs/ENDPOINTS.md`; `expiresIn` remains an additional field.
- Invalid credentials remain an authentication failure and the frontend continues displaying its existing generic message. The frontend is not required to interpret token claims or `expiresIn`.
- The issued credential remains usable as a bearer token by the existing tasks-service integration. Changing its representation requires coordinating the issuer and that service's validator; the API documentation does not promise that tokens are opaque.

## Components Affected

- `src/main/java/com/bcp/workshop/auth/AuthResource.java`: validate the request's `client` before issuing a token and assemble the existing success response without exposing sensitive values.
- `src/main/java/com/bcp/workshop/auth/LoginRequest.java`: retain the existing request shape; no new fields are required.
- `src/main/java/com/bcp/workshop/auth/LegacyTokenUtil.java`: replace the legacy token behavior with the chosen signed-token issuance boundary; remove the embedded secret.
- `src/main/resources/application.properties` and runtime configuration: define the signing-secret configuration contract without committing a secret.
- `src/test/java/com/bcp/workshop/auth/AuthResourceTest.java` and focused token tests: characterize the current endpoint, then verify the new behavior.
- `docs/ENDPOINTS.md` and `README.md`: document the additive `expiresIn` response field and the required isolated-workshop secret setup if implementation changes those operational/API contracts.
- The separate tasks API is an integration dependency only; no files in that service are in scope.

## Decisions

### Use a standard signed JWT/JWS with HMAC-SHA-256

Issue a compact, standards-based signed token using a maintained JWT/JWS library compatible with the project's Quarkus and Java versions. Use HMAC-SHA-256 and the configured shared secret; include `sub`, `client`, `iat`, and `exp`, with `iat` and `exp` represented as epoch-second NumericDate values. Do not put the secret in the token payload. Keep the current 3600-second lifetime unless a later requirement explicitly changes it. Calculate `expiresIn` from the same expiration instant used in the token, rather than maintaining a separate hardcoded response value.

The implementation selects Quarkus SmallRye JWT Build (`io.quarkus:quarkus-smallrye-jwt-build`), the Quarkus integration for SmallRye's JWT builder. The project Quarkus 3.27.4 BOM manages its compatible version, avoiding an independently pinned library version. This selection does not change the external contract or remove the consumer-compatibility gate in task 2.1.

**Alternative considered:** Continue with custom Base64/HMAC string construction. Rejected because it is not a standard verifiable token format and would leave parsing, claim validation, and signature correctness as bespoke security-sensitive code.

**Alternative considered:** Use an asymmetric signing algorithm. Deferred because the current system is a single issuer/consumer workshop flow and the issue does not require independent public-key verification or multiple verifiers. If the tasks service cannot safely share the configured HMAC secret, that constraint must be resolved before rollout rather than silently changing the token contract.

### Bind claims to authenticated identity and validated request client

Validate that `client` is present and non-blank before issuing the token, and use the authenticated user's canonical username for `sub`. Put the validated request value in `client`; do not infer identity or client from untrusted token data or add a client allow-list that is not part of the approved scope. Preserve the existing user response and credential-check flow.

**Alternative considered:** Keep ignoring `client`, or copy the username/role into claims without a client binding. Rejected because it fails the approved token contract and allows tokens to be detached from the requesting client context.

### Require explicit secret configuration and fail closed

Read the signing secret from runtime configuration backed by `AUTH_TOKEN_SECRET`. Provide no source-controlled or implicit production fallback. A non-laboratory deployment without a valid secret must fail closed before issuing tokens (preferably at startup); local workshop/test runs must supply an explicit, non-production secret through their environment or test configuration. Never log the secret or raw token.

**Alternative considered:** Retain the current hardcoded value as a convenience default. Rejected because every installation would share a recoverable signing key. **Alternative considered:** Generate a random key on each startup. Rejected because tokens become unverifiable after restart and separate service instances cannot validate each other's tokens.

### Keep token validation at the consuming service boundary

This backend owns issuance and publishes `exp`; the service that protects `/api/tasks` owns bearer-token signature and expiry validation. Before deploying the new token format, verify that the consumer accepts the selected signed-token contract and rejects tokens at/after `exp`. Cover this with a service-level integration/contract check coordinated with that consumer; do not add a tasks endpoint or duplicate its authorization logic here.

**Alternative considered:** Add task authorization to this backend solely to test expiry. Rejected because it would duplicate responsibilities and expand the application beyond this change.

## Incremental Strategy

1. **Characterize current behavior before changing it.** Extend or add tests that record the current successful login status and response fields, the `user` properties used by the frontend, invalid-credential status, and the current 3600-second response lifetime. Record that a request without `client` is currently accepted so the required rejection is an intentional, reviewed behavior change rather than an accidental regression.
2. **Establish compatibility checks.** Confirm the tasks-service consumer can validate the proposed signed-token contract, including expiry and the shared-secret/key-distribution model. Do not switch issuance until this integration dependency is understood.
3. **Add focused issuer tests.** Verify signature validity with the configured test secret; assert `sub`, `client`, `iat`, `exp`, and `expiresIn`; assert `exp > iat`; and check the one-second lifetime tolerance. Use controlled time or a short test lifetime for expiry checks rather than sleeping for an hour.
4. **Change client and secret behavior.** Add tests for missing/blank client and missing non-laboratory secret. Confirm these paths issue no token and do not expose the secret or token in logs or error responses.
5. **Switch issuance and verify consumers.** Replace the legacy representation, then run the backend tests and the agreed consumer integration check. Keep the response's `token`, `user`, and HTTP 200 behavior intact.
6. **Update operational/API documentation.** Document `expiresIn`, claim/lifetime semantics, and how an isolated workshop supplies its non-production secret. Do not document or commit a real secret.

## Tests Required Before Critical Behavior Changes

- Characterization coverage for the current valid login (`200`, `token`, `user.username`, optional `displayName`, and the observed `expiresIn`), and invalid credentials (authentication failure with no token).
- A characterization case for omitted/blank `client` to establish the current behavior before changing it; update its expected result only as part of the intentional client-validation change.
- Contract tests proving the signed token verifies with the configured test key and contains the required claims, with `exp` later than `iat`.
- Boundary tests for `expiresIn` versus `exp - now`, allowing no more than one second of difference, and for rejecting the token at/after `exp`.
- Configuration tests confirming issuance fails closed without a non-laboratory secret.
- Log/error-output inspection tests confirming token and secret values are not exposed.
- A consumer integration check confirming the tasks API accepts the newly issued token and rejects an expired one. Backend-only unit tests cannot establish this cross-service behavior.

## Risks / Trade-offs

- [Consumer incompatibility] The tasks API may rely on the legacy token representation or key independently of this repository → verify and coordinate consumer support before switching issuance; use a coordinated rollout.
- [Secret distribution] HMAC validation requires the issuer and consumer to receive the same confidential key → distribute it through the deployment's secret mechanism, restrict access, and never check it into configuration files or logs.
- [Stateless expiry] Already issued tokens cannot be revoked centrally → keep the existing one-hour lifetime and require reauthentication after expiry; revocation is outside scope.
- [Clock skew] Different clocks can affect expiry checks and the reported lifetime → synchronize service clocks and use the spec's one-second response tolerance; any verifier clock-skew leeway must not make tokens valid beyond the agreed expiry contract.
- [Unsafe rollback] Reverting to the legacy issuer restores a reversible token and embedded secret → do not use that rollback in any non-laboratory environment; on failure, disable login or roll back only to a release that still meets the signed-token requirements.
- [Test secret leakage] A real signing secret could accidentally enter fixtures or logs → use a clearly non-production test key and assert that test output contains neither token nor key values.

## Rollback

Deploy the issuer only after the consumer compatibility check passes. If the new issuer causes an operational failure, stop or disable login while correcting the issue; do not restore legacy issuance outside an isolated workshop. If the environment is strictly local and isolated, a temporary rollback may be used only with an explicit acceptance that existing/new tokens are incompatible and that the old implementation is insecure. Existing signed tokens remain valid only until their `exp`; clients must log in again after a rollback or key/format change. Keep the signing secret out of rollback artifacts and logs.
