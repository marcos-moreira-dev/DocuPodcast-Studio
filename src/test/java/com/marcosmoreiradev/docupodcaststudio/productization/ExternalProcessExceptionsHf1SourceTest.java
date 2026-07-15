package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** EXTERNAL-PROCESS-EXCEPTIONS-HF1 keeps process failures user-facing and diagnosable. */
final class ExternalProcessExceptionsHf1SourceTest {
    @Test
    void processExceptionsCarryExitCodeCommandOutputAndLog() throws Exception {
        String failed = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/errors/ExternalProcessFailedException.java");
        String timeout = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/errors/ExternalProcessTimeoutException.java");
        String cancelled = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/errors/ExternalProcessCancelledException.java");
        assertTrue(failed.contains("exitCode"));
        assertTrue(failed.contains("commandAuditId"));
        assertTrue(failed.contains("lastOutput"));
        assertTrue(failed.contains("logPath"));
        assertTrue(failed.contains("Código de salida"));
        assertTrue(timeout.contains("ExternalProcessFailedException"));
        assertTrue(cancelled.contains("ExternalProcessFailedException"));
    }

    @Test
    void processResultExposesOutputTailForDialogsAndDiagnostics() throws Exception {
        String result = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/process/ExternalProcessResult.java");
        assertTrue(result.contains("combinedOutputTail"));
        assertTrue(result.contains("4000"));
        assertTrue(result.contains("succeeded()"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
