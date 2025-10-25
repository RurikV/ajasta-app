# CMS Microservice (ajasta-cms)

This repository now contains a separate CMS microservice implemented in Kotlin/Spring Boot and a new main menu item in the React frontend that links to it.

- Backend (Kotlin): `ajasta-cms/`
- Frontend (React): Added a new main menu item “CMS” and a page renderer at route `/cms`.
- Universal controller: `/api/cms/pages/{slug}` returns a page dynamically composed from plugin-based components.

## What’s implemented

1) Microservice separation (as a separate application)
- New service directory `ajasta-cms` with its own Spring Boot application, Maven build and Dockerfile.
- Exposes endpoints under `/api/cms` (example: `/api/cms/pages/home`).

2) Universal controller and components as plugins
- Page is a set of components; every component has params.
- Core interfaces live in `top.ajasta.cms.core`:
  - `ComponentRenderer` (plugin interface)
  - `ComponentRegistry` (IoC-based discovery of renderers)
  - `PageDefinition`, `ComponentInstance`, `RenderedComponent`, `RenderContext`
- Sample dynamic components (plugins):
  - `TextComponent` (`type = "text"`)
  - `ImageComponent` (`type = "image"`)
- New components can be added without changing core code: just add a Spring bean implementing `ComponentRenderer` in any package or an external jar on the classpath.

3) Middleware/Interceptor
- `RequestContextInterceptor` registered via `WebConfig` acts as middleware: logs requests, attaches `X-Request-Id` header, and demonstrates a place to inject cross-cutting concerns.

4) IoC/DI
- Spring dependency injection is used across the service (`ComponentRegistry`, controllers, and interceptors). Renderers are auto-discovered from the application context (explicit dependency resolution via IoC container).

5) Frontend integration
- React app adds a new main menu item “CMS” in `Navbar.jsx`.
- New page component at `src/components/cms/CmsPage.jsx` fetches `/api/cms/pages/home` and renders basic components (Text, Image). This keeps the existing frontend as-is but adds a dedicated CMS menu entry and route.

## API

- GET `/api/cms/pages/{slug}`
  - Returns: `{ slug, title, components: [ { type, props, children? } ] }`
  - Example slug `home` is pre-populated in memory.
- PUT `/api/cms/pages/{slug}`
  - Upsert page definition.
  - Body:
    ```json
    {
      "slug": "home",
      "title": "Welcome",
      "components": [
        { "type": "text", "params": { "text": "Hello!", "tag": "h2" } },
        { "type": "image", "params": { "url": "https://placehold.co/600x200", "alt": "Banner" } }
      ]
    }
    ```

## Running locally

- Build CMS: `cd ajasta-cms && mvn package`
- Run CMS: `java -jar target/ajasta-cms-0.0.1-SNAPSHOT.jar` (defaults to port 8091)
- Open React app and navigate to `/cms`. In development, the CRA dev proxy now routes `/api/cms/*` to `http://localhost:8091` automatically and other `/api/*` to `http://localhost:8090`. The page will call `/api/cms/pages/home` by default.
- If your environment doesn’t use the CRA dev server proxy (e.g., you serve a static build via Nginx), you can override the CMS API target without affecting other APIs:
  - At runtime in the browser console: `window.__CMS_API_BASE_URL = 'http://localhost:8091/api/cms'` then refresh.
  - Or at build time: set `REACT_APP_CMS_API_BASE_URL=http://localhost:8091/api/cms` before running `npm run build`.
- Global override for all APIs remains available via `window.__API_BASE_URL`, but prefer the CMS-specific override if only CMS should point to a different service.

## Run ajasta-cms with Docker

You can run the CMS microservice as a standalone Docker container using the provided Dockerfile.

Prerequisites: Docker installed.

1) Build the image (from repository root):
```bash
# Build a local image named ajasta-cms:local
docker build -t ajasta-cms:local ./ajasta-cms
```

2) Run the container:
```bash
# Expose CMS on localhost:8091
# SERVER_PORT defaults to 8091 inside the container
docker run --rm -p 8091:8091 --name ajasta-cms ajasta-cms:local
```

3) Verify it’s up:
```bash
curl http://localhost:8091/api/cms/pages/home
```
You should get a JSON payload with the demo page definition.

4) Point the frontend to the CMS container when not using the CRA dev proxy:
- Runtime override (in browser console before opening /cms):
  ```js
  window.__CMS_API_BASE_URL = 'http://localhost:8091/api/cms';
  ```
- Or build-time env var before creating a production build:
  ```bash
  REACT_APP_CMS_API_BASE_URL=http://localhost:8091/api/cms npm run build
  ```

Notes:
- To run the container on a different host port, change the left side of -p, e.g. `-p 9091:8091`.
- To change the internal server port, pass `-e SERVER_PORT=8091` and adjust the port mapping accordingly.

## Architectural notes and criteria mapping

- Extension without changing core code (plugins/DSL)
  - Plugins: Add new component types by introducing new `ComponentRenderer` beans (no changes to core controller/registry required). This fulfills the "Dynamic plugins or DSL" criterion.
  - Middleware: Implemented via `RequestContextInterceptor` (cross-cutting concern), satisfying the middleware point.

- IoC usage
  - DI: Spring Boot manages controllers, registries, and plugins via DI. `ComponentRegistry` explicitly resolves plugin dependencies via the application context map of `ComponentRenderer` beans.

- Microservices
  - The CMS is an isolated Spring Boot application with its own Dockerfile, demonstrating microservice separation.

- Patterns used (besides Command/Factory/Adapter)
  - Strategy pattern: Each `ComponentRenderer` is a strategy for rendering a specific component type.
  - Registry pattern: `ComponentRegistry` provides a central lookup for renderers.
  - Interceptor (middleware): Cross-cutting concerns and request context propagation.

- Project-specific complexity and solutions
  - Dynamic composition: Pages can include arbitrary component trees. The registry resolves types at runtime.
  - Versioning/Schema: Rendering uses a simple DTO contract (`RenderedComponent`) to decouple backend component logic from frontend rendering.
  - Observability: Interceptor adds `X-Request-Id` for tracing; can be extended with metrics/log enrichment.
  - Deployment routing: In Kubernetes/Ingress, route `/api/cms/*` to the CMS service. For local dev, use `window.__API_BASE_URL` or a dev proxy.

## Files of interest
- `ajasta-cms/pom.xml` — Kotlin + Spring Boot dependencies
- `ajasta-cms/Dockerfile` — container build
- `ajasta-cms/src/main/kotlin/top/ajasta/cms/CmsApplication.kt` — entry point
- `ajasta-cms/src/main/kotlin/top/ajasta/cms/config/WebConfig.kt` — CORS + interceptor
- `ajasta-cms/src/main/kotlin/top/ajasta/cms/core/Components.kt` — core contracts and registry
- `ajasta-cms/src/main/kotlin/top/ajasta/cms/api/PageController.kt` — universal controller
- `ajasta-cms/src/main/kotlin/top/ajasta/cms/plugins/TextAndImageComponents.kt` — sample plugins
- `ajasta-react/src/components/cms/CmsPage.jsx` — frontend renderer
- `ajasta-react/src/components/common/Navbar.jsx` — new main menu item
- `ajasta-react/src/App.js` — route registration
