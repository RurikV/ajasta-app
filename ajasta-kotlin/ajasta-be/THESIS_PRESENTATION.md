# Development of a Kotlin Backend Application for Resource Booking System

## Thesis Presentation Document

---

## 1. Introduction

**Project:** Ajasta - Resource Booking System Backend
**Technology Stack:** Kotlin, Spring Boot, PostgreSQL, Kotlinx Coroutines
**Architecture:** Clean Architecture with Modular Design

This document presents the implementation of a Kotlin-based backend application for a resource booking system, demonstrating modular architecture principles and industry best practices.

---

## 2. Project Structure Overview

```
ajasta-be/
├── ajasta-api-v1-jackson/        # Transport Layer - DTOs & Serialization
├── ajasta-api-v1-mappers/        # Transport Layer - Request/Response Mapping
├── ajasta-app-spring/            # Framework Layer - Spring Boot Application
├── ajasta-app-common/            # Framework Layer - Common App Logic
├── ajasta-app-kafka/             # Framework Layer - Kafka Consumer
├── ajasta-biz/                   # Logic Layer - Business Logic
├── ajasta-lib-cor/               # Logic Layer - Chain of Responsibility Framework
├── ajasta-common/                # Domain Models
├── ajasta-repo-common/           # Storage Layer - Repository Interfaces
├── ajasta-repo-inmemory/         # Storage Layer - In-Memory Implementation
├── ajasta-repo-pgjvm/            # Storage Layer - PostgreSQL Implementation
├── ajasta-stubs/                 # Test Stubs
├── ajasta-repo-tests/            # Repository Tests
├── ajasta-repo-tests-booking/    # Booking Repository Tests
└── ajasta-repo-tests-resource/   # Resource Repository Tests
```

---

## 3. MODULE 1: Transport Module (1 point)

### Purpose
Handles external API communication, JSON serialization/deserialization, and request/response mapping between transport DTOs and internal domain models.

### Key Components

#### 3.1 API DTOs (Data Transfer Objects)
**Location:** `ajasta-api-v1-jackson/`

| File | Purpose |
|------|---------|
| `build/generate-resources/main/src/main/kotlin/top/ajasta/api/v1/models/*` | 46 auto-generated DTOs for API v1 |

**Key DTOs:**
- `BookingCreateRequest/Response` - Create booking operations
- `BookingReadRequest/Response` - Read booking operations
- `BookingUpdateRequest/Response` - Update booking operations
- `BookingDeleteRequest/Response` - Delete booking operations
- `BookingSearchRequest/Response` - Search bookings operations
- `ResourceCreateRequest/Response` - Create resource operations
- `ResourceReadRequest/Response` - Read resource operations
- `ResourceUpdateRequest/Response` - Update resource operations
- `ResourceDeleteRequest/Response` - Delete resource operations
- `ResourceSearchRequest/Response` - Search resources operations
- `AvailabilityRequest/Response` - Check resource availability
- `BookingObject`, `ResourceObject` - Entity representations
- `BookingSlot`, `AvailabilitySlot` - Time slot models
- `Error` - Error response model

**Reference:** `ajasta-api-v1-jackson/build/generate-resources/main/src/main/kotlin/top/ajasta/api/v1/models/`

#### 3.2 JSON Serialization Configuration
**File:** `ajasta-api-v1-jackson/src/main/kotlin/top/ajasta/api/v1/ApiV1Mapper.kt`

```kotlin
// Configures Jackson ObjectMapper with Kotlin module support
// Handles date/time serialization, null handling, etc.
```

#### 3.3 Request Mappers (Transport to Domain)
**File:** `ajasta-api-v1-mappers/src/main/kotlin/top/ajasta/api/v1/mappers/FromTransportMappers.kt` (250 lines)

Maps API requests to internal context:
- Booking operations mapping (Create, Read, Update, Delete, Search)
- Resource operations mapping
- Availability queries
- Debug/stub mode mapping
- Enum converters

#### 3.4 Response Mappers (Domain to Transport)
**File:** `ajasta-api-v1-mappers/src/main/kotlin/top/ajasta/api/v1/mappers/ToTransportMappers.kt` (216 lines)

Maps internal context to API responses:
- Response object builders for all operations
- Entity mappers to transport format
- Error serialization

### API Endpoints
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/v1/bookings/create` | POST | Create a new booking |
| `/v1/bookings/read` | POST | Read booking details |
| `/v1/bookings/update` | POST | Update a booking |
| `/v1/bookings/delete` | POST | Delete a booking |
| `/v1/bookings/search` | POST | Search bookings |
| `/v1/resources/create` | POST | Create a new resource |
| `/v1/resources/read` | POST | Read resource details |
| `/v1/resources/update` | POST | Update a resource |
| `/v1/resources/delete` | POST | Delete a resource |
| `/v1/resources/search` | POST | Search resources |
| `/v1/resources/availability` | POST | Check availability |

---

## 4. MODULE 2: Framework Module (1 point)

### Purpose
Provides the application infrastructure using Spring Boot framework, including dependency injection, configuration, and REST controllers.

### Key Components

#### 4.1 Application Entry Point
**File:** `ajasta-app-spring/src/main/kotlin/top/ajasta/app/spring/Application.kt`

```kotlin
@SpringBootApplication
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
```

#### 4.2 Application Configuration
**File:** `ajasta-app-spring/src/main/kotlin/top/ajasta/app/spring/config/AppConfig.kt` (84 lines)

- `ProdConfig`: PostgreSQL repositories with Exposed SQL framework
- `DevConfig`: In-memory repositories for testing
- `AppSettings` bean configuration

**File:** `ajasta-app-spring/src/main/resources/application.yaml`

```yaml
spring:
  application:
    name: ajasta-app-spring
  jackson:
    property-naming-strategy: LOWER_CAMEL_CASE
    default-property-inclusion: non_null

server:
  port: 8080

springdoc:
  swagger-ui:
    path: /swagger-ui.html
  api-docs:
    path: /api-docs
```

#### 4.3 REST Controllers

**BookingControllerV1** - `ajasta-app-spring/src/main/kotlin/top/ajasta/app/spring/controllers/BookingControllerV1.kt` (74 lines)

```kotlin
@RestController
@RequestMapping("/v1/bookings")
class BookingControllerV1(
    private val processor: AjastaProcessor
) {
    // POST /v1/bookings/create
    // POST /v1/bookings/read
    // POST /v1/bookings/update
    // POST /v1/bookings/delete
    // POST /v1/bookings/search
}
```

**ResourceControllerV1** - `ajasta-app-spring/src/main/kotlin/top/ajasta/app/spring/controllers/ResourceControllerV1.kt`

```kotlin
@RestController
@RequestMapping("/v1/resources")
class ResourceControllerV1(
    private val processor: AjastaProcessor
) {
    // POST /v1/resources/create
    // POST /v1/resources/read
    // POST /v1/resources/update
    // POST /v1/resources/delete
    // POST /v1/resources/search
    // POST /v1/resources/availability
}
```

**AdminController** - `ajasta-app-spring/src/main/kotlin/top/ajasta/app/spring/controllers/AdminController.kt` (291 lines)

```kotlin
@RestController
@RequestMapping("/admin")
class AdminController {
    // POST /admin/generate-test-data - Bulk data generation
    // DELETE /admin/clear-test-data - Clear all test data
}
```

#### 4.4 Security Configuration
**File:** `ajasta-app-spring/src/main/kotlin/top/ajasta/app/spring/config/SecurityConfig.kt`

#### 4.5 Common Application Components
**Location:** `ajasta-app-common/`

| File | Purpose |
|------|---------|
| `AjastaAppSettings.kt` | Application settings interface |
| `AjastaProcessorImpl.kt` | Processor implementation wrapper |
| `AjastaStubProcessor.kt` | Stub processor for testing |
| `ControllerHelper.kt` | Controller helper utilities |

#### 4.6 Kafka Integration (Optional)
**Location:** `ajasta-app-kafka/`

| File | Purpose |
|------|---------|
| `Main.kt` | Kafka consumer entry point |
| `AjastaKafkaConfig.kt` | Kafka configuration |

---

## 5. MODULE 3: Logic Module (1 point)

### Purpose
Implements core business logic using a custom Chain of Responsibility pattern with Kotlin coroutines.

### Key Components

#### 5.1 Chain of Responsibility Framework
**Location:** `ajasta-lib-cor/`

| File | Purpose |
|------|---------|
| `ICorExec.kt` | Core executor interface |
| `CorDslMarker.kt` | DSL marker annotation |
| `corDsl.kt` (83 lines) | DSL builders: rootChain, chain, worker |
| `handlers/AbstractCorExec.kt` | Abstract executor base |
| `handlers/CorChain.kt` | Chain implementation |
| `handlers/CorWorker.kt` | Worker implementation |

**DSL Example:**
```kotlin
rootChain<BizContext> {
    worker("Initialize status") { ... }
    chain("Validation") {
        worker("Validate ID") { ... }
        worker("Validate title") { ... }
    }
    chain("Repository operations") {
        worker("Read from DB") { ... }
    }
    worker("Prepare result") { ... }
}
```

#### 5.2 Business Logic Processor
**File:** `ajasta-biz/src/commonMain/kotlin/top/ajasta/biz/AjastaProcessor.kt` (234 lines)

Defines operation chains for all commands:
- **Booking operations:** Create, Read, Update, Delete, Search
- **Resource operations:** Create, Read, Update, Delete, Search, Get Availability
- **Flow:** Stubs → Validation → Repository → Result

#### 5.3 Business Context
**File:** `ajasta-biz/src/commonMain/kotlin/top/ajasta/biz/BizContext.kt`

Extends AjastaContext with business-specific operations.

#### 5.4 Validation Workers
**Location:** `ajasta-biz/src/commonMain/kotlin/top/ajasta/biz/validation/`

| File | Purpose |
|------|---------|
| `Validation.kt` | Validation helpers |
| `FinishBookingValidation.kt` | Finalize booking validation |
| `FinishBookingFilterValidation.kt` | Booking filter validation |
| `FinishResourceValidation.kt` | Finalize resource validation |
| `FinishResourceFilterValidation.kt` | Resource filter validation |
| `ValidateBookingIdFormat.kt` | Booking ID format check |
| `ValidateBookingIdNotEmpty.kt` | Booking ID presence check |
| `ValidateBookingTitleNotEmpty.kt` | Title presence check |
| `ValidateBookingTitleLength.kt` | Title length check |
| `ValidateBookingDescriptionLength.kt` | Description length check |
| `ValidateBookingResourceIdNotEmpty.kt` | Resource ID presence check |
| `ValidateResourceIdNotEmpty.kt` | Resource ID presence check |
| `ValidateResourceNameNotEmpty.kt` | Resource name presence check |
| `ValidateResourceNameLength.kt` | Resource name length check |
| `ValidateResourcePricePositive.kt` | Price positivity check |
| `ValidateSlotsNotEmpty.kt` | Slots presence check |
| `ValidateAvailabilityDateRange.kt` | Date range validation |
| `ValidateAvailabilityDatesProvided.kt` | Dates presence check |
| `ValidateAvailabilityResourceIdNotEmpty.kt` | Resource ID for availability |

#### 5.5 Repository Workers
**Location:** `ajasta-biz/src/commonMain/kotlin/top/ajasta/biz/repo/`

| File | Purpose |
|------|---------|
| `BookingRepoWorkers.kt` | Booking repository operations |
| `ResourceRepoWorkers.kt` | Resource repository operations |
| `RepoPrepareWorkers.kt` | Repository preparation |

#### 5.6 Stub Handlers (Testing)
**Location:** `ajasta-biz/src/commonMain/kotlin/top/ajasta/biz/stubs/`

| File | Purpose |
|------|---------|
| `Stubs.kt` | Stub configuration |
| `StubBookingCreateSuccess.kt` | Successful booking creation stub |
| `StubBookingDeleteSuccess.kt` | Successful booking deletion stub |
| `StubBookingReadSuccess.kt` | Successful booking read stub |
| `StubBookingSearchSuccess.kt` | Successful booking search stub |
| `StubBookingUpdateSuccess.kt` | Successful booking update stub |
| `StubResourceCreateSuccess.kt` | Successful resource creation stub |
| `StubResourceDeleteSuccess.kt` | Successful resource deletion stub |
| `StubResourceReadSuccess.kt` | Successful resource read stub |
| `StubResourceSearchSuccess.kt` | Successful resource search stub |
| `StubResourceUpdateSuccess.kt` | Successful resource update stub |
| `StubGetAvailabilitySuccess.kt` | Successful availability stub |
| `StubDeleteError.kt` | Error stub for deletion |
| `StubNotFoundError.kt` | Not found error stub |
| `StubSearchError.kt` | Search error stub |
| `StubValidationError.kt` | Validation error stub |

---

## 6. MODULE 4: Storage Module (Database) (1 point)

### Purpose
Provides data persistence using repository pattern with interface-based abstraction, supporting multiple implementations.

### Key Components

#### 6.1 Repository Interfaces
**Location:** `ajasta-repo-common/`

| File | Purpose |
|------|---------|
| `IRepoBooking.kt` (53 lines) | Booking repository interface |
| `IRepoResource.kt` | Resource repository interface |
| `IRepoInitializable.kt` | Initialization interface for testing |
| `DbRequest.kt` | Database request wrappers |
| `DbResponse.kt` | Database response wrappers (Ok, Err, ErrWithData) |
| `RepoInitialized.kt` | Initialization state tracking |

**Interface Example:**
```kotlin
interface IRepoBooking {
    suspend fun createBooking(request: DbBookingRequest): IDbBookingResponse
    suspend fun readBooking(request: DbBookingIdRequest): IDbBookingResponse
    suspend fun updateBooking(request: DbBookingRequest): IDbBookingResponse
    suspend fun deleteBooking(request: DbBookingIdRequest): IDbBookingResponse
    suspend fun searchBookings(request: DbBookingFilterRequest): IDbBookingsResponse
}
```

#### 6.2 In-Memory Repository (Development/Testing)
**Location:** `ajasta-repo-inmemory/`

| File | Purpose |
|------|---------|
| `RepoBookingInMemory.kt` (174 lines) | In-memory booking repository |
| `RepoResourceInMemory.kt` | In-memory resource repository |

**Features:**
- Thread-safe with Mutex
- Concurrency control via optimistic locking
- Pagination support
- Fast for development and testing

#### 6.3 PostgreSQL Repository (Production)
**Location:** `ajasta-repo-pgjvm/`

| File | Purpose |
|------|---------|
| `RepoBookingSql.kt` (199 lines) | PostgreSQL booking repository |
| `RepoResourceSql.kt` | PostgreSQL resource repository |
| `BookingTable.kt` | Exposed table definition for bookings |
| `ResourceTable.kt` | Exposed table definition for resources |
| `SqlProperties.kt` | Connection properties |
| `SqlFields.kt` | Field mappers |

**Features:**
- Exposed ORM (JetBrains SQL Framework)
- Transaction management
- Optimistic locking
- Pagination support
- Production-ready

#### 6.4 Domain Models
**Location:** `ajasta-common/src/jvmMain/kotlin/top/ajasta/common/models/`

| File | Purpose |
|------|---------|
| `AjastaBooking.kt` | Booking entity |
| `AjastaBookingId.kt` | Booking ID value object |
| `AjastaBookingFilter.kt` | Search filter |
| `AjastaBookingStatus.kt` | Status enum |
| `AjastaResource.kt` | Resource entity |
| `AjastaResourceId.kt` | Resource ID value object |
| `AjastaResourceFilter.kt` | Search filter |
| `AjastaResourceType.kt` | Type enum |
| `AjastaSlot.kt` | Time slot entity |
| `AjastaCommand.kt` | Command enum |
| `AjastaState.kt` | Processing state enum |
| `AjastaError.kt` | Error model |
| `AjastaUserId.kt` | User ID value object |
| `AjastaRequestId.kt` | Request ID value object |
| `AjastaLock.kt` | Optimistic lock value object |
| `AjastaPaymentStatus.kt` | Payment status enum |

#### 6.5 Context
**File:** `ajasta-common/src/jvmMain/kotlin/top/ajasta/common/AjastaContext.kt` (105 lines)

```kotlin
class AjastaContext(
    var state: AjastaState = AjastaState.NONE,
    var errors: MutableList<AjastaError> = mutableListOf(),
    // Request/Response objects for bookings and resources
    // Pagination metadata
)
```

---

## 7. MODULE 5: Tests in Project (2 points)

### Purpose
Comprehensive test coverage across all layers ensuring correctness and reliability.

### Test Statistics
- **Total Test Files:** 90+ test files
- **Test Categories:** Unit tests, Integration tests, Repository tests

### Test Files by Module

#### 7.1 Transport Layer Tests
**Location:** `ajasta-api-v1-jackson/src/test/`

| File | Purpose |
|------|---------|
| `RequestV1SerializationTest.kt` | Request serialization tests |
| `ResponseV1SerializationTest.kt` | Response serialization tests |

**Generated DTO Tests (46 files):**
- `BookingCreateRequestTest.kt`, `BookingCreateResponseTest.kt`
- `BookingReadRequestTest.kt`, `BookingReadResponseTest.kt`
- `BookingUpdateRequestTest.kt`, `BookingUpdateResponseTest.kt`
- `BookingDeleteRequestTest.kt`, `BookingDeleteResponseTest.kt`
- `BookingSearchRequestTest.kt`, `BookingSearchResponseTest.kt`
- `ResourceCreateRequestTest.kt`, `ResourceCreateResponseTest.kt`
- ... and 30+ more

#### 7.2 Mapper Tests
**Location:** `ajasta-api-v1-mappers/src/test/`

| File | Purpose |
|------|---------|
| `BookingMapperTest.kt` | Booking DTO-Domain mapping tests |
| `ResourceMapperTest.kt` | Resource DTO-Domain mapping tests |

#### 7.3 Business Logic Tests
**Location:** `ajasta-biz/src/commonTest/`

| File | Purpose |
|------|---------|
| `BookingStubTest.kt` | Booking stub handler tests |
| `ResourceStubTest.kt` | Resource stub handler tests |
| `BookingValidationTest.kt` | Booking validation tests |
| `ResourceValidationTest.kt` | Resource validation tests |
| `ResourcePartialUpdateTest.kt` | Partial update tests |

#### 7.4 Chain of Responsibility Tests
**Location:** `ajasta-lib-cor/src/commonTest/`

| File | Purpose |
|------|---------|
| `CorChainTest.kt` | Chain execution tests |
| `CorDslTest.kt` | DSL builder tests |
| `CorWorkerTest.kt` | Worker execution tests |
| `TestContext.kt` | Test context fixture |

#### 7.5 Repository Tests (Common)
**Location:** `ajasta-repo-tests/`

| File | Purpose |
|------|---------|
| `runRepoTest.kt` | Test runner |
| `RepoBookingCreateTest.kt` | Booking creation tests |
| `RepoBookingReadTest.kt` | Booking read tests |
| `RepoBookingUpdateTest.kt` | Booking update tests |
| `RepoBookingDeleteTest.kt` | Booking deletion tests |
| `RepoBookingSearchTest.kt` | Booking search tests |
| `RepoResourceCreateTest.kt` | Resource creation tests |
| `RepoResourceReadTest.kt` | Resource read tests |
| `RepoResourceUpdateTest.kt` | Resource update tests |
| `RepoResourceDeleteTest.kt` | Resource deletion tests |
| `RepoResourceSearchTest.kt` | Resource search tests |
| `RepoResourceAvailabilityFieldsTest.kt` | Availability field tests |

#### 7.6 Booking Repository Tests (Specialized)
**Location:** `ajasta-repo-tests-booking/src/commonMain/`

| File | Purpose |
|------|---------|
| `RunRepoTest.kt` | Booking test runner |
| `RepoBookingCreateTest.kt` | Create operation tests |
| `RepoBookingReadTest.kt` | Read operation tests |
| `RepoBookingUpdateTest.kt` | Update operation tests |
| `RepoBookingDeleteTest.kt` | Delete operation tests |
| `RepoBookingSearchTest.kt` | Search operation tests |

#### 7.7 Resource Repository Tests (Specialized)
**Location:** `ajasta-repo-tests-resource/src/commonMain/`

| File | Purpose |
|------|---------|
| `RunRepoTest.kt` | Resource test runner |
| `RepoResourceCreateTest.kt` | Create operation tests |
| `RepoResourceReadTest.kt` | Read operation tests |
| `RepoResourceUpdateTest.kt` | Update operation tests |
| `RepoResourceDeleteTest.kt` | Delete operation tests |
| `RepoResourceSearchTest.kt` | Search operation tests |
| `RepoResourceAvailabilityFieldsTest.kt` | Availability tests |

#### 7.8 In-Memory Repository Tests
**Location:** `ajasta-repo-inmemory/src/commonTest/`

| File | Purpose |
|------|---------|
| `RepoBookingInMemoryTest.kt` | In-memory booking repo tests |
| `RepoResourceInMemoryTest.kt` | In-memory resource repo tests |

#### 7.9 PostgreSQL Repository Tests
**Location:** `ajasta-repo-pgjvm/src/test/`

| File | Purpose |
|------|---------|
| `RepoBookingPgTest.kt` | PostgreSQL booking repo tests |
| `RepoResourcePgTest.kt` | PostgreSQL resource repo tests |

#### 7.10 Controller Tests
**Location:** `ajasta-app-spring/src/test/`

| File | Purpose |
|------|---------|
| `BookingControllerV1Test.kt` | Booking REST endpoint tests |
| `ResourceControllerV1Test.kt` | Resource REST endpoint tests |

#### 7.11 Kafka Tests
**Location:** `ajasta-app-kafka/src/test/`

| File | Purpose |
|------|---------|
| `AjastaKafkaTest.kt` | Kafka integration tests |
| `AjastaKafkaConsumerTest.kt` | Kafka consumer tests |

### Running Tests
```bash
# Run all tests
./gradlew jvmTest

# Run specific module tests
./gradlew :ajasta-biz:jvmTest
./gradlew :ajasta-repo-inmemory:jvmTest
./gradlew :ajasta-repo-pgjvm:test
```

---

## 8. MODULE 6: Monitor in Project (2 points)

### Purpose
Application monitoring and health checks using Spring Boot Actuator and OpenAPI documentation.

### Key Components

#### 8.1 Spring Boot Actuator Configuration
**File:** `ajasta-app-spring/src/main/resources/application.yaml`

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

#### 8.2 Monitoring Endpoints

| Endpoint | Purpose | URL |
|----------|---------|-----|
| Health Check | Application health status | `/actuator/health` |
| Application Info | Application metadata | `/actuator/info` |
| Metrics | Application metrics (Micrometer) | `/actuator/metrics` |
| Swagger UI | Interactive API documentation | `/swagger-ui.html` |
| OpenAPI Docs | OpenAPI specification | `/api-docs` |

#### 8.3 Health Check Details

The `/actuator/health` endpoint provides:
- Application status (UP/DOWN)
- Database connectivity status
- Disk space information
- Custom health indicators

**Example Response:**
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" }
  }
}
```

#### 8.4 Metrics Available

The `/actuator/metrics` endpoint provides:
- JVM memory usage
- HTTP request counts and timings
- Database connection pool statistics
- Custom business metrics

#### 8.5 Logging Configuration

```yaml
logging:
  level:
    root: INFO
    top.ajasta: DEBUG
```

---

## 9. MODULE 7: Demonstration of Working Application (2 points)

### System Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   React Client  │────▶│   NGINX Proxy   │────▶│  Kotlin Backend │
│   (Port 3000)   │     │   (Port 80)     │     │   (Port 8080)   │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                                                        │
                        ┌─────────────────┐             │
                        │    Keycloak     │◀────────────┤
                        │   (Port 8081)   │             │
                        └─────────────────┘             │
                                                        │
                        ┌─────────────────┐             │
                        │   PostgreSQL    │◀────────────┘
                        │   (Port 5432)   │
                        └─────────────────┘
```

### Docker Compose Services

**File:** `docker-compose.kotlin.yml`

| Service | Image | Port | Purpose |
|---------|-------|------|---------|
| `ajasta-kotlin-backend` | Custom | 8090 | Kotlin backend API |
| `ajasta-frontend` | Custom | 3000 | React frontend |
| `ajasta-db` | postgres:16 | 5432 | PostgreSQL database |
| `ajasta-keycloak` | quay.io/keycloak | 8081 | Authentication server |
| `ajasta-nginx` | nginx | 80 | Reverse proxy |

### API Demonstration

#### Resource Operations

**Create Resource:**
```bash
curl -X POST http://localhost:8090/v1/resources/create \
  -H "Content-Type: application/json" \
  -d '{
    "requestType": "createResource",
    "debug": { "mode": "PROD" },
    "resource": {
      "name": "Tennis Court 1",
      "type": "TURF_COURT",
      "location": "Building A",
      "description": "Indoor tennis court",
      "pricePerSlot": "25.00",
      "active": true,
      "unitsCount": 1,
      "openTime": "08:00",
      "closeTime": "22:00"
    }
  }'
```

**Search Resources:**
```bash
curl -X POST http://localhost:8090/v1/resources/search \
  -H "Content-Type: application/json" \
  -d '{"requestType": "searchResources", "resourceFilter": {}}'
```

**Check Availability:**
```bash
curl -X POST http://localhost:8090/v1/resources/availability \
  -H "Content-Type: application/json" \
  -d '{
    "requestType": "getAvailability",
    "resourceId": "resource-uuid",
    "startDate": "2025-03-01",
    "endDate": "2025-03-07"
  }'
```

#### Booking Operations

**Create Booking:**
```bash
curl -X POST http://localhost:8090/v1/bookings/create \
  -H "Content-Type: application/json" \
  -d '{
    "requestType": "createBooking",
    "debug": { "mode": "PROD" },
    "booking": {
      "resourceId": "resource-uuid",
      "title": "Tennis Match",
      "description": "Weekly tennis session",
      "slots": [
        {"startTime": "2025-03-01T10:00:00", "endTime": "2025-03-01T11:00:00"}
      ]
    }
  }'
```

### Frontend Integration

**React Components (ajasta-react/):**

| Component | File | Purpose |
|-----------|------|---------|
| `AdminResourcesPage` | `src/components/admin/AdminResourcesPage.jsx` | Resource management UI |
| `AdminResourceFormPage` | `src/components/admin/AdminResourceFormPage.jsx` | Resource create/edit form |
| `Guard.js` | `src/services/Guard.js` | Route protection with RBAC |
| `AdminSidebar` | `src/components/admin/navbar/AdminSidebar.jsx` | Admin navigation |

### Running the Application

```bash
# Start all services
podman-compose -f docker-compose.kotlin.yml up -d

# Check service status
podman ps

# View backend logs
podman logs ajasta-kotlin-backend

# Test health endpoint
curl http://localhost:8090/actuator/health
```

### Feature Demonstration

1. **Resource Management**
   - Create, read, update, delete resources
   - Set availability (opening hours, unavailable dates)
   - Assign resource managers/owners

2. **Booking System**
   - Create bookings with multiple time slots
   - Check resource availability
   - Search and filter bookings

3. **Authentication & Authorization**
   - Keycloak-based OAuth2/OIDC
   - Role-based access control (admin, manager, user)
   - Resource ownership validation

4. **API Documentation**
   - Swagger UI at `/swagger-ui.html`
   - OpenAPI specification at `/api-docs`

---

## 10. Summary

### Requirements Fulfillment

| Requirement | Status | Evidence |
|-------------|--------|----------|
| Transport Module Created | Completed | `ajasta-api-v1-jackson/`, `ajasta-api-v1-mappers/` |
| Framework Module Created | Completed | `ajasta-app-spring/`, `ajasta-app-common/`, `ajasta-app-kafka/` |
| Logic Module Created | Completed | `ajasta-biz/`, `ajasta-lib-cor/` |
| Storage Module Created | Completed | `ajasta-repo-common/`, `ajasta-repo-inmemory/`, `ajasta-repo-pgjvm/` |
| Tests in Project | Completed | 90+ test files across all modules |
| Monitor in Project | Completed | Spring Boot Actuator, Swagger UI |
| Working Application Demonstrated | Completed | Docker Compose deployment, API examples |

### Technologies Used

- **Language:** Kotlin 1.9+
- **Framework:** Spring Boot 3.x
- **Database:** PostgreSQL 16
- **ORM:** JetBrains Exposed
- **Serialization:** Jackson
- **Authentication:** Keycloak (OAuth2/OIDC)
- **Containerization:** Docker/Podman
- **Frontend:** React with Keycloak JS

### Architecture Highlights

1. **Clean Architecture** - Separation of concerns across layers
2. **Modular Design** - Independent, testable modules
3. **Repository Pattern** - Interface-based data access
4. **Chain of Responsibility** - Flexible business logic execution
5. **Value Objects** - Type-safe domain models
6. **Coroutines** - Asynchronous, non-blocking processing

