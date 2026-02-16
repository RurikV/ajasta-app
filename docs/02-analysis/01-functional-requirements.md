# Functional Requirements

## What the System Needs to Do

This document covers the functional requirements — the things the system actually does. Not how fast or how secure (that's the non-functional requirements), but what features exist and how they behave.

---

## User Accounts

### Registration

Users need accounts. We need to know who's booking what.

**How it works:**
- Email + password + name
- Password requirements: 8+ characters, mixed case, at least one number
- Email verification before account activates
- No duplicate emails

**Edge cases:**
- What if someone tries to register with an existing email? Show a clear message.
- What if the verification email bounces? Allow resend after 60 seconds.

### Login

**How it works:**
- Email + password
- Issue JWT token on success
- "Remember me" extends token lifetime
- Lock account after 5 failed attempts (15-minute cooldown)

**Edge cases:**
- What about unverified accounts? Let them log in, but prompt for email verification.
- What if the user forgot their password entirely? That's the password reset flow.

### Password Reset

**How it works:**
- User enters email
- We send a reset link
- Link expires in 24 hours
- New password can't be one of the last 5 used

**Edge cases:**
- What if the email doesn't exist? Show the same success message anyway (don't reveal which emails are registered).
- What if someone requests multiple resets? Only the latest link works.

---

## Resources

### Listing Resources

**How it works:**
- Show all active resources
- Display: name, type, location, price, rating, thumbnail
- Filter by type (sports, services, events)
- Filter by location/area
- Sort by: price, rating, distance
- Paginate at 20 per page

### Resource Details

**How it works:**
- Show full description, photos, location
- Show operating hours
- Show price per 30-minute slot
- Show availability calendar
- Show reviews with ratings

### Resource Types

We support these types:

| Type | What it is |
|------|------------|
| TURF_COURT | Football, soccer on artificial grass |
| VOLLEYBALL_COURT | Indoor or beach volleyball |
| PLAYGROUND | Kids' play areas, party venues |
| HAIRDRESSING_CHAIR | Salon appointments |
| OTHER | Everything else |

### Availability Rules

Resources have complex availability:

- **Operating hours**: "Open 9am to 9pm"
- **Blocked weekdays**: "Closed on Sundays"
- **Blocked dates**: "Closed December 25"
- **Daily blocked ranges**: "Lunch break 12-1pm"

All these rules combine. A slot is only available if it passes all of them.

---

## Bookings

### Single Slot Booking

The basic case: book one 30-minute slot.

**How it works:**
1. User picks a date
2. Sees available slots (green) and unavailable ones (gray)
3. Clicks a slot
4. Adds optional notes
5. Confirms
6. Gets booking ID and confirmation email

### Multi-Slot Booking

Book multiple consecutive slots on the same day.

**How it works:**
1. User selects multiple slots (click and drag, or shift-click)
2. System verifies all slots are available
3. System calculates total price
4. User confirms
5. One booking ID covers all slots

### Multi-Day Booking

Book slots across multiple days (for weekly classes, etc.)

**How it works:**
1. Calendar view shows multiple days
2. User picks slots on different days
3. Summary shows all selected slots
4. Total price calculated
5. One booking ID for everything

### Viewing Bookings

**How it works:**
- List view of all bookings
- Filter by: upcoming, past, cancelled
- Sort by date
- Quick actions: view details, cancel

### Cancelling Bookings

**How it works:**
- Cancel button on booking details
- Show refund policy before confirming
- Process refund if applicable
- Slot immediately becomes available for others

**Refund rules:**
- 24+ hours before: full refund
- 2-24 hours before: 50% refund
- Less than 2 hours: no refund

---

## Payments

### Stripe Integration

**How it works:**
1. Booking created (status: PENDING)
2. User redirected to Stripe checkout
3. User enters card details
4. Stripe processes payment
5. Webhook updates booking status to CONFIRMED
6. Confirmation email sent

### Payment Links

Sometimes users want to pay later.

**How it works:**
1. Booking created
2. Payment link generated
3. Link sent via email
4. Link valid for 24 hours
5. After payment, booking confirmed

### Refunds

**How it works:**
- Triggered by booking cancellation
- Uses Stripe refund API
- Refund amount based on cancellation timing
- User receives refund confirmation email

---

## Notifications

### Confirmation Emails

Sent immediately after booking (if paid) or after payment (if using payment link).

**What's in it:**
- Resource name and location
- Date and time
- Booking reference
- Total amount paid
- Link to manage booking

### Reminder Emails

**When sent:**
- 24 hours before booking
- 1 hour before booking

**What's in it:**
- Quick reminder of date/time/location
- Link to cancel if needed
- (If integrated with LinkSaver) Links to relevant resources

---

## Reviews

### Writing Reviews

**Rules:**
- Only after a completed booking
- One review per booking
- Rating: 1-5 stars
- Text: optional, max 1000 characters
- Can edit within 24 hours

### Displaying Reviews

**How it works:**
- Show on resource page
- Average rating displayed prominently
- Sort by: date, rating
- Paginate at 10 reviews

---

## Resource Management (For Business Users)

### Creating Resources

**Required fields:**
- Name
- Type
- Location
- Description
- Price per slot
- At least one photo

**Optional fields:**
- Capacity (how many simultaneous bookings)
- Operating hours
- Blocked days/times

### Managing Availability

**How it works:**
- Calendar view of current bookings
- Click to block/unblock specific slots
- Bulk actions for blocking date ranges
- Preview how availability looks to customers

### Viewing Bookings

**How it works:**
- Calendar view (daily, weekly, monthly)
- List view with export to CSV
- Filter by status
- See customer name and contact info

---

## Admin Functions

### User Management

**What admins can do:**
- View all users
- Search by email or name
- Activate/deactivate accounts
- Reset passwords
- View user's booking history

### Platform Analytics

**What's tracked:**
- Total bookings over time
- Revenue over time
- Most popular resources
- Booking cancellation rate
- Average booking value

---

## LinkSaver Integration

### Connecting Accounts

Service providers can link their LinkSaver account to Ajasta.

**How it works:**
1. Provider goes to settings
2. Clicks "Connect LinkSaver"
3. Authenticates with LinkSaver
4. Accounts linked

### Sharing Resources with Bookings

**How it works:**
1. Provider creates a booking for a client
2. Provider selects relevant LinkSaver content
3. Client receives booking confirmation with links
4. Client can access shared content

### What Can Be Shared

- Links (URLs to external resources)
- Documents (PDFs, etc.)
- Videos
- Notes
- Tagged collections

---

## API Summary

| Endpoint | Method | What it does |
|----------|--------|--------------|
| `/api/auth/register` | POST | Create account |
| `/api/auth/login` | POST | Get JWT token |
| `/api/resources` | GET | List resources |
| `/api/resources/:id` | GET | Get details |
| `/api/bookings` | POST | Create booking |
| `/api/bookings/:id` | GET | Get booking |
| `/api/bookings/:id/cancel` | POST | Cancel booking |
| `/api/payments` | POST | Process payment |

Full API docs in [02-api.md](./02-api.md).
