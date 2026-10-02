package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

final class LocalProcessExecutor {
    private static final int MAX_CAPTURE_BYTES = 64 * 1024;

    Result run(List<String> command, Path workingDirectory, ExecutionContext context)
            throws IOException, InterruptedException {
        return run(command, workingDirectory, context, null);
    }

    Result run(List<String> command, Path workingDirectory, ExecutionContext context,
               Consumer<String> outputLineConsumer)
            throws IOException, InterruptedException {
        if (command == null || command.isEmpty()) throw new IOException("external command is empty");
        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
        if (workingDirectory != null) builder.directory(workingDirectory.toFile());
        Process process = builder.start();
        ManagedProcessContainment containment = ManagedProcessContainment.create();
        containment.attach(process);
        String kind = OwnedProcessDiagnostics.kind(command);
        Instant started = OwnedProcessDiagnostics.started(kind, process, command,
                "LocalProcessExecutor", "one-shot operation completion", containment.diagnostics());
        TailCapture capture = new TailCapture(MAX_CAPTURE_BYTES);
        Thread outputReader = Thread.ofVirtual().name("local-process-output").start(
                () -> drain(process.getInputStream(), capture, outputLineConsumer));
        Duration timeout = context.policy().timeout();
        boolean hasTimeout = context.policy().hasTimeout();
        long deadline = hasTimeout ? System.nanoTime() + timeout.toNanos()
                : Long.MAX_VALUE;
        String reason = "NORMAL_COMPLETION";
        boolean forced = false;
        try {
            while (process.isAlive()) {
                if (context.cancellation().cancellationRequested()) {
                    reason = "CANCELLED";
                    process.destroy();
                    if (!process.waitFor(2, TimeUnit.SECONDS)) {
                        forced = true;
                        process.destroyForcibly();
                    }
                    outputReader.join();
                    throw new InterruptedException("media operation cancelled");
                }
                if (hasTimeout && System.nanoTime() >= deadline) {
                    reason = "TIMEOUT";
                    forced = true;
                    process.destroyForcibly();
                    process.waitFor();
                    outputReader.join();
                    throw new IOException("external command timed out after " + timeout.toSeconds() + " seconds");
                }
                process.waitFor(100, TimeUnit.MILLISECONDS);
            }
            outputReader.join();
            return new Result(process.exitValue(), capture.text());
        } finally {
            // Closing a Windows Job Object also removes an accidentally surviving child
            // of a completed one-shot script. It never targets an unowned system process.
            containment.close();
            Integer exit = process.isAlive() ? null : process.exitValue();
            OwnedProcessDiagnostics.stopped(kind, process, "LocalProcessExecutor",
                    started, reason, exit, forced);
        }
    }

    private static void drain(InputStream input, TailCapture capture,
                              Consumer<String> outputLineConsumer) {
        try (input; BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                byte[] encoded = (line + System.lineSeparator())
                        .getBytes(StandardCharsets.UTF_8);
                capture.append(encoded, encoded.length);
                if (outputLineConsumer != null) {
                    try {
                        outputLineConsumer.accept(line);
                    } catch (RuntimeException ignored) {
                        // Diagnostic observers must never abort or stall the owned process.
                    }
                }
            }
        } catch (IOException ignored) {
            // The stream is expected to close when cancellation or timeout destroys the process.
        }
    }

    private static final class TailCapture {
        private final byte[] bytes;
        private int length;

        private TailCapture(int capacity) { this.bytes = new byte[capacity]; }

        private synchronized void append(byte[] source, int count) {
            if (count <= 0) return;
            if (count >= bytes.length) {
                System.arraycopy(source, count - bytes.length, bytes, 0, bytes.length);
                length = bytes.length;
                return;
            }
            int overflow = Math.max(0, length + count - bytes.length);
            if (overflow > 0) {
                System.arraycopy(bytes, overflow, bytes, 0, length - overflow);
                length -= overflow;
            }
            System.arraycopy(source, 0, bytes, length, count);
            length += count;
        }

        private synchronized String text() {
            return new String(bytes, 0, length, StandardCharsets.UTF_8);
        }
    }

    record Result(int exitCode, String output) { }
}
