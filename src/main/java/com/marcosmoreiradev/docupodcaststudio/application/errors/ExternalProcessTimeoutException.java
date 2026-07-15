package com.marcosmoreiradev.docupodcaststudio.application.errors;

/** External process exceeded its configured timeout. */
public final class ExternalProcessTimeoutException extends ExternalProcessFailedException {
    public ExternalProcessTimeoutException(String headline, String message, String commandAuditId, String lastOutput, String logPath) {
        super(headline, message, -1, commandAuditId, lastOutput, logPath, null);
    }
}
