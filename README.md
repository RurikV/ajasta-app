# Ajasta

A resource booking platform that connects people with places and services they need.

## What This Is About

Ever tried booking a tennis court? Finding an available hairdresser slot? Organizing a kids' birthday party at a playground? It's usually a mess of phone calls, WhatsApp messages, and crossed fingers.

Ajasta fixes that. It's a booking system where:

- **Customers** find and book resources in seconds
- **Resource owners** manage their facilities without the back-and-forth
- **Everyone** gets confirmations, reminders, and peace of mind

## The Bigger Picture: LinkSaver Integration

Here's where it gets interesting. Ajasta isn't standing alone.

We're building an ecosystem with [LinkSaver](../linksaver) — a knowledge management tool. Imagine this scenario:

> A personal trainer uses LinkSaver to organize their materials: workout videos, nutrition guides, exercise tutorials. When a client books a session through Ajasta, the trainer can share relevant resources directly. The client gets their appointment *and* the materials they need.

It's Knowledge Management + Scheduling working together.

Service providers (trainers, consultants, coaches) build their knowledge base in LinkSaver. Their clients book through Ajasta. The two systems talk to each other.

## Who Uses This

**The everyday person** booking a volleyball court for Friday evening. They want to see what's available, pick a slot, pay, and get on with their life.

**The small business owner** running a salon. They're tired of no-shows and double-bookings. They want a system that handles it.

**The facility manager** overseeing a sports complex. They need to see utilization, handle peak times, and keep things organized.

## What's Built

### For Customers
- Browse resources by type and location
- See real-time availability
- Book single or multiple slots
- Pay online (Stripe)
- Get email confirmations and reminders
- Leave reviews

### For Resource Managers
- Create and manage bookable resources
- Set pricing per time slot
- Define availability rules (hours, blocked days, exceptions)
- View bookings and utilization

### For Admins
- User management
- Platform oversight
- Analytics and reporting

## Technical Stack

| Layer | Technology |
|-------|------------|
| Backend | Spring Boot 3.5 → *migrating to Kotlin* |
| Frontend | React 19 |
| Database | PostgreSQL 16 |
| Payments | Stripe |
| Auth | JWT (Keycloak) |
| Storage | AWS S3 |

The backend is currently Java, but we're in the process of rewriting it in Kotlin. The pattern we're following comes from the [marketplace project](../Kotlin/202508-ok-marketplace) — clean architecture, proper separation, modern practices.

## Running Locally

You'll need Docker. Then:

```bash
docker compose up -d
```

That spins up everything:
- Frontend at http://localhost:3000
- Backend API at http://localhost:8090
- Database admin at http://localhost:8080
- Mailhog (email testing) at http://localhost:8025

For the full monitoring stack (OpenSearch, Grafana, Keycloak):

```bash
docker compose --profile monitoring up -d
```

## Project Layout

```
ajasta-app/
├── ajasta-backend/      # Spring Boot → Kotlin backend
├── ajasta-react/        # React frontend
├── ajasta-postgres/     # DB init scripts
├── docs/                # Documentation
├── deploy/              # Infrastructure configs
├── k8s/                 # Kubernetes manifests
└── terraform/           # Cloud infrastructure
```

## Documentation

The detailed docs are in `docs/`:

**Business side:**
- [Target audience and personas](./docs/01-biz/01-target-audience.md)
- [Stakeholders](./docs/01-biz/02-stakeholders.md)
- [User stories](./docs/01-biz/03-bizreq.md)

**Technical side:**
- [Functional requirements](./docs/02-analysis/01-functional-requirements.md)
- [Non-functional requirements](./docs/02-analysis/02-nonfunctional-requirements.md)
- [Architecture decisions](./docs/03-architecture/01-adrs.md)
- [API overview](./docs/03-architecture/02-api.md)
- [Architecture diagrams](./docs/03-architecture/03-arch.md)

## Architecture

At a high level:

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Browser   │────►│   React     │────►│  Backend    │
│             │     │   Nginx     │     │  Spring/Kt  │
└─────────────┘     └─────────────┘     └──────┬──────┘
                                               │
                    ┌─────────────┐            │
                    │  PostgreSQL │◄───────────┘
                    └─────────────┘
```

For the full picture with monitoring, tracing, and auth — see the [architecture docs](./docs/03-architecture/03-arch.md).

## Why Kotlin

The migration to Kotlin isn't random. We've built a solid pattern in our marketplace project that demonstrates:
- Clean architecture that actually makes sense
- Proper separation between API, business logic, and data layers
- Type safety without the verbosity
- Coroutines for async operations
- Testability built in from the start

## Contributing

Standard flow: fork, branch, PR. We'll review.

## Related Projects

- [LinkSaver](../linksaver) — Knowledge management, integrates with Ajasta
- [Marketplace](../Kotlin/202508-ok-marketplace) — Reference Kotlin architecture we're following

---

*Building useful software, one commit at a time.*
