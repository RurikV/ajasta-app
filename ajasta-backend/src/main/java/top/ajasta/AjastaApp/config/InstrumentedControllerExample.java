package top.ajasta.AjastaApp.config;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Example Controller with OpenTelemetry Instrumentation
 *
 * This demonstrates various patterns for manual instrumentation:
 *
 * 1. Automatic HTTP tracing (already enabled by Spring Boot starter)
 * 2. Custom spans for business logic
 * 3. Adding attributes to spans
 * 4. Recording exceptions
 * 5. Adding events to spans
 *
 * Key points:
 * - HTTP requests are automatically traced with spring-boot-starter
 * - Use custom spans for database operations, external API calls, etc.
 * - Add meaningful attributes to make traces searchable
 * - Always record exceptions with span.recordException()
 *
 * To enable this controller, uncomment the @RestController annotation
 * and add appropriate endpoints.
 */
//@RestController
@RequestMapping("/api/example")
public class InstrumentedControllerExample {

    private static final Logger logger = LoggerFactory.getLogger(InstrumentedControllerExample.class);

    @Autowired
    private Tracer tracer;

    /**
     * Example 1: Simple GET request with custom attributes
     *
     * The HTTP span is automatically created by Spring Boot.
     * We add custom attributes to make the trace more useful.
     */
    @GetMapping("/data/{id}")
    public Map<String, Object> getDataById(@PathVariable Long id) {
        // Add custom attributes to the auto-created HTTP span
        TracingUtils.setAttribute("user.id", id);
        TracingUtils.setAttribute("operation", "getDataById");

        logger.info("Fetching data for ID: {}, TraceID: {}", id, TracingUtils.getCurrentTraceId());

        return Map.of(
                "id", id,
                "data", "example data",
                "traceId", TracingUtils.getCurrentTraceId()
        );
    }

    /**
     * Example 2: POST request with error handling
     *
     * Demonstrates proper exception recording in traces.
     */
    @PostMapping("/data")
    public Map<String, Object> createData(@RequestBody Map<String, Object> payload) {
        try {
            // Validate input
            if (payload.get("name") == null) {
                throw new IllegalArgumentException("Name is required");
            }

            // Add event for successful validation
            TracingUtils.addEvent("validation_passed");

            // Simulate processing
            TracingUtils.traceRunnable("database.saveData", () -> {
                // Database operation would go here
                logger.info("Data saved to database");
            });

            TracingUtils.setAttribute("data.created", true);

            return Map.of("message", "Data created successfully", "id", 123);

        } catch (IllegalArgumentException e) {
            // Record exception in the span
            TracingUtils.recordException(e);
            TracingUtils.setAttribute("error.type", "validation_error");

            logger.error("Validation error: {}", e.getMessage());

            return Map.of("error", e.getMessage());
        }
    }

    /**
     * Example 3: Complex operation with nested spans
     *
     * Demonstrates creating child spans for different operations.
     */
    @PostMapping("/process")
    public Map<String, Object> processComplexRequest(@RequestBody Map<String, Object> payload) {
        Span span = tracer.spanBuilder("processComplexRequest").startSpan();

        try (var scope = span.makeCurrent()) {
            // Add attributes
            span.setAttribute("payload.size", payload.size());

            // Step 1: Validate
            TracingUtils.traceRunnable("validation.step", () -> {
                TracingUtils.setAttribute("validation.status", "in_progress");
                // Validation logic
                TracingUtils.setAttribute("validation.status", "completed");
                TracingUtils.addEvent("validation_completed");
            });

            // Step 2: Process
            String result = TracingUtils.traceCallable("processing.step", () -> {
                // Simulate processing
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Processing interrupted", e);
                }
                return "processed";
            });

            // Step 3: Save
            TracingUtils.traceRunnable("database.save", () -> {
                TracingUtils.setAttribute("table", "results");
                // Database save
            });

            return Map.of("status", "success", "result", result);

        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, e.getMessage());
            return Map.of("error", e.getMessage());
        } finally {
            span.end();
        }
    }

    /**
     * Example 4: External API call with tracing
     *
     * Demonstrates tracing external service calls.
     */
    @GetMapping("/external-api")
    public Map<String, Object> callExternalAPI(@RequestParam String url) {
        return TracingUtils.traceCallable("external.http.call", () -> {
            Span span = Span.current();

            // Add HTTP attributes
            span.setAttribute("http.method", "GET");
            span.setAttribute("http.url", url);
            span.setAttribute("http.scheme", "https");

            try {
                // Simulate HTTP call
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("HTTP call interrupted", e);
                }

                // Add response attributes
                span.setAttribute("http.status_code", 200);
                span.setStatus(io.opentelemetry.api.trace.StatusCode.OK);

                return Map.of("status", "success", "data", "external data");

            } catch (Exception e) {
                span.recordException(e);
                span.setAttribute("http.status_code", 500);
                throw e;
            }
        });
    }

    /**
     * Example 5: Database operation with detailed tracing
     *
     * Demonstrates tracing a database query with details.
     */
    @GetMapping("/users/{userId}")
    public Map<String, Object> getUserWithDetails(@PathVariable Long userId) {
        return TracingUtils.traceCallable("database.getUser", () -> {
            Span span = Span.current();

            // Add database attributes
            span.setAttribute("db.system", "postgresql");
            span.setAttribute("db.name", "ajastadb");
            span.setAttribute("db.operation", "SELECT");
            span.setAttribute("db.table", "users");

            // Simulate database query
            TracingUtils.setAttribute("user.id", userId);

            return Map.of(
                    "id", userId,
                    "name", "Example User",
                    "email", "user@example.com"
            );
        });
    }

    /**
     * Example 6: Batch operation with metrics
     *
     * Demonstrates tracing batch operations with metrics.
     */
    @PostMapping("/batch")
    public Map<String, Object> processBatch(@RequestBody java.util.List<Map<String, Object>> items) {
        Span span = tracer.spanBuilder("batch.process").startSpan();

        try (var scope = span.makeCurrent()) {
            span.setAttribute("batch.size", items.size());

            int successCount = 0;
            int failureCount = 0;

            for (Map<String, Object> item : items) {
                try {
                    TracingUtils.traceRunnable("batch.item", () -> {
                        Span itemSpan = Span.current();
                        Object id = item.get("id");
                        if (id instanceof String) {
                            itemSpan.setAttribute("item.id", (String) id);
                        } else if (id instanceof Long) {
                            itemSpan.setAttribute("item.id", (Long) id);
                        } else if (id instanceof Integer) {
                            itemSpan.setAttribute("item.id", ((Integer) id).longValue());
                        } else {
                            itemSpan.setAttribute("item.id", id.toString());
                        }

                        // Process item
                        TracingUtils.addEvent("item_processed");
                    });
                    successCount++;
                } catch (Exception e) {
                    failureCount++;
                    TracingUtils.recordException(e);
                }
            }

            // Add batch metrics
            span.setAttribute("batch.success_count", successCount);
            span.setAttribute("batch.failure_count", failureCount);

            return Map.of(
                    "total", items.size(),
                    "success", successCount,
                    "failed", failureCount
            );

        } finally {
            span.end();
        }
    }
}
