# PostgreSQL Dockerfile (Alpine-based)
# This wraps the official postgres:16-alpine image so the project has exactly 3 Dockerfiles.
# It sets useful defaults that can be overridden at runtime.

FROM postgres:16-alpine

# Default credentials and database (override with -e at runtime)
ENV POSTGRES_DB=ajastadb \
    POSTGRES_USER=admin \
    POSTGRES_PASSWORD=adminpw

# Expose default PostgreSQL port
EXPOSE 5432

# Healthcheck to ensure DB is accepting connections
HEALTHCHECK --interval=10s --timeout=5s --start-period=20s --retries=5 \
  CMD pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB" || exit 1

# Data directory (volume defined by base image)
# VOLUME ["/var/lib/postgresql/data"]

# No CMD required; inherited from base image.
