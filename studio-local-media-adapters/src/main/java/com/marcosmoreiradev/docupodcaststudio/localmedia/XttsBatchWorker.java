package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.CancellationToken;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineDiagnosticCode;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineExecutionException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** Process-scoped XTTS model residency with one protocol command per chunk. */
final class XttsBatchWorker implements AutoCloseable {
    private static final String PREFIX = "DOCUPODCAST_XTTS_WORKER:";
    private final Process process;
    private final ManagedProcessContainment containment;
    private final Instant processStartedAt;
    private final BufferedWriter commands;
    private final LinkedBlockingQueue<String> events = new LinkedBlockingQueue<>();
    private final Path log;
    private final String effectiveDevice;
    private volatile boolean closed;

    XttsBatchWorker(Path python, Path wrapper, Path modelDirectory,
                    String device, Path workingDirectory,
                    CancellationToken cancellation, Duration timeout)
            throws IOException, InterruptedException {
        log = workingDirectory.resolve("xtts-worker.log");
        ProcessBuilder processBuilder = new ProcessBuilder(
                python.toString(), wrapper.toString(),
                "--model-dir", modelDirectory.toString(),
                "--device", device == null ? "" : device)
                .directory(workingDirectory.toFile())
                .redirectErrorStream(true);
        // Windows inherits a legacy console code page even though the worker
        // protocol and its input files are UTF-8. XTTS/TTS may print the
        // sentence being synthesized, so a mathematical or OCR Unicode
        // character used to abort an otherwise valid CUDA inference.
        configureUtf8Environment(processBuilder);
        process = processBuilder.start();
        containment = ManagedProcessContainment.create();
        containment.attach(process);
        processStartedAt = OwnedProcessDiagnostics.started("XTTS", process,
                processBuilder.command(), "XttsBatchWorker",
                "batch completion, cancellation, or application shutdown",
                containment.diagnostics());
        java.io.OutputStream processInput = process.getOutputStream();
        java.io.InputStream processOutput = process.getInputStream();
        commands = new BufferedWriter(new OutputStreamWriter(
                processInput, StandardCharsets.UTF_8));
        Thread.ofPlatform().daemon(true).name("xtts-worker-output").start(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    processOutput, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Files.writeString(log, line + System.lineSeparator(),
                            StandardCharsets.UTF_8,
                            java.nio.file.StandardOpenOption.CREATE,
                            java.nio.file.StandardOpenOption.APPEND);
                    if (line.startsWith(PREFIX)) {
                        events.offer(line.substring(PREFIX.length()));
                    }
                }
            } catch (IOException ignored) {
                // The caller reports the process state and retained log.
            }
        });
        String selectedDevice;
        try {
            String ready = event(cancellation, timeout);
            if (!"ready".equals(OllamaJson.stringProperty(ready, "event"))) {
                throw failure("XTTS no pudo cargar el worker persistente.", ready);
            }
            String reportedDevice = OllamaJson.stringProperty(ready, "device");
            selectedDevice = reportedDevice == null || reportedDevice.isBlank()
                    ? (device == null || device.isBlank() ? "auto" : device.strip())
                    : reportedDevice.strip();
        } catch (IOException | InterruptedException | RuntimeException failure) {
            process.descendants().toList().reversed().forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
            containment.close();
            OwnedProcessDiagnostics.stopped("XTTS", process, "XttsBatchWorker",
                    processStartedAt, "START_FAILURE", process.isAlive() ? null : process.exitValue(), true);
            throw failure;
        }
        effectiveDevice = selectedDevice;
    }

    void synthesize(String segmentId, Path textFile, Path outputFile,
                    Path speakerWav, String language,
                    CancellationToken cancellation, Duration timeout)
            throws IOException, InterruptedException {
        String command = "{\"command\":\"synthesize\",\"segmentId\":\""
                + escape(segmentId) + "\",\"textFile\":\""
                + escape(textFile.toString()) + "\",\"outputFile\":\""
                + escape(outputFile.toString()) + "\",\"speakerWav\":\""
                + escape(speakerWav.toString()) + "\",\"language\":\""
                + escape(language) + "\"}";
        commands.write(command);
        commands.newLine();
        commands.flush();
        String result = event(cancellation, timeout);
        String event = OllamaJson.stringProperty(result, "event");
        if (!"completed".equals(event)
                || !segmentId.equals(OllamaJson.stringProperty(
                result, "segmentId"))) {
            throw failure("XTTS falló al generar el chunk " + segmentId + ".",
                    result);
        }
    }

    private String event(CancellationToken cancellation, Duration timeout)
            throws IOException, InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            cancellation.throwIfCancellationRequested();
            String event = events.poll(250, TimeUnit.MILLISECONDS);
            if (event != null) return event;
            if (!process.isAlive()) {
                throw failure("El worker XTTS terminó antes de responder.", "");
            }
        }
        throw new EngineExecutionException(EngineDiagnosticCode.REQUEST_TIMEOUT,
                "XTTS superó el tiempo máximo del chunk.",
                Map.of("log", log.toString()));
    }

    private EngineExecutionException failure(String message, String event) {
        String workerMessage = OllamaJson.stringProperty(event, "message");
        String effectiveMessage = workerMessage == null || workerMessage.isBlank()
                ? message
                : message + " El motor informó: " + workerMessage.strip();
        return new EngineExecutionException(EngineDiagnosticCode.CHILD_EXIT,
                effectiveMessage, Map.of("event", event == null ? "" : event,
                "log", log.toString()));
    }

    Path logPath() {
        return log;
    }

    String effectiveDevice() {
        return effectiveDevice;
    }

    static void configureUtf8Environment(ProcessBuilder processBuilder) {
        processBuilder.environment().put("PYTHONUTF8", "1");
        processBuilder.environment().put("PYTHONIOENCODING", "utf-8");
    }

    @Override public void close() {
        if (closed) return;
        closed = true;
        try {
            commands.write("{\"command\":\"shutdown\"}");
            commands.newLine();
            commands.flush();
        } catch (IOException ignored) { }
        try {
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.descendants().toList().reversed()
                        .forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            process.descendants().toList().reversed()
                    .forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
        }
        try {
            commands.close();
        } catch (IOException ignored) { }
        containment.close();
        OwnedProcessDiagnostics.stopped("XTTS", process, "XttsBatchWorker",
                processStartedAt, "WORKER_CLOSE",
                process.isAlive() ? null : process.exitValue(), process.isAlive());
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\")
                .replace("\"", "\\\"").replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}
