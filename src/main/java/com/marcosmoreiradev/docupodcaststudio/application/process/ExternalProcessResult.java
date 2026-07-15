package com.marcosmoreiradev.docupodcaststudio.application.process;

import java.time.Duration;

/** Result of running a local external process. */
public record ExternalProcessResult(
        int exitCode,
        boolean timedOut,
        boolean cancelled,
        String stdout,
        String stderr,
        String commandAudit,
        Duration duration
) {
    public ExternalProcessResult {
        stdout = stdout == null ? "" : stdout.strip();
        stderr = stderr == null ? "" : stderr.strip();
        commandAudit = commandAudit == null ? "" : commandAudit.strip();
        duration = duration == null ? Duration.ZERO : duration;
    }

    public boolean succeeded() {
        return !timedOut && !cancelled && exitCode == 0;
    }

    public String combinedOutputTail() {
        String combined = (stdout + System.lineSeparator() + stderr).strip();
        if (combined.length() <= 4000) {
            return combined;
        }
        return combined.substring(combined.length() - 4000);
    }
}
