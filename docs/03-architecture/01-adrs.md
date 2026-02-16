# Architecture Decision Records

We document major architectural decisions here. Not every choice gets an ADR — just the ones that shape the system in significant ways.

The format is simple: context, decision, consequences. We want future-us (and anyone else) to understand *why* we built things this way.

---

## ADR-001: Spring Boot for the Backend

### What we decided
Use Spring Boot 3.x with Java 21 for the backend API.

### Why
We needed something mature, well-documented, with a strong ecosystem. Spring Boot gives us:
- Security handled (Spring Security)
- Database access sorted (Spring Data JPA)
- Production-ready features out of the box (actuator, metrics)
- A talent pool that actually knows the framework

### The trade-offs
It's not the lightest framework. Memory footprint is higher than something like Ktor or Quarkus. Startup time isn't instant.

For our use case — a booking system that doesn't need millisecond cold starts — the trade-off is worth it.

### What we considered

| Option | Why we passed |
|--------|---------------|
| Ktor | Smaller ecosystem, harder to hire for |
| Quarkus | Interesting, but less mature |
| Micronaut | Same as Quarkus |

---

## ADR-002: PostgreSQL as the Database

### What we decided
PostgreSQL 16 as our primary data store.

### Why
We're dealing with bookings. Bookings need transactions. Transactions need ACID guarantees.

PostgreSQL gives us:
- Solid transaction support (no double-bookings)
- JSON support when we need flexibility
- A familiar tool for most developers
- Good tooling (pgAdmin, monitoring, backups)

### The trade-offs
Horizontal scaling is harder than with some NoSQL options. For our expected scale, that's fine. We can add read replicas when needed.

---

## ADR-003: JWT for Authentication

### What we decided
JWT tokens for stateless authentication.

### Why
We want to scale horizontally without sticky sessions. JWT lets any instance validate a request without hitting a central session store.

### The trade-offs
Can't easily revoke tokens before expiration. We mitigate this with short expiration times (24 hours) and refresh tokens.

---

## ADR-004: React for Frontend

### What we decided
React 19 with Vite for the frontend.

### Why
It's the ecosystem. React has the most components, the most tutorials, the most developers who know it. That matters for velocity.

### The trade-offs
Client-side rendering has SEO implications. For a booking app behind a login, that's acceptable.

---

## ADR-005: Docker Compose for Development

### What we decided
All development happens in Docker Compose.

### Why
"It works on my machine" is not acceptable. Docker Compose gives everyone — developers, CI, staging — the same environment.

### The trade-offs
Requires Docker knowledge. Adds some overhead. The consistency is worth it.

---

## ADR-006: Kubernetes for Production

### What we decided
Deploy to Kubernetes for production workloads.

### Why
Self-healing, horizontal scaling, declarative configuration. Once you've experienced a system that automatically recovers from failures, you don't want to go back.

### The trade-offs
Complexity. There's a learning curve. But for a production system that needs to be reliable, it's the right tool.

---

## ADR-007: Kotlin Migration (Planned)

### What we decided
Migrate the backend from Java to Kotlin, following the patterns from our marketplace project.

### Why
- Less boilerplate, more expressiveness
- Null safety built into the type system
- Coroutines for async operations
- Same JVM ecosystem (Spring works great with Kotlin)

We've proven the pattern works in the marketplace project. Now we're bringing it here.

### The trade-offs
Team needs to learn Kotlin (not a big leap from Java). Some libraries might have better Java docs than Kotlin docs.

### Timeline
Gradual migration as we touch code. No big-bang rewrite.

---

## ADR-008: Keycloak for Identity (Planned)

### What we decided
Eventually migrate from custom JWT auth to Keycloak.

### Why
As we grow, we'll need:
- Social login (Google, etc.)
- Better user management
- OAuth2/OIDC compliance
- Audit logging of auth events

Keycloak handles all of this. Our current JWT setup works for now, but Keycloak is the future-proof choice.

### The trade-offs
Another service to run and maintain. Migration effort.

---

## ADR-009: OpenSearch for Logs (Planned)

### What we decided
OpenSearch + Fluent Bit for centralized logging.

### Why
When something breaks, we need logs. All the logs. Searchable.

OpenSearch gives us:
- Powerful search (filter by user, resource, error type)
- Dashboards for visualization
- Open-source (no vendor lock-in)

### The trade-offs
Another thing to operate. Storage costs accumulate.

---

## ADR-010: LinkSaver Integration

### What we decided
Build first-class integration with LinkSaver for knowledge management.

### Why
This is our differentiator. Service providers (trainers, consultants) don't just book time — they share knowledge. LinkSaver handles the knowledge part. Ajasta handles the booking part.

The integration points:
- Shared user accounts (eventually)
- Attach LinkSaver resources to bookings
- Display shared content in booking confirmations

### The trade-offs
Adds complexity. Two systems need to stay in sync. But the combined value is greater than either alone.

---

## How We Make These Decisions

1. **Identify the problem** — What are we trying to solve?
2. **Gather options** — What are the alternatives?
3. **Evaluate trade-offs** — Nothing is free
4. **Make a call** — Perfect is the enemy of good
5. **Document it** — Future-us will thank present-us
6. **Revisit if needed** — Decisions aren't permanent

---

## ADRs We Might Need Later

- Event-driven architecture for notifications
- Caching strategy (Redis?)
- CDN for static assets
- Multi-region deployment
- API versioning strategy
