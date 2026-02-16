# API Documentation Overview

## API Design Principles

### RESTful Design
- Resource-oriented URLs
- HTTP methods for CRUD operations
- Consistent response formats
- Proper HTTP status codes

### Authentication
- JWT Bearer token authentication
- Token in `Authorization` header
- Tokens expire after 24 hours

### Versioning
- URL-based versioning: `/api/v1/`
- Backward compatibility maintained

---

## API Endpoints Summary

### Authentication (`/api/auth`)

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/auth/register` | Register new user | No |
| POST | `/api/auth/login` | User login | No |
| POST | `/api/auth/refresh` | Refresh token | Yes |
| POST | `/api/auth/logout` | Logout user | Yes |

### Users (`/api/users`)

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/users/me` | Get current user | Yes |
| PUT | `/api/users/me` | Update profile | Yes |
| GET | `/api/users/{id}` | Get user by ID | Admin |

### Resources (`/api/resources`)

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/resources` | List all resources | No |
| GET | `/api/resources/{id}` | Get resource details | No |
| POST | `/api/resources` | Create resource | Manager |
| PUT | `/api/resources/{id}` | Update resource | Manager |
| DELETE | `/api/resources/{id}` | Delete resource | Admin |
| GET | `/api/resources/{id}/availability` | Get availability | No |

### Bookings (`/api/bookings`)

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/bookings` | List user bookings | Yes |
| GET | `/api/bookings/{id}` | Get booking details | Yes |
| POST | `/api/bookings` | Create booking | Yes |
| PUT | `/api/bookings/{id}` | Update booking | Yes |
| DELETE | `/api/bookings/{id}` | Cancel booking | Yes |

### Orders (`/api/orders`)

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/orders` | List user orders | Yes |
| GET | `/api/orders/{id}` | Get order details | Yes |
| POST | `/api/orders` | Create order | Yes |
| PUT | `/api/orders/{id}/cancel` | Cancel order | Yes |

### Payments (`/api/payments`)

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/payments` | Process payment | Yes |
| GET | `/api/payments/{id}` | Get payment status | Yes |
| POST | `/api/payments/{id}/refund` | Refund payment | Admin |

### Reviews (`/api/reviews`)

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/reviews` | List reviews | No |
| GET | `/api/reviews/{id}` | Get review | No |
| POST | `/api/reviews` | Create review | Yes |
| PUT | `/api/reviews/{id}` | Update review | Yes |
| DELETE | `/api/reviews/{id}` | Delete review | Owner/Admin |

---

## Common Request/Response Formats

### Success Response
```json
{
  "status": "success",
  "data": { ... },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### Error Response
```json
{
  "status": "error",
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid input parameters",
    "details": [
      { "field": "email", "message": "Invalid email format" }
    ]
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### Pagination
```json
{
  "status": "success",
  "data": [...],
  "pagination": {
    "page": 1,
    "pageSize": 20,
    "totalItems": 100,
    "totalPages": 5
  }
}
```

---

## HTTP Status Codes

| Code | Meaning |
|------|---------|
| 200 | OK - Request succeeded |
| 201 | Created - Resource created |
| 204 | No Content - Success (no body) |
| 400 | Bad Request - Invalid input |
| 401 | Unauthorized - Authentication required |
| 403 | Forbidden - Insufficient permissions |
| 404 | Not Found - Resource not found |
| 409 | Conflict - Resource conflict |
| 422 | Unprocessable Entity - Validation error |
| 500 | Internal Server Error |

---

## Rate Limiting

- **Authenticated users**: 100 requests/minute
- **Unauthenticated**: 20 requests/minute
- Rate limit headers included in response:
  - `X-RateLimit-Limit`
  - `X-RateLimit-Remaining`
  - `X-RateLimit-Reset`

---

## Security

- All endpoints use HTTPS
- CORS configured for allowed origins
- Input validation on all endpoints
- SQL injection prevention via parameterized queries
- XSS prevention via output encoding

For detailed API specifications, see the OpenAPI documentation.
