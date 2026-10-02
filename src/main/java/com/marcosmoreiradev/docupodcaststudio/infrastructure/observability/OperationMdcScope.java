package com.marcosmoreiradev.docupodcaststudio.infrastructure.observability;

import com.marcosmoreiradev.docupodcaststudio.application.observability.OperationContext;
import org.slf4j.MDC;

import java.util.LinkedHashMap;
import java.util.Map;

/** Restores the previous MDC values when an operation boundary closes. */
public final class OperationMdcScope implements AutoCloseable {
    private final Map<String, String> previous = new LinkedHashMap<>();

    private OperationMdcScope(OperationContext context) {
        context.values().forEach((key, value) -> {
            previous.put(key, MDC.get(key));
            MDC.put(key, value);
        });
    }

    public static OperationMdcScope open(OperationContext context) {
        return new OperationMdcScope(context);
    }

    @Override public void close() {
        previous.forEach((key, value) -> {
            if (value == null) MDC.remove(key); else MDC.put(key, value);
        });
    }
}
