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
 * Shuts down the Metrics Reporter after the current HTTP request has fully completed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShutdownAfterResponseInterceptor implements HandlerInterceptor {

    public static final String SHUTDOWN_AFTER_RESPONSE = "metricsReporter.shutdownAfterResponse";

    private final ConfigurableApplicationContext context;
    private final AtomicBoolean shutdownInitiated = new AtomicBoolean(false);

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {

        Object flag = request.getAttribute(SHUTDOWN_AFTER_RESPONSE);
        boolean shouldShutdown = Boolean.TRUE.equals(flag);

        if (!shouldShutdown) {
            return;
        }

        if (!shutdownInitiated.compareAndSet(false, true)) {
            return; // already shutting down
        }

        // Run shutdown outside request thread
        new Thread(() -> {
            log.info("Shutting down Metrics Reporter");
            int exitCode = SpringApplication.exit(context, () -> 0);
            System.exit(exitCode);
        }, "metrics-reporter-shutdown").start();
    }
}
