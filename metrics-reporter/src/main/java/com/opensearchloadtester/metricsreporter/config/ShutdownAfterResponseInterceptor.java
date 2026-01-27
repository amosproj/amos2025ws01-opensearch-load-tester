package com.opensearchloadtester.metricsreporter.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Intercepts HTTP requests to trigger a controlled shutdown of the Metrics Reporter
 * after the response has been fully sent to the client.
 *
 * <p>
 * The shutdown is initiated only when explicitly requested via a request attribute
 * and supports custom exit codes to distinguish between successful runs,
 * load generator failures, and internal reporter errors.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShutdownAfterResponseInterceptor implements HandlerInterceptor {

    public static final String SHUTDOWN_AFTER_RESPONSE = "metricsReporter.shutdownAfterResponse";
    public static final String EXIT_CODE = "metricsReporter.exitCode";

    // Exit Codes
    public static final int EXIT_OK = 0;
    public static final int EXIT_LOAD_GENERATOR_FAILED = 2;

    private final ConfigurableApplicationContext context;
    private final AtomicBoolean shutdownInitiated = new AtomicBoolean(false);

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {

        boolean shouldShutdown = Boolean.TRUE.equals(request.getAttribute(SHUTDOWN_AFTER_RESPONSE));

        if (!shouldShutdown) {
            return;
        }

        if (!shutdownInitiated.compareAndSet(false, true)) {
            return; // shutdown already initiated
        }

        Integer exitCodeAttr = (Integer) request.getAttribute(EXIT_CODE);
        int exitCode = exitCodeAttr != null ? exitCodeAttr : EXIT_OK;

        // Run shutdown outside request thread
        new Thread(() -> {
            log.info("Shutting down Metrics Reporter with exit code '{}'", exitCode);
            SpringApplication.exit(context, () -> exitCode);
            System.exit(exitCode);
        }, "metrics-reporter-shutdown").start();
    }
}
