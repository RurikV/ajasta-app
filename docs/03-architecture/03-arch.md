# Architecture

## The Big Picture

At its core, Ajasta is straightforward: a web app, an API, a database.

```
User → Browser → React SPA → Backend API → PostgreSQL
                         ↓
                    Stripe (payments)
                    AWS S3 (images)
                    Email (notifications)
```

The complexity comes from doing this reliably at scale, with proper observability and security.

---

## System Context

Who interacts with the system?

```
┌──────────────┐
│   Customer   │─── books resources
└──────────────┘
       │
       ▼
┌──────────────┐     ┌──────────────┐
│   Ajasta     │────►│    Stripe    │
│     App      │     └──────────────┘
└──────────────┘            │
       │                    │ payments
       ▼                    ▼
┌──────────────┐     ┌──────────────┐
│  PostgreSQL  │     │   AWS S3     │
│   Database   │     │   (images)   │
└──────────────┘     └──────────────┘
       │
       ▼
┌──────────────┐
│   Email      │ notifications
│   Provider   │
└──────────────┘
```

External systems we depend on:
- **Stripe** — payment processing
- **AWS S3** — image storage
- **Email provider** — transactional emails

---

## Container Architecture

The main components:

```
┌─────────────────────────────────────────────────────────────┐
│                        User's Browser                       │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                     React SPA (Nginx)                       │
│                  Serves the frontend bundle                 │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ REST API calls
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   Backend API (Spring Boot)                 │
│                                                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐          │
│  │    Auth     │  │   Booking   │  │   Payment   │          │
│  │  Service    │  │   Service   │  │   Service   │          │
│  └─────────────┘  └─────────────┘  └─────────────┘          │
│                                                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐          │
│  │   Resource  │  │ Notification│  │   Review    │          │
│  │   Service   │  │   Service   │  │   Service   │          │
│  └─────────────┘  └─────────────┘  └─────────────┘          │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ JDBC
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      PostgreSQL 16                          │
│                                                             │
│     users | resources | bookings | orders | payments        │
└─────────────────────────────────────────────────────────────┘
```

---

## Data Model

The core entities and how they relate:

```
┌─────────────┐       ┌─────────────┐
│    User     │       │    Role     │
├─────────────┤       ├─────────────┤
│ id          │───┐   │ id          │
│ email       │   │   │ name        │
│ password    │   │   └─────────────┘
│ name        │   │
│ phone       │   │   ┌─────────────┐
│ isActive    │   └──►│  UserRole   │
└─────────────┘       ├─────────────┤
      │               │ userId      │
      │               │ roleId      │
      ▼               └─────────────┘
┌─────────────┐
│   Resource  │
├─────────────┤       ┌─────────────┐
│ id          │       │    Order    │
│ name        │       ├─────────────┤
│ type        │◄──────│ id          │
│ location    │       │ userId      │
│ pricePerSlot│       │ resourceId  │
│ unitsCount  │       │ status      │
│ openTime    │       │ totalAmount │
│ closeTime   │       └─────────────┘
└─────────────┘             │
                            ▼
                      ┌─────────────┐
                      │ OrderItem   │
                      ├─────────────┤
                      │ id          │
                      │ orderId     │
                      │ slotStart   │
                      │ slotEnd     │
                      │ price       │
                      └─────────────┘
                            │
                            ▼
                      ┌─────────────┐
                      │  Payment    │
                      ├─────────────┤
                      │ id          │
                      │ orderId     │
                      │ amount      │
                      │ status      │
                      │ gatewayTxId │
                      └─────────────┘
```

### Key Relationships

- **User** has many **Roles** (ADMIN, CUSTOMER, RESOURCE_MANAGER)
- **User** makes many **Orders**
- **Order** contains many **OrderItems** (time slots)
- **Order** has one **Payment**
- **Resource** has many **Orders**
- **User** writes **Reviews** for Resources

---

## Infrastructure (Production)

In production, we run on Kubernetes:

```
┌──────────────────────────────────────────────────────────────┐
│                      Kubernetes Cluster                      │
│                                                              │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │                    Ingress (NGINX)                      │ │
│  └─────────────────────────────────────────────────────────┘ │
│                           │                                  │
│           ┌───────────────┼───────────────┐                  │
│           ▼               ▼               ▼                  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐           │
│  │  Frontend   │  │  Frontend   │  │   Backend   │           │
│  │    Pod      │  │    Pod      │  │    Pods     │           │
│  │   (Nginx)   │  │   (Nginx)   │  │ (Spring/Kt) │           │
│  └─────────────┘  └─────────────┘  └─────────────┘           │
│                                          │                   │
│  ┌───────────────────────────────────────┼─────────────────┐ │
│  │                Longhorn               │                 │ │
│  │             (Storage Layer)           ▼                 │ │
│  │                               ┌─────────────┐           │ │
│  │                               │  PostgreSQL │           │ │
│  │                               │   (Stateful)│           │ │
│  │                               └─────────────┘           │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                              │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │                 Observability Stack                     │ │
│  │                                                         │ │
│  │  Prometheus ──► Grafana    Tempo ──► Traces             │ │
│  │                          OpenSearch ──► Logs            │ │
│  └─────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────┘
```

---

## LinkSaver Integration

This is where it gets interesting. Ajasta and LinkSaver work together:

```
┌───────────────────────────────────────────────────────────────┐
│                                                               │
│    ┌─────────────────┐         ┌─────────────────┐            │
│    │     Ajasta      │         │   LinkSaver     │            │
│    │   (Booking)     │◄───────►│  (Knowledge)    │            │
│    └─────────────────┘         └─────────────────┘            │
│             │                          │                      │
│             │                          │                      │
│             ▼                          ▼                      │
│    ┌─────────────────────────────────────────────┐            │
│    │              Shared Layer                   │            │
│    │  • User accounts (eventually unified)       │            │
│    │  • Service provider profiles                │            │
│    │  • Resource-to-content mapping              │            │
│    └─────────────────────────────────────────────┘            │
│                                                               │
└───────────────────────────────────────────────────────────────┘
```

**The flow:**

1. Service provider (trainer) organizes content in LinkSaver
2. Client books a session through Ajasta
3. Provider attaches relevant resources to the booking
4. Client receives booking confirmation + links to resources
5. Client accesses materials via LinkSaver

---

## Security

### Authentication Flow

```
┌──────────┐     ┌──────────┐     ┌──────────┐
│  Login   │────►│ Validate │────►│  Issue   │
│  Page    │     │Password  │     │   JWT    │
└──────────┘     └──────────┘     └──────────┘
                                       │
                                       ▼
┌──────────┐     ┌──────────┐     ┌──────────┐
│ API      │────►│  Verify  │────►│ Process  │
│ Request  │     │   JWT    │     │ Request  │
└──────────┘     └──────────┘     └──────────┘
```

### Key Security Measures

- Passwords hashed with bcrypt
- JWT tokens expire after 24 hours
- HTTPS everywhere
- CORS restricted to known origins
- Input validation on all endpoints
- Rate limiting per user

---

## Observability

We need to see what's happening.

**Metrics (Prometheus + Grafana)**
- Request rates, latency percentiles
- Error rates
- Database connections
- JVM memory, GC pauses

**Logs (OpenSearch)**
- All application logs
- Structured JSON format
- Searchable by user, resource, error type

**Traces (Tempo)**
- Request flow across services
- Identify slow operations
- Debug distributed issues

---

## Scaling Strategy

### Current State
Single instance of each service. Good enough for initial load.

### When We Need More

1. **Horizontal scaling** — Add more backend pods
2. **Read replicas** — For database read scaling
3. **CDN** — For static assets
4. **Caching** — Redis for hot data

### What We're Not Doing (Yet)

- Microservices (monolith is fine for now)
- Event sourcing (Kafka is overkill)
- Multi-region (single region is enough)

---

## Tech Stack Summary

| Layer | Technology | Notes |
|-------|------------|-------|
| Frontend | React 19 + Vite | SPA served by Nginx |
| Backend | Spring Boot 3 → Kotlin | Gradual migration |
| Database | PostgreSQL 16 | ACID transactions |
| Cache | Redis (later) | For hot data |
| Payments | Stripe | Card processing |
| Storage | AWS S3 | Images and files |
| Auth | JWT → Keycloak | Migration planned |
| Container | Docker | Local and CI |
| Orchestration | Kubernetes | Production |
| Monitoring | Prometheus + Grafana | Metrics |
| Logs | OpenSearch | Centralized logging |
| Tracing | Tempo | Distributed traces |
