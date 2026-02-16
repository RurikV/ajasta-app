# User Stories

## How We Think About Features

We write user stories to keep ourselves honest. Every feature should serve a real person trying to accomplish something real.

Format: "As a [who], I want [what], so that [why]."

---

## Account & Authentication

### Registration
> As a new user, I want to create an account with my email, so that I can start booking resources.

What this means:
- Email and password registration
- Email verification (we don't want fake accounts)
- Password requirements that are actually secure but not annoying

### Login
> As a returning user, I want to log in quickly, so I can make a booking without friction.

What this means:
- Email + password login
- "Remember me" for convenience
- Lockout after too many failed attempts (security)

### Password Reset
> As a user who forgot my password, I want to reset it via email, so I'm not permanently locked out.

What this means:
- Password reset link sent to email
- Link expires after a reasonable time (24 hours)
- Can't reuse recent passwords

---

## Finding Resources

### Browse
> As a customer, I want to see available resources, so I can find something that fits my needs.

What this means:
- List view with key info (name, type, price, rating)
- Filter by type (sports, services, events)
- Filter by location/area
- Sort by price or rating

### Search
> As a customer, I want to search by keyword, so I can quickly find a specific resource.

What this means:
- Full-text search on names and descriptions
- Instant results as you type
- Recent searches saved for convenience

### Details
> As a customer, I want to see full details about a resource, so I can decide if it's right for me.

What this means:
- Photos, description, location
- Price per time slot
- Operating hours
- Reviews from other users
- Real-time availability calendar

---

## Making Bookings

### Single Slot
> As a customer, I want to book one time slot, so I can reserve the resource when I need it.

What this means:
- Pick a date and time (30-minute slots)
- See immediately if it's available
- Add notes if needed (special requests)
- Get confirmation

### Multiple Slots (Same Day)
> As a customer, I want to book several consecutive slots, so I can use the resource for longer.

What this means:
- Select multiple slots at once
- See total price before confirming
- All slots must be available
- One booking reference for the whole thing

### Multi-Day Booking
> As a customer, I want to book across multiple days, so I can set up recurring reservations.

What this means:
- Calendar view for picking dates
- Choose slots for each date
- Summary before confirming
- Useful for weekly classes, recurring appointments

### View My Bookings
> As a customer, I want to see all my bookings, so I know what I have coming up.

What this means:
- List of upcoming and past bookings
- Quick access to details
- Option to cancel if needed

### Cancel Booking
> As a customer, I want to cancel a booking, so I'm not charged for something I can't use.

What this means:
- Cancel button on booking details
- Refund policy shown clearly
- Slot becomes available immediately for others

---

## Payment

### Pay Online
> As a customer, I want to pay with my card, so my booking is confirmed immediately.

What this means:
- Stripe integration for card payments
- Secure payment form
- Payment confirmation page
- Receipt via email

### Payment Links
> As a customer, I want to receive a payment link, so I can pay later if needed.

What this means:
- Link sent via email after booking creation
- Link expires after 24 hours
- Booking confirmed once payment goes through

---

## Managing Resources (For Business Users)

### Create Resource
> As a resource manager, I want to list my resource on the platform, so customers can find and book it.

What this means:
- Name, description, type selection
- Upload photos
- Set price per slot
- Set capacity (how many simultaneous bookings)

### Set Availability
> As a resource manager, I want to control when my resource is available, so I don't get bookings I can't fulfill.

What this means:
- Set operating hours
- Block specific days (holidays, maintenance)
- Block time ranges within days
- Preview the availability calendar

### View Bookings
> As a resource manager, I want to see all bookings for my resources, so I can plan and prepare.

What this means:
- Calendar view with all bookings
- List view with filtering
- Export to CSV for external tools
- Basic stats (utilization, revenue)

---

## Reviews

### Leave a Review
> As a customer, I want to review a resource after using it, so others know what to expect.

What this means:
- 1-5 star rating
- Written review
- Only after a completed booking
- Can edit for 24 hours

### Read Reviews
> As a customer, I want to read other people's reviews, so I can make an informed decision.

What this means:
- Reviews displayed on resource page
- Average rating shown prominently
- Sort by date or rating

---

## Notifications

### Booking Confirmation
> As a customer, I want to receive a confirmation email, so I have proof of my booking.

What this means:
- Email sent immediately after booking
- All key details included
- Link to manage the booking

### Reminders
> As a customer, I want a reminder before my booking, so I don't forget about it.

What this means:
- Reminder 24 hours before
- Another reminder 1 hour before
- Configurable in settings

---

## Admin Functions

### User Management
> As an admin, I want to manage user accounts, so I can handle problems and maintain security.

What this means:
- View all users
- Search and filter
- Activate/deactivate accounts
- Reset passwords if needed

### Platform Analytics
> As an admin, I want to see platform metrics, so I understand how things are going.

What this means:
- Key metrics dashboard
- Booking trends
- Revenue reports
- User growth

---

## LinkSaver Integration

### Share Resources with Booking
> As a service provider using LinkSaver, I want to attach knowledge materials to my bookings, so my clients get more than just a time slot.

What this means:
- Link LinkSaver account to Ajasta profile
- Select materials to share with specific bookings
- Clients receive links to resources automatically

### Knowledge Base Preview
> As a customer, I want to see relevant resources when I book with a trainer/consultant, so I can prepare or follow up.

What this means:
- Shared materials appear in booking confirmation
- Access through LinkSaver or directly in Ajasta
- Provider controls what's shared

---

## Priorities

We divide stories into three buckets:

**Must-have for MVP:**
- Registration, login, password reset
- Browse and search resources
- Single slot booking
- Online payment
- Basic resource management
- Email confirmations

**Should-have:**
- Multi-slot booking
- Reviews
- Booking reminders
- Admin dashboard

**Nice-to-have:**
- Multi-day booking
- Advanced analytics
- LinkSaver integration
- Social login

---

## Acceptance Criteria

Each story has specific acceptance criteria that must be met. We don't consider a story "done" until:

1. Code is written and reviewed
2. Tests pass (unit and integration)
3. Product owner accepts the implementation
4. Documentation is updated if needed

This isn't bureaucracy — it's how we avoid building the wrong thing.
