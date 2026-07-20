/*
 *
 *  * Copyright 2026 SawCunha Open System - SawCunhaOS-Organization
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *
 */

package br.com.sawcunhaos.organization.grpc.boot.configuration.interceptor;

import br.com.sawcunhaos.foundation.privacy.SanitizationBodyComponent;
import br.com.sawcunhaos.foundation.privacy.SanitizationHeadersComponent;
import br.com.sawcunhaos.foundation.utils.configuration.rest.filter.properties.ScosFilterProperties;
import br.com.sawcunhaos.foundation.utils.utils.DateUtils;
import io.grpc.ForwardingServerCall;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static br.com.sawcunhaos.foundation.utils.configuration.rest.filter.LoggingInitialFilter.REQUEST_ID_HEADER;

/**
 * Global gRPC interceptor ({@code @Order(0)}) — gRPC equivalent of
 * {@link br.com.sawcunhaos.foundation.utils.configuration.rest.filter.LoggingInitialFilter}
 * and {@link br.com.sawcunhaos.foundation.utils.configuration.rest.filter.LoggingFinalFilter}.
 *
 * <p>Establishes per-call MDC context and logs both the inbound request and
 * the outbound response, following the same structure as the HTTP filters.</p>
 *
 * <p>gRPC uses async I/O — the request body arrives in {@link ServerCall.Listener#onMessage}
 * and the response is captured in {@link ServerCall#sendMessage} / {@link ServerCall#close},
 * so logging is split accordingly. MDC is captured at intercept time and restored
 * in every callback to ensure context is never lost across virtual-thread hops.</p>
 *
 * <p>As the outermost interceptor, it owns the single {@code MDC.clear()} in
 * the {@code close} and {@code onCancel} {@code finally} blocks.</p>
 *
 * <p>PII masking is delegated to the {@code scos-foundation-privacy} sanitization
 * components, identical to the HTTP filter chain.</p>
 */
@Slf4j
@Component
@GlobalServerInterceptor
@Order(0)
@RequiredArgsConstructor
public class GrpcLoggingInterceptor implements ServerInterceptor {

    // gRPC Metadata keys (lowercase — HTTP/2 headers are case-insensitive)
    private static final Metadata.Key<String> REQUEST_ID_KEY =
            Metadata.Key.of(REQUEST_ID_HEADER, Metadata.ASCII_STRING_MARSHALLER);

    private static final Metadata.Key<String> X_FORWARDED_FOR_KEY =
            Metadata.Key.of("x-forwarded-for", Metadata.ASCII_STRING_MARSHALLER);

    private static final Metadata.Key<String> X_REAL_IP_KEY =
            Metadata.Key.of("IS_IP", Metadata.ASCII_STRING_MARSHALLER);

    private final ScosFilterProperties scosFilterProperties;
    private final SanitizationHeadersComponent sanitizationHeadersComponent;
    private final SanitizationBodyComponent sanitizationBodyComponent;

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        final String requestId  = resolveRequestId(headers);
        final String clientIp   = extractClientIp(headers);
        final String methodName = call.getMethodDescriptor().getFullMethodName();
        final boolean shouldLog = shouldLog(methodName);

        // ── Establish MDC context (mirrors LoggingInitialFilter) ──────────────
        MDC.put(REQUEST_ID_HEADER, requestId);
        MDC.put("IS_IP", clientIp);

        // Capture snapshot: gRPC callbacks may run on different virtual threads
        final Map<String, String> mdcContext = MDC.getCopyOfContextMap();

        // Holds the response body until close() aggregates the final log entry
        final AtomicReference<String> responseBodyRef = new AtomicReference<>("No response body");

        // ── Wrap the ServerCall to intercept outbound messages ────────────────
        final ServerCall<ReqT, RespT> wrappedCall =
                new ForwardingServerCall.SimpleForwardingServerCall<>(call) {

                    /**
                     * Captures and sanitizes the response body.
                     * Actual logging is deferred to {@link #close} so a single log entry
                     * contains status code + body, mirroring LoggingFinalFilter.
                     */
                    @Override
                    public void sendMessage(RespT message) {
                        restoreMdc(mdcContext);
                        try {
                            if (shouldLog) {
                                responseBodyRef.set(sanitizeResponseBody(message));
                            }
                        } catch (Exception logEx) {
                            log.error("GrpcLoggingInterceptor: erro ao capturar resposta", logEx);
                        } finally {
                            super.sendMessage(message);  // ← DEVE sempre ser chamado
                        }
                    }

                    /**
                     * Logs the complete response — mirrors {@code LoggingFinalFilter.doFilterInternal}.
                     * Owns {@code MDC.clear()} in its {@code finally} block.
                     */
                    @Override
                    public void close(Status status, Metadata trailers) {
                        restoreMdc(mdcContext);
                        if (shouldLog) {
                            try {
                                MDC.put("Response-Time",         DateUtils.returnDateCurrent());
                                MDC.put("Response-Code",         status.getCode().name());
                                MDC.put("Response-Content-Type", "application/grpc");
                                MDC.put("Response-Body",         responseBodyRef.get());
                                log.info("Final gRPC Call");
                            } catch (Exception logEx) {
                                // Logging nunca pode impedir o close do stream
                                log.error("GrpcLoggingInterceptor: erro ao logar resposta", logEx);
                            } finally {
                                MDC.remove("Response-Time");
                                MDC.remove("Response-Code");
                                MDC.remove("Response-Content-Type");
                                MDC.remove("Response-Body");
                            }
                        }
                        try {
                            super.close(status, trailers);  // ← DEVE sempre ser chamado
                        } finally {
                            MDC.clear();
                        }
                    }
                };

        final ServerCall.Listener<ReqT> listener = next.startCall(wrappedCall, headers);

        // ── Log initial call — mirrors LoggingInitialFilter ───────────────────
        if (shouldLog) {
            MDC.put("Request-Time",         DateUtils.returnDateCurrent());
            MDC.put("Request-Method",       methodName);
            MDC.put("Request-URI",          "/" + methodName);
            MDC.put("Request-Content-Type", "application/grpc");
            MDC.put("Headers",              formatMetadata(headers));
            log.info("Initial gRPC Call");
            MDC.remove("Request-Time");
            MDC.remove("Request-Method");
            MDC.remove("Request-URI");
            MDC.remove("Request-Content-Type");
            MDC.remove("Headers");
        }

        // ── Wrap the Listener to intercept inbound messages ───────────────────
        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(listener) {

            /**
             * Logs the request body when it arrives.
             * Split from the initial log because gRPC delivers the body asynchronously,
             * unlike HTTP filters that wrap the stream upfront.
             */
            @Override
            public void onMessage(ReqT message) {
                restoreMdc(mdcContext);
                try {
                    if (shouldLog) {
                        MDC.put("Request-Body", sanitizeRequestBody(message));
                        log.info("Initial gRPC Call - Body");
                        MDC.remove("Request-Body");
                    }
                } catch (Exception logEx) {
                    log.error("GrpcLoggingInterceptor: erro ao logar request body", logEx);
                } finally {
                    super.onMessage(message);  // ← DEVE sempre ser chamado
                }
            }

            @Override
            public void onHalfClose() {
                restoreMdc(mdcContext);
                super.onHalfClose();
            }

            /** Owns {@code MDC.clear()} when the call is cancelled by the client. */
            @Override
            public void onCancel() {
                restoreMdc(mdcContext);
                if (shouldLog) {
                    log.warn("gRPC Call Cancelled: {}", methodName);
                }
                try {
                    super.onCancel();
                } finally {
                    MDC.clear();
                }
            }

            @Override
            public void onComplete() {
                restoreMdc(mdcContext);
                super.onComplete();
            }
        };
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * gRPC method name format: {@code package.ServiceName/MethodName}.
     * Reuses the same URI property as the HTTP filters for consistency.
     */
    private boolean shouldLog(String methodName) {
        String uri = scosFilterProperties.getURI();
        return uri != null && !uri.isBlank() && methodName.contains(uri);
    }

    /**
     * Reuses {@code X-Request-ID} from Metadata when the caller supplies one,
     * otherwise generates a fresh UUID — identical logic to LoggingInitialFilter.
     */
    private String resolveRequestId(Metadata headers) {
        String requestId = headers.get(REQUEST_ID_KEY);
        return (requestId != null && !requestId.isEmpty())
                ? requestId
                : UUID.randomUUID().toString();
    }

    /**
     * Extracts client IP from common forwarding headers in the gRPC Metadata.
     * gRPC has no direct equivalent of {@code HttpServletRequest.getRemoteAddr()}.
     */
    private String extractClientIp(Metadata headers) {
        String xForwardedFor = headers.get(X_FORWARDED_FOR_KEY);
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = headers.get(X_REAL_IP_KEY);
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return "unknown";
    }

    /**
     * Converts gRPC {@link Metadata} to the same formatted string used by the HTTP filter.
     * Binary headers ({@code -bin} suffix) are skipped — they are not human-readable.
     */
    private String formatMetadata(Metadata headers) {
        if (!scosFilterProperties.isShowRequestHeaders()) {
            return "Headers view not enabled";
        }

        final Map<String, String> raw = new LinkedHashMap<>();
        for (String key : headers.keys()) {
            if (key.endsWith(Metadata.BINARY_HEADER_SUFFIX)) {
                continue;
            }
            try {
                Metadata.Key<String> metaKey = Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER);
                String value = headers.get(metaKey);
                if (value != null) {
                    raw.put(key, value);
                }
            } catch (Exception ignored) {
                // Skip any key that cannot be decoded as ASCII
            }
        }

        final Map<String, String> sanitized = sanitizationHeadersComponent.sanitize(raw);
        final StringBuilder formatted = new StringBuilder();
        sanitized.forEach((name, value) ->
                formatted.append("Header Name -> ").append(name)
                        .append(" -- ").append(value).append('\n'));
        return formatted.toString();
    }

    private String sanitizeRequestBody(Object message) {
        if (!scosFilterProperties.isShowRequestBody()) {
            return "Body view not enabled";
        }
        return sanitizationBodyComponent.sanitize(message.toString());
    }

    private String sanitizeResponseBody(Object message) {
        if (!scosFilterProperties.isShowResponseBody()) {
            return "Body view not enabled";
        }
        return sanitizationBodyComponent.sanitize(message.toString());
    }

    /**
     * Restores the MDC snapshot captured at intercept time.
     * Necessary because gRPC callbacks can hop between virtual threads.
     */
    private void restoreMdc(Map<String, String> context) {
        MDC.clear();
        if (context != null) {
            MDC.setContextMap(context);
        }
    }
}
