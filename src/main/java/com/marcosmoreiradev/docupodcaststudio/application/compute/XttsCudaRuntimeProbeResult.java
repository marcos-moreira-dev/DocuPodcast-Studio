package com.marcosmoreiradev.docupodcaststudio.application.compute;

/** Raw result produced by the local Python CUDA probe for Voz IA avanzada. */
public record XttsCudaRuntimeProbeResult(
        int exitCode,
        String stdout,
        String stderr,
        boolean timedOut
) {
    public XttsCudaRuntimeProbeResult {
        stdout = stdout == null ? "" : stdout.strip();
        stderr = stderr == null ? "" : stderr.strip();
    }

    public boolean completed() {
        return !timedOut;
    }
}
