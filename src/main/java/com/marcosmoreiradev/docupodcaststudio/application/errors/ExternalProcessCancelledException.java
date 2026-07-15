package com.marcosmoreiradev.docupodcaststudio.application.errors;

/** External process was cancelled by the user or by a safe shutdown policy. */
public final class ExternalProcessCancelledException extends ExternalProcessFailedException {
    public ExternalProcessCancelledException(String headline, String message, String commandAuditId, String lastOutput, String logPath) {
        super(headline, message, -2, commandAuditId, lastOutput, logPath, null);
    }
}
