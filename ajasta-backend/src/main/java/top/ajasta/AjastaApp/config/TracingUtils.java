package top.ajasta.AjastaApp.config;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Utility class for OpenTelemetry distributed tracing
 *
 * Provides helper methods to create custom spans and instrument code
 * without cluttering business logic with tracing code.
 *
 * Usage examples:
 *
 * 1. Record execution time:
 *    <pre>
 *    TracingUtils.traceRunnable(tracer, "userService.createUser", () -> {
 *        userService.createUser(userDto);
 *    });
 *    </pre>
 *
 * 2. Add attributes to current span:
 *    <pre>
 *    TracingUtils.setAttribute("userId", userId);
 *    TracingUtils.setAttribute("email", user.getEmail());
 *    </pre>
 *
 * 3. Record exceptions:
 *    <pre>
 *    TracingUtils.recordException(e);
 *    </pre>
 *
 * 4. Get return value with tracing:
 *    <pre>
 *    User user = TracingUtils.traceCallable(tracer, "database.findUser", () -> {
 *        return userRepository.findById(userId);
 *    });
 *    </pre>
 */
@Component
public class TracingUtils {

    private static final Logger logger = LoggerFactory.getLogger(TracingUtils.class);

    private static Tracer tracer;

    @Autowired
    public void setTracer(Tracer tracer) {
        TracingUtils.tracer = tracer;
    }

    /**
     * Execute a Runnable with a new span
     *
     * @param spanName Name of the span
     * @param runnable Code to execute within the span
     */
    public static void traceRunnable(String spanName, Runnable runnable) {
        Span span = tracer.spanBuilder(spanName).startSpan();
        try (Scope scope = span.makeCurrent()) {
            runnable.run();
            span.setStatus(StatusCode.OK);
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Execute a Callable with a new span and return result
     *
     * @param spanName Name of the span
     * @param supplier Code to execute within the span
     * @param <T> Return type
     * @return Result of the supplier
     */
    public static <T> T traceCallable(String spanName, Supplier<T> supplier) {
        Span span = tracer.spanBuilder(spanName).startSpan();
        try (Scope scope = span.makeCurrent()) {
            T result = supplier.get();
            span.setStatus(StatusCode.OK);
            return result;
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Add a string attribute to the current span
     *
     * @param key Attribute key
     * @param value Attribute value
     */
    public static void setAttribute(String key, String value) {
        Span.current().setAttribute(key, value);
    }

    /**
     * Add a long attribute to the current span
     *
     * @param key Attribute key
     * @param value Attribute value
     */
    public static void setAttribute(String key, long value) {
        Span.current().setAttribute(key, value);
    }

    /**
     * Add a double attribute to the current span
     *
     * @param key Attribute key
     * @param value Attribute value
     */
    public static void setAttribute(String key, double value) {
        Span.current().setAttribute(key, value);
    }

    /**
     * Add a boolean attribute to the current span
     *
     * @param key Attribute key
     * @param value Attribute value
     */
    public static void setAttribute(String key, boolean value) {
        Span.current().setAttribute(key, value);
    }

    /**
     * Record an exception on the current span
     *
     * @param exception Exception to record
     */
    public static void recordException(Throwable exception) {
        Span.current().recordException(exception);
    }

    /**
     * Mark current span as an error
     *
     * @param message Error message
     */
    public static void markAsError(String message) {
        Span.current().setStatus(StatusCode.ERROR, message);
    }

    /**
     * Mark current span as successful
     */
    public static void markAsSuccess() {
        Span.current().setStatus(StatusCode.OK);
    }

    /**
     * Add an event to the current span
     *
     * @param eventName Event name
     */
    public static void addEvent(String eventName) {
        Span.current().addEvent(eventName);
    }

    /**
     * Add an event with attributes to the current span
     *
     * @param eventName Event name
     * @param attributes Event attributes
     */
    public static void addEvent(String eventName, java.util.Map<String, Object> attributes) {
        Span span = Span.current();
        // Convert attributes to proper format
        attributes.forEach((key, value) -> {
            if (value instanceof String) {
                span.setAttribute(key, (String) value);
            } else if (value instanceof Long) {
                span.setAttribute(key, (Long) value);
            } else if (value instanceof Integer) {
                span.setAttribute(key, ((Integer) value).longValue());
            } else if (value instanceof Double) {
                span.setAttribute(key, (Double) value);
            } else if (value instanceof Boolean) {
                span.setAttribute(key, (Boolean) value);
            } else {
                span.setAttribute(key, value.toString());
            }
        });
        span.addEvent(eventName);
    }

    /**
     * Get the current trace ID
     *
     * @return Trace ID or "unknown" if no trace is active
     */
    public static String getCurrentTraceId() {
        String traceId = Span.current().getSpanContext().getTraceId();
        return traceId.isEmpty() ? "unknown" : traceId;
    }

    /**
     * Get the current span ID
     *
     * @return Span ID or "unknown" if no span is active
     */
    public static String getCurrentSpanId() {
        String spanId = Span.current().getSpanContext().getSpanId();
        return spanId.isEmpty() ? "unknown" : spanId;
    }

    /**
     * Log current trace context (useful for debugging)
     */
    public static void logTraceContext() {
        logger.debug("Current trace context - TraceID: {}, SpanID: {}",
                getCurrentTraceId(), getCurrentSpanId());
    }
}
