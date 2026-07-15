package com.marcosmoreiradev.docupodcaststudio.infrastructure.process;

import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRequest;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessResult;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessObserver;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRunner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/** JDK ProcessBuilder adapter behind the common external-process contract. */
public final class DefaultExternalProcessRunner implements ExternalProcessRunner {
    private static final int MAX_CAPTURE_CHARS = 12_000;

    @Override
    public ExternalProcessResult run(ExternalProcessRequest request) throws IOException, InterruptedException {
        return run(request, ExternalProcessObserver.noop());
    }

    @Override
    public ExternalProcessResult run(ExternalProcessRequest request, ExternalProcessObserver observer)
            throws IOException, InterruptedException {
        Objects.requireNonNull(request, "request");
        ExternalProcessObserver safeObserver = observer == null ? ExternalProcessObserver.noop() : observer;
        Instant started = Instant.now();
        ProcessBuilder builder = new ProcessBuilder(request.command());
        if (request.workingDirectory() != null) {
            builder.directory(request.workingDirectory().toFile());
        }
        builder.environment().putAll(request.environment());
        builder.redirectErrorStream(request.redirectErrorStream());
        Process process = builder.start();
        OutputCapture stdout = new OutputCapture();
        OutputCapture stderr = new OutputCapture();
        Thread stdoutReader = startReader("external-process-stdout", process.getInputStream(), stdout, safeObserver);
        Thread stderrReader = request.redirectErrorStream()
                ? null
                : startReader("external-process-stderr", process.getErrorStream(), stderr, safeObserver);
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(request.timeout().toMillis());
        while (process.isAlive()) {
            if (safeObserver.cancellationRequested()) {
                destroyTree(process);
                join(stdoutReader, stderrReader);
                return new ExternalProcessResult(-2, false, true, stdout.text(), stderr.text(),
                        request.commandAudit(), Duration.between(started, Instant.now()));
            }
            long remainingMillis = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime());
            if (remainingMillis <= 0L) {
                destroyTree(process);
                join(stdoutReader, stderrReader);
                return new ExternalProcessResult(-1, true, false, stdout.text(), stderr.text(),
                        request.commandAudit(), Duration.between(started, Instant.now()));
            }
            process.waitFor(Math.min(250L, Math.max(1L, remainingMillis)), TimeUnit.MILLISECONDS);
        }
        join(stdoutReader, stderrReader);
        return new ExternalProcessResult(process.exitValue(), false, false, stdout.text(), stderr.text(),
                request.commandAudit(), Duration.between(started, Instant.now()));
    }

    private static Thread startReader(String name, InputStream stream, OutputCapture output, ExternalProcessObserver observer) {
        Thread thread = new Thread(() -> readLines(stream, output, observer), name);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private static void readLines(InputStream stream, OutputCapture output, ExternalProcessObserver observer) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
                observer.onOutputLine(line);
            }
        } catch (IOException ex) {
            output.append("No se pudo leer salida del proceso local: " + ex.getMessage());
        }
    }

    private static void destroyTree(Process process) {
        try {
            process.toHandle().descendants().forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
            process.waitFor(2, TimeUnit.SECONDS);
        } catch (RuntimeException ignored) {
            process.destroyForcibly();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
    }

    private static void join(Thread... threads) throws InterruptedException {
        if (threads == null) {
            return;
        }
        for (Thread thread : threads) {
            if (thread != null) {
                thread.join(1000L);
            }
        }
    }

    private static final class OutputCapture {
        private final StringBuilder text = new StringBuilder();

        synchronized void append(String line) {
            if (!text.isEmpty()) {
                text.append(System.lineSeparator());
            }
            text.append(line == null ? "" : line);
            if (text.length() > MAX_CAPTURE_CHARS) {
                text.delete(0, text.length() - MAX_CAPTURE_CHARS);
            }
        }

        synchronized String text() {
            return text.toString();
        }
    }
}
