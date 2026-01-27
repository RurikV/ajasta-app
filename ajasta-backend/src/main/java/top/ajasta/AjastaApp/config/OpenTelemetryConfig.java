package top.ajasta.AjastaApp.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.extension.trace.propagation.JaegerPropagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;
import java.util.List;

/**
 * OpenTelemetry Configuration for distributed tracing with Tempo backend
 *
 * This configuration sets up automatic tracing for Spring Boot applications
 * and exports traces to Tempo via OTLP (OpenTelemetry Protocol) over gRPC.
 *
 * Environment Variables:
 * - OTEL_EXPORTER_OTLP_ENDPOINT: Tempo OTLP endpoint (default: http://tempo:4317)
 * - OTEL_SERVICE_NAME: Service name for tracing (default: ajasta-backend)
 * - OTEL_EXPORTER_OTLP_HEADERS: Optional headers for authentication
 *
 * Usage:
 * - Automatic tracing of all HTTP requests via Spring Boot starter
 * - Manual instrumentation: @Autowired Tracer tracer
 */
@Configuration
public class OpenTelemetryConfig {

    private static final Logger logger = LoggerFactory.getLogger(OpenTelemetryConfig.class);

    @Value("${otel.exporter.otlp.endpoint:http://tempo:4317}")
    private String otelEndpoint;

    @Value("${otel.service.name:ajasta-backend}")
    private String serviceName;

    @Value("${otel.exporter.otlp.headers:}")
    private String otelHeaders;

    @Value("${otel.traces.exporter.timeout:30000}")
    private long exporterTimeoutMs;

    /**
     * Configure OTLP Span Exporter with gRPC
     */
    @Bean
    public SpanExporter spanExporter() {
        logger.info("Configuring OTLP Span Exporter with endpoint: {}", otelEndpoint);

        OtlpGrpcSpanExporterBuilder builder = OtlpGrpcSpanExporter.builder()
                .setEndpoint(otelEndpoint)
                .setTimeout(exporterTimeoutMs, TimeUnit.MILLISECONDS)
                .setCompression("gzip");

        // Add headers if provided (useful for authentication)
        if (otelHeaders != null && !otelHeaders.isEmpty()) {
            // Parse headers in format: "key1=value1,key2=value2"
            String[] headerPairs = otelHeaders.split(",");
            for (String pair : headerPairs) {
                String[] keyValue = pair.split("=", 2);
                if (keyValue.length == 2) {
                    builder.addHeader(keyValue[0].trim(), keyValue[1].trim());
                }
            }
        }

        OtlpGrpcSpanExporter exporter = builder.build();

        logger.info("OTLP Span Exporter configured successfully for service: {}", serviceName);
        return exporter;
    }

    /**
     * Configure Resource with service attributes
     */
    @Bean
    public Resource resource() {
        return Resource.getDefault()
                .toBuilder()
                .put("service.name", serviceName)
                .put("service.version", getClass().getPackage().getImplementationVersion() != null ?
                        getClass().getPackage().getImplementationVersion() : "0.0.1-SNAPSHOT")
                .put("telemetry.sdk.language", "java")
                .put("telemetry.sdk.name", "opentelemetry")
                .build();
    }

    /**
     * Configure SdkTracerProvider with batch span processor
     */
    @Bean
    public SdkTracerProvider sdkTracerProvider(Resource resource, SpanExporter spanExporter) {
        return SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(spanExporter)
                        .setScheduleDelay(1000, TimeUnit.MILLISECONDS)
                        .setMaxQueueSize(2048)
                        .setMaxExportBatchSize(512)
                        .build())
                .setResource(resource)
                .build();
    }

    /**
     * Configure OpenTelemetry instance
     */
    @Bean
    public OpenTelemetry openTelemetry(SdkTracerProvider sdkTracerProvider) {
        return OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                .setPropagators(ContextPropagators.create(
                        TextMapPropagator.composite(
                                JaegerPropagator.getInstance(),
                                W3CTraceContextPropagator.getInstance()
                        ))
                )
                .build();
    }

    /**
     * Configure Tracer for manual instrumentation
     * Use this in services and controllers for custom span creation
     */
    @Bean
    public Tracer tracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracerProvider()
                .tracerBuilder(serviceName)
                .setInstrumentationVersion("1.0.0")
                .build();
    }
}
