# GitLab CI/CD Configuration

This directory contains modular GitLab CI/CD pipeline components for the Ajasta App project.

## Directory Structure

```
.gitlab-ci/
├── README.md                          # This file
├── .gitlab-ci-MODULAR_STRUCTURE.md    # Detailed documentation
├── .gitlab-ci-vars.yml                # Global variables
├── .gitlab-ci-terraform.yml           # Infrastructure provisioning
├── .gitlab-ci-docker.yml              # Docker image builds
├── .gitlab-ci-validate.yml            # Validation and linting
├── .gitlab-ci-test.yml                # Automated tests
├── .gitlab-ci-package.yml             # Deployment packaging
└── .gitlab-ci-deploy.yml              # Deployment configurations
```

## Main Pipeline Entry Point

The main GitLab CI/CD configuration is located at `.gitlab-ci.yml` in the project root. It includes all components from this directory using GitLab's `include` directive.

## Component Files

### `.gitlab-ci-vars.yml`
- **Purpose**: Defines all global variables
- **Contents**: Docker, Terraform, Yandex Cloud, GitLab authentication variables
- **Size**: 52 lines

### `.gitlab-ci-terraform.yml`
- **Purpose**: Infrastructure as Code with Terraform
- **Jobs**: Format, validate, security scan, plan, apply, destroy (staging/production)
- **Size**: 656 lines

### `.gitlab-ci-docker.yml`
- **Purpose**: Docker multi-architecture image builds
- **Jobs**: Backend/frontend builds, registry cleanup
- **Size**: 226 lines

### `.gitlab-ci-validate.yml`
- **Purpose**: Code quality and configuration validation
- **Jobs**: Syntax checking, Dockerfile linting, Helm chart validation
- **Size**: 130 lines

### `.gitlab-ci-test.yml`
- **Purpose**: Automated test execution
- **Jobs**: Backend (Maven) and frontend (Jest) tests
- **Size**: 44 lines

### `.gitlab-ci-package.yml`
- **Purpose**: Deployment artifact creation
- **Jobs**: Docker Compose packaging
- **Size**: 43 lines

### `.gitlab-ci-deploy.yml`
- **Purpose**: Application deployment to production environments
- **Jobs**: Yandex Cloud VM, Kubernetes (Helm), ingress patching, health checks
- **Size**: 630 lines

## Pipeline Stages

The pipeline executes in the following order:

1. **validate** - Syntax and configuration validation
2. **plan** - Terraform plan creation
3. **build** - Docker image building
4. **test** - Automated testing
5. **package** - Deployment artifact creation
6. **deploy** - Application deployment

## Documentation

For detailed information about the modular structure, file descriptions, and usage guidelines, see [`.gitlab-ci-MODULAR_STRUCTURE.md`](.gitlab-ci-MODULAR_STRUCTURE.md).

## Quick Reference

### Adding New Jobs
1. Identify the appropriate file based on job type
2. Follow existing patterns for job configuration
3. Ensure proper stage assignment

### Modifying Existing Jobs
1. Navigate to the relevant file
2. Edit job configuration as needed
3. Validate YAML syntax before committing

### Validation
All YAML files are validated for syntax correctness:
```bash
python3 -c "import yaml; yaml.safe_load(open('.gitlab-ci.yml'))"
```

## Benefits of Modular Structure

- **Organization**: Each file has a single, focused purpose
- **Maintainability**: Changes isolated to relevant files
- **Readability**: Smaller file sizes (43-656 lines vs. 1721)
- **Collaboration**: Reduced merge conflicts in team environments
- **Scalability**: Easy to extend with new components

## Support

For questions or issues with the CI/CD pipeline:
1. Check [`.gitlab-ci-MODULAR_STRUCTURE.md`](.gitlab-ci-MODULAR_STRUCTURE.md) for detailed documentation
2. Review GitLab CI/CD pipeline logs in the GitLab UI
