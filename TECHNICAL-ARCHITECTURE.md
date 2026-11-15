# Technical architecture and interview notes

## Runtime topology

```text
Browser (React + Vite / Nginx)
          |
          | HTTPS in production / HTTP in local development
          v
Spring Boot REST API (modular monolith)
  |          |          |             |
  |          |          |             +--> Kafka producer: question-viewed
  |          |          +------------------> Redis cache
  |          +-----------------------------> MySQL / Hibernate / JPA
  +--> Spring Security JWT resource server
```

Docker Compose starts MySQL 8.4, Redis 7, Kafka 4.1, the Spring Boot API and an Nginx-hosted React build. Kafka has separate internal and host listeners so the API container uses `kafka:9092` and local tools can use `localhost:9092`.

## Backend modules

- **Question:** catalogue search, detail caching, create/update/archive and MCQ metadata.
- **Identity:** registration/login, BCrypt password hashes, signed JWT access tokens and roles.
- **Practice:** attempt lifecycle, selected question IDs, deadline, answer autosave, server scoring and answer review.
- **Bookmarks:** per-user saved questions with uniqueness at the database level.
- **Analytics:** per-user attempt summaries and technology-level score aggregates.
- **Admin:** content maintenance, bulk import and platform overview.

The first release is intentionally a modular monolith. It avoids premature distributed deployment while showing service boundaries suitable for later extraction.

## Authentication and authorization flow

1. Registration validates user input and stores a BCrypt hash. The public route always sets `USER`.
2. Login verifies the password and issues an HMAC-SHA256 JWT with `sub=email`, issuer, expiry, display name and role claim.
3. Spring Security's OAuth2 Resource Server validates JWT signature, issuer and expiry on protected requests.
4. The security filter maps role claims to Spring authorities.
5. Question reads are public; question writes and admin APIs require `ADMIN`; learner APIs require authentication and scope data by authenticated email.
6. Bootstrap admin creation is an environment-based one-time startup operation. There is no public role-promotion route.

The browser keeps the access token in `sessionStorage`, scoped to the current tab. Refresh-token rotation/revocation is not included; the user signs in again after the six-hour access token expires. Production frontends should consider an HttpOnly, Secure, SameSite cookie strategy, strict CSP and the correct CSRF model.

## Timed exam integrity

- Starting an exam selects active MCQ records, shuffles them and stores the exact selected question IDs in a user-owned attempt.
- The start response includes only question text and option labels/text. It never includes correct answers.
- The browser saves the entire current answer map to the attempt endpoint while the attempt is active.
- The server rejects answers for question IDs outside the stored set, invalid option keys, answers after the deadline and writes after submission.
- Submission reads the persisted answer map and compares each answer against the immutable exam-time snapshot, including its hidden answer key. This keeps past results stable if an administrator later edits the live question.
- The completed review, including the correct option and explanation, is stored against that user-owned attempt. A second submission is rejected.

This is stronger than trusting a score or answer key supplied by the browser. It is not intended to defeat a fully compromised client or external proctoring threats.

## Persistence and query design

- MySQL is the source of truth for users, questions, bookmarks and practice attempts.
- JPA uses `ddl-auto=update` for convenient local schema evolution. Production should use explicit migrations and `validate`.
- Search is implemented with Spring Data JPA Specifications and bounded pagination (1–50 records per page).
- Bookmark uniqueness is enforced by a compound database constraint on user and question.
- Attempt queries scope by user email to prevent cross-account result access.

## Cache and messaging

- Question detail reads use Spring Cache with Redis; question writes evict the question cache.
- A Kafka `question-viewed` event is sent best-effort from the question detail service. This is a demonstration seam, not a fully reliable analytics pipeline.
- Production-grade event delivery should use transactional outbox, explicit event IDs/schema versions, idempotent consumers, bounded retries and dead-letter handling.

## API and operations

- Bean Validation protects request DTOs; a centralized exception handler returns structured API errors.
- Springdoc exposes `/swagger-ui.html`.
- Actuator exposes health/info/metrics/Prometheus endpoints; health/info are publicly permitted and the remaining Actuator endpoints require the `ADMIN` role.
- Docker images run the backend as a non-root user. Nginx serves the static frontend and proxies `/api` to the backend.
- CI verifies the Maven backend and builds the Vite frontend.

## Future service extraction

Possible extraction boundaries:

1. Identity Service: users, credentials, JWT signing and account lifecycle.
2. Question Service: question catalogue and admin content APIs.
3. Practice Service: attempts, exams, scoring and review.
4. Analytics Service: consume attempt/view events and build read-optimized aggregates.
5. API Gateway: authentication-aware routing, throttling and request correlation.

Do not extract these until independent scaling, ownership, deployment or failure isolation justifies the extra network, observability and data consistency costs.

## Interview discussion prompts

1. Why is JWT validation implemented using Spring Security's OAuth2 Resource Server support?
2. Why should public registration never accept a role field?
3. Why should exam scoring happen on the server?
4. Why persist answer changes before a timed attempt is submitted?
5. How do you prevent one user from reading another user's attempt history?
6. Why use a database uniqueness constraint in addition to an application check for bookmarks?
7. When should a cached question detail be invalidated?
8. Why is best-effort Kafka publishing not equivalent to an outbox pattern?
9. Why is a modular monolith a sensible first architecture?
10. What operational work is still needed before production deployment?
