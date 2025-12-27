# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Ajasta App is a full-stack appointment booking platform with Spring Boot backend, React frontend, and comprehensive deployment automation for both local development and cloud VM environments. The project includes a Kubernetes deployment option with Ansible orchestration.

## Architecture

### Backend (ajasta-backend/)
- **Technology**: Spring Boot 3.5 with Java 21
- **Database**: PostgreSQL 16 with JPA/Hibernate
- **Authentication**: JWT-based security
- **Key Features**: User management, resource booking, payment processing (Stripe), email notifications, AWS S3 integration
- **Package Structure**:
  - `auth_users/` - Authentication and user management
  - `reservation/` - Booking and resource management
  - `payment/` - Payment processing with Stripe integration
  - `email_notification/` - Email services
  - `aws/` - AWS S3 file upload services
  - `security/` - JWT and user authentication logic
  - `role/` - Role-based access control

### Frontend (ajasta-react/)
- **Technology**: React SPA with Nginx static serving
- **Key Libraries**: React Router, Axios for API calls, Chart.js for visualizations, Stripe.js for payments
- **Structure**: Component-based with routing for admin/user interfaces

### Deployment Options
- **Local Development**: Docker Compose with development tools (Adminer, Mailhog, debug ports)
- **Cloud VM**: One-command deployment to Yandex Cloud with infrastructure provisioning
- **Kubernetes**: Ansible-based deployment with Longhorn storage and ingress configuration
- **Terraform**: GitLab CI/CD pipeline for infrastructure provisioning

## Common Development Commands

### Local Development
```bash
# Start full local environment
./scripts/deploy-all.zsh --mode local

# Start with clean state
./scripts/deploy-all.zsh --mode local --clean

# View status
./scripts/status-all.zsh --mode local

# View logs
./scripts/logs-all.zsh --mode local --follow
```

### Backend Development (ajasta-backend/)
```bash
# Build and run tests
cd ajasta-backend
./mvnw clean test
./mvnw clean package

# Run with Maven for development
./mvnw spring-boot:run

# Build Docker image
docker build --platform linux/amd64 -t ${DOCKERHUB_USER:-vladimirryrik}/ajasta-backend:alpine .
```

### Frontend Development (ajasta-react/)
```bash
cd ajasta-react
npm install
npm start          # Development server
npm test           # Run tests
npm run build      # Production build
npm run lint       # ESLint
npm run lint:fix   # Auto-fix ESLint issues
```

### Docker Compose Operations
```bash
# Start services
docker-compose up -d

# Rebuild specific service
docker-compose up -d --build ajasta-backend

# View logs
docker-compose logs -f ajasta-backend

# Stop services
docker-compose down
```

### Kubernetes Deployment
```bash
# Deploy full stack to Kubernetes
ansible-playbook k8s/deploy-ajasta.yml -i k8s/inventory.ini -vv

# Deploy only backend/frontend
ansible-playbook k8s/deploy-backend.yml -i k8s/inventory.ini -vv

# Get ingress external IP
ansible-playbook k8s/get-ingress-ip.yml -i k8s/inventory.ini
```

### Terraform Operations
```bash
# Local destroy with GitLab credentials
export GITLAB_PAT="glpat-xxxxxxxxxxxxxxxxxxxx"
./scripts/terraform-destroy-all.sh

# Fetch GitLab CI/CD variables
export GITLAB_PAT="glpat-xxxxxxxxxxxxxxxxxxxx"
source ./scripts/get-gitlab-vars.sh
```

## Configuration

### Environment Variables
- **Database**: `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`
- **Application**: `JWT_SECRET`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- **Yandex Cloud**: `YC_CLOUD_ID`, `YC_FOLDER_ID`, `YC_TOKEN`
- **GitLab CI/CD**: `GITLAB_PAT`, `GITLAB_USERNAME`
- **Optional**: `MAIL_USERNAME`, `MAIL_PASSWORD`, `AWS_*`, `STRIPE_*`
- **Docker**: `DOCKERHUB_USER` for custom image names

### Development Tools Access
- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8090
- **Database**: localhost:15432
- **Adminer (DB UI)**: http://localhost:8080
- **Mailhog (Email Testing)**: http://localhost:8025
- **Java Debug**: Port 5005

## Key Development Patterns

### Backend API Structure
- RESTful APIs with `/api` prefix
- JWT authentication required for most endpoints
- Role-based access control (Admin/User roles)
- Global exception handling with custom exception classes
- DTOs for request/response objects
- Service layer pattern with business logic separation

### Database Initialization
- Resource data initialization via `ResourceDataInitializer`
- Role-based data seeding on startup
- Uses JPA entity relationships with proper cascade handling

### Security Configuration
- JWT token-based authentication
- Custom user details service implementation
- Role-based method security
- CORS configuration for frontend integration

### Payment Processing
- Stripe integration for payment handling
- Payment status tracking with enums
- Success/failure page templates
- Async email notifications for payment events

## Terraform GitLab CI/CD Setup

### Authentication
The Terraform configuration uses GitLab HTTP backend with these requirements:

**Required GitLab CI/CD Variables:**
1. `GITLAB_PAT` - Personal Access Token with `api` scope
2. `GITLAB_USERNAME` - Your GitLab username (e.g., Vladimir.Rurik)

**Yandex Cloud Variables:**
1. `YC_CLOUD_ID` - Yandex Cloud ID
2. `YC_FOLDER_ID` - Yandex Cloud Folder ID
3. `YC_TOKEN` - Yandex Cloud OAuth token

**Critical Configuration Points:**
- Both `GITLAB_PAT` and `GITLAB_USERNAME` MUST be set
- Both MUST be Expanded and NOT Protected in GitLab CI/CD settings
- Same token used in all 6 Terraform jobs (plan + apply + destroy for staging/production)
- Provider configuration uses `default = ""` for yc_token to allow environment variable fallback

### Terraform Jobs

**All jobs now handle HTTP 409 lock release errors gracefully:**
- `terraform:plan:staging` - Creates plan with `|| true`
- `terraform:plan:production` - Creates plan with `|| true`
- `terraform:apply:staging` - Applies with `|| true` and validation
- `terraform:apply:production` - Applies with `|| true` and validation
- `terraform:destroy:staging` - Destroys with `|| true`
- `terraform:destroy:production` - Destroys with `|| true`

**Resources Created (12 total):**
- 4 yandex_vpc_address (master + 3 workers)
- 2 yandex_vpc_network (external + internal)
- 2 yandex_vpc_subnet (external + internal)
- 4 yandex_compute_instance (master + 3 workers)

### Local Terraform Execution

For local Terraform operations (like destroy), use the GitLab variable fetcher:

```bash
# Set GitLab PAT
export GITLAB_PAT="glpat-xxxxxxxxxxxxxxxxxxxx"

# Run destroy script (automatically fetches YC_* variables)
./scripts/terraform-destroy-all.sh
```

Or manually fetch variables:
```bash
source ./scripts/get-gitlab-vars.sh
```

### Provider Authentication

The Yandex provider configuration in `terraform/providers.tf`:
- Uses `yc_token` variable with `default = ""`
- When empty, provider automatically uses `YC_TOKEN` environment variable
- Prevents "one of token or service_account_key_file should be specified" error
- Allows seamless fallback from GitLab CI/CD to local execution

## Testing

### Backend Tests
- Unit tests with H2 in-memory database
- Integration tests for service layers
- Test data cleanup and isolation
- Precision tests for financial calculations (BigDecimal)

### Frontend Tests
- React Testing Library with Jest
- Component testing for critical flows
- Price precision validation tests
- ESLint for code quality

## Deployment Scripts

The `scripts/` directory contains comprehensive deployment automation:
- `deploy-all.zsh` - Main deployment orchestrator
- `status-all.zsh` - Health monitoring
- `logs-all.zsh` - Log aggregation
- `cleanup-all.zsh` - Resource cleanup
- `terraform-destroy-all.sh` - Complete Terraform cleanup
- `get-gitlab-vars.sh` - Fetch GitLab CI/CD variables via API
- SSH and infrastructure management scripts for Yandex Cloud

## Architecture Notes

### Service Communication
- Services communicate via Docker network `ajasta-net`
- Frontend serves React SPA, proxies API calls to backend
- Database access restricted to backend service
- Health checks implemented for all services

### Data Persistence
- PostgreSQL data persisted in Docker volumes
- Database schema managed by JPA/Hibernate migrations
- File uploads handled via AWS S3 integration

### Security Considerations
- JWT secrets should be changed for production
- Database credentials use environment variables
- Debug ports only exposed in development overrides
- HTTPS termination handled by ingress in Kubernetes

### Performance Optimizations
- JVM memory limits configured for container environment
- Frontend built assets optimized via Create React App
- Database connection pooling via HikariCP
- Nginx serves static assets efficiently

## Important Files

### Terraform Configuration
- `terraform/providers.tf` - Yandex provider configuration with environment variable support
- `terraform/backend.tf` - GitLab HTTP backend configuration
- `terraform/staging.tfvars` - Staging environment variables
- `terraform/production.tfvars` - Production environment variables
- `terraform/GITLAB_CI_TERRAFORM_SETUP.md` - Complete GitLab CI/CD setup guide

### CI/CD Configuration
- `.gitlab-ci.yml` - GitLab CI/CD pipeline with Terraform jobs
- All Terraform jobs handle HTTP 409 lock release errors with `|| true`

### Documentation
- `terraform/DESTROY_FIX_SUMMARY.md` - Destroy job fix explanation
- `terraform/COMPLETE_TERRAFORM_FIX_SUMMARY.md` - All Terraform fixes summary
