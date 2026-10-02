package com.marcosmoreiradev.docupodcaststudio.media.api;

/** Stable diagnostic codes shared by managed local engines and their UI. */
public enum EngineDiagnosticCode {
    MISSING_RESOURCE,
    INVALID_RESOURCE,
    PORT_CONFLICT,
    START_TIMEOUT,
    READINESS_TIMEOUT,
    RUNTIME_UNRESPONSIVE,
    REQUEST_TIMEOUT,
    REQUEST_STALL,
    TRANSPORT_TRANSIENT,
    OOM,
    DEVICE_MISMATCH,
    NO_SPACE,
    CANCELLED,
    CHILD_EXIT,
    OUTPUT_TRUNCATED,
    REQUEST_REJECTED,
    PROTOCOL_INVALID,
    INVALID_OUTPUT
}
