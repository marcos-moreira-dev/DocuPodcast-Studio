package com.marcosmoreiradev.docupodcaststudio.application.errors;

/** Failure returned by a local executable such as Python, FFmpeg, Piper or PowerShell. */
public class ExternalProcessFailedException extends InfrastructureOperationException {
    private final int exitCode;
    private final String commandAuditId;
    private final String lastOutput;
    private final String logPath;

    public ExternalProcessFailedException(String headline,
                                          String message,
                                          int exitCode,
                                          String commandAuditId,
                                          String lastOutput,
                                          String logPath,
                                          Throwable cause) {
        super(headline, message, technicalDetail(exitCode, commandAuditId, lastOutput, logPath), cause);
        this.exitCode = exitCode;
        this.commandAuditId = commandAuditId == null ? "" : commandAuditId.strip();
        this.lastOutput = lastOutput == null ? "" : lastOutput.strip();
        this.logPath = logPath == null ? "" : logPath.strip();
    }

    public int exitCode() {
        return exitCode;
    }

    public String commandAuditId() {
        return commandAuditId;
    }

    public String lastOutput() {
        return lastOutput;
    }

    public String logPath() {
        return logPath;
    }

    private static String technicalDetail(int exitCode, String commandAuditId, String lastOutput, String logPath) {
        return "Código de salida: " + exitCode
                + System.lineSeparator() + "Comando auditado: " + safe(commandAuditId)
                + System.lineSeparator() + "Última salida: " + safe(lastOutput)
                + System.lineSeparator() + "Log: " + safe(logPath);
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "no disponible" : value.strip();
    }
}
