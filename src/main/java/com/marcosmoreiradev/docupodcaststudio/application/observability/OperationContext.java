package com.marcosmoreiradev.docupodcaststudio.application.observability;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Correlation data propagated across UI, jobs and adapter boundaries. */
public record OperationContext(
        String operationId,
        String jobId,
        String capabilityId,
        String engineId,
        String projectId,
        String workspace,
        String actionId) {

    public OperationContext {
        operationId = clean(operationId);
        jobId = clean(jobId);
        capabilityId = clean(capabilityId);
        engineId = clean(engineId);
        projectId = clean(projectId);
        workspace = clean(workspace);
        actionId = clean(actionId);
        if (operationId.isBlank()) throw new IllegalArgumentException("operationId is required");
    }

    public static OperationContext operation(String operationId) {
        return new OperationContext(operationId, "", "", "", "", "", "");
    }

    public Map<String, String> values() {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        put(values, "operationId", operationId);
        put(values, "jobId", jobId);
        put(values, "capabilityId", capabilityId);
        put(values, "engineId", engineId);
        put(values, "projectId", projectId);
        put(values, "workspace", workspace);
        put(values, "actionId", actionId);
        return Map.copyOf(values);
    }

    private static void put(Map<String, String> values, String key, String value) {
        if (!value.isBlank()) values.put(key, value);
    }

    private static String clean(String value) {
        String clean = Objects.toString(value, "").strip();
        return clean.length() <= 160 ? clean : clean.substring(0, 160);
    }
}
