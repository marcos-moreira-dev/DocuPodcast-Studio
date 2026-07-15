package com.marcosmoreiradev.docupodcaststudio.infrastructure.process;

import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessObserver;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRequest;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DefaultExternalProcessRunnerTest {
    @TempDir
    Path tempDir;

    @Test
    void runsWithEnvironmentWorkingDirectoryAndIncrementalObserver() throws Exception {
        CopyOnWriteArrayList<String> lines = new CopyOnWriteArrayList<>();
        ExternalProcessRequest request = shell(echoEnvAndWorkingDirectory(), Duration.ofSeconds(5))
                .withEnvironment("DOCUPODCAST_TEST_ENV", "runner-ok")
                .withWorkingDirectory(tempDir)
                .redirectingErrorStream();

        ExternalProcessResult result = new DefaultExternalProcessRunner().run(request, new ExternalProcessObserver() {
            @Override
            public void onOutputLine(String line) {
                lines.add(line);
            }
        });

        assertTrue(result.succeeded(), result.combinedOutputTail());
        assertTrue(result.stdout().contains("runner-ok"), result.stdout());
        assertTrue(result.stdout().toLowerCase(Locale.ROOT)
                .contains(tempDir.toAbsolutePath().normalize().toString().toLowerCase(Locale.ROOT)), result.stdout());
        assertTrue(lines.stream().anyMatch(line -> line.contains("runner-ok")), lines.toString());
    }

    @Test
    void keepsStdoutAndStderrSeparatedWhenNotRedirected() throws Exception {
        ExternalProcessResult result = new DefaultExternalProcessRunner()
                .run(shell(stdoutAndStderr(), Duration.ofSeconds(5)));

        assertTrue(result.succeeded(), result.combinedOutputTail());
        assertTrue(result.stdout().contains("out"), result.stdout());
        assertTrue(result.stderr().contains("err"), result.stderr());
    }

    @Test
    void timesOutAndCapturesDiagnosticTail() throws Exception {
        ExternalProcessResult result = new DefaultExternalProcessRunner()
                .run(shell(longRunningCommand(), Duration.ofMillis(150)).redirectingErrorStream());

        assertTrue(result.timedOut(), result.combinedOutputTail());
        assertFalse(result.cancelled());
    }

    @Test
    void cancelsWhenObserverRequestsCancellation() throws Exception {
        ExternalProcessResult result = new DefaultExternalProcessRunner()
                .run(shell(longRunningCommand(), Duration.ofSeconds(10)), new ExternalProcessObserver() {
                    @Override
                    public boolean cancellationRequested() {
                        return true;
                    }
                });

        assertTrue(result.cancelled(), result.combinedOutputTail());
        assertFalse(result.timedOut());
    }

    private static ExternalProcessRequest shell(String script, Duration timeout) {
        return ExternalProcessRequest.of(command(script), "runner-test", timeout);
    }

    private static List<String> command(String script) {
        if (isWindows()) {
            return List.of("cmd.exe", "/c", script);
        }
        return List.of("sh", "-c", script);
    }

    private static String echoEnvAndWorkingDirectory() {
        return isWindows() ? "echo %DOCUPODCAST_TEST_ENV% && cd" : "printf '%s\\n' \"$DOCUPODCAST_TEST_ENV\" && pwd";
    }

    private static String stdoutAndStderr() {
        return isWindows() ? "echo out && echo err 1>&2" : "echo out; echo err >&2";
    }

    private static String longRunningCommand() {
        return isWindows() ? "ping -n 6 127.0.0.1 > nul" : "sleep 5";
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }
}
