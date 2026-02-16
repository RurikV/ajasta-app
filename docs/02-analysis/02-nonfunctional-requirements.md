# Non-Functional Requirements

## The "How" Not the "What"

Functional requirements tell us what the system does. Non-functional requirements tell us how well it does it — speed, reliability, security, all that good stuff.

---

## Performance

### Response Times

| Operation | Target | Why it matters |
|-----------|--------|----------------|
| Page load | < 2 seconds | Users abandon slow sites |
| API response | < 500ms (95th percentile) | Booking should feel instant |
| Search results | < 1 second | Search needs to feel snappy |
| Booking confirmation | < 3 seconds | Payment processing takes time |

### How We Measure

- Frontend: Browser's Navigation Timing API
- Backend: Spring Boot Actuator metrics
- Aggregated in Prometheus, visualized in Grafana

### When It's Slow

If we're not hitting targets:
1. Check database queries (add indexes)
2. Check for N+1 queries
3. Add caching for hot data
4. Scale horizontally if CPU-bound

---

## Availability

### Target

99.9% uptime. That's about 8.7 hours of downtime per year.

### What Counts as Downtime

- Users can't access the site
- Bookings fail unexpectedly
- Payments don't process

### What Doesn't Count

- Planned maintenance (announced 48 hours in advance)
- Issues with third parties (Stripe, AWS) that are out of our control

### How We Achieve This

- Kubernetes self-healing (restart failed pods)
- Database replication (failover in 30 seconds)
- Health checks and automatic recovery

---

## Security

### Authentication

| Requirement | Implementation |
|-------------|----------------|
| Password hashing | bcrypt with salt |
| Session tokens | JWT, 24-hour expiry |
| Refresh tokens | 7-day expiry |
| Brute force protection | 5 attempts, then lockout |

### Data Protection

| Requirement | Implementation |
|-------------|----------------|
| Data in transit | TLS 1.2+ everywhere |
| Passwords at rest | Hashed (never stored plain) |
| Payment data | Never stored (Stripe handles it) |
| PII access | Logged for audit |

### API Security

| Requirement | Implementation |
|-------------|----------------|
| Authentication | JWT on every endpoint (except public) |
| Rate limiting | 100 requests/minute per user |
| Input validation | Server-side on all inputs |
| SQL injection | Parameterized queries only |
| XSS | Output encoding |
| CSRF | Token validation |

### What We Don't Do (Yet)

- Content Security Policy headers (planned)
- Two-factor authentication (planned)
- Security audit by external firm (planned)

---

## Reliability

### Data Integrity

Bookings are the critical path. We can't have:
- Double bookings (two people, same slot)
- Lost payments
- Corrupted data

**How we prevent this:**
- Database transactions (ACID)
- Unique constraints on (resource_id, slot_start)
- Payment idempotency keys

### Error Handling

When things go wrong:
1. Log the error with context
2. Show user-friendly message
3. Offer recovery path if possible
4. Alert on-call if critical

**What users see:**
- "Something went wrong. Please try again." (not a stack trace)
- "This slot is no longer available." (not "ConstraintViolationException")

### Graceful Degradation

If non-critical services fail:
- Payment processing down? Allow booking, send payment link later
- Email service down? Queue emails, send when recovered
- Analytics down? Log locally, sync later

---

## Scalability

### Current Capacity

- 1,000 concurrent users
- 100 bookings per second
- 500 searches per second

### When to Scale

Horizontal scaling when:
- CPU consistently above 70%
- Response times degrade
- Queue depths increase

### How to Scale

1. **Backend**: Add more pods (Kubernetes handles this)
2. **Database**: Add read replicas
3. **Cache**: Introduce Redis for hot data
4. **CDN**: Serve static assets from edge

### What We're Not Doing (Yet)

- Database sharding (single DB is fine for now)
- Multi-region deployment (single region is enough)
- Auto-scaling based on load (manual scaling first)

---

## Maintainability

### Code Quality

| Metric | Target | Why |
|--------|--------|-----|
| Test coverage | > 70% | Catch regressions |
| Critical path coverage | > 90% | Booking/payment must work |
| Cyclomatic complexity | < 10 per method | Keep it readable |

### Documentation

| What | Where |
|------|-------|
| API | OpenAPI spec |
| Architecture | This docs folder |
| Runbooks | ops/ folder |
| ADRs | docs/03-architecture/01-adrs.md |

### Deployment

| Requirement | Implementation |
|-------------|----------------|
| Deploy frequency | On every merge to main |
| Rollback time | < 5 minutes |
| Downtime during deploy | Zero (rolling updates) |
| CI/CD | GitLab CI |

---

## Usability

### Design Principles

- **Mobile-first**: Most bookings happen on phones
- **Forgiving**: Easy to undo mistakes
- **Clear**: No jargon, obvious actions
- **Fast**: Every extra click loses users

### Accessibility

- WCAG 2.1 AA compliance
- Keyboard navigation works
- Screen reader compatible
- Color contrast sufficient

### Internationalization

- Date/time in user's timezone
- Number formatting (1,000 vs 1.000)
- Initially: English and Russian
- Structure supports more languages

---

## Observability

### Metrics (Prometheus)

| Metric | Alert threshold |
|--------|-----------------|
| Error rate | > 5% |
| P95 latency | > 2 seconds |
| CPU usage | > 80% |
| Memory usage | > 85% |
| Pod restarts | > 3 in 5 minutes |

### Logs (OpenSearch)

| Log type | Retention |
|----------|-----------|
| Application logs | 30 days |
| Access logs | 30 days |
| Audit logs | 90 days |
| Error logs | 90 days |

### Tracing (Tempo)

- Distributed traces for all requests
- Retained for 7 days
- Correlated with logs via trace ID

---

## Compliance

### GDPR (For EU Users)

- Right to access their data
- Right to delete their data
- Right to export their data
- Consent for marketing emails
- Privacy policy accessible

### PCI DSS (Payment Card Industry)

We use Stripe, so we don't store card data. This means:
- Lower compliance burden
- Stripe handles the sensitive parts
- We still need to follow their integration guidelines

---

## Summary Table

| Category | Key Target |
|----------|------------|
| Performance | < 2s page load, < 500ms API |
| Availability | 99.9% uptime |
| Security | JWT auth, TLS everywhere |
| Reliability | ACID transactions, graceful degradation |
| Scalability | 1000 concurrent users |
| Maintainability | 70% test coverage |
| Usability | Mobile-first, accessible |
| Observability | Metrics, logs, traces |

---

## What We're Not Doing

Some things are explicitly out of scope for now:

- Real-time collaboration (not needed)
- Offline mode (not needed)
- Native mobile apps (PWA is enough)
- AI/ML features (not needed)
- Blockchain (definitely not needed)

We can revisit if requirements change. YAGNI.
