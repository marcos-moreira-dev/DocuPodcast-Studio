package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** EXTERNAL-PROCESS-RUNNER-RF1 protects the common process contract for Python/FFmpeg/Piper/PowerShell. */
final class ExternalProcessRunnerRf1SourceTest {
    @Test
    void commonProcessContractExists() throws Exception {
        String request = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/process/ExternalProcessRequest.java");
        String result = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/process/ExternalProcessResult.java");
        String runner = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/process/ExternalProcessRunner.java");
        String observer = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/process/ExternalProcessObserver.java");
        String implementation = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/process/DefaultExternalProcessRunner.java");
        assertTrue(request.contains("auditLabel"));
        assertTrue(request.contains("timeout"));
        assertTrue(request.contains("environment"));
        assertTrue(request.contains("withWorkingDirectory"));
        assertTrue(request.contains("redirectingErrorStream"));
        assertTrue(result.contains("exitCode"));
        assertTrue(result.contains("timedOut"));
        assertTrue(result.contains("stdout"));
        assertTrue(result.contains("stderr"));
        assertTrue(runner.contains("ExternalProcessResult run"));
        assertTrue(runner.contains("ExternalProcessObserver"));
        assertTrue(observer.contains("onOutputLine"));
        assertTrue(observer.contains("cancellationRequested"));
        assertTrue(implementation.contains("ProcessBuilder"));
        assertTrue(implementation.contains("destroyTree"));
    }

    @Test
    void cudaSmokeCanUseInjectedExternalProcessRunner() throws Exception {
        String probe = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/compute/ProcessXttsCudaRuntimeProbeGateway.java");
        String factory = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/bootstrap/ApplicationServicesFactory.java");
        assertTrue(probe.contains("ExternalProcessRunner"));
        assertTrue(probe.contains("ExternalProcessRequest"));
        assertTrue(probe.contains("xtts-cuda-smoke"));
        assertTrue(factory.contains("new DefaultExternalProcessRunner()"));
        assertTrue(factory.contains("new ProcessXttsCudaRuntimeProbeGateway"));
    }

    @Test
    void processBuilderIsOnlyOwnedByDefaultExternalProcessRunner() throws Exception {
        Path main = Path.of("src/main/java");
        try (Stream<Path> files = Files.walk(main)) {
            List<Path> offenders = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.endsWith(Path.of("DefaultExternalProcessRunner.java")))
                    .filter(ExternalProcessRunnerRf1SourceTest::containsDirectProcessBuilder)
                    .toList();
            assertTrue(offenders.isEmpty(), "ProcessBuilder directo fuera del runner comun: " + offenders);
        }
    }

    private static boolean containsDirectProcessBuilder(Path path) {
        try {
            return Files.readString(path).contains("new ProcessBuilder");
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
