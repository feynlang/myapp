# Docker Migration Design

## Summary

This project will migrate from a JAR-based Azure VM deployment to a Docker-based deployment in the same repository.

The migration will happen in two learning-focused phases:

1. Standardize local and VM runtime around Docker Compose with `app + postgres`
2. Replace JAR artifact delivery with container image delivery from GitHub Actions

The existing repository stays in place. A dedicated branch such as `docker-migration` will isolate the work, while the current JAR deployment remains available until the Docker path is verified.

## Current State

The current deployment flow is:

1. GitHub Actions runs Gradle build and tests
2. A JAR artifact is uploaded
3. The JAR is copied to the Azure VM over SSH
4. A `systemd` service restarts the application
5. Nginx proxies traffic to the Spring Boot process on `localhost:8080`

Important current assumptions:

- Production DB connectivity is configured around `localhost`
- The application binds to `127.0.0.1` in production
- Azure VM setup is optimized for `java -jar` plus `systemd`

These assumptions work for a host-based Java process, but not for a containerized runtime.

## Goals

- Learn a realistic migration path from host-based deployment to container deployment
- Make local and VM runtime behavior more consistent
- Keep the current application codebase intact and avoid a duplicate repository
- Preserve the current safety net until the Docker deployment is proven
- End with CI/CD that builds and deploys container images instead of JAR files

## Non-Goals

- Re-architect the application domain logic
- Introduce Kubernetes or a full container orchestration platform
- Redesign Nginx or public networking beyond what is needed for Docker deployment
- Change application features during the migration

## Decision

The migration will use the existing repository, not a new repository.

Why this is the preferred option:

- The project already has working CI/CD, so migration value comes from evolving a real pipeline
- Git history will clearly show what changed between JAR deployment and Docker deployment
- Avoiding a second repository prevents application code drift
- For a learning project, branch isolation gives enough safety without losing migration context

The recommended working model is:

- Keep `main` stable
- Create a migration branch such as `docker-migration`
- Optionally tag the last known-good JAR deployment state before migration begins

## Target End State

After migration:

1. The app and database run with Docker Compose
2. The VM runtime is driven by `docker compose up -d`
3. GitHub Actions builds and pushes a container image
4. Deployment pulls the new image on the VM and recreates containers
5. Nginx continues to reverse proxy to the application on the VM host
6. The old `app.jar` and `systemd` deployment path can be removed

## Architecture

### Phase 1: Compose Runtime Standardization

The first phase introduces a single runtime model for local development and VM deployment.

Components:

- `app` container for the Spring Boot application
- `db` container for PostgreSQL
- Docker named volume for PostgreSQL persistence
- Host-level Nginx on the VM for reverse proxying

Runtime behavior:

- Spring Boot listens on `0.0.0.0` inside the container
- The application connects to PostgreSQL through service discovery on the Docker network
- Nginx proxies to the exposed application port on the VM host
- Database ports stay private in production

This phase is primarily about operational consistency, not image distribution.

### Phase 2: Image-Based CI/CD

The second phase changes the deployment artifact and rollout path.

Components:

- GitHub Actions for build, test, image build, and image push
- A container registry, preferably GHCR
- VM-side `docker compose pull` and `docker compose up -d`

Runtime behavior:

- Pull requests run tests and image build validation
- Pushes to `main` publish the production image
- The VM deploy job pulls the new image and recreates containers
- Health checks still determine deployment success

This phase replaces the current `scp app.jar` workflow.

## Configuration Design

### Application Configuration

Production configuration must stop assuming host-local process semantics.

Required changes:

- Replace hard-coded production DB host assumptions with environment-driven datasource settings
- Remove or override `server.address: 127.0.0.1` for container runtime
- Move environment-specific values such as DB credentials and OAuth secrets into environment variables
- Keep actuator health enabled for deployment verification

Preferred pattern:

- Let Spring read standard datasource env vars such as `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD`
- Keep application YAML focused on defaults and invariant behavior

### Compose Configuration

The repository will add:

- `Dockerfile`
- `.dockerignore`
- `compose.yaml`
- optionally `.env.example`
- optionally a production-specific compose override if separation becomes useful

The compose setup should define:

- `app` service
- `db` service
- persistent PostgreSQL volume
- health-aware startup dependencies where practical

## CI/CD Design

### CI

The existing Gradle build and test stage remains.

Additions:

- Docker Buildx setup
- Container registry authentication
- Image build
- Image push on `main`

Recommended image tagging:

- `latest`
- commit SHA tag

### CD

The current deployment path:

- download JAR
- copy JAR by SCP
- restart `systemd` service

will be replaced with:

- connect to VM over SSH
- authenticate Docker if needed
- run `docker compose pull`
- run `docker compose up -d`
- run health check against `http://localhost:8080/actuator/health`

Rollback should be possible by redeploying a prior image tag.

## Infrastructure Design

### VM

The VM will shift from Java runtime hosting to container runtime hosting.

Required changes:

- Install Docker Engine
- Install Docker Compose plugin
- Store compose files and env files under `/opt/myapp`
- Retain Nginx on the host unless there is a clear reason to containerize it later

### Networking

Production exposure should remain minimal.

- Public ports: `22`, `80`, `443`
- Application port should only be reachable from the host or trusted reverse proxy path
- Database port should not be exposed publicly

### Persistence

If PostgreSQL is containerized on the VM, persistence must use a named volume or bind-mounted data directory.

The design assumes container recreation is common and data loss is unacceptable.

## Migration Plan Overview

1. Preserve current deployment state with a branch and optional tag
2. Make production configuration environment-driven and container-safe
3. Add Dockerfile and Compose assets
4. Validate local `app + db` runtime with Docker Compose
5. Prepare the VM for Docker Compose runtime
6. Switch VM runtime from host JAR to Compose-managed containers
7. Update GitHub Actions to build and push images
8. Replace JAR delivery steps with image pull and container recreation
9. Remove legacy JAR deployment assets after successful validation

## Validation Strategy

Success criteria:

- Local `docker compose up` starts both app and database successfully
- Core application flows still work after containerization
- VM deployment works through Docker Compose
- GitHub Actions builds and publishes a usable image
- Deployment health check passes after container-based rollout
- Legacy deployment can be removed without feature regression

Verification focus:

- datasource connectivity
- application bind address
- container networking
- persistence behavior for PostgreSQL
- deployment rollback using prior image tags

## Risks And Mitigations

### Risk: `localhost` and `127.0.0.1` assumptions break inside containers

Mitigation:

- move to env-driven configuration
- bind Spring Boot for container access
- use Compose service names for database connectivity

### Risk: migration touches runtime, CI, and infrastructure at once

Mitigation:

- split work into two phases
- keep JAR deployment available until Docker path is proven
- validate locally before switching the VM

### Risk: data loss if database container is recreated incorrectly

Mitigation:

- use persistent volumes
- do not expose DB lifecycle to disposable containers only
- document backup and persistence assumptions in VM setup notes

## Open Decisions

These decisions are intentionally deferred to implementation planning:

- whether to keep a single `compose.yaml` or add a production override
- whether to keep PostgreSQL on the VM host temporarily or containerize it immediately on the VM
- whether to deploy through GHCR only or support an additional registry later

For now, the recommended baseline is:

- single repository
- migration branch
- Compose-managed `app + db`
- GHCR-based image delivery

